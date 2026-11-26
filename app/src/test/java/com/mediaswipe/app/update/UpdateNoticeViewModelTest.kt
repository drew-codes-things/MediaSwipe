package com.mediaswipe.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class RememberedStore(override var checksEnabled: Boolean = true, override var dismissedVersion: String = "") : UpdateNoticeStore

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateNoticeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelFor(store: UpdateNoticeStore, latest: () -> String) =
        UpdateNoticeViewModel(UpdateNotice("1.2.0", store, latest, {}), dispatcher)

    @Test
    fun aNewerReleaseShowsTheNoticeAndDismissingRemembersThatVersion() = runTest(dispatcher) {
        val store = RememberedStore()
        val viewModel = viewModelFor(store) { "1.3.0" }
        viewModel.checkForUpdate()
        advanceUntilIdle()
        assertEquals("1.3.0", viewModel.state.value.availableVersion)
        viewModel.onDismiss()
        assertNull(viewModel.state.value.availableVersion)
        assertEquals("1.3.0", store.dismissedVersion)
    }

    @Test
    fun turningTheCheckOffHidesTheNoticeAndIsRemembered() = runTest(dispatcher) {
        val store = RememberedStore()
        val viewModel = viewModelFor(store) { "1.3.0" }
        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.onChecksChanged(false)
        assertNull(viewModel.state.value.availableVersion)
        assertFalse(viewModel.state.value.checksEnabled)
        assertFalse(store.checksEnabled)
        assertFalse(viewModelFor(store) { "1.3.0" }.state.value.checksEnabled)
    }
}
