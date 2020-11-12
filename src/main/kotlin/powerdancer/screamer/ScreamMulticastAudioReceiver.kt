package powerdancer.screamer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import org.slf4j.LoggerFactory
import powerdancer.dsp.ObjectPool
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.FormatChange
import powerdancer.dsp.event.PcmData
import powerdancer.dsp.filter.AbstractFilter
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat

class ScreamMulticastAudioReceiver: AbstractFilter() {
    companion object {
        val logger = LoggerFactory.getLogger(ScreamMulticastAudioReceiver::class.java)
        val screamAddress = InetSocketAddress(InetAddress.getByName("239.255.77.77"), 4010)
        val screamSocket = MulticastSocket(4010).apply {
            joinGroup(screamAddress, null)
        }
    }

    val buf = ByteBuffer.allocate(1157).order(ByteOrder.LITTLE_ENDIAN)
    var encodedSampleRate: Byte = 0
    var bitSize: Byte = 0
    var channels: Byte = 0
    lateinit var format: AudioFormat

    override suspend fun onBump(): Flow<Event> = flow {

        buf.clear()
        
        val packet = DatagramPacket(buf.array(), 1157)
        screamSocket.receive(packet)
        buf.limit(packet.length)

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
            emit(
                FormatChange(
                    ScreamUtils.audioFormat(
                        ScreamUtils.decodeSampleRate(encodedSampleRate),
                        bitSize.toInt(),
                        channels.toInt()
                    )
                )
            )
        }
        emit(PcmData(buf))
    }

}