package cn.numeron.http

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

fun HttpUrl.Builder.setUrl(url: String) {
    val newBuilder = url.toHttpUrl().newBuilder()
    HttpUrl.Builder::class.java.declaredFields.forEach {
        it.isAccessible = true
        it.set(this, it.get(newBuilder))
    }
}