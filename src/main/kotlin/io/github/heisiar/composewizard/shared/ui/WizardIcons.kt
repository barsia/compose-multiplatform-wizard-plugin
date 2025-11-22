package io.github.heisiar.composewizard.shared.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FolderIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Folder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(20f, 6f)
            horizontalLineTo(12f)
            lineTo(10f, 4f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
            lineTo(2f, 18f)
            curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
            horizontalLineTo(20f)
            curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
            verticalLineTo(8f)
            curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
            close()
        }
    }.build()

val FolderOutlineIcon: ImageVector
    get() = ImageVector.Builder(
        name = "FolderOutline",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            fill = null
        ) {
            moveTo(20f, 6f)
            horizontalLineTo(12f)
            lineTo(10f, 4f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
            lineTo(2f, 18f)
            curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
            horizontalLineTo(20f)
            curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
            verticalLineTo(8f)
            curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
            close()
        }
    }.build()

val BugIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Bug",
        defaultWidth = 16.dp,
        defaultHeight = 16.dp,
        viewportWidth = 16f,
        viewportHeight = 16f
    ).apply {
        path(
            fill = SolidColor(Color.White),
            fillAlpha = 1f,
            pathFillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
        ) {
            moveTo(10.877f, 4.5f)
            verticalLineToRelative(-0.582f)
            arcToRelative(2.918f, 2.918f, 0f, true, false, -5.836f, 0f)
            verticalLineTo(4.5f)
            horizontalLineToRelative(-0.833f)
            lineTo(2.545f, 2.829f)
            lineToRelative(-0.593f, 0.59f)
            lineToRelative(1.611f, 1.619f)
            lineToRelative(-0.019f, 0.049f)
            arcToRelative(8.03f, 8.03f, 0f, false, false, -0.503f, 2.831f)
            curveToRelative(0f, 0.196f, 0.007f, 0.39f, 0.02f, 0.58f)
            lineToRelative(0.003f, 0.045f)
            horizontalLineTo(1f)
            verticalLineToRelative(0.836f)
            horizontalLineToRelative(2.169f)
            lineToRelative(0.006f, 0.034f)
            curveToRelative(0.172f, 0.941f, 0.504f, 1.802f, 0.954f, 2.531f)
            lineToRelative(0.034f, 0.055f)
            lineTo(2.2f, 13.962f)
            lineToRelative(0.592f, 0.592f)
            lineToRelative(1.871f, -1.872f)
            lineToRelative(0.058f, 0.066f)
            curveToRelative(0.868f, 0.992f, 2.002f, 1.589f, 3.238f, 1.589f)
            curveToRelative(1.218f, 0f, 2.336f, -0.579f, 3.199f, -1.544f)
            lineToRelative(0.057f, -0.064f)
            lineToRelative(1.91f, 1.92f)
            lineToRelative(0.593f, -0.591f)
            lineToRelative(-1.996f, -2.006f)
            lineToRelative(0.035f, -0.056f)
            curveToRelative(0.467f, -0.74f, 0.81f, -1.619f, 0.986f, -2.583f)
            lineToRelative(0.006f, -0.034f)
            horizontalLineToRelative(2.171f)
            verticalLineToRelative(-0.836f)
            horizontalLineToRelative(-2.065f)
            lineToRelative(0.003f, -0.044f)
            arcToRelative(8.43f, 8.43f, 0f, false, false, 0.02f, -0.58f)
            arcToRelative(8.02f, 8.02f, 0f, false, false, -0.517f, -2.866f)
            lineToRelative(-0.019f, -0.05f)
            lineToRelative(1.57f, -1.57f)
            lineToRelative(-0.592f, -0.59f)
            lineTo(11.662f, 4.5f)
            horizontalLineToRelative(-0.785f)
            close()
            moveTo(5.877f, 4.5f)
            verticalLineToRelative(-0.582f)
            arcToRelative(2.082f, 2.082f, 0f, true, true, 4.164f, 0f)
            verticalLineTo(4.5f)
            horizontalLineTo(5.878f)
            close()
            moveTo(11.574f, 5.337f)
            lineToRelative(0.02f, 0.053f)
            curveToRelative(0.283f, 0.753f, 0.447f, 1.61f, 0.447f, 2.528f)
            curveToRelative(0f, 1.61f, -0.503f, 3.034f, -1.274f, 4.037f)
            curveToRelative(-0.77f, 1.001f, -1.771f, 1.545f, -2.808f, 1.545f)
            curveToRelative(-1.036f, 0f, -2.037f, -0.544f, -2.807f, -1.545f)
            curveToRelative(-0.772f, -1.003f, -1.275f, -2.427f, -1.275f, -4.037f)
            curveToRelative(0f, -0.918f, 0.164f, -1.775f, 0.448f, -2.528f)
            lineToRelative(0.02f, -0.053f)
            horizontalLineToRelative(7.229f)
            close()
        }
    }.build()
