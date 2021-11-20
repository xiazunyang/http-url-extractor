package cn.numeron.http

import okhttp3.HttpUrl

interface ExtractorScope {

    var HttpUrl.Builder.path: String

    var HttpUrl.Builder.url: String

}