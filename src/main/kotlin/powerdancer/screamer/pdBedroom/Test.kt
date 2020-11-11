package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import powerdancer.dsp.*
import powerdancer.dsp.filter.impl.AudioPlayer
import powerdancer.screamer.ScreamMulticastAudioReceiver

object Test {
    fun run(): Job {
        return Processor.process(
            Channel(),
            ScreamMulticastAudioReceiver(),
            AudioPlayer(2048)
        )
    }



}