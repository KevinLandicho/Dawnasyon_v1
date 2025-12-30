package com.example.dawnasyon_v1

import android.graphics.Bitmap
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream

object QrCodeHelper {

    /**
     * Generates a QR code for the given userId, uploads it to Supabase,
     * and returns the public URL.
     */
    suspend fun generateAndUploadQrCode(userId: String): String? {
        try {
            // 1. Generate QR Code Bitmap
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(userId, BarcodeFormat.QR_CODE, 400, 400)

            // 2. Convert Bitmap to ByteArray
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            // 3. Upload to Supabase Storage
            val fileName = "$userId/qr_code.png"
            val bucket = SupabaseManager.client.storage.from("images") // Using existing 'images' bucket
            bucket.upload(fileName, byteArray) {
                upsert = true
            }

            // 4. Get and return the public URL
            return bucket.publicUrl(fileName)

        } catch (e: Exception) {
            Log.e("QrCodeHelper", "Error generating/uploading QR code: ${e.message}")
            return null
        }
    }
}