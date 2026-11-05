package com.ehs.finances.core.prefs

import android.content.Context

/** Persistência local do app (sessão, tema e idioma). */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("ehs-finances", Context.MODE_PRIVATE)

    fun readString(key: String): String? = prefs.getString(key, null)

    fun writeString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }
}
