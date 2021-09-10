package cn.numeron.http.urlextractor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import cn.numeron.http.HttpUrlExtractor
import cn.numeron.http.setUrl
import retrofit2.Response
import retrofit2.http.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        HttpUrlExtractor("http://192.168.3.213:6680/")
            .addAnnotationParser<Url> { builder, annotation, _ ->
                builder.setUrl(annotation.value)
            }
            .extract<TestApi> {
                post(123, "money")
            }
            .let {
                Log.e("MainActivity", it)
            }
    }
}

annotation class Port(val value: Int)

annotation class Url(val value: String)

@Url("http://google.com")
interface TestApi {

    @GET("/get")
    suspend fun get(@Query("aaa") value: String): Response<Unit?>

    @POST("post/{position}/{archive}")
    suspend fun post(
        @Path("position") position: Int,
        @Path("archive") archive: String?
    ): Response<Unit?>

    @DELETE("/delete")
    suspend fun delete(): Response<Unit?>

    @HEAD("/head")
    suspend fun head(): Response<Unit?>

    @PATCH("/patch")
    suspend fun patch(): Response<Unit?>

    @PUT("/put")
    suspend fun put(): Response<Unit?>

    @OPTIONS("/options")
    suspend fun options(): Response<Unit?>

    @HTTP(method = "GET", path = "/http")
    suspend fun http(): Response<Unit?>

    @GET
    suspend fun url(@retrofit2.http.Url url: String, @Path("money") money: Double): Response<Unit?>

}