package desu.tei.classicnetworkqs

import android.app.Activity
import android.content.Intent
import android.content.ActivityNotFoundException
import android.os.Bundle
import android.provider.Settings

class InternetPanelActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            // SystemUI collapses QS before launching the long-press preferences activity.
            // AOSP maps the public Wi-Fi/wireless actions to Internet/Network & internet.
            try {
                startActivity(Intent(
                    if (getSharedPreferences("tile", MODE_PRIVATE).getBoolean("open_network_settings", false))
                        Settings.ACTION_WIRELESS_SETTINGS else Settings.ACTION_WIFI_SETTINGS,
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            } catch (_: ActivityNotFoundException) {
                try {
                    startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                } catch (_: ActivityNotFoundException) {
                    startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                }
            }
        }
        // Finish the separate NoDisplay task without bringing the existing settings task forward.
        finish()
    }
}
