package com.hourlyvoiceclock.tts.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class LocalTtsEnginePerfTest {

    `@Test`
    fun benchmarkWaitMethodsConcurrent() = runBlocking {
        val durationMs = 50L
        val concurrentTasks = 100 // Simulate 100 concurrent playback waits

        // Baseline: Thread.sleep blocks the thread
        val sleepTime = measureTimeMillis {
            val jobs = List(concurrentTasks) {
                launch(Dispatchers.IO) {
                    Thread.sleep(durationMs)
                }
            }
            jobs.forEach { it.join() }
        }

        // Optimized: delay suspends, freeing the thread
        val delayTime = measureTimeMillis {
            val jobs = List(concurrentTasks) {
                launch(Dispatchers.IO) {
                    delay(durationMs)
                }
            }
            jobs.forEach { it.join() }
        }

        println("Baseline time (Thread.sleep) for 100 concurrent tasks: $sleepTime ms")
        println("Optimized time (delay) for 100 concurrent tasks: $delayTime ms")
        assertTrue("delay should be faster than Thread.sleep under concurrency",
            delayTime < sleepTime)
    }
}
