package com.mediaswipe.app.data

import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPhotoDeleter(private val context: Context) : PhotoDeleter {

    private val resolver get() = context.contentResolver

    override suspend fun requestDelete(uris: List<Uri>): DeleteOutcome = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteOutcome.Done(emptyList(), 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestDeleteApi30Plus(uris)
        } else {
            deleteRemainingApi29(uris, deletedSoFar = emptyList(), failureSoFar = 0)
        }
    }

    override suspend fun onConfirmationResult(
        resultOk: Boolean,
        remaining: List<Uri>,
        deletedSoFar: List<Uri>,
        failureSoFar: Int,
    ): DeleteOutcome = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (resultOk) {
                DeleteOutcome.Done(deleted = deletedSoFar + remaining, failureCount = failureSoFar)
            } else {
                DeleteOutcome.Cancelled(deletedSoFar, failureSoFar)
            }
        } else {
            if (!resultOk) {
                DeleteOutcome.Cancelled(deletedSoFar, failureSoFar + remaining.size)
            } else {
                deleteRemainingApi29(remaining, deletedSoFar, failureSoFar)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestDeleteApi30Plus(uris: List<Uri>): DeleteOutcome = try {
        val pendingIntent = MediaStore.createDeleteRequest(resolver, uris)
        DeleteOutcome.NeedsConfirmation(
            intentSender = pendingIntent.intentSender,
            remaining = uris,
            deletedSoFar = emptyList(),
            failureSoFar = 0,
        )
    } catch (t: Exception) {
        DeleteOutcome.Error(t)
    }

    private fun deleteRemainingApi29(uris: List<Uri>, deletedSoFar: List<Uri>, failureSoFar: Int): DeleteOutcome {
        val deleted = deletedSoFar.toMutableList()
        var failure = failureSoFar
        for (index in uris.indices) {
            val uri = uris[index]
            try {
                val rows = resolver.delete(uri, null, null)
                if (rows > 0) deleted += uri else failure++
            } catch (recoverable: RecoverableSecurityException) {
                return DeleteOutcome.NeedsConfirmation(
                    intentSender = recoverable.userAction.actionIntent.intentSender,
                    remaining = uris.subList(index, uris.size),
                    deletedSoFar = deleted.toList(),
                    failureSoFar = failure,
                )
            } catch (t: SecurityException) {
                failure++
            }
        }
        return DeleteOutcome.Done(deleted = deleted.toList(), failureCount = failure)
    }
}
