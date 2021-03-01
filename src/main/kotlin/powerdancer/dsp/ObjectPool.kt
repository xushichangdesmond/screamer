package powerdancer.dsp

import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

class ObjectPool<T>(maxInstances: Int = Integer.MAX_VALUE, val createFunc: ()->T) {
    companion object {
        val logger = LoggerFactory.getLogger(ObjectPool::class.java)
    }

    private val limiter = AtomicInteger(maxInstances) // once countdown to zero, no more creating of instances
    private val pool = Channel<T>(maxInstances)

    suspend fun take(): T {
        val t = pool.poll()
        if (t != null) {
            if (logger.isDebugEnabled) logger.debug("took from pool")
            return t
        }
        if (limiter.decrementAndGet() < 0) {
            if (logger.isDebugEnabled) logger.debug("maxInstances reached, wait from channel")
            return pool.receive()
        }
        if (logger.isDebugEnabled) logger.debug("creating new obj, countdown={}", limiter.get())
        return createFunc()
    }

    suspend fun put(t: T) {
        if (logger.isDebugEnabled) logger.debug("put into pool")
        pool.send(t)
    }
}