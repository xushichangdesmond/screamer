# screamer
This project is meant to be used in tandem with https://github.com/duncanthrax/scream to enable high resolution audio streaming over TCP from a single source to multiple receivers.

The intended target use case (which is what the author is using for himself) is to stream the audio output from a windows PC to multiple receivers over wifi for home theater setup
and importantly, where there is virtually no latency (as long as your local network is robust enough). The author achieves by having 1 raspberry pi per speaker running the receiver.

No downscaling or upscaling os performed, so audio resolution depends solely on the source.

For a full complete home theater solution though, at least as of now, you would also need to run some DSP externally (downmixing/muting unwanted audio channels on each individual receiver).

