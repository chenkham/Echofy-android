package com.Chenkham.Echofy.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.Chenkham.innertube.utils.parseCookieString
import com.Chenkham.Echofy.constants.InnerTubeCookieKey
import com.Chenkham.Echofy.constants.YtmSyncKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import kotlinx.coroutines.runBlocking



fun Context.isInternetConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
}
