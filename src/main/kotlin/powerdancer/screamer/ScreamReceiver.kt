package powerdancer.screamer

import kotlinx.coroutines.Job
import powerdancer.dsp.AudioPlayer
import powerdancer.dsp.Processor
import powerdancer.dsp.SizedBuffer

object ScreamReceiver {
    fun run(): Job = Processor.process(
        TcpAudioSource(6789),
        SizedBuffer(20000),
        AudioPlayer(2048)
    )
}