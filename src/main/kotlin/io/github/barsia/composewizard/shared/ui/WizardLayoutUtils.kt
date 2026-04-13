package io.github.barsia.composewizard.shared.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp

internal val SPACING_BETWEEN_SECTIONS = 8.dp
internal val SPACING_BEFORE_LOCATION = 16.dp
internal val LOCATION_SECTION_VERTICAL_OFFSET = 4.dp
internal val TEXTFIELD_VERTICAL_OFFSET = 8.dp
internal val TEXTFIELD_HEIGHT_REDUCTION = 16.dp
internal val LIBRARIES_SECTION_SPACING = 4.dp
internal val LIBRARY_ITEM_SPACING = 4.dp

internal const val LEFT_COLUMN_LIBRARIES_COUNT = 4
internal const val RIGHT_COLUMN_BASE_LIBRARIES_COUNT = 4

internal fun Modifier.compactVerticalSpacing(): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val verticalOffset = TEXTFIELD_VERTICAL_OFFSET.roundToPx()
    val heightReduction = TEXTFIELD_HEIGHT_REDUCTION.roundToPx()
    
    layout(placeable.width, placeable.height - heightReduction) {
        placeable.place(0, -verticalOffset)
    }
}

