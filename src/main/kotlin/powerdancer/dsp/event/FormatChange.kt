package powerdancer.dsp.event

import javax.sound.sampled.AudioFormat

data class FormatChange(
    val format: AudioFormat
): Event {
    override suspend fun clone(): Pair<Event, suspend () -> Unit> = this to {}
}