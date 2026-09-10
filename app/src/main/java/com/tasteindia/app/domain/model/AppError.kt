package com.tasteindia.app.domain.model

sealed class AppError(override val message: String, override val cause: Throwable? = null) : Throwable(message, cause) {
    data class Network(override val message: String = "Network connection unavailable", override val cause: Throwable? = null) : AppError(message, cause)
    data class Timeout(override val message: String = "Request timed out", override val cause: Throwable? = null) : AppError(message, cause)
    data class ServerError(val code: Int? = null, override val message: String = "Server error occurred", override val cause: Throwable? = null) : AppError(message, cause)
    data class Unknown(override val message: String = "An unexpected error occurred", override val cause: Throwable? = null) : AppError(message, cause)
}
