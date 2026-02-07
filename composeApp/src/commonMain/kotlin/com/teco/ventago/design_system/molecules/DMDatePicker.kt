package com.teco.ventago.design_system.molecules

import androidx.compose.foundation.clickable
import com.teco.ventago.design_system.textfields.DMOutlinedTextField

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.cardContainerColor
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// --- tiny helpers ---
private fun todayLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun LocalDate.toIso(): String =
    "${year.toString().padStart(4, '0')}-${monthNumber.toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}"

private fun parseIsoDateOrNull(value: String?): LocalDate? = try {
    if (value.isNullOrBlank()) null
    else {
        val y = value.substring(0,4).toInt()
        val m = value.substring(5,7).toInt()
        val d = value.substring(8,10).toInt()
        LocalDate(y, m, d)
    }
} catch (_: Throwable) { null }

private fun daysInMonth(year: Int, month: Int): Int {
    val leap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    return when (month) {
        1,3,5,7,8,10,12 -> 31
        4,6,9,11 -> 30
        2 -> if (leap) 29 else 28
        else -> 30
    }
}

// Lightweight dropdown that works everywhere
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntDropDown(
    label: String,
    options: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier) {
        OutlinedTextField(
            readOnly = true,
            value = selected.toString(),
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.toString()) },
                    onClick = { onSelected(opt); expanded = false }
                )
            }
        }
    }
}

/**
 * KMP-safe date field that opens a bottom-sheet date picker (Year/Month/Day).
 * Returns an ISO "YYYY-MM-DD" through onDatePickedIso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentDueDateFieldKmp(
    valueIso: String,                             // current value, "" if none
    onDatePickedIso: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Fecha de vencimiento",
    minSelectableDate: LocalDate? = null,
    maxSelectableDate: LocalDate? = null
) {
    var open by remember { mutableStateOf(false) }

    // Read-only field that opens the sheet
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clickable { open = true }
    ) {
        DMOutlinedTextField(
            text = valueIso,
            label = label,
            onChange = { /* read-only */ },
            modifier = modifier
                .fillMaxWidth(), // placeholder for extra modifiers
            maxLines = 1,
            imeAction = ImeAction.Done,
            readOnly = true,
            trailingIcon = Icons.Rounded.CalendarMonth,
            trailingIconClick = { open = true }
            )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { open = true }
        )
    }


    if (open) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val today = todayLocalDate()
        val lowerBound = minSelectableDate
        val upperBound = maxSelectableDate
        val initial = remember(valueIso, lowerBound, upperBound) {
            val parsed = parseIsoDateOrNull(valueIso) ?: today
            when {
                lowerBound != null && parsed < lowerBound -> lowerBound
                upperBound != null && parsed > upperBound -> upperBound
                else -> parsed
            }
        }

        var year by remember(initial) { mutableStateOf(initial.year) }
        var month by remember(initial) { mutableStateOf(initial.monthNumber) }
        var day by remember(initial) { mutableStateOf(initial.dayOfMonth) }

        val fallbackMinYear = today.year - 10
        val fallbackMaxYear = today.year + 10
        val yearOptions = remember(lowerBound, upperBound, fallbackMinYear, fallbackMaxYear) {
            val startYear = lowerBound?.year ?: fallbackMinYear
            val endYear = upperBound?.year ?: fallbackMaxYear
            (startYear..endYear).toList()
        }

        val monthOptions = remember(year, lowerBound, upperBound) {
            (1..12).filter { monthCandidate ->
                val monthStart = LocalDate(year, monthCandidate, 1)
                val monthEnd = LocalDate(year, monthCandidate, daysInMonth(year, monthCandidate))
                (lowerBound == null || monthEnd >= lowerBound) &&
                    (upperBound == null || monthStart <= upperBound)
            }
        }

        val dayOptions = remember(year, month, lowerBound, upperBound) {
            val totalDays = daysInMonth(year, month)
            (1..totalDays).filter { dayCandidate ->
                val candidate = LocalDate(year, month, dayCandidate)
                (lowerBound == null || candidate >= lowerBound) &&
                    (upperBound == null || candidate <= upperBound)
            }
        }

        LaunchedEffect(yearOptions, year) {
            if (yearOptions.isNotEmpty() && year !in yearOptions) {
                year = yearOptions.first()
            }
        }
        LaunchedEffect(monthOptions, month) {
            if (monthOptions.isNotEmpty() && month !in monthOptions) {
                month = monthOptions.first()
            }
        }
        LaunchedEffect(dayOptions, day) {
            if (dayOptions.isNotEmpty() && day !in dayOptions) {
                day = dayOptions.first()
            }
        }

        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheetState,
            containerColor = cardContainerColor()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text("Seleccionar fecha", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                // Pickers
                IntDropDown(
                    label = "Año",
                    options = yearOptions,
                    selected = year,
                    onSelected = { year = it }
                )
                Spacer(Modifier.height(8.dp))
                IntDropDown(
                    label = "Mes",
                    options = monthOptions,
                    selected = month,
                    onSelected = { month = it }
                )
                Spacer(Modifier.height(8.dp))
                IntDropDown(
                    label = "Día",
                    options = dayOptions,
                    selected = if (dayOptions.isEmpty()) 1 else day.coerceIn(dayOptions.first(), dayOptions.last()),
                    onSelected = { day = it }
                )

                Spacer(Modifier.height(16.dp))

                val pickedIso = remember(year, month, day, dayOptions) {
                    val safeDay = if (dayOptions.isEmpty()) 1 else day.coerceIn(dayOptions.first(), dayOptions.last())
                    LocalDate(year, month, safeDay).toIso()
                }

                // Preview
                Text("Fecha: $pickedIso", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { open = false }) { Text("Cancelar") }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = {
                        onDatePickedIso(pickedIso)
                        open = false
                    }) { Text("Aceptar") }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
