package com.example.api_manager

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.api_manager.model.ApiError
import com.example.api_manager.model.Response
import com.example.api_manager.model.User
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Constants {
    //const val BASE_URL = "https://rickandmortyapi.com/api/"
    const val BASE_URL = "http://192.168.10.66:3000/"
    const val TIME_OUT = 30000
}

class MainActivity : AppCompatActivity() {

    private val logRequests: Queue<() -> Unit> = LinkedList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        doGet()
        doPost()
    }

    private fun doPost() {

        val headers = listOf(
            Pair("Content-Type", "application/json"),
            Pair("charset", "utf-8")
        )

        lifecycleScope.launch {
            val characterResponsePost =
                doACall<User>(endpoint = "user", bodyParam = User(name = "asasd"), method = "POST", headers = headers)


            while (logRequests.isNotEmpty()) {
                logRequests.first().invoke()
                logRequests.remove()
            }

            when (characterResponsePost.responseCode) {
                in 200..300 -> {}
                null -> {
                    //exception error code gestion}
                    characterResponsePost.exceptionError?.let { Log.e("EXCEPTION_ERROR", it) }
                }
                else -> {
                    //api error gestion}
                    characterResponsePost.apiError?.error?.descripcion?.let { Log.e("API_ERROR", "${characterResponsePost.responseCode} $it") }
                }
            }
        }
    }

    private fun doGet() {
        val headers = listOf(
            Pair("Content-Type", "application/json")
        )
        val pathParameters = listOf(
            Pair("id", 1)
        )
        val queryParameters = listOf(
            Pair("role", "admin"),
            Pair("id", 1)
        )
        lifecycleScope.launch {
            val characterResponse =
                doACall<User>(endpoint = "user/me", headers = headers, pathParams = pathParameters, queryParams = queryParameters)

            when (characterResponse.responseCode) {
                in 200..300 -> {
                    findViewById<TextView>(R.id.name_text).apply {
                        text = characterResponse.result?.name
                    }
                }
                null -> {
                    //exception error code gestion}
                    characterResponse.exceptionError?.let { Log.e("EXCEPTION_ERROR", it) }
                }
                else -> {
                    //api error gestion}
                    characterResponse.apiError?.error?.descripcion?.let { Log.e("API_ERROR", "${characterResponse.responseCode} $it") }
                }
            }
        }
    }

    private suspend inline fun <reified T> doACall(
        endpoint: String,
        headers: List<Pair<String, String>>? = null,
        pathParams: List<Pair<String, Any>>? = null,
        queryParams: List<Pair<String, Any>>? = null,
        bodyParam: Any? = null,
        method: String? = "GET"
    ): Response<T> =
        suspendCoroutine { cont ->
            lateinit var response: Response<T>
            lifecycleScope.launch {
                var connection: HttpURLConnection? = null
                var parameterizedEndpoint: String

                try {
                    parameterizedEndpoint = putPathParams(pathParams, endpoint)
                    parameterizedEndpoint = putQueryParams(queryParams, parameterizedEndpoint)

                    val url = URL(Constants.BASE_URL + parameterizedEndpoint)

                    withContext(Dispatchers.IO) {
                        val starTime = System.currentTimeMillis()

                        connection = url.openConnection() as HttpURLConnection
                        connection?.configureConnection(method)
                        connection?.addHeaders(headers)

                        logRequests.add { requestLog(connection, headers, bodyParam) }

                        if (method == "POST") {
                            connection?.putBodyParam(bodyParam)
                        }

                        response = Response(responseCode = connection?.responseCode)

                        val bufferedReader = response.responseCode?.let { connection?.selectStreamToRead(it) }
                        val jsonStringHolder = readResponseBuffer(bufferedReader)
                        formatResult(response, jsonStringHolder)

                        val elapsedTime = System.currentTimeMillis() - starTime
                        logRequests.add { responseLog(connection, jsonStringHolder,elapsedTime) }

                        cont.resume(response)
                    }
                } catch (ioException: IOException) {
                    response = Response(exceptionError = ioException.toString())
                    cont.resume(response)
                } finally {
                    connection?.disconnect()
                }
            }
        }

    private fun requestLog(
        connection: HttpURLConnection?,
        headers: List<Pair<String, String>>?,
        bodyParam: Any?
    ) {
        val gson = GsonBuilder().setPrettyPrinting().create()

        Log.d("HTTP-REQUEST", "┌────── HTTP REQUEST ────────────────────────────────────────────────────────────────────────")
        Log.d("HTTP-REQUEST", "│ ${connection?.requestMethod} ${connection?.url.toString()}")
        Log.d("HTTP-REQUEST", "│")
        Log.d("HTTP-REQUEST", "│ Headers: $headers\n")
        Log.d("HTTP-REQUEST", "│")
        if (bodyParam == null) {
            Log.d("HTTP-REQUEST", "│ Omitted request body")
        } else {
            Log.d("HTTP-REQUEST", "│ RequestBody:")
            gson.toJson(bodyParam).split("\n").forEach {
                Log.d("HTTP-REQUEST", "│ $it")
            }
        }
        Log.d("HTTP-REQUEST", "│")
        Log.d("HTTP-REQUEST", "└─────────────────────────────────────────────────────────────────────────")
    }

    private fun responseLog(
        connection: HttpURLConnection?,
        jsonStringHolder: StringBuilder,
        elapsedTime: Long
    ) {
        val gson = GsonBuilder().setPrettyPrinting().create()

        if(connection?.responseCode in 200..300)
        {
            Log.d("HTTP-RESPONSE", "┌────── HTTP RESPONSE ────────────────────────────────────────────────────────────────────────")
            Log.d("HTTP-RESPONSE", "│ ${connection?.requestMethod} ${connection?.url.toString()}")
            Log.d("HTTP-RESPONSE", "│")
            Log.d("HTTP-RESPONSE", "│ STATUS CODE: ${connection?.responseCode} - Received in: ${elapsedTime}ms")
            Log.d("HTTP-RESPONSE", "│")
            Log.d("HTTP-RESPONSE", "│")
            Log.d("HTTP-RESPONSE", "│")
            Log.d("HTTP-RESPONSE", "│ Body:")
            Log.d("HTTP-RESPONSE", "│ [")
            gson.toJson(gson.fromJson(jsonStringHolder.toString(),JsonObject::class.java)).split("\n").forEach {
                Log.d("HTTP-RESPONSE", "│      $it")
            }
            Log.d("HTTP-RESPONSE", "│ ]")
            Log.d("HTTP-RESPONSE", "│")
            Log.d("HTTP-RESPONSE", "└─────────────────────────────────────────────────────────────────────────")
        }else{
            Log.e("HTTP-RESPONSE", "┌────── HTTP RESPONSE ────────────────────────────────────────────────────────────────────────")
            Log.e("HTTP-RESPONSE", "│ ${connection?.requestMethod} ${connection?.url.toString()}")
            Log.e("HTTP-RESPONSE", "│")
            Log.e("HTTP-RESPONSE", "│ STATUS CODE: ${connection?.responseCode} - Received in: ${elapsedTime}ms")
            Log.e("HTTP-RESPONSE", "│")
            Log.e("HTTP-RESPONSE", "│")
            Log.e("HTTP-RESPONSE", "│")
            Log.e("HTTP-RESPONSE", "│ Body:")
            Log.e("HTTP-RESPONSE", "│ [")
            gson.toJson(gson.fromJson(jsonStringHolder.toString(),JsonObject::class.java)).split("\n").forEach {
                Log.e("HTTP-RESPONSE", "│      $it")
            }
            Log.e("HTTP-RESPONSE", "│ ]")
            Log.e("HTTP-RESPONSE", "│")
            Log.e("HTTP-RESPONSE", "└─────────────────────────────────────────────────────────────────────────")
        }

    }
    
    private inline fun <reified T> formatResult(response: Response<T>, jsonStringHolder: StringBuilder) {
        if (response.responseCode in 200..300) {
            val parsedResponse = Gson().fromJson(jsonStringHolder.toString(), T::class.java)
            response.result = parsedResponse
        } else {
            val parsedResponse = Gson().fromJson(jsonStringHolder.toString(), ApiError::class.java)
            response.apiError = parsedResponse
        }
    }

    private fun readResponseBuffer(bufferedReader: BufferedReader?): StringBuilder {
        val jsonStringHolder = StringBuilder()
        while (true) {
            val readLine = bufferedReader?.readLine() ?: break
            jsonStringHolder.append(readLine)
        }
        return jsonStringHolder
    }


    private fun HttpURLConnection.selectStreamToRead(responseCode: Int): BufferedReader =
        BufferedReader(
            if (responseCode in 200..300)
                InputStreamReader(this.inputStream) else
                InputStreamReader(this.errorStream)
        )


    private fun HttpURLConnection.putBodyParam(bodyParam: Any? = null) {
        val outputStreamWriter = OutputStreamWriter(this.outputStream)
        outputStreamWriter.write(Gson().toJson(bodyParam))
        outputStreamWriter.flush()
    }

    private fun HttpURLConnection.configureConnection(method: String?) {
        this.requestMethod = method
        this.connectTimeout = Constants.TIME_OUT
        if (method == "POST") {
            this.doOutput = true
            this.doInput = true
        }
    }

    private fun HttpURLConnection.addHeaders(headers: List<Pair<String, String>>?) {
        headers?.forEach { header ->
            this.setRequestProperty(header.first, header.second)
        }
    }

    private fun putQueryParams(queryParams: List<Pair<String, Any>>?, pathParameterizedEndpoint: String): String {
        var parameterizedEndpoint: String = pathParameterizedEndpoint
        queryParams?.forEachIndexed { i, param ->
            parameterizedEndpoint += if (i == 0)
                "?${param.first}=${param.second}" else
                "&${param.first}=${param.second}"
        }
        return parameterizedEndpoint
    }

    private fun putPathParams(pathParameters: List<Pair<String, Any>>?, endpoint: String): String {
        var parameterizedEndpoint: String = endpoint
        pathParameters?.forEach { param ->
            parameterizedEndpoint = parameterizedEndpoint.replace("{${param.first}}", param.second.toString())
        }
        return parameterizedEndpoint
    }
}