package com.example.dslrcam.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dslrcam.ui.theme.Accent
import com.example.dslrcam.ui.theme.SurfaceDark
import com.example.dslrcam.ui.theme.TextPrimary
import com.example.dslrcam.ui.theme.TextSecondary

@Composable
fun ManualPanel(
    iso: Int?,
    shutterNs: Long?,
    whiteBalanceK: Int?,
    focusDistance: Float?,
    onIsoChange: (Int?) -> Unit,
    onShutterChange: (Long?) -> Unit,
    onWhiteBalanceChange: (Int?) -> Unit,
    onFocusChange: (Float?) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                SurfaceDark.copy(alpha = 0.95f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mode Manual",
                color = TextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onReset) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Reset",
                    tint = Accent
                )
            }
        }

        SliderRow(
            label = "ISO",
            valueText = iso?.toString() ?: "Auto",
            value = (iso ?: 100).toFloat(),
            range = 100f..3200f,
            steps = 30,
            onChange = { onIsoChange(it.toInt()) },
            onAuto = { onIsoChange(null) }
        )

        SliderRow(
            label = "Rana",
            valueText = shutterNs?.let { formatShutter(it) } ?: "Auto",
            value = (shutterNs ?: 8_000_000L).toFloat(),
            range = 100_000f..1_000_000_000f,
            steps = 0,
            onChange = { onShutterChange(it.toLong()) },
            onAuto = { onShutterChange(null) }
        )

        SliderRow(
            label = "WB",
            valueText = whiteBalanceK?.let { "${it}K" } ?: "Auto",
            value = (whiteBalanceK ?: 5500).toFloat(),
            range = 2000f..8000f,
            steps = 60,
            onChange = { onWhiteBalanceChange(it.toInt()) },
            onAuto = { onWhiteBalanceChange(null) }
        )

        SliderRow(
            label = "Fokus",
            valueText = focusDistance?.let { "%.2f".format(it) } ?: "Auto",
            value = (focusDistance ?: 0.5f) * 100f,
            range = 0f..100f,
            steps = 50,
            onChange = { onFocusChange(it / 100f) },
            onAuto = { onFocusChange(null) }
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
    onAuto: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.width(60.dp)
            )
            Text(
                text = valueText,
                color = Accent,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Auto",
                color = Accent,
                fontSize = 11.sp,
                modifier = Modifier
                    .background(Color(0x33FFC107), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAuto
                    )
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps
        )
    }
}

private fun formatShutter(ns: Long): String {
    if (ns >= 1_000_000_000L) {
        return "%.1fs".format(ns / 1_000_000_000.0)
    }
    val denom = (1_000_000_000L / ns).toInt()
    return "1/$denom"
}
