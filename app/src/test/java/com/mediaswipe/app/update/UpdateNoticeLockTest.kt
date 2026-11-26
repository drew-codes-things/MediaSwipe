package com.mediaswipe.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis
import org.junit.Test
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

private const val FETCH_HOLD_MS = 1_500L
private const val QUICK_MS = 500L
private const val SETTLE_ROUNDS = 50
private const val SETTLE_STEP_MS = 20L

private class SlowStore(override var checksEnabled: Boolean = true, override var dismissedVersion: String = "") : UpdateNoticeStore

private class HeldFetch(private val version: String = "1.5.0") {
    val started = CountDownLatch(1)
    private val release = CountDownLatch(1)

    fun fetch(): String {
        started.countDown()
        release.await()
        return version
    }

    fun releaseAfter(millis: Long) = thread {
        Thread.sleep(millis)
        release.countDown()
    }
}

class UpdateNoticeLockTest {
    @Test
    fun dismissingDoesNotWaitForAFetchInFlight() {
        val held = HeldFetch()
        val notice = UpdateNotice("1.4.0", SlowStore(), held::fetch, {})
        val checking = thread { notice.checkIfDue() }
        assertTrue(held.started.await(2, TimeUnit.SECONDS))
        held.releaseAfter(FETCH_HOLD_MS)
        val tookMillis = measureTimeMillis { notice.dismiss() }
        checking.join()
        assertTrue("dismiss took $tookMillis ms", tookMillis < QUICK_MS)
    }

    @Test
    fun turningChecksOffDuringAFetchLeavesNoBanner() {
        val store = SlowStore()
        val held = HeldFetch()
        val notice = UpdateNotice("1.4.0", store, held::fetch, {})
        var shown: String? = "unset"
        val checking = thread { shown = notice.checkIfDue() }
        assertTrue(held.started.await(2, TimeUnit.SECONDS))
        held.releaseAfter(FETCH_HOLD_MS)
        val tookMillis = measureTimeMillis { notice.turnChecks(false) }
        checking.join()
        assertTrue("turnChecks took $tookMillis ms", tookMillis < QUICK_MS)
        assertNull(shown)
        assertNull(notice.availableVersion())
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateNoticeViewModelLockTest {
    private val main = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(main)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun settle() = repeat(SETTLE_ROUNDS) {
        main.scheduler.advanceUntilIdle()
        Thread.sleep(SETTLE_STEP_MS)
    }

    private fun startCheck(held: HeldFetch, store: SlowStore): UpdateNoticeViewModel {
        val viewModel = UpdateNoticeViewModel(UpdateNotice("1.2.0", store, held::fetch, {}), Dispatchers.IO)
        viewModel.checkForUpdate()
        main.scheduler.runCurrent()
        assertTrue(held.started.await(2, TimeUnit.SECONDS))
        held.releaseAfter(FETCH_HOLD_MS)
        return viewModel
    }

    @Test
    fun turningTheCheckOffDuringAFetchLeavesNoBanner() {
        val held = HeldFetch()
        val viewModel = startCheck(held, SlowStore())
        val tookMillis = measureTimeMillis { viewModel.onChecksChanged(false) }
        Thread.sleep(FETCH_HOLD_MS)
        settle()
        assertTrue("the switch took $tookMillis ms", tookMillis < QUICK_MS)
        assertNull(viewModel.state.value.availableVersion)
    }

    @Test
    fun dismissingDuringAFetchReturnsAtOnce() {
        val held = HeldFetch()
        val viewModel = startCheck(held, SlowStore())
        val tookMillis = measureTimeMillis { viewModel.onDismiss() }
        Thread.sleep(FETCH_HOLD_MS)
        settle()
        assertTrue("dismiss took $tookMillis ms", tookMillis < QUICK_MS)
    }
}
