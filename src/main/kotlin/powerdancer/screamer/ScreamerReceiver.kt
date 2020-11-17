package powerdancer.screamer

import kotlinx.coroutines.Job
import powerdancer.dsp.Processor
import powerdancer.dsp.filter.impl.AudioPlayer
import powerdancer.dsp.filter.impl.ConfigurationFilter

object ScreamerReceiver {
    fun run(): Job {

        return Processor.process(
            ConfigurationFilter(),
            TcpAudioReceiver(),
            AudioPlayer(2048)
        )
    }
}