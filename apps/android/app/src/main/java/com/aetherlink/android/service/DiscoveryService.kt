package com.aetherlink.android.service

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.aetherlink.android.domain.model.Device
import com.aetherlink.android.service.discovery.NsdDiscoveryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.coroutines.resume

class DiscoveryService(
    private val context: Context
) {

    private val nsdDiscoveryManager =
        NsdDiscoveryManager(context)

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.lowercase().contains("emulator")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk")
    }

    private fun probeHost(host: String, port: Int = 45876, timeoutMs: Int = 800): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            socket.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getLocalWifiSubnetPrefix(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.isLoopback || !intf.isUp) continue
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            val lastDot = ip.lastIndexOf('.')
                            if (lastDot != -1) {
                                return ip.substring(0, lastDot + 1)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AetherLink", "Failed to detect local IP subnet", e)
        }
        return null
    }

    private suspend fun scanSubnetParallel(prefix: String, port: Int = 45876): List<Device> = coroutineScope {
        val found = mutableListOf<Device>()
        val tasks = (1..254).map { hostNum ->
            async(Dispatchers.IO) {
                val ip = "$prefix$hostNum"
                if (probeHost(ip, port, timeoutMs = 400)) {
                    Log.d("AetherLink", "Subnet probe hit AetherLink server at $ip:$port")
                    Device(
                        id = "$ip:$port",
                        name = "AetherLink Desktop ($ip)",
                        ipAddress = ip,
                        isConnected = false
                    )
                } else null
            }
        }

        val results = tasks.awaitAll().filterNotNull()
        found.addAll(results)
        found
    }

    suspend fun discoverDevices(): List<Device> = withContext(Dispatchers.IO) {
        val foundDevices = mutableListOf<Device>()

        // 1. If running in an emulator, probe host loopback at 10.0.2.2:45876
        if (isEmulator()) {
            Log.d("AetherLink", "Emulator environment detected, probing host at 10.0.2.2:45876")
            if (probeHost("10.0.2.2", 45876, 800)) {
                foundDevices.add(
                    Device(
                        id = "10.0.2.2:45876",
                        name = "AetherLink Desktop (Host PC)",
                        ipAddress = "10.0.2.2",
                        isConnected = false
                    )
                )
            }
        }

        // 2. Direct probe known host IP (192.168.0.118:45876)
        val defaultLanIp = "192.168.0.118"
        if (foundDevices.none { it.ipAddress == defaultLanIp }) {
            if (probeHost(defaultLanIp, 45876, 600)) {
                Log.d("AetherLink", "Found active AetherLink server at $defaultLanIp:45876")
                foundDevices.add(
                    Device(
                        id = "$defaultLanIp:45876",
                        name = "AetherLink Desktop (Linux)",
                        ipAddress = defaultLanIp,
                        isConnected = false
                    )
                )
            }
        }

        // 3. Check last successfully connected IP from SharedPreferences
        val prefs = context.getSharedPreferences("aetherlink_prefs", Context.MODE_PRIVATE)
        val lastIp = prefs.getString("last_connected_ip", null)
        if (!lastIp.isNullOrBlank() && foundDevices.none { it.ipAddress == lastIp }) {
            if (probeHost(lastIp, 45876, 600)) {
                val lastName = prefs.getString("last_connected_name", "AetherLink Desktop (Saved)")
                foundDevices.add(
                    Device(
                        id = "$lastIp:45876",
                        name = lastName ?: "AetherLink Desktop",
                        ipAddress = lastIp,
                        isConnected = false
                    )
                )
            }
        }

        // 4. Perform mDNS Discovery with a 3.5-second timeout
        val mdnsResult = withTimeoutOrNull(3500) {
            suspendCancellableCoroutine<Device?> { continuation ->
                var resumed = false

                nsdDiscoveryManager.startDiscovery { host, port, name ->
                    if (!resumed) {
                        resumed = true
                        continuation.resume(
                            Device(
                                id = "$host:$port",
                                name = name,
                                ipAddress = host,
                                isConnected = false
                            )
                        )
                    }
                }

                continuation.invokeOnCancellation {
                    nsdDiscoveryManager.stopDiscovery()
                }
            }
        }

        nsdDiscoveryManager.stopDiscovery()

        if (mdnsResult != null && foundDevices.none { it.ipAddress == mdnsResult.ipAddress }) {
            foundDevices.add(mdnsResult)
        }

        // 5. Subnet Scan Fallback: If no devices found, scan local /24 subnet in parallel
        if (foundDevices.isEmpty()) {
            val subnetPrefix = getLocalWifiSubnetPrefix()
            if (subnetPrefix != null) {
                Log.d("AetherLink", "mDNS returned no devices, performing parallel subnet scan on $subnetPrefix*")
                val subnetMatches = scanSubnetParallel(subnetPrefix, 45876)
                foundDevices.addAll(subnetMatches)
            }
        }

        foundDevices
    }

    fun stopDiscovery() {
        nsdDiscoveryManager.stopDiscovery()
    }
}
