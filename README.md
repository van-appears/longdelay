# longdelay

A very basic application that reads data from an audio source (e.g. inbuilt microphone), holds it in memory for an amount of time, then outputs it again (e.g. via inbuilt speakers). No adjustment is made to the level, use whatever tools the output has (e.g. volume controls!)

_Source section_
* `Input`: the audio device the application reads from (e.g. inbuilt microphone).
* `Output`: the audio device the application plays through (e.g. inbuilt speakers).
* `Refresh` button: If you plug in additional inputs and outputs (say a headset, or set of external speakers) after starting the application, then pressing 'Refresh' will find the new devices.
* `Restart` button: If you change the input or output, then pressing 'Restart' will reconnect the application to use those instead. 

_Loop section_
* `Length`: The length of the loop in seconds, minimum length is 1s, max is 60s.
* `Clear` button: Clear out the current loop data.

_Recording section_
* `Record output`: When recording, write to file the values currently being played via the speaker.
* `Record input`: When recording, write to file the values currently being recorded via the microphone.
* `Recording length`: The length of recording to make, minimum length is 1s, max is 10 minutes.
* `File prefix`: Recordings are created in timestamped files, setting this value adds a prefix to the filename.
* `Clear on record` button: When record is pressed, clear what data is already in both loops.
* `Record` button: Start recording. This will change to a `Cancel` button that allows you to exit recording early.
* `Load properties` button. When recording, the application will save a properties file with the same name as the recording file. This button allows you to reload the settings the application was using at that time.
