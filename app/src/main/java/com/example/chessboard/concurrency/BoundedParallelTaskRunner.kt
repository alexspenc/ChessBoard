package com.example.chessboard.concurrency

/*
 * File role: provides a small coroutine runner for bounded parallel task execution.
 * Allowed here:
 * - generic concurrency helpers that limit active work and report completed task results
 * - coroutine-based task orchestration that is independent from chess, UI, and persistence models
 * Not allowed here:
 * - PGN parsing, screen workflow logic, database access, or feature-specific import behavior
 * Validation date: 2026-06-29
 */

import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Runs caller-submitted tasks while keeping at most [parallelism] task callbacks active at once.
 *
 * The runner does not keep a waiting queue for tasks. [trySubmit] accepts a task only when a slot is
 * free at the moment of the call; otherwise it returns false and leaves the task with the caller.
 *
 * @param Result result type produced by successful tasks.
 * @param parallelism maximum number of task callbacks that may run at the same time.
 * @param dispatcher coroutine dispatcher used to execute accepted task callbacks.
 * @param scope parent coroutine scope for launched task jobs; cancelling the scope cancels active tasks.
 */
class BoundedParallelTaskRunner<Result>(
    parallelism: Int,
    private val dispatcher: CoroutineDispatcher,
    private val scope: CoroutineScope,
) {
    private val parallelism = parallelism.also { value ->
        require(value > 0) {
            "parallelism must be positive"
        }
    }
    private val activeTaskCount = AtomicInteger(0)
    private val completedTasks = Channel<CompletedTask<Result>>(capacity = this.parallelism)
    private val activeJobs = Collections.synchronizedSet(mutableSetOf<Job>())

    /**
     * Number of accepted tasks that have not yet finished or been cancelled.
     *
     * The value is intended for diagnostics and coarse control flow. It may change immediately after
     * it is read because task jobs complete concurrently.
     */
    val activeCount: Int
        get() = activeTaskCount.get()

    /**
     * Returns whether [trySubmit] is likely to accept a task at this instant.
     *
     * This is a snapshot rather than a reservation. Callers should still handle a false return value
     * from [trySubmit] because another caller may take the slot first.
     */
    fun hasFreeSlot(): Boolean = activeTaskCount.get() < parallelism

    /**
     * Attempts to start [task] with the provided stable [taskId].
     *
     * Returns true when the task was accepted and launched. Returns false when all slots are already
     * active; in that case [task] is not invoked. The [taskId] is copied into the completed result so
     * callers can restore their own ordering after tasks finish out of order.
     */
    fun trySubmit(
        taskId: Int,
        task: suspend () -> Result,
    ): Boolean {
        if (!reserveSlot()) {
            return false
        }

        val job =
            scope.launch(dispatcher, start = CoroutineStart.LAZY) {
                try {
                    completedTasks.send(
                        CompletedTask.Success(
                            taskId = taskId,
                            value = task(),
                        ),
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    completedTasks.send(
                        CompletedTask.Failure(
                            taskId = taskId,
                            error = error,
                        ),
                    )
                } finally {
                    activeTaskCount.decrementAndGet()
                }
            }

        activeJobs.add(job)
        job.invokeOnCompletion { activeJobs.remove(job) }
        val started = job.start()
        if (!started) {
            activeJobs.remove(job)
            activeTaskCount.decrementAndGet()
            return false
        }

        return true
    }

    /**
     * Suspends until one accepted task reports a completed result.
     *
     * Results are emitted in completion order, not submission order. Use [CompletedTask.taskId] when a
     * caller needs to sort values back into the original input order.
     */
    suspend fun receiveCompleted(): CompletedTask<Result> = completedTasks.receive()

    /**
     * Cancels all currently active task jobs and clears the active job set.
     *
     * Cancelled tasks do not emit [CompletedTask] values. This method does not close the completed
     * result channel, so a runner instance should not be reused after cancelling active work.
     */
    fun cancelActiveTasks() {
        val jobsSnapshot =
            synchronized(activeJobs) {
                activeJobs.toList().also { activeJobs.clear() }
            }
        jobsSnapshot.forEach { job -> job.cancel() }
    }

    private fun reserveSlot(): Boolean {
        while (true) {
            val currentCount = activeTaskCount.get()
            if (currentCount >= parallelism) {
                return false
            }

            if (activeTaskCount.compareAndSet(currentCount, currentCount + 1)) {
                return true
            }
        }
    }
}

/** Result emitted by [BoundedParallelTaskRunner] when an accepted task finishes without cancellation. */
sealed interface CompletedTask<out Result> {
    /** Stable caller-provided id for matching the completed task to its original input. */
    val taskId: Int

    /** Successful completion of a task callback. */
    data class Success<Result>(
        override val taskId: Int,
        val value: Result,
    ) : CompletedTask<Result>

    /** Failed completion of a task callback. */
    data class Failure(
        override val taskId: Int,
        val error: Throwable,
    ) : CompletedTask<Nothing>
}
