package powerdancer.screamer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.slf4j.LoggerFactory
import powerdancer.dsp.event.ConfigPush
import powerdancer.dsp.event.Event
import powerdancer.dsp.filter.AbstractFilter

class AlsaMixerVolumeControl(val controlName: String, val controlKey: String = "alsaVol"): AbstractFilter() {
    companion object {
        val logger = LoggerFactory.getLogger(AlsaMixerVolumeControl::class.java)
    }

    override suspend fun onConfigPush(key: String, value: String): Flow<Event> {
        if (key == controlKey) {
            try {
                Runtime.getRuntime().exec("amixer sset $controlName $value")
            }catch (e:Exception) {
                logger.error("error setting alsa vol", e)
            }
        }
        return flowOf(ConfigPush(key, value))
    }
}