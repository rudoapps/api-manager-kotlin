package com.example.api_manager.ui

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.api_manager.App
import com.example.api_manager.R
import com.example.api_manager.model.User
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            val pathParameters = listOf(
                Pair("id", 1)
            )
            val queryParameters = listOf(
                Pair("role", "admin"),
                Pair("id", 1)
            )
            val me = App.apiManager.getMe()
            when (me.responseCode) {
                in 200..300 -> {
                    findViewById<TextView>(R.id.name_text).apply {
                        text = me.result?.name
                    }
                }
                null -> {
                    //exception error code gestion}
                    me.exceptionError?.let { Log.e("EXCEPTION_ERROR", it) }
                }
                else -> {
                    //api error gestion}
                    me.apiError?.error?.descripcion?.let { Log.e("API_ERROR", "${me.responseCode} $it") }
                }
            }
            App.apiManager.tryPost(
                bodyParam = User(email = "asdas@asd.com", name = "asasd")
            )
        }
    }
}