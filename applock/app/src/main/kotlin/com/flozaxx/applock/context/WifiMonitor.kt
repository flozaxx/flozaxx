package com.flozaxx.applock.context

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface WifiSnapshot {
    data object NotConnected : WifiSnapshot
    data class Connected(val bssid: String, val ssid: String) : WifiSnapshot

    /** Wi-Fi is connected but the system hides which access point it is (permission or location off). */
    data object Unreadable : WifiSnapshot
}

class WifiMonitor(context: Context) {

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val wifiRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    private val _state = MutableStateFlow<WifiSnapshot>(WifiSnapshot.Unreadable)
    val state: StateFlow<WifiSnapshot> = _state

    private var callback: ConnectivityManager.NetworkCallback? = null

    /**
     * (Re)subscribes to Wi-Fi changes. Called again whenever the location permission or the
     * location switch changes, because the system only redacts or reveals the BSSID on a new
     * registration.
     */
    @Synchronized
    fun start() {
        callback?.let(connectivity::unregisterNetworkCallback)
        _state.value = if (hasWifiNetwork()) WifiSnapshot.Unreadable else WifiSnapshot.NotConnected
        callback = WifiCallback().also { connectivity.registerNetworkCallback(wifiRequest, it) }
    }

    @Suppress("DEPRECATION")
    private fun hasWifiNetwork(): Boolean = connectivity.allNetworks.any { network ->
        connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    private inner class WifiCallback : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
        private var current: Network? = null

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            current = network
            val info = capabilities.transportInfo as? WifiInfo
            val bssid = info?.bssid
            _state.value =
                if (bssid == null || bssid == REDACTED_BSSID) WifiSnapshot.Unreadable
                else WifiSnapshot.Connected(bssid.lowercase(), info.ssid.orEmpty().trim('"'))
        }

        override fun onLost(network: Network) {
            if (network == current) {
                current = null
                _state.value = WifiSnapshot.NotConnected
            }
        }
    }

    private companion object {
        const val REDACTED_BSSID = "02:00:00:00:00:00"
    }
}
