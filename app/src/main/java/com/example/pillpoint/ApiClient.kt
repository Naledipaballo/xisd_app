package com.example.pillpoint

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object ApiClient {

    private const val BASE_URL =
        "http://10.0.2.2/pillpointdelivery/api/"

    private const val PREFS_NAME = "PillPointSession"
    private const val TOKEN_KEY = "auth_token"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    data class ApiResult(
        val success: Boolean,
        val message: String,
        val data: JSONObject? = null
    )

    fun login(
        context: Context,
        usernameOrEmail: String,
        password: String,
        callback: (ApiResult) -> Unit
    ) {
        val body = JSONObject().apply {
            put("username_or_email", usernameOrEmail.trim())
            put("password", password)
        }

        post("auth.php?action=login", body) { result ->
            if (result.success) {
                val token = result.data?.optString("token", "")

                if (!token.isNullOrBlank() && token != "null") {
                    context.applicationContext
                        .getSharedPreferences(
                            PREFS_NAME,
                            Context.MODE_PRIVATE
                        )
                        .edit()
                        .putString(TOKEN_KEY, token)
                        .apply()

                    callback(result)
                } else {
                    callback(
                        ApiResult(
                            false,
                            "Login response did not contain a token."
                        )
                    )
                }
            } else {
                callback(result)
            }
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String,
        callback: (ApiResult) -> Unit
    ) {
        val body = JSONObject().apply {
            put("first_name", firstName.trim())
            put("last_name", lastName.trim())
            put("username", username.trim())
            put("email", email.trim())
            put("password", password)
        }

        post("auth.php?action=register", body, callback)
    }

    fun getProfile(
        context: Context,
        callback: (ApiResult) -> Unit
    ) {
        val token = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(TOKEN_KEY, null)

        if (token.isNullOrBlank()) {
            callback(ApiResult(false, "Please log in first."))
            return
        }

        executor.execute {
            var connection: HttpURLConnection? = null

            try {
                connection = (URL(BASE_URL + "me.php")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty(
                        "Authorization",
                        "Bearer $token"
                    )
                }

                val status = connection.responseCode
                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val text = stream?.bufferedReader()?.use {
                    it.readText()
                } ?: ""

                val json = JSONObject(text)

                val result = ApiResult(
                    json.optBoolean("success", false),
                    json.optString("message", "Request completed."),
                    json.optJSONObject("user")
                )

                mainHandler.post {
                    callback(result)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback(
                        ApiResult(
                            false,
                            "Connection failed: ${e.message ?: "Unknown error"}"
                        )
                    )
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun logout(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(TOKEN_KEY)
            .apply()
    }

    private fun post(
        endpoint: String,
        body: JSONObject,
        callback: (ApiResult) -> Unit
    ) {
        executor.execute {
            var connection: HttpURLConnection? = null

            try {
                connection = (URL(BASE_URL + endpoint)
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 15000
                    doOutput = true
                    setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                    )
                    setRequestProperty(
                        "Accept",
                        "application/json"
                    )
                }

                OutputStreamWriter(
                    connection.outputStream,
                    Charsets.UTF_8
                ).use { writer ->
                    writer.write(body.toString())
                    writer.flush()
                }

                val status = connection.responseCode
                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val responseText = stream?.bufferedReader()?.use {
                    it.readText()
                } ?: ""

                val json = JSONObject(responseText)

                val result = ApiResult(
                    json.optBoolean("success", false),
                    json.optString(
                        "message",
                        "Request completed."
                    ),
                    json
                )

                mainHandler.post {
                    callback(result)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback(
                        ApiResult(
                            false,
                            "Connection failed: ${e.message ?: "Unknown error"}"
                        )
                    )
                }
            } finally {
                connection?.disconnect()
            }
        }
    }
}