package com.jjw.blackscreen

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.core.net.toUri

/**
 * 이 앱이 **"제한된 설정"** 에 걸렸을 가능성.
 *
 * Android 13+ 는 스토어가 아닌 곳(브라우저·파일 관리자)에서 설치한 앱의 접근성·알림 접근 토글을 막는다.
 * 푸는 순서가 직관과 반대라 안내가 필요하다 — 토글을 한 번 눌러 경고를 봐야 앱 정보 ⋮ 메뉴에
 * '제한된 설정 허용' 이 나타난다. Android 15 는 이를 더 조였다(Enhanced Confirmation Mode).
 *
 * "제한됐는지" 를 직접 묻는 공개 API 는 없어서 **설치 출처로 추정**한다. 추정이라 안내만 하고 막지 않는다.
 * adb 설치는 설치 출처가 `null` 이고 제한에 안 걸린다 — 개발 중에 이 문제를 못 본 이유다.
 */
object InstallSource {

    private val STORES = setOf(
        "com.android.vending",               // Google Play
        "com.sec.android.app.samsungapps",   // Galaxy Store
        "com.amazon.venezia",
        "com.huawei.appmarket",
        "com.android.shell",                 // adb (기기에 따라 null 대신 이 값)
    )

    fun installer(context: Context): String? = runCatching {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(context.packageName)
        }
    }.getOrNull()

    fun mayBeRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val installer = installer(context) ?: return false
        return installer !in STORES
    }

    /** 앱 정보 화면. '제한된 설정 허용' 은 여기 ⋮ 메뉴에 있다. */
    fun openAppInfo(context: Context) {
        context.startActivity(
            Intent(
                AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${context.packageName}".toUri(),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
