package powerdancer.screamer

import org.slf4j.LoggerFactory
import powerdancer.dsp.Worker
import powerdancer.screamer.old.ScreamerClient
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class ScreamerMulticastAudioSource: Worker {
    companion object {
        val logger = LoggerFactory.getLogger(ScreamerMulticastAudioSource::class.java)
    }

    val screamAddress = InetSocketAddress(InetAddress.getByName("239.255.77.77"), 4010)
    val screamSocket = MulticastSocket(4010).apply {
        joinGroup(screamAddress, null)
    }

    var encodedSampleRate: Byte = 0
    var bitSize: Byte = 0
    var channels: Byte = 0
    var format: AudioFormat? = null

    override suspend fun apply(f: AudioFormat, buf: ByteBuffer): AudioFormat {
        val originalPosition = buf.position()
        val packet = DatagramPacket(buf.array(), buf.position(), 1157)

        var newEncodedSampleRate = buf.get()
        var newBitSize = buf.get()
        var newChannels = buf.get()

        if ((format == null) ||
                (encodedSampleRate != newEncodedSampleRate) ||
                (bitSize != newBitSize) ||
                (channels != newChannels)
        ) {
            encodedSampleRate = newEncodedSampleRate
            bitSize = newBitSize
            channels = newChannels
            format = audioFormat(decodeSampleRate(encodedSampleRate), bitSize.toInt(), channels.toInt())
        }

        buf.position(originalPosition + 5)
        return format!!
    }

    fun decodeSampleRate(encodedSampleRate: Byte): Int {
        return (encodedSampleRate.toInt() and 0x4f) * if (encodedSampleRate.toInt() and 0x80 == 0) 48000 else 44100
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
}