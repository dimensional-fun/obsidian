# Web-socket Payloads

Magma provides several operations for controlling players and the web-socket connection.

## Op Codes

| Code | Name                                            | Description                                                  |
| :--: | :---------------------------------------------- | :----------------------------------------------------------- |
|  0   | [Submit Voice Server](#submit-voice-server)     | allows obsidian to play music in a voice channel             |
|  1   | [**Stats**](#stats)                             | contains useful statistics like resource usage and player count |
|  2   | [Setup Resuming](#setup-resuming)               | configures session resuming, see [resuming](/resuming.md)    |
|  3   | [Setup Dispatch Buffer](#setup-dispatch-buffer) | configures he dispatch buffer, see [resuming#buffer](/resuming.md#buffer) |
|  4   | [**Player event**](#player-events)              | dispatched when a player event occurs, e.g. track end        |
|  5   | [**Player update**](#player-update)             | contains possibly useful information about a player, e.g. track position |
|  6   | [Play Track](#play-track)                       | plays the supplied track                                     |
|  7   | [Stop track](#stop-track)                       | stops the currently playing track, if any.                   |
|  8   | [Pause](#pause)                                 | configures the pause state of a player                       |
|  9   | [Filters](#filters)                             | configures the filters for the player                        |
|  10  | [Seek](#seek)                                   | seeks to the specified position in the current track, if any. |
|  11  | [Destroy](#destroy)                             | destroys the player with the supplied guild id.              |
|  12  | [Configure](#configure)                         | configures multiple player options, such as the pause state and filters. |

<sub>Names in **bold** represent payloads that are received by the client, non-bolded names represent payloads received by the server</sub>

## Submit Voice Server

The equivalent of `voiceUpdate` for lavalink and `voice-state-update` for Andesite.

```json
{
    "op": 0,
    "d": {
        "guild_id": "751571246189379610",
        "token": "the voice server token",
        "session_id": "voice server session id",
        "endpoint": "smart.loyal.discord.gg"
    }
}
```

- `token` and `endpoint` can be received from Discord's `VOICE_SERVER_UPDATE` dispatch event.
- `session_id` can be received from Discord's `VOICE_STATE_UPDATE` dispatch event.

## Stats

```json
{
	"op": 1,
  "d": {
    "memory": {
      "heap_used": {
        "init": 130023424,
        "max": 2061500416,
        "committed": 132120576,
        "used": 55584624
      },
      "non_heap_used": {
        "init": 39387136,
        "max": -1,
        "committed": 72859648,
        "used": 39730328
      }
    },
    "cpu": {
      "cores": 4,
      "system_load": 0.009758602978941962,
      "process_load": 0.09655880842321521
    },
    "threads": {
      "running": 17,
      "daemon": 16,
      "peak": 17,
      "total_started": 19
    },
    "frames": [
       {
         "guild_id": "751571246189379610",
         "loss": 0,
         "sent": 3011,
         "usable": true
       }
    ],
    "players": {
      "active": 1,
      "total": 0
    }
  }
}
```

## Setup Resuming

- **timeout**: is the amount of time in milliseconds before the session gets destroyed.

```json
{
  "op": 2,
  "d": {
    "key": "fduivbubBIvVuVDwabu",
    "timeout": 60000
  }
}
```

<sub>if any players are active the client won't be removed due to the implementation of player endpoints.</sub>

## Setup Dispatch Buffer 

- **timeout**: timeout *in milliseconds*

```json
{
  "op": 3,
  "d": {
    "timeout": 60000
  }
}
```

## Player Events

List of current player events. *Example:*

```json
{
  "op": 4,
  "d": {
    "type": "TRACK_START",
    "guild_id": "751571246189379610",
    "track": "QAAAmAIAO0tTSSDigJMgUGF0aWVuY2UgKGZlYXQuIFlVTkdCTFVEICYgUG9sbyBHKSBbT2ZmaWNpYWwgQXVkaW9dAANLU0kAAAAAAALG8AALTXJmVTRhVGNVYU0AAQAraHR0cHM6Ly93d3cueW91dHViZS5jb20vd2F0Y2g/dj1NcmZVNGFUY1VhTQAHeW91dHViZQAAAAAAAAAA"
  }
}
```

<sub>code blocks below represent data in the d field</sub>

### `TRACK_START`

```JSON
{
  "type": "TRACK_START",
  "track": "..."
}
```

---

### `TRACK_END`

```json
{
  "type": "TRACK_END",
  "track": "...",
  "reason": "REPLACED"
}
```

#### End Reasons

- **STOPPED**, **REPLACED**, **CLEANUP**, **LOAD_FAILED**, **FINISHED**

<sub>for more information visit [AudioTrackEndReason.java](https://github.com/sedmelluq/lavaplayer/blob/master/main/src/main/java/com/sedmelluq/discord/lavaplayer/track/AudioTrackEndReason.java)</sub>

---

### `TRACK_STUCK`

- `threshold_ms` the wait threshold that was exceeded for this event to trigger.

```json
{
  "track": "...",
  "threshold_ms": 1000
}
```

---

### `TRACK_EXCEPTION`

```json
{
  "track": "...",
  "exception": {
    "message": "This video is too cool for the people listening.",
    "cause": "Lack of coolness by the listeners",
    "severity": "COMMON"
  }
}
```

#### Exception Severities

- **COMMON**, **FAULT**, **SUSPICIOUS**

<sub>for more information visit [FriendlyException.java](https://github.com/sedmelluq/lavaplayer/blob/master/main/src/main/java/com/sedmelluq/discord/lavaplayer/tools/FriendlyException.java#L30-L46)</sub>

---

### `WEBSOCKET_OPEN`

```json
{
  "target: "420.69.69.9", 
  "ssrc": 42069
}
```

<sub>Refer to the Dscord docs for what the *ssrc* is</sub>

---

### `WEBSOCKET_CLOSED`

```json
{
  "code": 4014,
  "reason": "",
  "by_remote": true
}
```

<sub>For more information about close codes, visit [this page](https://discord.com/developers/docs/topics/opcodes-and-status-codes#voice-voice-close-event-codes)</sub>

## Player Update

- `frames`
  - `sent` the number of successfully sent frames
  - `lost` the number of frames that failed to send
  - `usable` whether this data is usable
- `current_track` the currently playing track.
- `filters` the current filters, see [Filters](#filters)

```json
{
  "op": 5,
  "d": {
    "guild_id": "751571246189379610", 
  	"frames": {
      "sent": 3011,
      "lost": 0,
      "usable": true
    },
    "current_track": {
      "track": "...",
      "paused": false,
      "position": 42069
    },
    "filters": {}
  }
}
```

---

## Play Track

- `start_time` specifies the number of milliseconds to offset the track by.
- `end_time` specifies what position to stop the track at *(in milliseconds)*

```json
{
  "op": 6,
  "d": {
    "guild_id": "751571246189379610",
    "track": "...",
    "start_time": 30000,
    "end_time": 130000,
    "no_replace": false
  }
}
```

## Stop Track

```json
{
  "op": 7,
  "d": {
    "guild_id": "751571246189379610"
  }
}
```

## Pause

- `state` whether or not to pause the player.

```json
{
  "op": 8,
  "d": {
    "guild_id": "751571246189379610",
    "state": true
  }
}
```

## Filters

- `volume` the volume to set. `0.0` through `5.0` is accepted, where `1.0` is 100%


- `tremolo` creates a shuddering effect, where the volume quickly oscillates
  - `frequency` Effect frequency &bull; `0 < x`
  - `depth` Effect depth &bull; `0 < x ≤ 1`


- `equalizer` There are 15 bands (0-14) that can be configured. Each band has a gain and band field, band being the band
  number and gain being a number between `-0.25` and `1.0`
  `-0.25` means the band is completed muted and `0.25` meaning it's doubled


- `distortion` Distortion effect, allows some unique audio effects to be generated.


- `timescale` [Time stretch and pitch scale](https://en.wikipedia.org/wiki/Audio_time_stretching_and_pitch_scaling)
  filter implementation
  - `pitch` Sets the audio pitch
  - `pitch_octaves` Sets the audio pitch in octaves, this cannot be used in conjunction with the other two options
  - `pitch_semi_tones` Sets the audio pitch in semi tones, this cannot be used in conjunction with the other two pitch
    options
  - `rate` Sets the audio rate, cannot be used in conjunction with `rate_change`
  - `rate_change` Sets the audio rate, in percentage, relative to the default
  - `speed` Sets the playback speed, cannot be used in conjunction with `speed_change`
  - `speed_change` Sets the playback speed, in percentage, relative to the default


- `karaoke` Uses equalization to eliminate part of a band, usually targeting vocals. None of these i have explanations
  for... ask [natan](https://github.com/natanbc/lavadsp) ig
  - `filter_band`, `filter_width`, `level`, `mono_level`


- `channel_mix` This filter mixes both channels (left and right), with a configurable factor on how much each channel
  affects the other. With the defaults, both channels are kept independent of each other. Setting all factors to `0.5`
  means both channels get the same audio
  - `right_to_left` The current right-to-left factor. The default is `0.0`
  - `right_to_right` The current right-to-right factor. The default is `1.0`
  - `left_to_right` The current left-to-right factor. The default is `0.0`
 - `left_to_left` The current left-to-left factor. The default is `1.0`


- `vibrato` Similar to tremolo. While tremolo oscillates the volume, vibrato oscillates the pitch
  - `frequency` Effect frequency &bull; `0 < x ≤ 14`
  - `depth` Effect depth &bull; `0 < x ≤ 1`


- `rotation` The frequency the audio should rotate around the listener, in Hertz
  - This filter simulates an audio source rotating around the listener.


- `low_pass` Smoothing to use. 20 is the default
  - Higher frequencies get suppressed, while lower frequencies pass through this filter, thus the name low pass

```json
{
  "op": 9,
  "d": {
    "guild_id": "751571246189379610",
    "filters": {
      "distortion": {},
      "equalizer": [],
      "karaoke": {},
      "low_pass": 20.0,
      "rotation": 5.0,
      "timescale": {},
      "tremolo": {},
      "vibrato": {},
      "volume": 1.0
    }
  }
}
```

## Seek

- `position` the position to seek to, in milliseconds.

```json
{
  "op": 10,
  "d": {
    "guild_id": "751571246189379610",
    "position": 30000
  }
}
```

## Destroy

```json
{
  "op": 11,
  "d": {
    "guild_id": "751571246189379610"
  }
}
```

