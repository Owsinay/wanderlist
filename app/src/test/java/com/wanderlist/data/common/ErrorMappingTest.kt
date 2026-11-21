package com.wanderlist.data.common

import android.database.sqlite.SQLiteException
import com.wanderlist.domain.model.AppError
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ErrorMappingTest {
    @Test
    fun `io exception maps to network error`() {
        assertTrue(IOException().toAppError() is AppError.Network)
    }

    @Test
    fun `sqlite exception maps to database error`() {
        assertTrue(SQLiteException().toAppError() is AppError.Database)
    }

    @Test
    fun `app error is passed through unchanged`() {
        val error = AppError.NotFound()
        assertSame(error, error.toAppError())
    }

    @Test
    fun `runCatchingApp wraps failures as AppError`() {
        val result = runCatchingApp { throw IllegalStateException() }
        assertTrue(result.exceptionOrNull() is AppError.Unknown)
    }

    @Test(expected = CancellationException::class)
    fun `runCatchingApp does not swallow cancellation`() {
        runCatchingApp { throw CancellationException() }
    }
}
