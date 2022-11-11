package com.example.api_manager

import android.annotation.SuppressLint
import android.app.Application
import com.example.api_manager.api.APIManager

class App : Application() {
    companion object {
        lateinit var instance: App private set

        @SuppressLint("StaticFieldLeak")
        val apiManager: APIManager = APIManager()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}