package cn.numeron.http

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.lang.reflect.Modifier

internal class HttpUrlExtractorScope : ExtractorScope {

    override var HttpUrl.Builder.path: String
        get() = javaClass.getDeclaredField("encodedPathSegments").run {
            isAccessible = true
            val segmentPath = get(this@path) as List<*>
            segmentPath.joinToString("/", prefix = "/")
        }
        set(value) {
            var path = value
            if (path.isNotEmpty()) {
                if (!path.startsWith('/')) {
                    path = "/$path"
                }
                encodedPath(path)
            }
        }

    override var HttpUrl.Builder.url: String
        get() = toString()
        set(value) {
            val newBuilder = value.toHttpUrl().newBuilder()
            HttpUrl.Builder::class.java.declaredFields.forEach {
                it.isAccessible = true
                if (!Modifier.isStatic(it.modifiers)) {
                    it.set(this, it.get(newBuilder))
                }
            }
        }

}