package com.terinit.rhythmicmeditation.integration.intent

import android.content.Intent
import android.content.pm.PackageManager

/** Launcher lookup for the policy-owning Routine app and its standalone QA build. */
object RoutineAppLauncher {
    val supportedPackages = listOf(
        "com.terinit.rhythmicroutine",
        "com.terinit.rhythmicroutine.qa"
    )

    fun findLaunchIntent(packageManager: PackageManager): Intent? =
        supportedPackages.firstNotNullOfOrNull(packageManager::getLaunchIntentForPackage)
}
