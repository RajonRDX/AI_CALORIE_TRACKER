package com.bdcalorietracker.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageCompressor {
    fun compressAndSave(context: Context, sourceUri: Uri, maxDimension: Int = 1024): Pair<Bitmap, String> {
        val original = context.contentResolver.openInputStream(sourceUri).use {
            BitmapFactory.decodeStream(it)
        } ?: throw IllegalArgumentException("Cannot decode image URI")

        val maxSide = max(original.width, original.height)
        val scaledBitmap = if (maxSide > maxDimension) {
            val ratio = maxDimension.toFloat() / maxSide.toFloat()
            val targetW = (original.width * ratio).roundToInt()
            val targetH = (original.height * ratio).roundToInt()
            Bitmap.createScaledBitmap(original, targetW, targetH, true)
        } else {
            original
        }

        val mealsDir = File(context.filesDir, "meal_photos").apply { mkdirs() }
        val outFile = File(mealsDir, "meal_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
        }

        return Pair(scaledBitmap, outFile.absolutePath)
    }
}
