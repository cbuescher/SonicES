# SonicES
Playground for connecting some ES generated data to a sound output device via MIDI

## Some Example Sketches

### Mapping discrete events to densities
* probably a good approach for relatively low-frequency, single event

Getting log level data from 2024-05-30T09:21:22.274Z to 2024-05-30T09:22:22.274Z. (QA)
Mapping WARN log events to higher pitch left channel (A4 - 440Hz) and more frequent DEBUG events to lower pitch (E3 - ~164Hz)

e.g. here we have DEBUG log events with a frequency of approx. 10 to 40 events/sec, which mapped to bell-like “clicks” is in the audible range, and WARN log messages in the 1-6 range (audible as single events)

![Screenshot 2024-05-30 at 11 27 29](https://github.com/cbuescher/SonicES/assets/10398885/6858d2d5-19f0-4e83-b46a-86df6250af36)


![Screenshot 2024-05-30 at 11 27 39](https://github.com/cbuescher/SonicES/assets/10398885/5b96ecc0-b6f8-403f-854a-9a28041baee6)

https://github.com/cbuescher/SonicES/assets/10398885/32b6fa4e-f510-40c9-aef4-5956f3bb1e55

### Mapping continuous values to control messages

- useful to monitor a continously changing value
- can be mapped to an expression parameter via appropriate scaling
- example: [50p Response Latency Percentiles](https://overview.qa.cld.elstc.co/app/r/s/L9Lzm%22%20height=%22600%22%20width=%22800%22%3E%3C/iframe%3E "https://%3Ciframe%20src=%22https://overview.qa.cld.elstc.co/app/r/s/L9Lzm%22%20height=%22600%22%20width=%22800%22%3E%3C/iframe%3E") from 2024-05-29T21:06:57.963Z to 2024-05-29T21:08:01.327Z (QA)

![grafik](https://github.com/cbuescher/SonicES/assets/10398885/9a0c54e0-0fef-49b8-9006-b9855cc3ee64)

https://github.com/cbuescher/SonicES/assets/10398885/d60237c6-0990-4970-95b1-bea6190a7954

### Mapping GC overhead info and warn messages to sample

The idea here is to take long GC log events of the form `[gc][10803] overhead, spent [1.5s] collecting in the last [1.9s]` from the JvmGcMonitorService logger, 
parse them to get the exact long GC duration in ms (or seconds) and play a sample (some modem dial-up sound in this case to have something harsh) for the same duration,
also to play them in the same distance the events occurred in real-time.

Two examples from QA evnironment in some less dense / more dense minute long period. Most of the time on QA there are almost no events, so this would be a relatively low-traffic
channel that could be overlayed with other information.

#### Lower density
![Screenshot 2024-05-31 at 10 49 21](https://github.com/cbuescher/SonicES/assets/10398885/2530719e-ace1-4ae9-afec-9a33bd916367)



https://github.com/cbuescher/SonicES/assets/10398885/09aaed8b-ea8d-44d4-a42d-d80388b2442d


#### Higher density
![Screenshot 2024-05-31 at 10 48 43](https://github.com/cbuescher/SonicES/assets/10398885/3758e5c1-8ee7-4c03-b683-5f2f26708c3c)



https://github.com/cbuescher/SonicES/assets/10398885/701cbdf6-82c3-496e-b4ba-af005d7d3bc9

