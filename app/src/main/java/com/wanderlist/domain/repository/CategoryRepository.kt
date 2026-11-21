package com.wanderlist.domain.repository

import com.wanderlist.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun addCategory(category: Category): Result<Long>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(id: Long): Result<Unit>
}
