package com.example.api_manager.model

data class Response<T>(
    var result: T? = null,
    val responseCode: Int? = null,
    val exceptionError: String? = null,
    var apiError: ApiError? = null
)

data class ApiError(
    val error: Error
)

data class Error(
    val descripcion: String
)
