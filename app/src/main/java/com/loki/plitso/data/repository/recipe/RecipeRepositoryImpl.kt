package com.loki.plitso.data.repository.recipe

import com.loki.plitso.data.local.dao.CategoryDao
import com.loki.plitso.data.local.dao.DayRecipeDao
import com.loki.plitso.data.local.dao.RecipeDao
import com.loki.plitso.data.local.dao.RecipeDetailDao
import com.loki.plitso.data.local.models.Category
import com.loki.plitso.data.local.models.DayRecipe
import com.loki.plitso.data.local.models.Recipe
import com.loki.plitso.data.local.models.RecipeDetail
import com.loki.plitso.data.remote.mealdb.MealdbApi
import com.loki.plitso.data.remote.mealdb.mappers.toCategory
import com.loki.plitso.data.remote.mealdb.mappers.toDayRecipe
import com.loki.plitso.data.remote.mealdb.mappers.toRecipe
import com.loki.plitso.data.remote.mealdb.mappers.toRecipeDetail
import com.loki.plitso.data.remote.mealdb.models.RecipeDetailDto
import com.loki.plitso.util.Resource
import com.loki.plitso.util.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date

class RecipeRepositoryImpl(
    private val api: MealdbApi,
    private val categoryDao: CategoryDao,
    val recipeDao: RecipeDao,
    val recipeDetailDao: RecipeDetailDao,
    private val dayRecipeDao: DayRecipeDao
) : RecipeRepository {

    override fun getDayRecipe(): Flow<DayRecipe> = flow {
        try {
            val recipe = dayRecipeDao.getRecipes().first().isEmpty()
            if (recipe) {
                val randomRecipe = api.getRandomRecipe().meals[0]
                dayRecipeDao.insert(randomRecipe.toDayRecipe(Date()))
                val dayRecipe = dayRecipeDao.getRecipes().first()[0]
                emit(dayRecipe)
            } else {
                val dayRecipe = dayRecipeDao.getRecipes().first()[0]
                emit(dayRecipe)
            }
        } catch (e: Exception) {
            Timber.tag("day recipe repo").d(e)
        }
    }

    override fun getRandomRecipe(): Flow<Resource<RecipeDetailDto>> =
        safeApiCall(
            apiCall = { api.getRandomRecipe().meals[0] }
        )

    override val categories: Flow<List<Category>>
        get() = categoryDao.getCategories()

    override fun getRecipeDetail(id: String): Flow<RecipeDetail> {
        return recipeDetailDao.getRecipeDetail(id)
    }

    override suspend fun getRecipes(categoryId: String): List<Recipe> {
        return categoryDao.getCategoriesWithRecipes(categoryId).recipes
    }

    override suspend fun refreshDatabase() {
        withContext(Dispatchers.IO) {
            try {
                val categories = api.getCategories().categories
                categoryDao.insert(categories.map { it.toCategory() })

                for (category in categories) {
                    val title = category.strCategory
                    val recipes = api.getCategoryRecipe(title).meals

                    recipeDao.insert(recipes.map { it.toRecipe(category.idCategory) })

                    for (recipe in recipes) {
                        val recipeDetail = api.getRecipeDetail(recipe.idMeal).meals
                        recipeDetailDao.insert(recipeDetail.map { it.toRecipeDetail() })
                    }
                }
            } catch (e: Exception) {
                Timber.tag("API ERR").d(e)
            }
        }
    }
}