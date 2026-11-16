package dev.juanpe.habittracker.ui.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.data.repository.ProgressRepository
import dev.juanpe.habittracker.ui.components.AnimatedBar
import dev.juanpe.habittracker.ui.components.targetText
import dev.juanpe.habittracker.ui.theme.color

/**
 * Panel inferior para registrar una cantidad exacta en un hábito con objetivo
 * (por ejemplo, 30 de los 45 minutos). Se abre con una pulsación larga en el check.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountSheet(item: TodayHabit, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val habit = item.habit
    val target = habit.target ?: return
    val color = habit.area.color
    // Con objetivos grandes (minutos) se avanza de 5 en 5.
    val step = if (target.amount > 20) 5 else 1
    var amount by remember(habit.id) { mutableIntStateOf(item.amount) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(habit.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.amount_sheet_target, targetText(target)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = { amount = (amount - step).coerceAtLeast(0) },
                    enabled = amount > 0,
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.action_decrease))
                }
                Text(
                    text = amount.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.width(132.dp),
                    textAlign = TextAlign.Center,
                )
                FilledTonalIconButton(
                    onClick = { amount = (amount + step).coerceAtMost(ProgressRepository.MAX_AMOUNT) },
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_increase))
                }
            }
            Spacer(Modifier.height(16.dp))
            AnimatedBar(fraction = amount.toFloat() / target.amount, color = color)
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { amount = 0 }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.amount_sheet_reset))
                }
                OutlinedButton(onClick = { amount = target.amount }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.amount_sheet_fill))
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onSave(amount) },
                colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

/**
 * El "+12 XP" que sale flotando de la fila al cumplir un hábito:
 * sube, se desvanece y desaparece en un segundo.
 */
@Composable
fun XpPopLabel(pop: XpPop?, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    var shown by remember { mutableStateOf<XpPop?>(null) }

    LaunchedEffect(pop?.id) {
        if (pop != null) {
            shown = pop
            progress.snapTo(0f)
            progress.animateTo(1f, tween(TodayViewModel.XP_POP_MILLIS.toInt() - 100, easing = LinearOutSlowInEasing))
            shown = null
        }
    }

    val current = shown ?: return
    Text(
        text = stringResource(R.string.xp_gain, current.amount),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.tertiary,
        // graphicsLayer con lambda: mover y desvanecer no provoca recomposiciones.
        modifier = modifier.graphicsLayer {
            val t = progress.value
            translationY = -30.dp.toPx() * t + 6.dp.toPx()
            alpha = (1f - t * t).coerceIn(0f, 1f)
            val pop = 0.7f + 0.3f * (t * 5f).coerceAtMost(1f)
            scaleX = pop
            scaleY = pop
        },
    )
}
