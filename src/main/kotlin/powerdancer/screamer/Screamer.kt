package powerdancer.screamer

import kotlinx.coroutines.runBlocking
import powerdancer.screamer.pdBedroom.Test

class Scream {
}

fun main(args:Array<String>) = runBlocking{
    when(args[0]) {
        "test" -> Test.run().join()
    }
}
