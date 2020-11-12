package powerdancer.screamer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.slf4j.LoggerFactory
import powerdancer.dsp.event.Close
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.FormatChange
import powerdancer.dsp.event.PcmData
import powerdancer.dsp.filter.AbstractFilter
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class TcpAudioReceiver(val port: Int): AbstractFilter() {
    companion object {
        val logger = LoggerFactory.getLogger(TcpAudioReceiver::class.java)
    }

    val serverSocket = ServerSocket().apply {
        bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), port))
    }
    var socket: Socket? =  null

    val buf: ByteBuffer = ByteBuffer.allocate(3000).limit(0)

    var encodedSampleRate: Byte = 0
    var bitSize: Byte = 0
    var channels: Byte = 0

    override suspend fun onBump(): Flow<Event> = flow {
        if (buf.remaining() > 1) {
            val initialReadIndex = buf.position()
            val frameSize = buf.getShort()
            if (buf.remaining() >= frameSize) {
                var newEncodedSampleRate = buf.get()
                var newBitSize = buf.get()
                var newChannels = buf.get()
                buf.get()
                buf.get()

                if (
                    (encodedSampleRate != newEncodedSampleRate) ||
                    (bitSize != newBitSize) ||
                    (channels != newChannels)
                ) {
                    encodedSampleRate = newEncodedSampleRate
                    bitSize = newBitSize
                    channels = newChannels
                    emit(FormatChange(
                        ScreamUtils.audioFormat(ScreamUtils.decodeSampleRate(encodedSampleRate), bitSize.toInt(), channels.toInt())
                    ))
                }
                emit(PcmData(buf.slice(buf.position(), frameSize - 5)))
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

    override suspend fun onClose(): Flow<Event> {
        kotlin.runCatching {
            socket?.let {
                it.close()
            }
            serverSocket.close()
        }
        return flowOf(Close)
    }
}