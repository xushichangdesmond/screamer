package powerdancer.screamer

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import powerdancer.dsp.filter.AbstractTerminalFilter
import java.net.Socket
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class TcpAudioSender(val host: String, val port: Int): AbstractTerminalFilter() {
    companion object {
        val logger = LoggerFactory.getLogger(TcpAudioSender::class.java)
    }

    var output: Socket? = null

    var currentFormat: AudioFormat? = null
    var encodedSampleRate: Byte = 0
    var bitSize: Byte = 0
    var channels: Byte = 0

    suspend fun socket(): Socket {
        while (output == null) {
            output = runCatching {
                Socket(host, port)
            }.getOrNull()
            if (output == null) delay(1000L) else {
                logger.info("connected")
            }
        }
        return output!!
    }

    override suspend fun onFormatChange(format: AudioFormat) {
        bitSize = format.sampleSizeInBits.toByte()
        channels = format.channels.toByte()
        encodedSampleRate = ScreamUtils.encodeSampleRate(format.sampleRate.toInt())
    }

    override suspend fun onPcmData(b: ByteBuffer) {
        val payloadSize = b.remaining() + 5
        socket().getOutputStream().write(
            byteArrayOf(
                (payloadSize ushr 8).toByte(),
                payloadSize.toByte(),
                encodedSampleRate,
                bitSize,
                channels,
                0,
                0
            )
        )

        socket().getOutputStream().write(b.array(), b.position(), b.remaining())
    }


    override suspend fun onClose() {
        kotlin.runCatching {
            output?.let {
                it.close()
            }
        }
    }

}