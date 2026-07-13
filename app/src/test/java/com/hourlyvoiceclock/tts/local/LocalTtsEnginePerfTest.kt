package com.hourlyvoiceclock.tts.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class LocalTtsEnginePerfTest {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun benchmarkWaitMethodsConcurrent() = runBlocking {
        val durationMs = 10L
        val concurrentTasks = 20
        val singleWorker = Dispatchers.IO.limitedParallelism(1)

        // Baseline: Thread.sleep blocks the thread
        val sleepTime = measureTimeMillis {
            val jobs = List(concurrentTasks) {
                launch(singleWorker) {
                    Thread.sleep(durationMs)
                }
            }
            jobs.forEach { it.join() }
        }

        // Optimized: delay suspends, freeing the thread
        val delayTime = measureTimeMillis {
            val jobs = List(concurrentTasks) {
                launch(singleWorker) {
                    delay(durationMs)
                }
            }
            jobs.forEach { it.join() }
        }

        println("Baseline time (Thread.sleep) for $concurrentTasks concurrent tasks: $sleepTime ms")
        println("Optimized time (delay) for $concurrentTasks concurrent tasks: $delayTime ms")
        assertTrue("delay should be faster than Thread.sleep under concurrency",
            delayTime < sleepTime)
    }
}
