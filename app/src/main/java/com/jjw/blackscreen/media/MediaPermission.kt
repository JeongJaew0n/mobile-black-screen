package com.jjw.blackscreen.media

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/** 알림 접근 권한 — 재생 정보를 얻는 데 필요한 유일한 권한. */
object MediaPermission {
    fun component(context: Context) = ComponentName(context, MediaListenerService::class.java)

    fun isGranted(context: Context): Boolean {
        val cn = component(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return context.getSystemService(NotificationManager::class.java)
                .isNotificationListenerAccessGranted(cn)
        }
        // API 26: 허용된 리스너 목록 문자열을 직접 본다.
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(':').any { ComponentName.unflattenFromString(it) == cn }
    }

    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
