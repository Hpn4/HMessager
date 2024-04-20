package com.hpn.hmessager.presentation.composable.qr

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

enum class BarcodeType(private val barcodeFormat: BarcodeFormat) {
    QR_CODE(BarcodeFormat.QR_CODE);

    private fun BitMatrix.toBitmap(): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT
                }
            }
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    internal fun getImageBitmap(width: Int, height: Int, value: String): ImageBitmap =
        MultiFormatWriter().encode(value, barcodeFormat, width, height)
            .toBitmap()
            .asImageBitmap()

    fun isValueValid(valueToCheck: String): Boolean {
        val barcode = try {
            MultiFormatWriter().encode(valueToCheck, barcodeFormat, 25, 25)
        } catch (e: Exception) {
            null
        }

        return barcode != null
    }
}