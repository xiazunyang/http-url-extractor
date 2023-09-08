package cn.numeron.http

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.internal.notifyAll
import okhttp3.internal.wait
import retrofit2.Retrofit
import retrofit2.http.Url
import retrofit2.http.*
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class HttpUrlExtractor(private var baseUrl: String) {

    constructor(retrofit: Retrofit) : this(retrofit.baseUrl().toString())

    private var firstParserChain: ParserChain<Annotation>? = null

    private val extractorScope = HttpUrlExtractorScope()

    init {
        addAnnotationParser<GET> { builder, annotation, _ ->
            builder.path = annotation.value
        }
        addAnnotationParser<POST> { builder, annotation, _ ->
            builder.path = annotation.value
        }
        addAnnotationParser<PUT> { builder, annotation, _ ->
            builder.path = annotation.value
        }
        addAnnotationParser<DELETE> { builder, annotation, _ ->
            builder.path = annotation.value
        }
        addAnnotationParser<HEAD> { builder, annotation, _ ->
            builder.path = annotation.value
        }
        addAnnotationParser<PATCH> { builder, annotation, _ ->
            builder.path = annotation.value
        }
        addAnnotationParser<OPTIONS> { builder, annotation, _ ->
            builder.path = annotation.value
        }
        addAnnotationParser<HTTP> { builder, annotation, _ ->
            builder.path = annotation.path
        }
        addAnnotationParser<Url> { builder, _, parameter ->
            builder.url = (parameter as String)
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

    inline fun <reified T : Annotation> addAnnotationParser(noinline parser: ExtractorScope.(builder: HttpUrl.Builder, annotation: T, parameter: Any?) -> Unit): HttpUrlExtractor {
        return addAnnotationParser(T::class.java, parser)
    }

    fun <T : Annotation> addAnnotationParser(
        annotationClass: Class<out T>,
        parser: ExtractorScope.(builder: HttpUrl.Builder, annotation: T, parameter: Any?) -> Unit
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
        val completion = object : Continuation<Unit> {
            private var result: Result<Unit>? = null
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                synchronized(this) {
                    this.result = result
                    notifyAll()
                }
            }

            fun await() {
                synchronized(this) {
                    while (result == null) {
                        wait()
                    }
                }
            }
        }
        invocation.startCoroutine(clazz.cast(proxy), completion)
        completion.await()
        return builder.build().toString()
    }

    private fun doParse(builder: HttpUrl.Builder, annotation: Annotation, parameter: Any? = null) {
        val simpleName = annotation.annotationClass.simpleName
        if (simpleName == "Metadata") return
        var chain = firstParserChain
        while (chain != null && !chain.accept(annotation)) {
            chain = chain.next
        }
        chain?.parser?.invoke(extractorScope, builder, annotation, parameter)
    }

    private class ParserChain<out T : Annotation>(
        val annotationClass: Class<out T>,
        val parser: ExtractorScope.(builder: HttpUrl.Builder, annotation: @UnsafeVariance T, parameter: Any?) -> Unit
    ) {

        var next: ParserChain<Annotation>? = null

        fun accept(annotation: Annotation) = annotationClass.isInstance(annotation)

    }

}

