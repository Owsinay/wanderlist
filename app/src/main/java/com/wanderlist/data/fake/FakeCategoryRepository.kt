package com.wanderlist.data.fake

import com.wanderlist.data.local.AppDatabase
import com.wanderlist.domain.model.Category
import com.wanderlist.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Tymczasowa implementacja w pamięci; zastąpiona przez repozytorium Room w zadaniu A1. */
@Singleton
class FakeCategoryRepository @Inject constructor() : CategoryRepository {
    private val categories = MutableStateFlow(
        AppDatabase.DEFAULT_CATEGORIES.mapIndexed { index, entity ->
            Category(id = index + 1L, name = entity.name, iconKey = entity.iconKey, color = entity.color)
        },
    )

    override fun observeCategories(): Flow<List<Category>> = categories

    override suspend fun addCategory(category: Category): Result<Long> {
        val id = (categories.value.maxOfOrNull { it.id } ?: 0) + 1
        categories.update { it + category.copy(id = id) }
        return Result.success(id)
    }

    override suspend fun updateCategory(category: Category): Result<Unit> {
        categories.update { list -> list.map { if (it.id == category.id) category else it } }
        return Result.success(Unit)
    }

    override suspend fun deleteCategory(id: Long): Result<Unit> {
        categories.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }
}
