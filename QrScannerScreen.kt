package com.edm.downloadmanager.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * Live QR scanner: on detecting a URL-shaped barcode, calls [onLinkFound] once
 * and the caller (MainActivity/NavHost) is responsible for navigating back
 * and starting the download.
 */
@Composable
fun QrScannerScreen(onLinkFound: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    if (!hasCameraPermission) {
        Text("Camera permission is required to scan QR codes.")
        return
    }

    val scanner = remember { BarcodeScanning.getClient() }
    var handled = false

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !handled) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull()?.rawValue?.let { value ->
                                    if (value.startsWith("http://") || value.startsWith("https://")) {
                                        handled = true
                                        onLinkFound(value)
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
