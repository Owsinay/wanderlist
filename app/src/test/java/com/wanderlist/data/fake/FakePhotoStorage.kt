package com.wanderlist.data.fake

import com.wanderlist.domain.repository.PhotoStorage

class FakePhotoStorage(var importResult: (String) -> Result<String> = { Result.success("file:///photos/copy.jpg") }) : PhotoStorage {
    val deleted = mutableListOf<String>()

    override suspend fun importPhoto(sourceUri: String): Result<String> = importResult(sourceUri)

    override suspend fun deletePhoto(uri: String) {
        deleted += uri
    }
}
