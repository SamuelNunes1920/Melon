package com.forcetower.uefs.core.util

import androidx.annotation.WorkerThread
import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

object ImgurUploader {
    @WorkerThread
    fun upload(client: OkHttpClient, base64: String, name: String): ImgurUpload? {
        val body = FormBody.Builder()
            .add("album", "YuKcQI3mEuYFu3q")
            .add("image", base64)
            .add("type", "base64")
            .add("name", name)
            .build()

        val request = Request.Builder()
            .addHeader("Authorization", "Client-ID 5becc567d624bcf")
            .addHeader("Accept", "application/json")
            .url("https://api.imgur.com/3/image")
            .post(body)
            .build()

        val call = client.newCall(request)
        try {
            val response = call.execute()
            return if (response.isSuccessful) {
                Timber.d("Upload Success")
                val string = response.body()!!.string()
                val converted = Gson().fromJson(string, UploadResponse::class.java)
                Timber.d("Converted $converted")
                converted.data
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    internal data class UploadResponse(
        val data: ImgurUpload,
        val success: Boolean,
        val status: Int
    )
}

data class ImgurUpload(
    val link: String,
    val deletehash: String
)