package desu.tei.classicnetworkqs

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.provider.Settings

class NetworkStateRepository(private val context: Context, private val onChanged: (InternetState) -> Unit) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val wifi = WifiStateSource(context, ::publishState)
    private val cellular = CellularStateSource(context, ::publishState)
    private var network: Network? = null
    private var capabilities: NetworkCapabilities? = null
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var listening = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            cellular.refreshSubscription()
            wifi.refreshAvailability()
            publishState()
        }
    }

    fun startObserving() {
        if (listening) return
        listening = true
        val flags = if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO else 0
        val observer = object : ConnectivityManager.NetworkCallback(flags) {
            override fun onAvailable(network: Network) {
                if (!listening || callback !== this) return
                this@NetworkStateRepository.network = network
                capabilities = null
            }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                if (!listening || callback !== this || network != this@NetworkStateRepository.network) return
                capabilities = networkCapabilities
                cellular.refreshSubscription()
                publishState()
            }
            override fun onLost(network: Network) {
                if (!listening || callback !== this || network != this@NetworkStateRepository.network) return
                this@NetworkStateRepository.network = null
                capabilities = null
                cellular.refreshSubscription()
                publishState()
            }
        }
        manager.registerDefaultNetworkCallback(observer, handler)
        callback = observer
        network = manager.activeNetwork
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(LocationManager.MODE_CHANGED_ACTION)
        }, Context.RECEIVER_EXPORTED)
        wifi.startObserving()
        cellular.startObserving()
        publishState()
    }

    private fun publishState() {
        if (!listening) return
        val current = capabilities
        if (network != null && current == null) return
        val validated = current?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        onChanged(when {
            current?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> wifi.getState(current)
            current?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> InternetState.Ethernet(validated)
            current?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> cellular.getState(validated)
            current != null -> InternetState.Other(validated)
            network == null && wifi.getHasAvailableNetworks() -> InternetState.NetworksAvailable
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0 -> InternetState.AirplaneMode
            else -> InternetState.Disconnected
        })
    }

    fun stopObserving() {
        if (!listening) return
        listening = false
        callback?.let(manager::unregisterNetworkCallback)
        callback = null
        context.unregisterReceiver(receiver)
        cellular.stopObserving()
        wifi.stopObserving()
        network = null
        capabilities = null
    }
}
