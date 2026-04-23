package com.Chenkham.Echofy.utils

import android.content.Context
import android.net.wifi.WifiManager
import java.util.Locale

object IpCodeUtils {

    /**
     * Helper to get the local WiFi IP address as a string.
     */
    fun getLocalIpAddress(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = wifiManager.connectionInfo.ipAddress
        if (ipAddress == 0) return null

        // WifiInfo.ipAddress is little-endian int
        return String.format(
            Locale.US,
            "%d.%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff,
            ipAddress shr 24 and 0xff
        )
    }

    /**
     * Converts an IPv4 string (like "192.168.1.100") into a short alphanumeric code (Base36).
     */
    fun ipToCode(ip: String): String {
        val parts = ip.split(".")
        if (parts.size != 4) return ""

        return try {
            var ipLong = 0L
            for (i in 0..3) {
                ipLong = (ipLong shl 8) or (parts[i].toLong() and 0xFF)
            }
            // Base36 gives numbers and letters, making it nice and short
            ipLong.toString(36).uppercase(Locale.ROOT)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Converts a short alphanumeric code (Base36) back to an IPv4 string.
     */
    fun codeToIp(code: String): String? {
        if (code.isBlank()) return null
        return try {
            val ipLong = code.trim().toLong(36)
            val b1 = (ipLong shr 24) and 0xFF
            val b2 = (ipLong shr 16) and 0xFF
            val b3 = (ipLong shr 8) and 0xFF
            val b4 = ipLong and 0xFF
            "$b1.$b2.$b3.$b4"
        } catch (e: Exception) {
            null
        }
    }
}
