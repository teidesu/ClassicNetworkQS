package desu.tei.classicnetworkqs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager

class CellularStateSource(private val context: Context, private val onChanged: () -> Unit) {
    private val baseManager = context.getSystemService(TelephonyManager::class.java)
    private var manager: TelephonyManager? = null
    private var callback: TelephonyCallback? = null
    private var subscriptionCallback: TelephonyCallback? = null
    private var subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private var level = 0
    private var displayInfo: TelephonyDisplayInfo? = null
    private var listening = false

    fun startObserving() {
        listening = true
        refreshSubscription()
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            val observer = object : TelephonyCallback(), TelephonyCallback.ActiveDataSubscriptionIdListener {
                override fun onActiveDataSubscriptionIdChanged(subId: Int) {
                    if (!listening) return
                    refreshSubscription()
                    onChanged()
                }
            }
            try {
                baseManager?.registerTelephonyCallback(context.mainExecutor, observer)
                subscriptionCallback = observer
            } catch (_: SecurityException) { }
        }
    }

    fun refreshSubscription() {
        if (!listening) return
        val id = SubscriptionManager.getActiveDataSubscriptionId()
        if (id == subscriptionId && callback != null) return
        unregisterSignalCallback()
        subscriptionId = id
        level = 0
        displayInfo = null
        manager = if (SubscriptionManager.isValidSubscriptionId(id)) baseManager?.createForSubscriptionId(id) else null
        val currentManager = manager ?: return
        val observer = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener,
            TelephonyCallback.DisplayInfoListener {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                if (!listening || manager !== currentManager) return
                level = signalStrength.level.coerceIn(0, 4)
                onChanged()
            }
            override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                if (!listening || manager !== currentManager) return
                displayInfo = telephonyDisplayInfo
                onChanged()
            }
        }
        try {
            currentManager.registerTelephonyCallback(context.mainExecutor, observer)
            callback = observer
        } catch (_: SecurityException) { }
    }

    fun getState(validated: Boolean): InternetState.Cellular {
        val type = displayInfo?.let {
            when (it.overrideNetworkType) {
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5G+"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "5G"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> "LTE+"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "LTE+"
                else -> when (it.networkType) {
                    TelephonyManager.NETWORK_TYPE_NR -> "5G"
                    TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                    TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSDPA,
                    TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA,
                    TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_TD_SCDMA,
                    TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A,
                    TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
                    TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyManager.NETWORK_TYPE_GSM, TelephonyManager.NETWORK_TYPE_CDMA,
                    TelephonyManager.NETWORK_TYPE_1xRTT -> "2G"
                    else -> null
                }
            }
        }
        val advancedType = when (displayInfo?.networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_IDEN -> "iDEN"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA"
            TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
            TelephonyManager.NETWORK_TYPE_NR -> "NR"
            else -> null
        }
        val carrier = try { manager?.networkOperatorName?.takeIf { it.isNotBlank() } } catch (_: SecurityException) { null }
        val roaming = try { manager?.isNetworkRoaming == true } catch (_: SecurityException) { false }
        return InternetState.Cellular(carrier, type, advancedType, level, roaming, validated)
    }

    private fun unregisterSignalCallback() {
        callback?.let { manager?.unregisterTelephonyCallback(it) }
        callback = null
        manager = null
    }

    fun stopObserving() {
        listening = false
        unregisterSignalCallback()
        subscriptionCallback?.let { baseManager?.unregisterTelephonyCallback(it) }
        subscriptionCallback = null
    }
}
