package powerdancer.dsp.filter.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import powerdancer.dsp.HexUtils
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.FormatChange
import powerdancer.dsp.filter.AbstractFilter
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import javax.sound.sampled.AudioFormat

class ImpulseLogger(val logger: Logger = LoggerFactory.getLogger(ImpulseLogger::class.java)): AbstractFilter() {

    lateinit var format: AudioFormat

    override suspend fun onFormatChange(format: AudioFormat): Flow<Event> {
        this.format = format
        return flowOf(FormatChange(format))
    }

    override suspend fun onPcmData(data: ByteBuffer): Flow<Event> {
        data.mark()
        while(data.hasRemaining()) {
            if (format.sampleSizeInBits == 16)
                logger.info("short impulse ${data.getShort()}")
            else if (format.sampleSizeInBits == 24)
                logger.info("24bit impulse ${HexUtils.bytesToHex(data.get(), data.get(), data.get())}")
            else if (format.sampleSizeInBits == 32)
                logger.info("int impulse ${data.getInt()}")
        }
        data.reset()
        return super.onPcmData(data)
    }

    override suspend fun onFloat64PcmData(data: Array<DoubleBuffer>): Flow<Event> {
        data.forEachIndexed { i, b ->
            b.mark()
            while(b.hasRemaining()) {
                logger.info("channel $i - double impulse ${b.get()}")
            }
            b.reset()
        }

        return super.onFloat64PcmData(data)
    }
}