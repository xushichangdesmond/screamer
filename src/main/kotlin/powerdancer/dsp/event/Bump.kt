package powerdancer.dsp.event

object Bump: Event {
    override suspend fun clone(): Pair<Event, suspend () -> Unit> = Bump to {}
}