package desu.tei.classicnetworkqs

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class InternetTileService : TileService() {
    private val preferences by lazy { getSharedPreferences("tile", MODE_PRIVATE) }
    private val tilePresence by lazy { getSharedPreferences("tile_presence", MODE_PRIVATE) }
    private var lastState: InternetState? = null
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "qpr1_icons" || key == "advanced_network_type") lastState?.let(::updateTile)
    }
    private val repository by lazy { NetworkStateRepository(this, ::updateTile) }

    override fun onTileAdded() {
        super.onTileAdded()
        tilePresence.edit().putBoolean("added", true).apply()
    }

    override fun onTileRemoved() {
        tilePresence.edit().putBoolean("added", false).apply()
        super.onTileRemoved()
    }

    override fun onStartListening() {
        super.onStartListening()
        tilePresence.edit().putBoolean("added", true).apply()
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        repository.startObserving()
    }

    override fun onStopListening() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        lastState = null
        repository.stopObserving()
        super.onStopListening()
    }

    override fun onDestroy() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        lastState = null
        repository.stopObserving()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        // AOSP Settings forwards this action to SystemUI as a broadcast. Sending it directly
        // keeps QS expanded; the receiver is undocumented and may differ on OEM builds.
        sendBroadcast(
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                .setPackage("com.android.systemui")
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND),
        )
    }

    private fun updateTile(state: InternetState) {
        lastState = state
        val tile = qsTile ?: return
        val useQpr1 = preferences.getBoolean("qpr1_icons", true)
        tile.label = getString(R.string.internet)
        val wifiIcons = if (useQpr1) intArrayOf(
            R.drawable.ic_qpr1_wifi_0,
            R.drawable.ic_qpr1_wifi_1,
            R.drawable.ic_qpr1_wifi_2,
            R.drawable.ic_qpr1_wifi_3,
            R.drawable.ic_qpr1_wifi_4
        ) else intArrayOf(
            R.drawable.ic_wifi_0,
            R.drawable.ic_wifi_1,
            R.drawable.ic_wifi_2,
            R.drawable.ic_wifi_3,
            R.drawable.ic_wifi_4
        )
        val wifiNoInternetIcons = if (useQpr1) intArrayOf(
            R.drawable.ic_qpr1_wifi_0_no_internet,
            R.drawable.ic_qpr1_wifi_1_no_internet,
            R.drawable.ic_qpr1_wifi_2_no_internet,
            R.drawable.ic_qpr1_wifi_3_no_internet,
            R.drawable.ic_qpr1_wifi_4_no_internet
        ) else intArrayOf(
            R.drawable.ic_wifi_0_no_internet,
            R.drawable.ic_wifi_1_no_internet,
            R.drawable.ic_wifi_2_no_internet,
            R.drawable.ic_wifi_3_no_internet,
            R.drawable.ic_wifi_4_no_internet
        )
        val cellularNoInternetIcons = if (useQpr1) intArrayOf(
            R.drawable.ic_qpr1_cellular_0_no_internet,
            R.drawable.ic_qpr1_cellular_1_no_internet,
            R.drawable.ic_qpr1_cellular_2_no_internet,
            R.drawable.ic_qpr1_cellular_3_no_internet,
            R.drawable.ic_qpr1_cellular_4_no_internet
        ) else intArrayOf(
            R.drawable.ic_cellular_0_no_internet,
            R.drawable.ic_cellular_1_no_internet,
            R.drawable.ic_cellular_2_no_internet,
            R.drawable.ic_cellular_3_no_internet,
            R.drawable.ic_cellular_4_no_internet
        )
        val cellularIcons = if (useQpr1) intArrayOf(
            R.drawable.ic_qpr1_cellular_0,
            R.drawable.ic_qpr1_cellular_1,
            R.drawable.ic_qpr1_cellular_2,
            R.drawable.ic_qpr1_cellular_3,
            R.drawable.ic_qpr1_cellular_4
        ) else intArrayOf(
            R.drawable.ic_cellular_0,
            R.drawable.ic_cellular_1,
            R.drawable.ic_cellular_2,
            R.drawable.ic_cellular_3,
            R.drawable.ic_cellular_4
        )
        val validated = when (state) {
            is InternetState.Wifi -> state.validated
            is InternetState.Cellular -> state.validated
            is InternetState.Ethernet -> state.validated
            is InternetState.Other -> state.validated
            else -> false
        }
        val subtitle = when (state) {
            is InternetState.Wifi -> state.ssid ?: getString(R.string.wifi)
            is InternetState.Cellular -> {
                val networkType = if (preferences.getBoolean("advanced_network_type", false)) state.advancedType else state.displayType
                val dataType = when {
                    state.roaming && !networkType.isNullOrEmpty() ->
                        getString(R.string.mobile_data_text_format, getString(R.string.roaming), networkType)
                    state.roaming -> getString(R.string.roaming)
                    else -> networkType
                }
                when {
                    dataType.isNullOrEmpty() -> state.carrier.orEmpty()
                    state.carrier.isNullOrEmpty() -> dataType
                    else -> getString(R.string.mobile_carrier_text_format, state.carrier, dataType)
                }
            }
            is InternetState.Ethernet -> getString(R.string.ethernet)
            is InternetState.Other -> getString(R.string.connected)
            InternetState.AirplaneMode -> getString(R.string.airplane_mode)
            InternetState.NetworksAvailable -> getString(R.string.networks_available)
            InternetState.Disconnected -> getString(R.string.disconnected)
        }
        tile.subtitle = subtitle
        tile.contentDescription = "${tile.label}, ${tile.subtitle}"
        tile.state = if (validated) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, when (state) {
            is InternetState.Wifi -> (if (state.validated) wifiIcons else wifiNoInternetIcons)[state.level.coerceIn(0, 4)]
            is InternetState.Cellular -> (if (state.validated) cellularIcons else cellularNoInternetIcons)[state.level.coerceIn(0, 4)]
            is InternetState.Ethernet -> if (state.validated) R.drawable.ic_ethernet else R.drawable.ic_ethernet_no_internet
            is InternetState.Other -> R.drawable.ic_internet
            InternetState.AirplaneMode -> R.drawable.ic_airplane
            InternetState.NetworksAvailable -> R.drawable.ic_networks_available
            InternetState.Disconnected -> R.drawable.ic_disconnected
        })
        tile.updateTile()
    }
}
