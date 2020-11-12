package powerdancer.dsp.event

interface Event {
    suspend fun clone(): Pair<Event, suspend ()->Unit> // make sure to call right hand side to clean up the event
}