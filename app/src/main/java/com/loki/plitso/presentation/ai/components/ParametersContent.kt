package com.loki.plitso.presentation.ai.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loki.plitso.R
import com.loki.plitso.presentation.ai.generative.GenerativeViewModel
import com.loki.plitso.presentation.components.TypeWriterTextEffect
import com.loki.plitso.presentation.document.MealType

@Composable
fun ParametersContent(
    modifier: Modifier = Modifier,
    generativeViewModel: GenerativeViewModel,
    onSuggestClick: () -> Unit,
) {
    val parameters by generativeViewModel.parameters.collectAsStateWithLifecycle()
    val uiState by generativeViewModel.genState.collectAsStateWithLifecycle()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            TypeWriterTextEffect(
                text = stringResource(R.string.tune_your_meal),
                maxDelayInMillis = 200,
                onEffectComplete = { },
            ) { text ->
                Text(
                    text = text,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            MealTypeSection(
                selectedType = parameters.mealType,
                onChangeSelected = generativeViewModel::onMealTypeChange,
            )

            CuisineSection(
                countries = generativeViewModel.aiData.countries.distinct(),
                selectedCuisine = parameters.cuisine,
                onChangeSelected = generativeViewModel::onCuisineChange,
            )

            MoodSection(
                selectedMood = parameters.mood,
                onChangeSelected = generativeViewModel::onMoodChange,
            )

            DietarySection(
                selectedDietary = parameters.dietary,
                onChangeSelected = generativeViewModel::onDietaryChange,
            )

            QuickSection(
                selectedQuick = parameters.isQuick,
                onChangeSelected = generativeViewModel::isQuickMealChange,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSuggestClick,
                modifier = Modifier.align(Alignment.End),
                enabled =
                    !uiState.isLoading && (
                        parameters.mealType.isNotEmpty() &&
                            parameters.cuisine.isNotEmpty() && parameters.mood.isNotEmpty()
                    ),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(text = stringResource(R.string.suggest_a_meal), color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealTypeSection(
    modifier: Modifier = Modifier,
    selectedType: String,
    onChangeSelected: (String) -> Unit,
) {
    Column {
        Text(text = stringResource(R.string.select_meal))
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MealType.entries.forEach {
                val selected = it.name == selectedType
                Selectable(
                    label = it.name,
                    selected = selected,
                    onSelected = onChangeSelected,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CuisineSection(
    modifier: Modifier = Modifier,
    countries: List<String>,
    selectedCuisine: String,
    onChangeSelected: (String) -> Unit,
) {
    Column {
        Text(text = stringResource(R.string.select_cuisine))
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            countries.forEach {
                val selected = it == selectedCuisine
                Selectable(
                    label = it,
                    selected = selected,
                    onSelected = onChangeSelected,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodSection(
    modifier: Modifier = Modifier,
    selectedMood: String,
    onChangeSelected: (String) -> Unit,
) {
    val moods = listOf("Savory", "Sweet", "Spicy", "Healthy", "Comfort")
    Column {
        Text(text = stringResource(R.string.select_mood))
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            moods.forEach {
                val selected = it == selectedMood
                Selectable(
                    label = it,
                    selected = selected,
                    onSelected = onChangeSelected,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DietarySection(
    modifier: Modifier = Modifier,
    selectedDietary: String,
    onChangeSelected: (String) -> Unit,
) {
    val dietaries = listOf("Vegeterian", "Vegan", "Glutten-free", "Dairy-free")
    Column {
        Text(text = stringResource(R.string.select_dietary))
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            dietaries.forEach {
                val selected = it == selectedDietary
                Selectable(
                    label = it,
                    selected = selected,
                    onSelected = onChangeSelected,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSection(
    modifier: Modifier = Modifier,
    selectedQuick: Boolean,
    onChangeSelected: (Boolean) -> Unit,
) {
    val quicks = listOf("Yes", "No")
    Column {
        Text(text = stringResource(R.string.want_a_quick_meal))
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quicks.forEach { quick ->
                val selected =
                    quick ==
                        if (selectedQuick) {
                            stringResource(R.string.yes)
                        } else {
                            stringResource(
                                R.string.no,
                            )
                        }
                Selectable(
                    label = quick,
                    selected = selected,
                    onSelected = {
                        onChangeSelected(
                            it == "Yes",
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun Selectable(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onSelected: (text: String) -> Unit,
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = { onSelected(label) },
        label = {
            Text(text = label)
        },
    )
}
