package powerdancer.screamer

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import org.slf4j.LoggerFactory
import powerdancer.dsp.filter.AbstractTerminalFilter
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.sound.sampled.AudioFormat
import kotlin.coroutines.coroutineContext

class TcpAudioSender(val host: String, val port: Int): AbstractTerminalFilter() {
    companion object {
        val logger = LoggerFactory.getLogger(TcpAudioSender::class.java)
    }

    val output: AtomicReference<Socket?> = AtomicReference(null)

    var currentFormat: AudioFormat? = null
    var encodedSampleRate: Byte = 0
    var bitSize: Byte = 0
    var channels: Byte = 0

    var connectionJob: Job? = null

    override suspend fun onInit() {
        connectionJob = CoroutineScope(coroutineContext).launch {
            ticker(1000).receiveAsFlow().collect {
                if (output.get() == null) {
                    val socket = runCatching {
                        Socket(host, port)
                    }.getOrNull()
                    if (socket != null) {
                        output.set(socket)
                        logger.info("connected")
                    }
                }
            }
        }
    }

    override suspend fun onFormatChange(format: AudioFormat) {
        bitSize = format.sampleSizeInBits.toByte()
        channels = format.channels.toByte()
        encodedSampleRate = ScreamUtils.encodeSampleRate(format.sampleRate.toInt())
    }

    override suspend fun onPcmData(b: ByteBuffer) {
        val payloadSize = b.remaining() + 5
        output.get()?.getOutputStream()?.let {
            runCatching {
                it.write(
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

                it.write(b.array(), b.position(), b.remaining())
            }.getOrElse {
                output.set(null)
                logger.error(it.message, it)
            }
        }

    }


    override suspend fun onClose() {
        kotlin.runCatching {
            connectionJob?.let { it.cancel() }

            output.get()?.let {
                it.close()
            }
        }
    }

}