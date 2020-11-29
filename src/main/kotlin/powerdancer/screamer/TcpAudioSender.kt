package powerdancer.screamer

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import org.slf4j.LoggerFactory
import powerdancer.dsp.filter.AbstractTerminalFilter
import java.lang.Exception
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.sound.sampled.AudioFormat
import kotlin.coroutines.coroutineContext

class TcpAudioSender(val host: String, val port: Int = 6789): AbstractTerminalFilter() {
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
        connectionJob = CoroutineScope(Dispatchers.Default).launch {
            ticker(1000).receiveAsFlow().collect {
                if (output.get() == null) {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(InetAddress.getByName(host), port), 200)
                        socket.soTimeout = 1000
                        output.set(socket)
                        logger.info("connected")
                    } catch (e:Exception) {
//                        logger.error(e.message, e)
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