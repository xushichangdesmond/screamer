package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import powerdancer.dsp.*
import powerdancer.dsp.filter.impl.AudioFileLoader
import powerdancer.dsp.filter.impl.AudioPlayer
import java.io.File

object Test {
    fun run(): Job {
        return Processor.process(
            Channel(),
            AudioFileLoader(File("try.wav")),
            AudioPlayer(2048)
        )
    }



}