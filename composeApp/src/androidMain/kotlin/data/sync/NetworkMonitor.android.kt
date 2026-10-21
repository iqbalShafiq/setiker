package data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class NetworkMonitor(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isOnline = MutableStateFlow(checkCurrentNetwork())
    actual val isOnline: StateFlow<Boolean> = _isOnline

    /**
     * How many callers want the callback registered.
     *
     * The callback lives in the connectivity manager, which holds a strong reference to it. If
     * every caller registered its own, a screen that came and went would leave a callback behind
     * each time, and the manager would keep calling all of them. The count makes the registration
     * happen once and the unregistration happen when the last caller is gone.
     */
    private var registrations = 0

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            setOnline(true)
        }

        override fun onLost(network: Network) {
            setOnline(checkCurrentNetwork())
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            setOnline(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        }
    }

    actual fun startMonitoring() {
        registrations++
        if (registrations > 1) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    actual fun stopMonitoring() {
        if (registrations == 0) return
        registrations--
        if (registrations > 0) return

        // The callback can be gone already when the connectivity manager tears it down on its own.
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    private fun setOnline(online: Boolean) {
        _isOnline.value = online
        ConnectivityState.update(online)
    }

    private fun checkCurrentNetwork(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    init {
        ConnectivityState.update(_isOnline.value)
    }
}
