package com.wanderlist.data.local.mapper

import com.wanderlist.data.local.entity.CategoryEntity
import com.wanderlist.data.local.entity.PlaceEntity
import com.wanderlist.data.local.entity.VisitEntity
import com.wanderlist.domain.model.Category
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.Visit

fun PlaceEntity.toDomain() = Place(
    id = id,
    name = name,
    location = GeoPoint(latitude, longitude),
    address = address,
    categoryId = categoryId,
    status = status,
    priority = priority,
    note = note,
    createdAt = createdAt,
)

fun Place.toEntity() = PlaceEntity(
    id = id,
    name = name,
    latitude = location.latitude,
    longitude = location.longitude,
    address = address,
    categoryId = categoryId,
    status = status,
    priority = priority,
    note = note,
    createdAt = createdAt,
)

fun VisitEntity.toDomain() = Visit(id = id, placeId = placeId, date = date, rating = rating, memo = memo, photoUri = photoUri)

fun Visit.toEntity() = VisitEntity(id = id, placeId = placeId, date = date, rating = rating, memo = memo, photoUri = photoUri)

fun CategoryEntity.toDomain() = Category(id = id, name = name, iconKey = iconKey, color = color)

fun Category.toEntity() = CategoryEntity(id = id, name = name, iconKey = iconKey, color = color)
