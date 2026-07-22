package com.swipeswipe.app.data

import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Isolates the SDK_INT branching for permanent photo/video removal: API 30+
 * gets a single batch delete request, API 29 falls back to per-item deletes
 * with per-item system consent. Both paths are permanent - there is no
 * recoverable trash API pre-30, and we don't use the system trash on 30+
 * either (the app manages its own Recently Deleted staging area instead).
 */
class AndroidPhotoDeleter(private val context: Context) : PhotoDeleter {

    private val resolver get() = context.contentResolver

    override suspend fun requestDelete(uris: List<Uri>): DeleteOutcome = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteOutcome.Done(0, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestDeleteApi30Plus(uris)
        } else {
            deleteRemainingApi29(uris, successSoFar = 0, failureSoFar = 0)
        }
    }

    override suspend fun onConfirmationResult(
        resultOk: Boolean,
        remaining: List<Uri>,
        successSoFar: Int,
        failureSoFar: Int,
    ): DeleteOutcome = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (resultOk) {
                DeleteOutcome.Done(successCount = remaining.size, failureCount = 0)
            } else {
                DeleteOutcome.Cancelled(successSoFar, failureSoFar)
            }
        } else {
            if (!resultOk) {
                DeleteOutcome.Cancelled(successSoFar, failureSoFar + remaining.size)
            } else {
                deleteRemainingApi29(remaining, successSoFar, failureSoFar)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestDeleteApi30Plus(uris: List<Uri>): DeleteOutcome = try {
        val pendingIntent = MediaStore.createDeleteRequest(resolver, uris)
        DeleteOutcome.NeedsConfirmation(
            intentSender = pendingIntent.intentSender,
            remaining = uris,
            successSoFar = 0,
            failureSoFar = 0,
        )
    } catch (t: Exception) {
        DeleteOutcome.Error(t)
    }

    private fun deleteRemainingApi29(uris: List<Uri>, successSoFar: Int, failureSoFar: Int): DeleteOutcome {
        var success = successSoFar
        var failure = failureSoFar
        for (index in uris.indices) {
            val uri = uris[index]
            try {
                val rows = resolver.delete(uri, null, null)
                if (rows > 0) success++ else failure++
            } catch (recoverable: RecoverableSecurityException) {
                return DeleteOutcome.NeedsConfirmation(
                    intentSender = recoverable.userAction.actionIntent.intentSender,
                    remaining = uris.subList(index, uris.size),
                    successSoFar = success,
                    failureSoFar = failure,
                )
            } catch (t: SecurityException) {
                failure++
            }
        }
        return DeleteOutcome.Done(successCount = success, failureCount = failure)
    }
}
