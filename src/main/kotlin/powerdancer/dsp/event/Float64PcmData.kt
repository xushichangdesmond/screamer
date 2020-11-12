package powerdancer.dsp.event

import powerdancer.dsp.ObjectPool
import java.nio.DoubleBuffer

data class Float64PcmData(
    val data: Array<DoubleBuffer> // separate channels in separate buffers
): Event {
    companion object {
        private val bufferPool = ObjectPool<DoubleBuffer> {
            DoubleBuffer.allocate(2000)
        }

        suspend fun takeBufferFromPool(minSize: Int): DoubleBuffer {
            val b = bufferPool.take()
            if (b.capacity() < minSize) {
                return DoubleBuffer.allocate(minSize * 2)
            } else {
                return b
            }
        }

        suspend fun putBufferIntoPool(b: DoubleBuffer) {
            bufferPool.put(b)
        }
    }

    override suspend fun clone(): Pair<Event, suspend () -> Unit> {
        val output = Array<DoubleBuffer>(data.size) { i->
            data[i].mark()
            val copy = takeBufferFromPool(data[i].remaining())
                .clear()
                .put(data[i])
                .flip()
            data[i].reset()
            copy
        }
        return Float64PcmData(output) to {
            output.forEach { putBufferIntoPool(it) }
        }
    }
}