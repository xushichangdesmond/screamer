package powerdancer.dsp.filter.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.FormatChange
import powerdancer.dsp.event.Init
import powerdancer.dsp.event.PcmData
import powerdancer.dsp.filter.AbstractFilter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine

class CapturingDeviceReceiver(
    val format: AudioFormat = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        44100f,
        16,
        2,
        4,44100f,false
    ),
    mixerName: String?
): AbstractFilter() {

    val buf = ByteBuffer.allocate(10000).order(
        if (format.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
    )
    val input: TargetDataLine = if (mixerName == null) {
        AudioSystem.getTargetDataLine(
            format,
            null
        )
    } else {
        AudioSystem.getTargetDataLine(
            format,
            AudioSystem.getMixerInfo().firstOrNull { it.name==mixerName }
        )
    }
    val m = AudioSystem.getMixer(AudioSystem.getMixerInfo().firstOrNull { it.name==mixerName })

    override suspend fun onInit(): Flow<Event> {
        m.targetLineInfo.forEach {
            println(it)
        }
        input.open(format)
        input.start()
        return flowOf(Init, FormatChange(input.format))
    }

    override suspend fun onBump(): Flow<Event> {
        buf.clear()
        buf.limit(input.read(buf.array(), 0, buf.capacity()))
        return flowOf(PcmData(buf))
    }

}