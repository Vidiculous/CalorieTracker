package com.calorietracker.util

import android.util.Base64
import java.io.File

object AudioUtils {
    fun fileToBase64(file: File): String {
        val bytes = file.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
