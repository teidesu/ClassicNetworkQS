package desu.tei.classicnetworkqs

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.color.DynamicColors
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    private val tilePresence by lazy { getSharedPreferences("tile_presence", MODE_PRIVATE) }
    private val tilePresenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "added") updateAddTileControls()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val preferences = getSharedPreferences("tile", MODE_PRIVATE)
        val selector = findViewById<RadioGroup>(R.id.icon_style)
        selector.check(if (preferences.getBoolean("qpr1_icons", true)) R.id.qpr1_style else R.id.classic_style)
        selector.setOnCheckedChangeListener { _, checkedId ->
            preferences.edit().putBoolean("qpr1_icons", checkedId == R.id.qpr1_style).apply()
        }
        findViewById<MaterialSwitch>(R.id.advanced_network_type).apply {
            isChecked = preferences.getBoolean("advanced_network_type", false)
            setOnCheckedChangeListener { _, checked ->
                preferences.edit().putBoolean("advanced_network_type", checked).apply()
            }
        }
        findViewById<RadioGroup>(R.id.long_tap_action).apply {
            check(if (preferences.getBoolean("open_network_settings", false)) R.id.open_network_settings else R.id.open_internet_settings)
            setOnCheckedChangeListener { _, checkedId ->
                preferences.edit().putBoolean("open_network_settings", checkedId == R.id.open_network_settings).apply()
            }
        }
        findViewById<MaterialButton>(R.id.add_tile).setOnClickListener {
            getSystemService(StatusBarManager::class.java).requestAddTileService(
                ComponentName(this, InternetTileService::class.java), getString(R.string.internet),
                Icon.createWithResource(this, if (preferences.getBoolean("qpr1_icons", true)) R.drawable.ic_qpr1_wifi_4 else R.drawable.ic_wifi_4),
                mainExecutor,
            ) { result ->
                if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                    tilePresence.edit().putBoolean("added", true).apply()
                }
            }
        }
        findViewById<MaterialButton>(R.id.location_permission).setOnClickListener {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                !preferences.getBoolean("requested_location", false)) {
                preferences.edit().putBoolean("requested_location", true).apply()
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.location_settings_title)
                    .setMessage(getString(R.string.location_settings_message, packageManager.backgroundPermissionOptionLabel))
                    .setNegativeButton(R.string.not_now, null)
                    .setPositiveButton(R.string.open_settings) { _, _ ->
                        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                            checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            (!preferences.getBoolean("requested_background_location", false) ||
                                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION))) {
                            preferences.edit().putBoolean("requested_background_location", true).apply()
                            requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 3)
                        } else {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                        }
                    }.show()
            }
        }
        findViewById<MaterialButton>(R.id.phone_permission).setOnClickListener {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED &&
                !preferences.getBoolean("requested_phone", false)) {
                preferences.edit().putBoolean("requested_phone", true).apply()
                requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 2)
            } else {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        tilePresence.registerOnSharedPreferenceChangeListener(tilePresenceListener)
        updateAddTileControls()
    }

    override fun onStop() {
        tilePresence.unregisterOnSharedPreferenceChangeListener(tilePresenceListener)
        super.onStop()
    }

    private fun updateAddTileControls() {
        val visibility = if (tilePresence.getBoolean("added", false)) View.GONE else View.VISIBLE
        findViewById<View>(R.id.add_tile).visibility = visibility
        findViewById<View>(R.id.add_tile_instructions).visibility = visibility
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val hasLocation = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasPhone = checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val locationEnabled = getSystemService(LocationManager::class.java).isLocationEnabled
        findViewById<View>(R.id.location_setup).visibility = if (hasLocation) View.GONE else View.VISIBLE
        findViewById<View>(R.id.phone_setup).visibility = if (hasPhone) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.location_title).apply {
            setText(if (hasLocation) R.string.location_permission_label else R.string.network_name)
            contentDescription = if (hasLocation) getString(R.string.permission_granted_format, text) else null
            setCompoundDrawablesRelativeWithIntrinsicBounds(if (hasLocation) R.drawable.ic_check else 0, 0, 0, 0)
        }
        findViewById<TextView>(R.id.phone_title).apply {
            setText(if (hasPhone) R.string.phone_permission_label else R.string.sim_updates)
            contentDescription = if (hasPhone) getString(R.string.permission_granted_format, text) else null
            setCompoundDrawablesRelativeWithIntrinsicBounds(if (hasPhone) R.drawable.ic_check else 0, 0, 0, 0)
        }
        findViewById<View>(R.id.location_service_warning).visibility = if (hasLocation && !locationEnabled) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.location_status).setText(when {
            !locationEnabled -> R.string.location_disabled
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED -> R.string.location_denied
            else -> R.string.location_foreground
        })
        findViewById<TextView>(R.id.phone_status).setText(R.string.permission_optional)
    }
}
