package powerdancer.dsp.event

data class ConfigPush(
    val key: String,
    val value: String
): Event {
    override suspend fun clone(): Pair<Event, suspend () -> Unit> {
        return this to {}
    }
}