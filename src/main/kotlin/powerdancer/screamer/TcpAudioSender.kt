package powerdancer.screamer

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.reactor.asFlux
import org.slf4j.LoggerFactory
import powerdancer.dsp.Worker
import reactor.core.publisher.Mono
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpClient
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import javax.sound.sampled.AudioFormat

class TcpAudioSender(val host: String, val port: Int): Worker {
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

    override suspend fun apply(format: AudioFormat, b: ByteBuffer): AudioFormat {
        val payloadSize = b.remaining() + 5
        val msg = ByteBuffer.allocate(payloadSize + 2)

        if (!format.equals(currentFormat)) {
            currentFormat = format
            bitSize = format.sampleSizeInBits.toByte()
            channels = format.channels.toByte()
            encodedSampleRate = encodeSampleRate(format.sampleRate.toInt())
        }

        msg.putShort(payloadSize.toShort())
            .put(encodedSampleRate)
            .put(bitSize)
            .put(channels)
            .put(0)
            .put(0)
            .put(b)
            .flip()

//        logger.info("sending {}", msg)
        socket().apply {
            getOutputStream().write(msg.array(), msg.position(), msg.limit())
        }

        return format
    }

    private val hexArray = "0123456789ABCDEF".toCharArray()

    fun toHex(b: Byte): String {
        val hexChars = CharArray(2)
        val v = b.toInt()
        hexChars[0] = hexArray[v and 0xF0 shr 4]
        hexChars[1] = hexArray[v and 0x0F]
        return String(hexChars)
    }

    fun encodeSampleRate(sampleRate: Int): Byte {
        return (if (sampleRate == 44100) 0x81.toByte()
            else (sampleRate / 48000).toByte())
    }

    override fun close() {
        output?.let {
            it.close()
        }
    }
}