## http-url-extractor

当你们使用`Retrofit`，并且你们的后台服务提供了多个端口的服务，在某些时候，你可能会需要拿到一个`Retrofit Api`接口具体的HttpUrl地址；面对着接口上的注解们，你陷入了深思：我TM是真的不想拼接字符串。这时，这个项目的价值就体现出来了！


### 原理

通过动态代理，模拟`Retrofit`创建`API`实例，解析接口上的注解，由用户自行决定调用的方法，并提供对应的参数，最终处理成正确的`HttpUrl`。

### 安装

当前最新版本：[![](https://jitpack.io/v/cn.numeron/http-url-extractor.svg)](https://jitpack.io/#cn.numeron/http-url-extractor)

1. 在根模块的`build.gradle`的适当位置添加以下代码：
    ```kotlin
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
   ```

2. 在业务模块的`build.gradle`文件中添加以下代码：
    ```kotlin
    dependencies {
        implementation 'cn.numeron:http-url-extractor:latest_version'
    }
    ```

### 使用
    //Retrofit Api接口
    interface TestApi {

        @GET("/get")
        suspend fun get(@Query("aaa") value: String): Response<Unit?>

        @POST("post/{position}/{archive}")
        suspend fun post(
            @Path("position") position: Int,
            @Path("archive") archive: String?
        ): Response<Unit?>

    }
    //使用`baseUrl`或`Retrofit`实例即可初始化，调用`extract`方法即可获取到对应方法的`url`
    val httpUrlExtractor = HttpUrlExtractor("http://192.168.3.213:6680/")
        /* 可通过此方法支持自定义注解的解析
        .addAnnotationParser<YourAnnotation> { builder, annotation, parameter ->
            TODO("执行你的解析操作，并设置为builder设置解析结果")
        }
        */

    val getUrl = httpUrlExtractor.extract<TestApi> {
        //执行想要获取到HttpUrl的方法
        get("bbb")
    }
    //getUrl = "http://192.168.3.213:6680/get?aaa=bbb"

    val postUrl = httpUrlExtractor.extract<TestApi> {
        post(123, "money")
    }
    //postUrl = "http://192.168.3.213:6680/post/123/money"

	
