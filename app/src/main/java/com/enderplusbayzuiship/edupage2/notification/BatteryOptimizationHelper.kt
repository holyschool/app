package com.enderplusbayzuiship.edupage2.notification

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class BatteryOptimizationHelper(private val context: Context) {

    @SuppressLint("BatteryLife")
    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun createBatteryOptimizationIntent(): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun createBatterySettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    fun registerBatteryOptimizationLauncher(
        activity: ComponentActivity,
        onResult: (Boolean) -> Unit
    ): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            onResult(isIgnoringBatteryOptimizations())
        }
    }

    fun requestBatteryOptimizationExemption(
        launcher: ActivityResultLauncher<Intent>,
        onError: (String) -> Unit = {}
    ) {
        try {
            if (isIgnoringBatteryOptimizations()) {
                return
            }

            val intent = createBatteryOptimizationIntent()

            if (intent.resolveActivity(context.packageManager) != null) {
                launcher.launch(intent)
            } else {

                val fallbackIntent = createBatterySettingsIntent()
                if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                    launcher.launch(fallbackIntent)
                } else {
                    onError("Battery optimization settings not available on this device")
                }
            }
        } catch (e: Exception) {
            onError("Failed to open battery optimization settings: ${e.message}")
        }
    }

    companion object {
        const val TAG = "BatteryOptimizationHelper"

        fun isAppWhitelisted(context: Context): Boolean {
            return BatteryOptimizationHelper(context).isIgnoringBatteryOptimizations()
        }
    }
}
