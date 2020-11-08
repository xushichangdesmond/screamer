package powerdancer.screamer

import kotlinx.coroutines.Job
import powerdancer.dsp.AudioPlayer
import powerdancer.dsp.Processor

object ScreamReceiver {
    fun run(): Job = Processor.process(
        TcpAudioSource(6789),
        AudioPlayer(2048)
    )
}