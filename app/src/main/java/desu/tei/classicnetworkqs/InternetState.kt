package desu.tei.classicnetworkqs

sealed interface InternetState {
    data class Wifi(val ssid: String?, val level: Int, val validated: Boolean) : InternetState
    data class Cellular(
        val carrier: String?, val displayType: String?, val advancedType: String?, val level: Int,
        val roaming: Boolean, val validated: Boolean,
    ) : InternetState
    data class Ethernet(val validated: Boolean) : InternetState
    data class Other(val validated: Boolean) : InternetState
    data object AirplaneMode : InternetState
    data object NetworksAvailable : InternetState
    data object Disconnected : InternetState
}
