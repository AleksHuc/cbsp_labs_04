# Audio credits

## Intractable — Tasks 5 and 7

- Title: **Intractable**
- Creator: **Kevin MacLeod** (incompetech.com)
- Source: https://incompetech.com/music/royalty-free/index.html?Search=Search&isrc=USUAN1100194
- License: **Creative Commons Attribution 4.0 International (CC BY 4.0)**
- License link: https://creativecommons.org/licenses/by/4.0/
- Local source supplied by the user: `Intractable.mp3`.
- Derived file: `intractable-excerpt.wav`.

Changes: extracted source interval **00:30–01:00**; averaged the left and right
channels into mono; exported at **44100 Hz, signed 16-bit little-endian PCM WAV**.
A fixed gain of 0.94 (about -0.54 dB) prevents clipping from MP3 decoding
overshoot (the decoded mono peak was about 1.06189). No fade was applied. Filtered
experiment exports additionally apply the documented EQ and common listening
attenuation. These modifications do not imply endorsement by the creator.

The excerpt contains exactly 1323000 samples (30 seconds). Only the WAV excerpt
is required for Tasks 5 and 7; the original MP3 is not included in the distribution ZIP.

Reproduce from the project root using FFmpeg:

```bash
ffmpeg -ss 30 -i media/Intractable.mp3 -t 30 -map 0:a:0 \
  -af "pan=mono|c0=0.5*c0+0.5*c1,volume=0.94" -ac 1 -ar 44100 \
  -c:a pcm_s16le -map_metadata -1 media/intractable-excerpt.wav
```

## siney.wav — Tasks 1, 3, 4 and 6

Copied unchanged from the supplied legacy exercise archive. The Intractable
license above applies to Intractable, not to this separate recording.
