package powerdancer.screamer

import org.slf4j.LoggerFactory
import powerdancer.dsp.old.Worker
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class TcpAudioSource(val port: Int): Worker {
    companion object {
        val logger = LoggerFactory.getLogger(TcpAudioSource::class.java)
    }

    val serverSocket = ServerSocket().apply {
        bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), port))
    }
    var socket: Socket? =  null

    val buf: ByteBuffer = ByteBuffer.allocate(3000).limit(0)

    var currentFormat: AudioFormat = AudioFormat(
        44100F,
        16,
        1,
        true,
        false
    )
    var encodedSampleRate: Byte = 0
    var bitSize: Byte = 0
    var channels: Byte = 0

    override suspend fun apply(format: AudioFormat, b: ByteBuffer): AudioFormat {
        if (buf.remaining() > 1) {
            val initialReadIndex = buf.position()
            val frameSize = buf.getShort()
            if (buf.remaining() >= frameSize) {
                var newEncodedSampleRate = buf.get()
                var newBitSize = buf.get()
                var newChannels = buf.get()
                buf.get()
                buf.get()

                if ((currentFormat == null) ||
                    (encodedSampleRate != newEncodedSampleRate) ||
                    (bitSize != newBitSize) ||
                    (channels != newChannels)
                ) {
                    encodedSampleRate = newEncodedSampleRate
                    bitSize = newBitSize
                    channels = newChannels
                    currentFormat = audioFormat(decodeSampleRate(encodedSampleRate), bitSize.toInt(), channels.toInt())
                }
                val p = b.position()
                b.limit(b.position() + frameSize - 5)
                    .put(buf.array(), buf.position(), frameSize - 5)
                    .position(p)
                buf.position(buf.position() + frameSize - 5)
                buf.compact()
                    .limit(buf.position())
                    .position(0)
            } else {
                buf.position(initialReadIndex)
                buf.compact()
                val read = socket().getInputStream().read(buf.array(), buf.position(), buf.capacity() - buf.position())
                buf.limit(buf.position() + read).position(0)
            }
        } else {
            buf.compact()
            val read = read(buf.array(), buf.position(), buf.capacity() - buf.position())
            buf.limit(buf.position() + read).position(0)
        }
        return currentFormat
    }

    fun decodeSampleRate(encodedSampleRate: Byte): Int {
        return (encodedSampleRate.toInt() and 0x7f) * if (encodedSampleRate.toInt() and 0x80 == 0) 48000 else 44100
    }

    fun audioFormat(sampleRate: Int, bitSize: Int, channels: Int): AudioFormat {
        return AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate.toFloat(),
            bitSize,
            channels,
            (bitSize + 7) / 8 * channels,
            sampleRate.toFloat(),
            false
        ).apply {
            logger.info("sampleRate {}, bitSize{}, channels{}", sampleRate, bitSize, channels)
        }
    }

    fun socket(): Socket {
        if (socket == null) {
            socket = serverSocket.accept()
        }
        return socket!!
    }

    private fun read(
        dest: ByteArray,
        offset: Int,
        length: Int
    ): Int {
        return try {
            socket().getInputStream().read(dest, offset, length)
        } catch(e: IOException) {
            runCatching { socket!!.close() }
            socket = null
            0
        }
    }

    override fun close() {
        socket?.let {
            it.close()
        }
        serverSocket.close()
    }
}