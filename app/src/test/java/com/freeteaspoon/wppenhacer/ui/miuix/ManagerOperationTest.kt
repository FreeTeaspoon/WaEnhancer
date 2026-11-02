package com.freeteaspoon.wppenhacer.ui.miuix

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ManagerOperationTest {
    @Test fun nextOperationWaitsForPopupExit() = runBlocking {
        val operation = ManagerOperation()
        operation.attach()
        val first = launch(start = CoroutineStart.UNDISPATCHED) { operation.run("Importing") {} }
        assertTrue(operation.busy.value)
        assertFalse(operation.visible.value)
        var secondStarted = false
        val second = launch(start = CoroutineStart.UNDISPATCHED) { operation.run("Exporting") { secondStarted = true } }
        assertFalse(secondStarted)
        operation.dismissed()
        first.join()
        yield()
        assertTrue(secondStarted)
        assertTrue(operation.busy.value)
        operation.dismissed()
        second.join()
        assertFalse(operation.busy.value)
        operation.detach()
    }

    @Test fun cancellationReleasesOwnershipAndDetachedHostCannotDeadlock() = runBlocking {
        val operation = ManagerOperation()
        operation.attach()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { operation.run("Saving") { awaitCancellation() } }
        job.cancelAndJoin()
        assertFalse(operation.busy.value)
        operation.detach()
        withTimeout(1000) { operation.run("Retrying") {} }
        assertFalse(operation.busy.value)
    }
}
