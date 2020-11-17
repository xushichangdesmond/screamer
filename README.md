# screamer
THe primary use case and motivation behind this project is for the author to stream his window's machine's audio (be it youtube/netflix or any other audio output on Windows) over local network to his DIY smart speakers and subwoofer, and performing DSP (digital signal processing) as well. Other primary requirements are real-time and high resolution for the audio stream since it has to be synchronized with the on-screen video feed for a good experience.

To capture the audio output on the windows machine for streaming, installing the windows driver from https://github.com/duncanthrax/scream will be required.

Whereas https://github.com/duncanthrax/scream also provides their own receivers that get the audio data via UDP multicasting, this project uses TCP instead to stream the audio data data to the receivers because the UDP multicasting transport proved to be to unreliable even in the author's own local network.

# Features
- TCP realtime lossless streaming of multichannel audio content
- Receivers listen on a TCP port and output the audio on their local sound device
- The audio source inititates TCP connections to the receivers to send the audio data
- DSP (digital signal processing) filters which can be used in both the receiver or source (or can be used standalone even without the TCP transport)
- simple http api to control playback or the DSP parameters

# DSP filters
- volume control
- high pass filter
- low pass filter
- channel mix

# Channel mixing DSP filter
Channel mixing is an important feature, since the author has a 3.1 setup, where there is one raspberry pi that has stereo output, where the left channel of the pi is connected to the 'right' speaker of the surround setup, and where the right channel of the pi is connected to the subwoofer. This dsp filter allows this configuration via the following snipper
```
                    Mix(
                        doubleArrayOf(0.0, 1.0), // first channel of the pi to output 1.0 times of source's right audio channel
                        doubleArrayOf(0.5, 0.5), // second channel of the pi to output 0.5 times of source's left audio channel plus 0.5 times of source's right audio channel
                    ),
                    LowPassFilter(125, IntPredicate { i -> i == 1 }),  //low pass filter for 125hz to apply to subwoofer (channel 0 is left, channel 1 is right)
                    HighPassFilter(125, IntPredicate { i -> i == 0 }), //high pass filter for 125hz to apply to speaker
```

# Usage
For now, you need to compile and run this on your own. The project is written using kotlin, and here is the example to run the receiver
```
import kotlinx.coroutines.Job
import powerdancer.dsp.Processor
import powerdancer.dsp.filter.impl.AudioPlayer
import powerdancer.dsp.filter.impl.ConfigurationFilter
import powerdancer.screamer.AlsaMixerVolumeControl
import powerdancer.screamer.TcpAudioReceiver

object BedroomReceiver {
    fun run(): Job {

        return Processor.process(
            ConfigurationFilter(),        // this listens to http requests to control playback and volume
            TcpAudioReceiver(),           // receive audio by listening to TCP on default port 6789
            AlsaMixerVolumeControl("'DAC'"),      // allows configuring of system'level ALSA volume via the control name 'DAC' (this is optional of course)
            AudioPlayer(2048)             // plays audio out on this system via the system's soundcard with a buffer size of 2048 bytes (higher buffer means higher latency, but from experience even a few milliseconds is enough which wont be noticed even when watching videos)
        )
    }
}
```

The point of this project is to enable flexibility in the actual audio setup, so there is no one single way to configure the sender, but here is an example, based on the author's own bedroom surround setup
```
import kotlinx.coroutines.Job
import powerdancer.dsp.Processor
import powerdancer.dsp.filter.impl.*
import powerdancer.screamer.ScreamMulticastAudioReceiver
import powerdancer.screamer.TcpAudioSender
import java.util.function.IntPredicate

object BedroomTheater {
    fun run(): Job {

        return Processor.process(
            ConfigurationFilter(
                repeatTo = arrayOf("http://192.168.1.91:6788/", "http://192.168.1.89:6788/")
            ),  // this listens to http requests to control playback and volume, also it repeats the confiuration requests to 2 other addresses to ease management for the user
            ScreamMulticastAudioReceiver(),  // this receives the actual audio data from windows
            ToFloat64Converter(),            // converts the audio data to 64bit PCM float (which the DSP filters require)
            Forker(                          // fork the processing into three different pipelines (pipeline1 for the right speaker and subwoofer, pipeline2 for the left speaker and pipeline3 for the center speaker
                //right speaker and subwoofer
                arrayOf(
                    Mix(
                        doubleArrayOf(0.0, 1.0), // right channel to right speaker
                        doubleArrayOf(0.5, 0.5), // mix both channels to subwoofer
                    ),
                    LowPassFilter(125, IntPredicate { i -> i == 1 }), // low pass filter (125hz) for the subwoofer
                    HighPassFilter(125, IntPredicate { i -> i == 0 }), // high pass filter(125hz) for the right speaker
                    VolumeMultiplier(doubleArrayOf(0.05, 0.4)),   // multiply left audio channel(for right speaker) with 0.05 and right audio channel (for subwoofer) with 0.4
                    FromFloat64Converter(4),                      // convert from PCM FLOAT 64 bit back to PCM SIGNED 32bit(the parameter means 4 bytes or 32bit)
                    TcpAudioSender("192.168.1.91")                // send the audio to this ip address(default port 6789)
                ),
                //left speaker
                arrayOf(
                    Mix(
                        doubleArrayOf(1.0), // left channel to left speaker
                    ),
                    HighPassFilter(125),                    // high pass filter(125hz)
                    VolumeMultiplier(doubleArrayOf(0.05)),  // multiple volume with 0.05
                    FromFloat64Converter(4),                // convert from PCM FLOAT 64 bit back to PCM SIGNED 32bit(the parameter means 4 bytes or 32bit)
                    TcpAudioSender("192.168.1.89")          // send the audio to this ip address(default port 6789)
                ),
                //center speaker
                arrayOf(
                    FromFloat64Converter(2),
                    VolumeMultiplier(doubleArrayOf(0.5, 0.5), "centerVol"),
                    AudioPlayer(
                        2048,
                        "Laser Proj (NVIDIA High Definition Audio)" 
                    )
                )
            )
        )
    }
}
```
