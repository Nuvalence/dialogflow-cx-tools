package io.nuvalence.cx.tools.shared

import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Convenient wrapper to parallelize and throttle blocks of code. It will parallelize the execution
 * but waiting for periodMs between calls, even if there are threads available.
 *
 * @param poolSize thread pool size used for throttling
 * @param initialDelayMs initial wait period in msec before start executing
 * @param periodMs minimum wait time between requests
 */
class Throttler(poolSize: Int, private val initialDelayMs: Long = 0, private val periodMs: Long): AutoCloseable {
    private val throttler = Executors.newScheduledThreadPool(poolSize)

    /**
     * Submits the block of code to the throttler.
     *
     * @param block the block of code
     */
    fun throttle(block: () -> Unit) {
        throttler.scheduleAtFixedRate(block, initialDelayMs, periodMs, TimeUnit.MILLISECONDS)
    }

    /**
     * Throttles the application of a transformation to a list, returning a list with results
     * that match the order of the original list.
     *
     * @param sources the list with elements to process
     * @param transform the block of code that transforms a list element
     *
     * @return a transformed list matching the order of the input list
     */
    fun <T, R> throttle(sources: List<T>, transform: (T) -> R): List<R> {
        val results = ConcurrentSkipListMap<Int, R>()
        val countDownLatch = CountDownLatch(sources.size)
        sources.forEachIndexed { index, source ->
            throttle {
                results[index] = transform(source)
                countDownLatch.countDown()
            }
        }
        countDownLatch.await()
        return results.values.toList()
    }

    override fun close() {
        throttler.shutdownNow()
    }

}