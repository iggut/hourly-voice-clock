package com.hourlyvoiceclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Run the given [block] asynchronously after [BroadcastReceiver.onReceive]
 * returns. The block runs on [Dispatchers.Default] and is wrapped in a
 * try/catch that logs and a finally that calls
 * [android.content.BroadcastReceiver.PendingResult.finish] so the system
 * does not kill the receiver for taking too long.
 *
 * All three receivers in this app (AlarmReceiver, BootReceiver,
 * TimeChangedReceiver) used to repeat the same 8-line scaffold around
 * their actual work; this helper absorbs the boilerplate.
 *
 * Use it like:
 *
 * ```
 * class MyReceiver : BroadcastReceiver() {
 *     override fun onReceive(context: Context, intent: Intent?) {
 *         if (intent?.action != MY_ACTION) return
 *         launchAsync(context) { appContext ->
 *             // any suspend work, may call withContext(Main) for UI/TTS
 *         }
 *     }
 * }
 * ```
 *
 * The receiver passes the [Context] it received to [launchAsync] because
 * [BroadcastReceiver.getContext] is `protected` and cannot be reached
 * from an extension function.
 */
fun BroadcastReceiver.launchAsync(
    context: Context,
    block: suspend CoroutineScope.(appContext: Context) -> Unit
) {
    val pending = goAsync()
    val appContext = context.applicationContext
    CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
        try {
            block(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Receiver ${this@launchAsync.javaClass.simpleName} failed", e)
        } finally {
            pending.finish()
        }
    }
}

private const val TAG = "ReceiverAsync"
