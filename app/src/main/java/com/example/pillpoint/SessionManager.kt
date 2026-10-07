package com.example.pillpoint

import android.content.Context

/** Keeps track of whether the user has successfully authenticated. */
object SessionManager {
    private const val PREFS = "pill_point_session"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_EMAIL = "logged_in_email"

    fun login(context: Context, email: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOGGED_IN, false)

    fun email(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, "").orEmpty()

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
