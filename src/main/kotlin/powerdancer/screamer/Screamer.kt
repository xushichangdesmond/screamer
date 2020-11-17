package powerdancer.screamer

import kotlinx.coroutines.runBlocking
import powerdancer.dsp.Processor
import powerdancer.dsp.filter.impl.AudioPlayer
import powerdancer.screamer.pdBedroom.BedroomReceiver
import powerdancer.screamer.pdBedroom.BedroomTheater
import powerdancer.screamer.pdBedroom.Karaoke
import powerdancer.screamer.pdBedroom.Test

class Scream {
}

fun main(args:Array<String>) = runBlocking{
    if (args.size == 0) {
        Processor.process(
            ScreamMulticastAudioReceiver(),
            AudioPlayer(2048)
        ).join()
    } else {
        when(args[0]) {
            "receiver" -> ScreamerReceiver.run().join()
            "bedroomReceiver" -> BedroomReceiver.run().join()
            "test" -> Test.run().join()
            "bedroom" -> BedroomTheater.run().join()
            "karaoke" -> Karaoke.run().join()
        }
    }

}
