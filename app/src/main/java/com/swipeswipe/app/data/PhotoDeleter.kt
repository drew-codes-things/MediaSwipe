package com.swipeswipe.app.data

import android.content.IntentSender
import android.net.Uri

interface PhotoDeleter {

    suspend fun requestDelete(uris: List<Uri>): DeleteOutcome

    suspend fun onConfirmationResult(
        resultOk: Boolean,
        remaining: List<Uri>,
        successSoFar: Int,
        failureSoFar: Int,
    ): DeleteOutcome
}

sealed interface DeleteOutcome {

    data class NeedsConfirmation(
        val intentSender: IntentSender,
        val remaining: List<Uri>,
        val successSoFar: Int,
        val failureSoFar: Int,
    ) : DeleteOutcome

    data class Done(val successCount: Int, val failureCount: Int) : DeleteOutcome

    data class Cancelled(val successSoFar: Int, val failureSoFar: Int) : DeleteOutcome

    data class Error(val cause: Throwable) : DeleteOutcome
}
