package com.wanderlist.ui.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.wanderlist.domain.model.Place

/** Adapter miejsca dla klastrowania pinezek (maps-compose-utils). */
class PlaceClusterItem(val place: Place) : ClusterItem {
    override fun getPosition(): LatLng = LatLng(place.location.latitude, place.location.longitude)
    override fun getTitle(): String = place.name
    override fun getSnippet(): String? = place.address
    override fun getZIndex(): Float = 0f
}
