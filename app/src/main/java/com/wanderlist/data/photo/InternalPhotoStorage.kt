package com.wanderlist.data.photo

import android.content.Context
import androidx.core.net.toUri
import com.wanderlist.data.common.runCatchingApp
import com.wanderlist.di.IoDispatcher
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.repository.PhotoStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Zdjęcia odwiedzin trzymane w pamięci wewnętrznej aplikacji (niedostępnej dla innych aplikacji),
 * więc nie znikają po wygaśnięciu uprawnienia do pliku z galerii.
 */
class InternalPhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PhotoStorage {
    private val directory: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    override suspend fun importPhoto(sourceUri: String): Result<String> = withContext(ioDispatcher) {
        runCatchingApp {
            val target = File(directory, "${UUID.randomUUID()}.jpg")
            val input = context.contentResolver.openInputStream(sourceUri.toUri()) ?: throw AppError.NotFound()
            input.use { source -> target.outputStream().use { source.copyTo(it) } }
            target.toUri().toString()
        }
    }

    override suspend fun deletePhoto(uri: String) {
        withContext(ioDispatcher) {
            val path = uri.toUri().path ?: return@withContext
            val file = File(path)
            // Usuwamy wyłącznie pliki z własnego katalogu.
            if (file.parentFile?.canonicalPath == directory.canonicalPath) file.delete()
        }
    }
}
