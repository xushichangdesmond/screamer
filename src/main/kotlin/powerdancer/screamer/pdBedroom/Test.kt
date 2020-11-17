package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

object Test {
    fun run(): Job {
        return GlobalScope.launch {

        }
    }
}

fun main() {
    runBlocking { Test.run().join() }
}