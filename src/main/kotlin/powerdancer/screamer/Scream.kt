package powerdancer.screamer

import kotlinx.coroutines.runBlocking
import powerdancer.screamer.pdBedroom.Bedroom
import powerdancer.screamer.pdBedroom.Test

class Scream {
}

fun main(args:Array<String>) = runBlocking{
    if (args[0] == "receiver") {
        ScreamReceiver.run().join()
    } else if(args[0] == "bedroom") {
        Bedroom.run().join()
    } else if(args[0] == "test") {
        Test.run().join()
    } else {

    }
}
