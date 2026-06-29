package com.example.chessboard.concurrency

/*
 * File role: verifies the generic bounded parallel task runner contract.
 * Allowed here:
 * - unit tests for task submission, completion, failure, ordering ids, and cancellation behavior
 * - coroutine synchronization primitives that make runner behavior deterministic
 * Not allowed here:
 * - PGN import, UI, database, or feature-specific parallel processing tests
 * Validation date: 2026-06-29
 */

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedParallelTaskRunnerTest {
    // Checks that a full runner rejects new work instead of queuing it internally.
    @Test
    fun `trySubmit rejects task when all slots are active`() = runBlocking {
        val runner = testRunner<String>(parallelism = 1)
        val firstTaskStarted = CompletableDeferred<Unit>()
        val allowFirstTaskToFinish = CompletableDeferred<Unit>()
        var rejectedTaskCalled = false

        val firstAccepted =
            runner.trySubmit(taskId = 1) {
                firstTaskStarted.complete(Unit)
                allowFirstTaskToFinish.await()
                "first"
            }
        firstTaskStarted.await()

        val secondAccepted =
            runner.trySubmit(taskId = 2) {
                rejectedTaskCalled = true
                "second"
            }

        assertTrue(firstAccepted)
        assertFalse(secondAccepted)
        assertFalse(rejectedTaskCalled)

        allowFirstTaskToFinish.complete(Unit)
        runner.receiveCompleted()
        Unit
    }

    // Checks that completed successful work carries both caller id and callback value.
    @Test
    fun `receiveCompleted returns submitted task id and value`() = runBlocking {
        val runner = testRunner<String>(parallelism = 1)

        val accepted =
            runner.trySubmit(taskId = 7) {
                "done"
            }
        val completed = runner.receiveCompleted()

        assertTrue(accepted)
        when (completed) {
            is CompletedTask.Success -> {
                assertEquals(7, completed.taskId)
                assertEquals("done", completed.value)
            }

            is CompletedTask.Failure -> throw AssertionError("Expected successful task")
        }
        Unit
    }

    // Checks that finishing one task frees its slot for later submissions.
    @Test
    fun `trySubmit accepts new task after completed task is received`() = runBlocking {
        val runner = testRunner<String>(parallelism = 1)

        val firstAccepted =
            runner.trySubmit(taskId = 1) {
                "first"
            }
        val firstCompleted = runner.receiveCompleted()
        waitForActiveCount(runner, activeCount = 0)
        val secondAccepted =
            runner.trySubmit(taskId = 2) {
                "second"
            }
        val secondCompleted = runner.receiveCompleted()

        assertTrue(firstAccepted)
        assertEquals(1, firstCompleted.taskId)
        assertTrue(secondAccepted)
        assertEquals(2, secondCompleted.taskId)
        Unit
    }

    // Checks that completion order is independent from submission order while task ids are preserved.
    @Test
    fun `receiveCompleted emits completion order while preserving task ids`() = runBlocking {
        val runner = testRunner<String>(parallelism = 2)
        val firstTaskStarted = CompletableDeferred<Unit>()
        val allowFirstTaskToFinish = CompletableDeferred<Unit>()

        val firstAccepted =
            runner.trySubmit(taskId = 0) {
                firstTaskStarted.complete(Unit)
                allowFirstTaskToFinish.await()
                "first"
            }
        firstTaskStarted.await()
        val secondAccepted =
            runner.trySubmit(taskId = 1) {
                "second"
            }

        val firstCompleted = runner.receiveCompleted()
        allowFirstTaskToFinish.complete(Unit)
        val secondCompleted = runner.receiveCompleted()

        assertTrue(firstAccepted)
        assertTrue(secondAccepted)
        assertEquals(1, firstCompleted.taskId)
        assertEquals(0, secondCompleted.taskId)
        Unit
    }

    // Checks that callback failures are emitted as task failures instead of escaping the runner.
    @Test
    fun `failed task is returned as failure`() = runBlocking {
        val runner = testRunner<String>(parallelism = 1)
        val expectedError = IllegalStateException("boom")

        val accepted =
            runner.trySubmit(taskId = 5) {
                throw expectedError
            }
        val completed = runner.receiveCompleted()

        assertTrue(accepted)
        when (completed) {
            is CompletedTask.Success -> throw AssertionError("Expected failed task")
            is CompletedTask.Failure -> {
                assertEquals(5, completed.taskId)
                assertSame(expectedError, completed.error)
            }
        }
        Unit
    }

    // Checks that invalid parallelism is rejected before any task can be submitted.
    @Test
    fun `constructor rejects non-positive parallelism`() {
        assertInvalidParallelism(0)
        assertInvalidParallelism(-1)
    }

    // Checks that explicit cancellation stops running tasks and releases the active slot.
    @Test
    fun `cancelActiveTasks cancels running tasks and frees active count`() = runBlocking {
        val runner = testRunner<String>(parallelism = 1)
        val taskStarted = CompletableDeferred<Unit>()
        val taskCancelled = CompletableDeferred<Unit>()

        val accepted =
            runner.trySubmit(taskId = 1) {
                try {
                    taskStarted.complete(Unit)
                    awaitCancellation()
                } finally {
                    taskCancelled.complete(Unit)
                }
            }
        taskStarted.await()

        runner.cancelActiveTasks()
        taskCancelled.await()
        waitForActiveCount(runner, activeCount = 0)

        assertTrue(accepted)
        assertEquals(0, runner.activeCount)
        Unit
    }

    private fun <Result> CoroutineScope.testRunner(parallelism: Int): BoundedParallelTaskRunner<Result> {
        return BoundedParallelTaskRunner(
            parallelism = parallelism,
            dispatcher = Dispatchers.Default,
            scope = this,
        )
    }

    private suspend fun waitForActiveCount(
        runner: BoundedParallelTaskRunner<*>,
        activeCount: Int,
    ) {
        withTimeout(5_000) {
            while (runner.activeCount != activeCount) {
                yield()
            }
        }
    }

    private fun assertInvalidParallelism(parallelism: Int) {
        try {
            BoundedParallelTaskRunner<String>(
                parallelism = parallelism,
                dispatcher = Dispatchers.Default,
                scope = CoroutineScope(Dispatchers.Default),
            )
        } catch (_: IllegalArgumentException) {
            return
        }

        throw AssertionError("Expected parallelism $parallelism to be rejected")
    }
}
