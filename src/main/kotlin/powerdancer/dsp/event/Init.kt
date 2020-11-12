package powerdancer.dsp.event

object Init: Event {
    override suspend fun clone(): Pair<Event, suspend () -> Unit> = Init to {}
}