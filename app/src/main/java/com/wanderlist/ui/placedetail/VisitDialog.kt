package com.wanderlist.ui.placedetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wanderlist.R
import com.wanderlist.domain.model.VisitInputError
import com.wanderlist.domain.usecase.MarkPlaceVisitedUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDialog(
    state: VisitDialogState,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, rating: Int, memo: String, photoUri: String?) -> Unit,
) {
    var dateEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var rating by rememberSaveable { mutableIntStateOf(0) }
    var memo by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(dateEpochDay)

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    if (showDatePicker) {
        val today = LocalDate.now()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        dateEpochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(android.R.string.cancel)) } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.visit_dialog_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    Text(
                        stringResource(R.string.visit_date, date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))),
                        Modifier.weight(1f).padding(start = 8.dp),
                    )
                }
                state.errors.ErrorText(VisitInputError.DATE_IN_FUTURE)

                Text(stringResource(R.string.visit_rating), style = MaterialTheme.typography.labelLarge)
                RatingBar(rating = rating, onRatingChange = { rating = it })
                state.errors.ErrorText(VisitInputError.RATING_OUT_OF_RANGE)

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text(stringResource(R.string.visit_memo)) },
                    isError = VisitInputError.MEMO_TOO_LONG in state.errors,
                    supportingText = { Text("${memo.length}/${MarkPlaceVisitedUseCase.MAX_MEMO_LENGTH}") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                    Text(
                        stringResource(if (photoUri == null) R.string.visit_add_photo else R.string.visit_photo_selected),
                        Modifier.weight(1f).padding(start = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(date, rating, memo, photoUri) },
                enabled = !state.isSaving,
                modifier = Modifier.testTag(PlaceDetailTags.SAVE_VISIT),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
fun RatingBar(rating: Int, onRatingChange: ((Int) -> Unit)?, modifier: Modifier = Modifier, starSize: Int = 36) {
    Row(modifier.selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
        (1..MarkPlaceVisitedUseCase.MAX_RATING).forEach { value ->
            val description = pluralStringResource(R.plurals.visit_stars, value, value)
            // Interaktywna gwiazdka ma pole dotyku min. 48 dp (wytyczne dostępności Material 3).
            val starModifier = if (onRatingChange != null) {
                Modifier
                    .minimumInteractiveComponentSize()
                    .selectable(selected = rating == value, role = Role.RadioButton, onClick = { onRatingChange(value) })
            } else {
                Modifier
            }
            Icon(
                imageVector = if (value <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (onRatingChange != null) description else null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = starModifier
                    .size(starSize.dp)
                    .testTag("${PlaceDetailTags.STAR_PREFIX}$value"),
            )
        }
    }
}

@Composable
private fun List<VisitInputError>.ErrorText(error: VisitInputError) {
    if (error in this) {
        Text(stringResource(error.messageRes()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

private fun VisitInputError.messageRes() = when (this) {
    VisitInputError.RATING_OUT_OF_RANGE -> R.string.error_rating
    VisitInputError.DATE_IN_FUTURE -> R.string.error_visit_date
    VisitInputError.MEMO_TOO_LONG -> R.string.error_memo_too_long
}
