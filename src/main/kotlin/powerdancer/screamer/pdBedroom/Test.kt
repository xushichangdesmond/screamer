package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import powerdancer.dsp.*
import powerdancer.dsp.filter.impl.*
import powerdancer.screamer.ScreamMulticastAudioReceiver

object Test {
    fun run(): Job {
        return Processor.process(
            Channel(),
            ScreamMulticastAudioReceiver(),
            ToFloat64Converter(),
            VolumeMultiplier(0.1),
            FromFloat64Converter(2),
            AudioPlayer(2048)
        )
    }



}