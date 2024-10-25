package com.loki.plitso.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.content
import com.loki.plitso.data.local.dao.AiAnswerDao
import com.loki.plitso.data.local.dao.ChatHistoryDao
import com.loki.plitso.data.local.models.AiAnswer
import com.loki.plitso.data.local.models.ChatHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AiViewModel(
    private val aiAnswerDao: AiAnswerDao,
    private val chatHistoryDao: ChatHistoryDao,
    private val generativeModel: GenerativeModel,
    val aiData: AiData,
) : ViewModel() {
    val chatHistory =
        chatHistoryDao.getAllChats().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            emptyList(),
        )

    private val _genState = MutableStateFlow(GenerativeState())
    val genState = _genState.asStateFlow()

    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Success())
    val chatState = _chatState.asStateFlow()

    private val _newMessageId = MutableStateFlow<String?>(null)
    val newMessageId = _newMessageId.asStateFlow()

    private val _parameters = MutableStateFlow(GenerativeParameters())
    val parameters = _parameters.asStateFlow()

    private val currentChat = MutableStateFlow<ChatHistory?>(null)

    init {
        viewModelScope.launch {
            // Combine currentChat with its messages
            combine(
                currentChat,
                currentChat.flatMapLatest { chat ->
                    chat?.let {
                        aiAnswerDao.getAnswersByChat(it.id)
                    } ?: flowOf(emptyList())
                },
            ) { chat, messages ->
                if (chat == null) {
                    ChatUiState.Success()
                } else {
                    ChatUiState.Success(
                        messages = messages,
                        title = chat.title,
                    )
                }
            }.catch { e ->
                ChatUiState.Error(e.message ?: "Unknown error occurred")
            }.collect { state ->
                _chatState.value = state
            }
        }
    }

    fun resetChat() {
        currentChat.value = null
    }

    fun resetMessageId() {
        _newMessageId.value = null
    }

    fun onStartNewChat() {
        val newChat =
            ChatHistory(
                id = UUID.randomUUID(),
                title = "",
                startedOn = Date(),
            )
        currentChat.value = newChat
    }

    fun setCurrentChat(chatId: UUID) {
        viewModelScope.launch {
            chatHistoryDao.getChat(chatId).firstOrNull()?.let {
                currentChat.value = it
            }
        }
    }

    fun askQuestion(question: String) {
        viewModelScope.launch {
            val chat = currentChat.value ?: return@launch

            try {
                _chatState.update {
                    (it as? ChatUiState.Success)?.copy(isProcessing = true) ?: it
                }

                // Save chat if new
                if (chatHistoryDao.getChat(chat.id).firstOrNull() == null) {
                    chatHistoryDao.insert(chat)
                }

                // Add user message
                val userMessage =
                    AiAnswer(
                        role = "user",
                        content = question,
                        chatId = chat.id,
                    )
                aiAnswerDao.insert(userMessage)

                // Generate title for new chats
                if (chat.title.isEmpty()) {
                    generateAndUpdateTitle(question, chat)
                }

                // Get AI response
                getAiResponse(question)?.let { response ->
                    response.text?.let { modelResponse ->
                        val modelMessage =
                            AiAnswer(
                                content = modelResponse,
                                role = "model",
                                chatId = chat.id,
                            )
                        val id = aiAnswerDao.insert(modelMessage)
                        _newMessageId.value = id.toString()
                    }
                }

                // Clear any existing error
                _chatState.update {
                    (it as? ChatUiState.Success)?.copy(
                        isProcessing = false,
                        error = null,
                    ) ?: it
                }
            } catch (e: Exception) {
                _chatState.update {
                    (it as? ChatUiState.Success)?.copy(
                        isProcessing = false,
                        error = e.localizedMessage ?: "Something went wrong!",
                    ) ?: it
                }
            }
        }
    }

    private suspend fun generateAndUpdateTitle(
        question: String,
        chat: ChatHistory,
    ) {
        val titleResponse =
            generativeModel.generateContent(
                content {
                    text(PromptUtil.generateTitle(question))
                },
            )
        titleResponse.text?.let { modelResponse ->
            val updatedChat = chat.copy(title = modelResponse)
            chatHistoryDao.update(updatedChat)
            currentChat.value = updatedChat
        }
    }

    private suspend fun getAiResponse(question: String): GenerateContentResponse? {
        return when (val currentState = _chatState.value) {
            is ChatUiState.Success -> {
                val chatSession =
                    generativeModel.startChat(
                        history =
                            currentState.messages.map { value ->
                                content(role = value.role) { text(value.content) }
                            },
                    )
                chatSession.sendMessage(question)
            }

            else -> {
                _chatState.update {
                    (it as? ChatUiState.Success)?.copy(
                        isProcessing = false,
                        error = "Unable to process message",
                    ) ?: it
                }
                null
            }
        }
    }

    fun deleteChat(id: UUID) {
        viewModelScope.launch {
            chatHistoryDao.deleteChat(id)
            if (currentChat.value?.id == id) {
                currentChat.value = null
            }
        }
    }

    fun onMealTypeChange(newValue: String) {
        _parameters.value =
            _parameters.value.copy(
                mealType = newValue,
            )
    }

    fun onCuisineChange(newValue: String) {
        _parameters.value =
            _parameters.value.copy(
                cuisine = newValue,
            )
    }

    fun onMoodChange(newValue: String) {
        _parameters.value =
            _parameters.value.copy(
                mood = newValue,
            )
    }

    fun onDietaryChange(newValue: String) {
        _parameters.value =
            _parameters.value.copy(
                dietary = newValue,
            )
    }

    fun isQuickMealChange(newValue: Boolean) {
        _parameters.value =
            _parameters.value.copy(
                isQuick = newValue,
            )
    }

    fun generateSuggestions(onSuccess: () -> Unit) {
        if (
            _parameters.value.mealType.isEmpty() ||
            _parameters.value.mood.isEmpty() ||
            _parameters.value.cuisine.isEmpty()
        ) {
            _genState.value =
                _genState.value.copy(
                    errorMessage = "Please select required",
                )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _genState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = "",
                )
            }

            try {
                val response =
                    generativeModel.generateContent(
                        content {
                            text(
                                PromptUtil.generativePrompt(
                                    recipeData = aiData.recipes,
                                    pastMeal = aiData.pastMeals,
                                    parameters = _parameters.value,
                                ),
                            )
                        },
                    )

                response.text?.let { modelResponse ->
                    _genState.update {
                        it.copy(
                            isLoading = false,
                            generativeAnswer = modelResponse,
                        )
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                _genState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Something went wrong!",
                    )
                }
            }
        }
    }
}
