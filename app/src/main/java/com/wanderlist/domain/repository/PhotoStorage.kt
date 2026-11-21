package com.wanderlist.domain.repository

interface PhotoStorage {
    /** Kopiuje zdjęcie wybrane przez użytkownika do prywatnego katalogu aplikacji; zwraca URI kopii. */
    suspend fun importPhoto(sourceUri: String): Result<String>

    suspend fun deletePhoto(uri: String)
}
