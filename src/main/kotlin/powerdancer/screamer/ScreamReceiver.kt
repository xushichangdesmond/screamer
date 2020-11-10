package powerdancer.screamer

import kotlinx.coroutines.Job
import powerdancer.dsp.old.AudioPlayer
import powerdancer.dsp.old.Processor

object ScreamReceiver {
    fun run(): Job = Processor.process(
        TcpAudioSource(6789),
        AudioPlayer(2048)
    )
}