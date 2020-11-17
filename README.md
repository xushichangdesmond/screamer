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
