package com.wanderlist.data.remote

import com.wanderlist.domain.model.AppError
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RemoteErrorMappingTest {
    private fun http(code: Int) =
        HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))

    @Test
    fun `io exception is Network`() {
        assertTrue(IOException("offline").toRemoteAppError() is AppError.Network)
    }

    @Test
    fun `429 and 5xx are Network with code`() {
        listOf(429, 500, 503).forEach { code ->
            val error = http(code).toRemoteAppError()
            assertTrue(error is AppError.Network)
            assertEquals(code, (error as AppError.Network).httpCode)
        }
    }

    @Test
    fun `400 and 404 are NotFound only when requested`() {
        assertTrue(http(400).toRemoteAppError(badRequestMeansNotFound = true) is AppError.NotFound)
        assertTrue(http(404).toRemoteAppError(badRequestMeansNotFound = true) is AppError.NotFound)
        assertTrue(http(404).toRemoteAppError() is AppError.Unknown)
    }

    @Test
    fun `auth errors are Unknown`() {
        assertTrue(http(401).toRemoteAppError(badRequestMeansNotFound = true) is AppError.Unknown)
        assertTrue(http(403).toRemoteAppError() is AppError.Unknown)
    }

    @Test
    fun `serialization error is Unknown and AppError passes through`() {
        assertTrue(SerializationException("bad").toRemoteAppError() is AppError.Unknown)
        val original = AppError.NotFound()
        assertEquals(original, original.toRemoteAppError())
    }
}
