/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.qr

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.lineageos.aperture.models.QrResult
import zxingcpp.BarcodeReader

class QrImageAnalyzer(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) : ImageAnalysis.Analyzer {
    // QR
    private val reader by lazy {
        BarcodeReader().apply {
            options.tryInvert = true
            options.tryRotate = true
        }
    }

    private val qrTextClassifier by lazy { QrTextClassifier(context) }

    private val _qrResult = MutableStateFlow<QrResult?>(null)
    val qrResult = _qrResult.filterNotNull()

    override fun analyze(image: ImageProxy) {
        image.use {
            if (_qrResult.value != null) {
                return
            }

            processResults(reader.read(image))
        }
    }

    fun dismissResult() {
        _qrResult.value = null
    }

    private fun processResults(results: List<BarcodeReader.Result>) {
        results.firstOrNull()?.let { result ->
            coroutineScope.launch(Dispatchers.IO) {
                // Classify message
                val qrResult = qrTextClassifier.classify(result)

                _qrResult.emit(qrResult)
            }
        }
    }
}
