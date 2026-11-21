package com.wanderlist.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    /** Klucz ikony mapowany w UI na ImageVector, np. "museum", "nature". */
    val iconKey: String,
    /** Kolor ARGB, np. 0xFF4CAF50. */
    val color: Long,
)
