package powerdancer.dsp.event

object Close: Event {
    override suspend fun clone(): Pair<Event, suspend () -> Unit> = Close to {}
}