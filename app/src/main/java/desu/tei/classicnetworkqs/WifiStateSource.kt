package desu.tei.classicnetworkqs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager

class WifiStateSource(private val context: Context, private val onChanged: () -> Unit) {
    private val manager = context.getSystemService(WifiManager::class.java)

    private var callback: WifiManager.ScanResultsCallback? = null
    private var availableNetworks = false

    fun startObserving() {
        if (manager == null || callback != null) return
        refreshAvailability()
        val observer = object : WifiManager.ScanResultsCallback() {
            override fun onScanResultsAvailable() {
                if (callback !== this) return
                refreshAvailability()
                onChanged()
            }
        }
        manager.registerScanResultsCallback(context.mainExecutor, observer)
        callback = observer
    }

    fun refreshAvailability() {
        availableNetworks = if (manager?.isWifiEnabled == true &&
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            context.getSystemService(LocationManager::class.java).isLocationEnabled) {
            try { manager.scanResults.isNotEmpty() } catch (_: SecurityException) { false }
        } else false
    }

    fun getHasAvailableNetworks(): Boolean {
        refreshAvailability()
        return availableNetworks
    }

    fun stopObserving() {
        callback?.let { manager?.unregisterScanResultsCallback(it) }
        callback = null
        availableNetworks = false
    }

    fun getState(capabilities: NetworkCapabilities): InternetState.Wifi {
        val info = capabilities.transportInfo as? WifiInfo
        val ssid = info?.ssid?.takeUnless { it == WifiManager.UNKNOWN_SSID }
            ?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }
        val level = if (info == null || manager == null) 0 else {
            val maximum = manager.maxSignalLevel.coerceAtLeast(1)
            (manager.calculateSignalLevel(info.rssi) * 4 / maximum).coerceIn(0, 4)
        }
        return InternetState.Wifi(ssid, level,
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
    }
}
