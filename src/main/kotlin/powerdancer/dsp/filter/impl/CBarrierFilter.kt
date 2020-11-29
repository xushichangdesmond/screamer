package powerdancer.dsp.filter.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import powerdancer.dsp.event.Bump
import powerdancer.dsp.event.Event
import powerdancer.dsp.filter.AbstractFilter
import java.util.concurrent.CyclicBarrier

class CBarrierFilter(parties:Int): AbstractFilter() {
    val b = CyclicBarrier(parties)

    override suspend fun onBump(): Flow<Event> {
        b.await()
        return flowOf(Bump)
    }
}