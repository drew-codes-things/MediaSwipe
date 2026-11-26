package com.mediaswipe.app.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UpdateNoticeState(val availableVersion: String? = null, val checksEnabled: Boolean = true)

class UpdateNoticeViewModel(
    private val notice: UpdateNotice,
    private val network: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateNoticeState(checksEnabled = notice.checksEnabled))
    val state: StateFlow<UpdateNoticeState> = _state.asStateFlow()

    fun checkForUpdate() {
        viewModelScope.launch {
            withContext(network) { notice.checkIfDue() }
            _state.update { it.copy(availableVersion = notice.availableVersion()) }
        }
    }

    fun onDismiss() {
        notice.dismiss()
        _state.update { it.copy(availableVersion = null) }
    }

    fun onChecksChanged(on: Boolean) {
        notice.turnChecks(on)
        _state.update { it.copy(checksEnabled = on, availableVersion = notice.availableVersion()) }
        if (on) checkForUpdate()
    }
}
