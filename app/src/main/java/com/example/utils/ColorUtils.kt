package com.example.utils

import androidx.compose.ui.graphics.Color

object ColorUtils {
    fun parseColor(hex: String?, fallback: Color = Color(0xFF20222A)): Color {
        if (hex.isNullOrBlank()) return fallback
        return try {
            val cleanHex = hex.trim().removePrefix("#")
            when (cleanHex.length) {
                6 -> {
                    val colorLong = cleanHex.toLong(16) or 0xFF000000L
                    Color(colorLong)
                }
                8 -> {
                    val colorLong = cleanHex.toLong(16)
                    Color(colorLong)
                }
                3 -> {
                    // e.g. #abc -> #aabbcc
                    val r = cleanHex[0].toString().repeat(2)
                    val g = cleanHex[1].toString().repeat(2)
                    val b = cleanHex[2].toString().repeat(2)
                    val colorLong = "$r$g$b".toLong(16) or 0xFF000000L
                    Color(colorLong)
                }
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }
}
