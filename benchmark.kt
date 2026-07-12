import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

fun main() = runBlocking {
    val durationToWaitMs = 1000L

    // Simulating the original code using Thread.sleep (which blocks)
    val timeThreadSleep = measureTimeMillis {
        Thread.sleep(durationToWaitMs)
    }

    // Simulating the new code using coroutines delay (which doesn't block)
    val timeCoroutineDelay = measureTimeMillis {
        delay(durationToWaitMs)
    }

    println("Thread.sleep time: $timeThreadSleep ms")
    println("delay time: $timeCoroutineDelay ms")
}
