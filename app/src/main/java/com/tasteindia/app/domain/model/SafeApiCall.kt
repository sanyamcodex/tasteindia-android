package com.tasteindia.app.domain.model

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

inline suspend fun <T> safeApiCall(crossinline block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: AppError) {
        Result.failure(e)
    } catch (e: SocketTimeoutException) {
        Result.failure(AppError.Timeout(message = e.message ?: "Connection timed out", cause = e))
    } catch (e: TimeoutException) {
        Result.failure(AppError.Timeout(message = e.message ?: "Request timed out", cause = e))
    } catch (e: UnknownHostException) {
        Result.failure(AppError.Network(message = "Unable to resolve host. Check network connection.", cause = e))
    } catch (e: IOException) {
        Result.failure(AppError.Network(message = e.message ?: "Network communication failure", cause = e))
    } catch (e: HttpException) {
        Result.failure(AppError.ServerError(code = e.code(), message = e.message ?: "HTTP server error: ${e.code()}", cause = e))
    } catch (e: Throwable) {
        Result.failure(AppError.Unknown(message = e.message ?: "An unexpected error occurred", cause = e))
    }
}
