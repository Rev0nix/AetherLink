package com.vexyron.android.core.utils

import android.content.Context
import android.util.Log
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

object QrCodeScannerHelper {

    fun parsePayload(raw: String): Pair<String, Int>? {
        return try {
            val clean = raw.trim()
                .removePrefix("vexyron://")
                .removePrefix("http://")
                .removePrefix("https://")
            val parts = clean.split(":")
            val ip = parts.getOrNull(0)?.trim() ?: return null
            val port = parts.getOrNull(1)?.toIntOrNull() ?: 45876
            Pair(ip, port)
        } catch (e: Exception) {
            null
        }
    }

    fun startScan(
        context: Context,
        onSuccess: (ip: String, port: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build()

            val scanner = GmsBarcodeScanning.getClient(context, options)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val rawValue = barcode.rawValue ?: barcode.displayValue ?: ""
                    Log.d("VEXYRON", "Scanned QR code: $rawValue")
                    val parsed = parsePayload(rawValue)
                    if (parsed != null) {
                        onSuccess(parsed.first, parsed.second)
                    } else {
                        onError("Invalid VEXYRON QR code: $rawValue")
                    }
                }
                .addOnCanceledListener {
                    Log.d("VEXYRON", "QR Scan canceled by user")
                }
                .addOnFailureListener { e ->
                    Log.e("VEXYRON", "QR Scan failed", e)
                    onError("QR Scanner: ${e.localizedMessage ?: "Camera or Play Services error"}")
                }
        } catch (e: Exception) {
            Log.e("VEXYRON", "Failed to launch scanner", e)
            onError("Scanner error: ${e.localizedMessage}")
        }
    }
}
