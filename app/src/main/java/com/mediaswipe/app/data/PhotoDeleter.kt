package com.mediaswipe.app.data

import android.content.IntentSender
import android.net.Uri

interface PhotoDeleter {

    suspend fun requestDelete(uris: List<Uri>): DeleteOutcome

    suspend fun onConfirmationResult(
        resultOk: Boolean,
        remaining: List<Uri>,
        deletedSoFar: List<Uri>,
        failureSoFar: Int,
    ): DeleteOutcome
}

sealed interface DeleteOutcome {

    data class NeedsConfirmation(
        val intentSender: IntentSender,
        val remaining: List<Uri>,
        val deletedSoFar: List<Uri>,
        val failureSoFar: Int,
    ) : DeleteOutcome

    data class Done(val deleted: List<Uri>, val failureCount: Int) : DeleteOutcome {
        val successCount: Int get() = deleted.size
    }

    data class Cancelled(val deletedSoFar: List<Uri>, val failureSoFar: Int) : DeleteOutcome

    data class Error(val cause: Throwable) : DeleteOutcome
}
