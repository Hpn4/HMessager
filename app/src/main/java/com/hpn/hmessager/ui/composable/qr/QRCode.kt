package com.hpn.hmessager.ui.composable.qr

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Barcode asynchronously creates a barcode bitmap in the background and then displays
 * the barcode via an Image composable. A progress indicator shows, optionally, until
 * the barcode value has been encoded to a bitmap.
 *
 * Note: if the barcode is not a valid format, the spinner will continue forever.
 *
 * @param modifier the modifier to be applied to the layout
 * @param showProgress true will show the progress indicator. Defaults to true.
 * @param resolutionFactor multiplied on the width/height to get the resolution, in px, for the bitmap
 * @param width for the generated bitmap multiplied by the resolutionFactor
 * @param height for the generated bitmap multiplied by the resolutionFactor
 * @param type the type of barcode to render
 * @param value the value of the barcode to show
 */
@Composable
fun Barcode(
    modifier: Modifier = Modifier,
    showProgress: Boolean = true,
    resolutionFactor: Int = 1,
    width: Dp = 128.dp,
    height: Dp = 128.dp,
    type: BarcodeType,
    value: String
) {
    val barcodeBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    val scope = rememberCoroutineScope()

    // The launched effect will run every time the value changes. So, if the barcode changes,
    // the coroutine to get the bitmap will be started.
    LaunchedEffect(value) {
        scope.launch {
            withContext(Dispatchers.Default) {
                barcodeBitmap.value = try {
                    type.getImageBitmap(
                        width = (width.value * resolutionFactor).toInt(),
                        height = (height.value * resolutionFactor).toInt(),
                        value = value
                    )
                } catch (e: Exception) {
                    Log.e("ComposeBarcodes", "Invalid Barcode Format", e)
                    null
                }
            }
        }
    }

    // Contain the barcode in a box that matches the provided dimensions
    barcodeBitmap.value?.let { barcode ->
        Image(
            modifier = modifier,
            painter = BitmapPainter(barcode),
            contentDescription = value
        )
    } ?: run {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize(0.5f)
            )
        }
    }
}

@Composable
fun QRScanner(
    onQrCodeScanned: (result: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val contextL = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(contextL) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                contextL,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        launcher.launch(Manifest.permission.CAMERA)
    }


    if (hasCameraPermission) {
        AndroidView(
            factory = { context ->
                val previewView = PreviewView(context)
                val preview = Preview.Builder().build()
                val selector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                    QRCodeAnalyzer { result ->
                        result?.let { onQrCodeScanned(it) }
                    }
                )

                try {
                    cameraProviderFuture.get().unbindAll()
                    cameraProviderFuture.get().bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return@AndroidView previewView
            },
            modifier = modifier
        )
    }
}