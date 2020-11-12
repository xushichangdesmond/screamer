package powerdancer.dsp

import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger

class ObjectPool<T>(maxInstances: Int = Integer.MAX_VALUE, val createFunc: ()->T) {
    private val limiter = AtomicInteger(maxInstances) // once countdown to zero, no more creating of instances
    private val pool = Channel<T>(maxInstances)

    suspend fun take(): T {
        val t = pool.poll()
        if (t != null) return t
        if (limiter.decrementAndGet() < 0) {
            return pool.receive()
        }
        return createFunc()
    }

    suspend fun put(t: T) = pool.send(t)
}