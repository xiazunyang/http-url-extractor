package cn.numeron.http

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.http.*
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class HttpUrlExtractor(private var baseUrl: String) {

    constructor(retrofit: Retrofit) : this(retrofit.baseUrl().toString())

    private var firstParserChain: ParserChain<Annotation>? = null

    init {
        addAnnotationParser<GET> { builder, annotation, _ ->
            builder.setPath(annotation.value)
        }
        addAnnotationParser<POST> { builder, annotation, _ ->
            builder.setPath(annotation.value)
        }
        addAnnotationParser<PUT> { builder, annotation, _ ->
            builder.setPath(annotation.value)
        }
        addAnnotationParser<DELETE> { builder, annotation, _ ->
            builder.setPath(annotation.value)
        }
        addAnnotationParser<HEAD> { builder, annotation, _ ->
            builder.setPath(annotation.value)
        }
        addAnnotationParser<PATCH> { builder, annotation, _ ->
            builder.setPath(annotation.value)
        }
        addAnnotationParser<OPTIONS> { builder, annotation, _ ->
            builder.setPath(annotation.value)
        }
        addAnnotationParser<HTTP> { builder, annotation, _ ->
            builder.setPath(annotation.path)
        }
        addAnnotationParser<Url> { builder, _, parameter ->
            builder.setUrl(parameter as String)
        }
        addAnnotationParser<Query> { builder, annotation, parameter ->
            val name = annotation.value
            builder.addQueryParameter(name, parameter?.toString())
        }
        addAnnotationParser<QueryMap> { builder, _, parameter ->
            val parameters = parameter as Map<*, *>
            parameters.forEach { (name, value) ->
                builder.addQueryParameter(name.toString(), value?.toString())
            }
        }
        addAnnotationParser<Path> { builder, annotation, parameter ->
            val path = builder.path
            val placeholder = annotation.value
            val newPath = path.replace("%7B${placeholder}%7D", parameter.toString())
            builder.encodedPath(newPath)
        }
    }

    fun setRetrofit(retrofit: Retrofit): HttpUrlExtractor {
        return setBaseUrl(retrofit.baseUrl().toString())
    }

    fun setBaseUrl(baseUrl: String): HttpUrlExtractor {
        this.baseUrl = baseUrl
        return this
    }

    inline fun <reified T : Annotation> addAnnotationParser(noinline parser: (builder: HttpUrl.Builder, annotation: T, parameter: Any?) -> Unit): HttpUrlExtractor {
        return addAnnotationParser(T::class.java, parser)
    }

    fun <T : Annotation> addAnnotationParser(
        annotationClass: Class<out T>,
        parser: (builder: HttpUrl.Builder, annotation: T, parameter: Any?) -> Unit
    ): HttpUrlExtractor {
        val newParserChain = ParserChain(annotationClass, parser)
        if (firstParserChain == null) {
            firstParserChain = newParserChain
        } else {
            var chain = firstParserChain
            while (chain?.next != null) {
                chain = chain.next
            }
            chain?.next = newParserChain
        }
        return this
    }

    inline fun <reified T> extract(noinline invocation: suspend T.() -> Unit): String {
        return extract(T::class.java, invocation)
    }

    fun <T> extract(clazz: Class<T>, invocation: suspend T.() -> Unit): String {
        val builder = baseUrl.toHttpUrl().newBuilder()
        val proxy = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { _, method, args ->
            //解析类
            clazz.annotations.forEach { annotation ->
                doParse(builder, annotation)
            }
            //解析方法
            method.annotations.forEach { annotation ->
                doParse(builder, annotation)
            }
            //解析参数
            method.parameterAnnotations.forEachIndexed { index, annotations ->
                annotations.forEach { annotation ->
                    doParse(builder, annotation, args[index])
                }
            }
        }
        val atomicBoolean = AtomicBoolean()
        invocation.startCoroutine(clazz.cast(proxy), object : Continuation<Unit> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                atomicBoolean.set(true)
                result.onFailure {
                    throw it
                }
            }
        })
        while (!atomicBoolean.get()) {
            Thread.yield()
        }
        return builder.build().toString()
    }

    private fun doParse(builder: HttpUrl.Builder, annotation: Annotation, parameter: Any? = null) {
        val simpleName = annotation.annotationClass.simpleName
        if (simpleName == "Metadata") return
        var chain = firstParserChain
        while (chain != null && !chain.accept(annotation)) {
            chain = chain.next
        }
        chain?.parser?.invoke(builder, annotation, parameter)
    }

    private val HttpUrl.Builder.path: String
        get() {
            return javaClass.getDeclaredField("encodedPathSegments").run {
                isAccessible = true
                val segmentPath = get(this@path) as List<*>
                segmentPath.joinToString("/", prefix = "/")
            }
        }

    private fun HttpUrl.Builder.setPath(annotationValue: String) {
        var path = annotationValue
        if (path.isNotEmpty()) {
            if (!path.startsWith('/')) {
                path = "/$path"
            }
            encodedPath(path)
        }
    }

    private class ParserChain<out T : Annotation>(
        val annotationClass: Class<out T>,
        val parser: (builder: HttpUrl.Builder, annotation: @UnsafeVariance T, parameter: Any?) -> Unit
    ) {

        var next: ParserChain<Annotation>? = null

        fun accept(annotation: Annotation) = annotationClass.isInstance(annotation)

    }

}

