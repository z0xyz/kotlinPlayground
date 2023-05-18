package core

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class Connection(private val kotlinVersion: String) {

    private var baseUrl: String = "https://api.kotlinlang.org//api/${kotlinVersion}/"
    private var arguments: String = ""

    private interface KotlinApiInterface {
        @POST("compiler/run")
        fun sendData(@Body requestBody: RequestBody): Call<JsonObject>;
    }

    /*
        - okHttp interceptor
            - It entails modifications that can be applied to all retrofit requests e.g., modifying the headers for the request
     */
    private val okHttpClient = OkHttpClient().newBuilder()
        .addInterceptor { chain ->
            val modifiedRequest = chain.request().newBuilder()
                .headers(
                    okhttp3.Headers.of(
                        "user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36",
                        "content-type", "application/json; charset=UTF-8"
                    )
                )
                .build()
            chain.proceed(modifiedRequest)
            //            .header(okhttp3.Headers.of(headers).toString(), "")
        }.build()

    // Create a retrofit instance
    private fun sendPostRequest(sourceCode: String, callback: (JsonObject?) -> Unit): Unit {
        // Creating a retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Creating an instance of our API interface
        val apiService = retrofit.create(KotlinApiInterface::class.java)

        // Making an API call and handling the response
        val call = apiService.sendData(
            RequestBody.create(
                MediaType.parse("application/json"),
                Gson().toJson(
                    mapOf(
                        "args" to "${this.arguments}",
                        "confType" to "java",
                        "files" to listOf(
                            mapOf(
                                "name" to "File.kt",
                                "publicId" to "",
                                "text" to "${sourceCode}"
                            )
                        )
                    )
                )
            )
        )

        call.enqueue(object : retrofit2.Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: retrofit2.Response<JsonObject>) {
                if (response.code() == 200) {
                    callback(response.body())
                } else if (response.code() == 404) {
                }
            }

            override fun onFailure(call: Call<JsonObject>, throwable: Throwable) {
                println("Error: ${throwable.message}")
            }
        })
    }

    /*
        - Obtaining the response from the server, then parse it according to its status e.g., valid, compile time error, runtime error.
        - As of now the output is being bring printed out from within the same callback, but the encompassing method in general should implement some for of
        callback or something i.e., onValid, onCompileTimeError, onException.
     */
    private fun parseResponse(sourceCode: String) {
        this.sendPostRequest(sourceCode) { response ->
            val jsonObject: JsonObject = Gson().fromJson(response, JsonObject::class.java)
            if (jsonObject.get("text").asString.isEmpty()) {     // Mitigate compile time errors
                println("Compile Time error")
                // Handling compile time errors
                println(jsonObject.get("errors").asJsonObject)
//                val message: String = exceptions.get("message").asString
//                    println(message)
            } else {
                if (jsonObject.get("exception").toString() == "null") {
                    val codeOutput: String =
                        jsonObject.get("text").asString.replace("<outStream>", "").replace(Regex("<(/)?outStream>"), "")
                    println(codeOutput)
                } else {    // Handling runtime errors
//                    val exceptions: JsonObject =
                    val stackTrace: JsonArray = jsonObject.get("exception")
                        .asJsonObject.
                        getAsJsonArray("stackTrace")
                    /*
                        - I guess this part should be refactored to use data classes, instead of a map
                     */
                    for (element in stackTrace) {
                        val exception = element.asJsonObject
                        val what = mapOf<String, String>(
                            "className" to exception.get("className").asString,
                            "methodName" to exception.get("methodName").asString,
                            "fileName" to exception.get("fileName").toString(),
                            "lineNumber" to exception.get("lineNumber").asString
                        )
                        println(what)
//                        println("at ${className}.${methodName} (${fileName}:${lineNumber})")
                    }
                }
            }
        }
    }

    fun setArguments(arguments: String) {
        this.arguments = arguments
    }

    fun run(sourceCode: String) {
        parseResponse(sourceCode)
    }

}
