package com.wanderlist.ui.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.wanderlist.R
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.ui.theme.PinVisited
import com.wanderlist.ui.theme.PinWantToVisit

/** Filtr miejsc po statusie, wspólny dla mapy i listy. */
enum class StatusFilter(@StringRes val labelRes: Int, val color: Color?) {
    ALL(R.string.filter_all, null),
    WANT_TO_VISIT(R.string.status_want_to_visit, PinWantToVisit),
    VISITED(R.string.status_visited, PinVisited),
    ;

    fun matches(place: Place): Boolean = when (this) {
        ALL -> true
        WANT_TO_VISIT -> place.status == PlaceStatus.WANT_TO_VISIT
        VISITED -> place.status == PlaceStatus.VISITED
    }
}
