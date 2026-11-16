package dev.juanpe.habittracker.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Estado del permiso de notificaciones visto desde una pantalla.
 *
 * @property granted true si la app puede mostrar notificaciones ahora mismo.
 * @property request pide el permiso. Si el usuario ya lo rechazó, Android no
 *   vuelve a mostrar el diálogo, así que en ese caso abre los ajustes del sistema.
 */
class NotificationPermissionState(val granted: Boolean, val request: () -> Unit)

/**
 * Gestiona el permiso POST_NOTIFICATIONS (obligatorio desde Android 13).
 * En versiones anteriores no hay que pedirlo, pero el usuario puede haber
 * desactivado las notificaciones a mano, así que se comprueba igualmente.
 */
@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(notificationsEnabled(context)) }
    var deniedBefore by rememberSaveable { mutableStateOf(false) }

    // Al volver a la app (por ejemplo desde Ajustes) se vuelve a comprobar.
    LifecycleResumeEffect(Unit) {
        granted = notificationsEnabled(context)
        onPauseOrDispose { }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { accepted ->
        granted = notificationsEnabled(context)
        if (!accepted) deniedBefore = true
    }

    return NotificationPermissionState(
        granted = granted,
        request = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !deniedBefore) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings(context)
            }
        },
    )
}

private fun notificationsEnabled(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
