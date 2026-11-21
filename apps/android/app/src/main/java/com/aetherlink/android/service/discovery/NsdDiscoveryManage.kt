package com.aetherlink.android.service.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log

class NsdDiscoveryManager(
    private val context: Context
) {

    private val nsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery(
        onDeviceFound: (host: String, port: Int, name: String) -> Unit
    ) {
        stopDiscovery()

        try {
            multicastLock = wifiManager?.createMulticastLock("aetherlink_mdns_lock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w("AetherLink", "Could not acquire MulticastLock: ${e.message}")
        }

        val listener = object : NsdManager.DiscoveryListener {

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d("AetherLink", "NSD discovery started: $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(
                    "AetherLink",
                    "NSD service found: ${serviceInfo.serviceName} (${serviceInfo.serviceType})"
                )

                if (serviceInfo.serviceType.contains("_aetherlink._tcp")) {
                    nsdManager.resolveService(
                        serviceInfo,
                        object : NsdManager.ResolveListener {

                            override fun onResolveFailed(
                                serviceInfo: NsdServiceInfo,
                                errorCode: Int
                            ) {
                                Log.e(
                                    "AetherLink",
                                    "NSD resolve failed: $errorCode"
                                )
                            }

                            override fun onServiceResolved(
                                resolvedInfo: NsdServiceInfo
                            ) {
                                val host = resolvedInfo.host
                                val port = resolvedInfo.port
                                val name = resolvedInfo.serviceName

                                Log.d(
                                    "AetherLink",
                                    "Desktop resolved: $name $host:$port"
                                )

                                onDeviceFound(
                                    host.hostAddress ?: return,
                                    port,
                                    name
                                )
                            }
                        }
                    )
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(
                    "AetherLink",
                    "NSD service lost: ${serviceInfo.serviceName}"
                )
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(
                    "AetherLink",
                    "NSD discovery stopped: $serviceType"
                )
            }

            override fun onStartDiscoveryFailed(
                serviceType: String,
                errorCode: Int
            ) {
                Log.e(
                    "AetherLink",
                    "NSD discovery start failed: $errorCode"
                )
                try {
                    nsdManager.stopServiceDiscovery(this)
                } catch (_: Exception) {}
            }

            override fun onStopDiscoveryFailed(
                serviceType: String,
                errorCode: Int
            ) {
                Log.e(
                    "AetherLink",
                    "NSD discovery stop failed: $errorCode"
                )
                try {
                    nsdManager.stopServiceDiscovery(this)
                } catch (_: Exception) {}
            }
        }

        discoveryListener = listener

        try {
            // Note: Android NSD API expects "_service._protocol" WITHOUT trailing dot
            nsdManager.discoverServices(
                "_aetherlink._tcp",
                NsdManager.PROTOCOL_DNS_SD,
                listener
            )
        } catch (e: Exception) {
            Log.e("AetherLink", "discoverServices failed", e)
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (_: Exception) {}
        }
        discoveryListener = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }
}
