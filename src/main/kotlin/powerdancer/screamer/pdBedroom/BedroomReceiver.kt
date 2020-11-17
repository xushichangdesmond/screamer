package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.Job
import powerdancer.dsp.Processor
import powerdancer.dsp.filter.impl.AudioPlayer
import powerdancer.dsp.filter.impl.ConfigurationFilter
import powerdancer.screamer.AlsaMixerVolumeControl
import powerdancer.screamer.TcpAudioReceiver

object BedroomReceiver {
    fun run(): Job {

        return Processor.process(
            ConfigurationFilter(),
            TcpAudioReceiver(),
            AlsaMixerVolumeControl("'DAC'"),
            AudioPlayer(2048)
        )
    }
}