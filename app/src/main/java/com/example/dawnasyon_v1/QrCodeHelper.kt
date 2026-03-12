package com.example.dawnasyon_v1

import android.graphics.Bitmap
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream

object QrCodeHelper {

    suspend fun generateAndUploadQrCode(userId: String): String? {
        try {
            // ⭐ Simple 400x400 generation (No extra hints)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(
                userId,
                BarcodeFormat.QR_CODE,
                400,
                400
            )

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            val fileName = "$userId/qr_code.png"
            val bucket = SupabaseManager.client.storage.from("images")
            bucket.upload(fileName, byteArray) {
                upsert = true
            }

            val publicUrl = bucket.publicUrl(fileName)
            val cacheBuster = System.currentTimeMillis()
            return "$publicUrl?t=$cacheBuster"

        } catch (e: Exception) {
            Log.e("QrCodeHelper", "Error generating/uploading QR code: ${e.message}")
            return null
        }
    }
}