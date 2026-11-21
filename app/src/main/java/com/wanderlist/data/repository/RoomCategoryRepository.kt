package com.wanderlist.data.repository

import com.wanderlist.data.common.runCatchingApp
import com.wanderlist.data.local.dao.CategoryDao
import com.wanderlist.data.local.mapper.toDomain
import com.wanderlist.data.local.mapper.toEntity
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.Category
import com.wanderlist.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomCategoryRepository @Inject constructor(
    private val dao: CategoryDao,
) : CategoryRepository {
    override fun observeCategories(): Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addCategory(category: Category): Result<Long> = runCatchingApp {
        dao.insert(category.copy(id = 0).toEntity())
    }

    override suspend fun updateCategory(category: Category): Result<Unit> = runCatchingApp {
        if (dao.update(category.toEntity()) == 0) throw AppError.NotFound()
    }

    override suspend fun deleteCategory(id: Long): Result<Unit> = runCatchingApp {
        if (dao.deleteById(id) == 0) throw AppError.NotFound()
    }
}
