package com.example.familieconnect

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

object TrackerRepository {

    private val client = OkHttpClient()

    fun load(groupCode: String): Result<List<Tracker>> {
        return runCatching {

            val url =
                "https://my-tracker-dc65.onrender.com/api/location"
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("pin", groupCode)
                    .build()

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->

                if (!response.isSuccessful) {
                    error("Serverfout: HTTP ${response.code}")
                }

                val body = response.body?.string() ?: "[]"
                val json = JSONArray(body)

                buildList {
                    for (i in 0 until json.length()) {
                        val item = json.getJSONObject(i)

                        add(
                            Tracker(
                                name = item.optString(
                                    "device",
                                    "Onbekend"
                                ),
                                lat = item.optDouble(
                                    "lat",
                                    0.0
                                ),
                                lng = item.optDouble(
                                    "lng",
                                    0.0
                                ),
                                accuracy = item.optDouble(
                                    "accuracy",
                                    999.0
                                ),
                                battery = item.optInt(
                                    "battery",
                                    -1
                                ),
                                active = item.optBoolean(
                                    "active",
                                    false
                                ),
                                sos = item.optBoolean(
                                    "sos",
                                    false
                                ),
                                time = item.optLong(
                                    "time",
                                    0L
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}