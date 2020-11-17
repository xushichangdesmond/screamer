package powerdancer.screamer

import kotlinx.coroutines.runBlocking
import powerdancer.screamer.pdBedroom.BedroomReceiver
import powerdancer.screamer.pdBedroom.BedroomTheater
import powerdancer.screamer.pdBedroom.Karaoke
import powerdancer.screamer.pdBedroom.Test

class Scream {
}

fun main(args:Array<String>) = runBlocking{
    when(args[0]) {
        "receiver" -> ScreamerReceiver.run().join()
        "bedroomReceiver" -> BedroomReceiver.run().join()
        "test" -> Test.run().join()
        "bedroom" -> BedroomTheater.run().join()
        "karaoke" -> Karaoke.run().join()
    }
}
