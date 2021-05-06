# Obsidian API Documentation

Welcome to the Obsidian API Documentation! This document describes mostly everything about the WebSocket and HTTP
server.

###### What's Magma?

Magma is the name for the WebSocket and REST server!

---

- **Current Version:** 2.0.0

## Magma REST

### Route Planner

Allows clients to view the route planner and free-up addresses.

#### Routeplanner Status

```
GET /routeplanner/status
Authorization: <password>
```

**Example Response**

```json
{
  "class": "RotatingNanoIpRoutePlanner",
  "details": {
    "ip_block": {
      "type": "Inet6Address",
      "size": "1208925819614629174706176"
    },
    "failing_addresses": [
      {
        "address": "/1.0.0.0",
        "failing_timestamp": 1573520707545,
        "failing_time": "Mon Nov 11 20:05:07 EST 2019"
      }
    ],
    "block_index": "0",
    "current_address_index": "36792023813"
  }
}
```

*If no route planner was configured, both `class` and `details` will be null, the responses vary depending on what route planner was configured. Fields that are consistent:*

- `class` *string* name of the route planner
- `details.ip_block` *string* the current ip-block
- `details.failing_addresses` *array* of objects describing a failed address.

**RotatingIpRoutePlanner**

- `details.rotate_index` *string* containing the number of rotations which happened since the server had started.
- `details.ip_index` *string* containing the current offset of the ip-block.
- `details.current_address` *string* containing the currently used ip-address.

**NanoIpRoutePlanner**

- `details.current_address_index` *long* representing the current offset in the ip-block.

**RotatingNanoIpRoutePlanner**

- `details.block_index` *string* containing the file information in which /64 block ips are chosen, this number increases on each ban.
- `details.current_address_index` *long* representing the current offset in the ip-block.

#### Unmark a failed address

```
GET /routeplanner/free/address
Authorization: <password>
```

*body:*

```json
{
  "address": "420.69.69.420"
}
```

**Example Response**

*204 - No Content*

#### Unmark all failed addresses

```
GET /routeplanner/free/all
Authorization: <password>
```

**Example Response**

*204 - No Content*

### Player Controller

Each request must have a `User-Id` header or query parameter containing your bot's user id

- **Base Path:** `/players/{guild id}`

<sub>The `Client-Name` header or query parameter may be required if the node you're using requires it.</sub>

| endpoint | description                |
| :------- | :------------------------- |
| /        | returns info on the player |
| /play    | plays 
|
|
|

### Tracks Controller

Allows non-jvm clients to search and decode tracks using Obsidian!

#### Load Tracks

```
GET /loadtracks?identifier=D-ocerKPufk
Authorization: <configured password> 
```

**Example Response**

*this was actually taken from a response btw*

```json
{
  "load_type": "TRACK_LOADED",
  "playlist_info": null,
  "tracks": [
    {
      "track": "...",
      "info": {
        "title": "The Kid LAROI - SELFISH (Official Video)",
        "author": "TheKidLAROIVEVO",
        "uri": "https://www.youtube.com/watch?v=D-ocerKPufk",
        "identifier": "D-ocerKPufk",
        "length": 270000,
        "position": 0,
        "is_stream": false,
        "is_seekable": true,
        "source_name": "youtube"
      }
    }
  ],
  "exception": null
}
```

#### Decode Track(s)

**Singular**

```
GET /decodetrack?track=QAAAkQIAKF...
Authorization: <configured password> 
```

**Example Response**

```json
{
  "title": "The Kid LAROI - SELFISH (Official Video)",
  "author": "TheKidLAROIVEVO",
  "uri": "https://www.youtube.com/watch?v=D-ocerKPufk",
  "identifier": "D-ocerKPufk",
  "length": 270000,
  "position": 0,
  "is_stream": false,
  "is_seekable": true
}
```

**Multiple**

```
POST /decodetracks
Authorization: <configured password> 
```

*body:*

```json
{
  "tracks": [
    "..."
  ]
}
```

**Example Response**

ok, yknow what... idc man

jk the response is just an array filled with track info objects (refer to the singular track response)

## Magma WebSocket

Magma also has a WebSocket used for real-time player updates, it also allows for player control

##### Connecting

To connect you must have these headers assigned

```
Authorization: Password configured in `.obsidianrc`
User-Id: The user id of the bot you're playing music with
Resume-Key: The resume key (like lavalink), however this is only needed if the client needs to be resumed.
Client-Name: Name of your bot or project, may be required but the node you are connecting to.
```

**Close Codes**

| close code | reason                                                      |
| :--------- | :---------------------------------------------------------- |
| 4001       | You specified invalid authorization                         |
| 4002       | No `User-Id` header was specified                           |
| 4004       | A session for the `User-Id` header already exists           |  
| 4005       | An error occurred while handling incoming frames            |
| 4006       | The server requires the `Client-Name` header to be present. |

###### Payload Structure

- **op:** numeric op code
- **d:** payload data

```json
{
  "op": 0,
  "d": {}
}
```

### Operations

| op code/name | description |
| :------------------------------ | :------------------------------------------------------------------------- |
| 0 &bull; submit voice update    | allows obsidian to connect to the discord voice server                     |
| 1 &bull; stats                  | has resource usage for both the system and jvm, also includes player count |
| 2 &bull; player event           | dispatched when a player event occurs, e.g. track end, track start         |
| 3 &bull; player update          | used to keep track of player state, e.g. current position and filters      |
| 4 &bull; play track             | used to play tracks                                                        |
| 5 &bull; stop track             | stops the current track                                                    |
| 6 &bull; pause                  | configures the pause state of the player                                   |
| 7 &bull; filters                | configures the current filters                                             |
| 8 &bull; seek                   | seeks to the specified position                                            |
| 9 &bull; destroy                | used to destroy players                                                    |
| 10 &bull; setup resuming        | configures resuming                                                        |
| 11 &bull; setup dispatch buffer | configures dispatch buffer                                                 |

#### Submit Voice Update

The equivalent of `voiceUpdate` for lavalink and `voice-state-update` for andesite

```json
{
  "op": 0,
  "d": {
    "guild_id": "751571246189379610",
    "token": "your token",
    "session_id": "your session id",
    "endpoint": "smart.loyal.discord.gg"
  }
}
```

- `token` and `endpoint` can be received from Discord's `VOICE_SERVER_UPDATE` dispatch event
- `session_id` can be received from Discord's `VOICE_STATE_UPDATE` dispatch event

#### Stats

Stats on the node

```json
{
  "op": 1,
  "d": {
    "memory": {
      "free": 46609784,
      "used": 26790536,
      "allocated": 73400320,
      "reservable": 2061500416
    },
    "cpu": {
      "cores": 4,
      "system_load": 0.011964233239578523,
      "process_load": 0.15813777759120104
    },
    "links": { 
      "active": 1, 
      "total": 1
    },
    "frames": { 
      "sent": 3000, 
      "nulled": 0, 
      "deficit": 0
    }
  }
}
```


#### Player Events

List of current player events Example:

```json
{
  "op": 2,
  "d": {
    "type": "TRACK_START",
    "guild_id": "751571246189379610",
    "track": "QAAAmAIAO0tTSSDigJMgUGF0aWVuY2UgKGZlYXQuIFlVTkdCTFVEICYgUG9sbyBHKSBbT2ZmaWNpYWwgQXVkaW9dAANLU0kAAAAAAALG8AALTXJmVTRhVGNVYU0AAQAraHR0cHM6Ly93d3cueW91dHViZS5jb20vd2F0Y2g/dj1NcmZVNGFUY1VhTQAHeW91dHViZQAAAAAAAAAA"
  }
}
```

---

*code blocks below represent data in the `d` field*

##### `TRACK_START`

Dispatched when a track starts

---

- `track` *self-explanatory*

```json
{
  ...
  "track": "..."
}
```

##### `TRACK_END`

Dispatched when a track ends

---

- `track` the track that had ended
- `reason` Reason for why the track had ended

```json
{
  ...
  "track": "...",
  "reason": "REPLACED"
}
```

**End Reasons**

- `STOPPED`, `REPLACED`, `CLEANUP`, `LOAD_FAILED`, `FINISHED`

For more information visit [**
AudioTrackEndReason.java**](https://github.com/sedmelluq/lavaplayer/blob/master/main/src/main/java/com/sedmelluq/discord/lavaplayer/track/AudioTrackEndReason.java)

##### `TRACK_STUCK`

dispatched when track playback is stuck

---

- `track` *self-explanatory*
- `threshold_ms` The wait threshold that was exceeded for this event to trigger

```json
{
  ...
  "track": "",
  "threshold_ms": 1000
}
```

- [**Lavaplayer**](https://github.com/sedmelluq/lavaplayer/blob/bec39953a037b318663fad76873fbab9ce13c033/main/src/main/java/com/sedmelluq/discord/lavaplayer/player/event/TrackStuckEvent.java)

##### `TRACK_EXCEPTION`

Dispatched when lavaplayer encounters an error while playing a track

---

- `track` the track that threw an exception during playback
- `exception` *everything is self-explanatory*
  - `message`
  - `cause`
  - `severity` *see [Severity Types](#severity-types)*

```json
{
  ...
  "track": "...",
  "exception": {
    "message": "This video is too cool for the people listening",
    "cause": "Lack of coolness by the listeners",
    "severity": "COMMON"
  }
}
```

###### Severity Types

- **COMMON**, **FAULT**, **SUSPICIOUS**

For more information
visit **[this](https://github.com/sedmelluq/lavaplayer/blob/master/main/src/main/java/com/sedmelluq/discord/lavaplayer/tools/FriendlyException.java#L30-L46)**

##### `WEBSOCKET_READY`

Dispatched when the voice websocket receives the `READY` op

---

- `target` voice server ip
- `ssrc` voice ssrc

*refer to the Discord voice docs for what ssrc is for*

```json
{
  ...
  "target": "420.69.69.9",
  "ssrc": 42069
}
```

##### `WEBSOCKET_CLOSED`

Dispatched when the voice websocket closes

- `code`  close code
- `reason` close reason

```json
{
  ...
  "code": 4000,
  "reason": ""
}
```

#### Player Update

- `frames` describes the number of frames, in the last minute, that have been sent or lost
- `current_track` describes the current playing track
  - `track` the base64 encoded track
  - `position` the current playback position
  - `paused` whether playback is paused

```json
{
  "op": 3,
  "d": {
    "guild_id": "751571246189379610",
    "frames": {
      "lost": 0,
      "sent": 3000
    },
    "current_track": {
      "track": "...",
      "position": 3192719,
      "paused": false
    }
  }
}
```

#### Play Track

- `start_time` specifies the number of milliseconds to offset the track by
- `end_time` specifies when to stop the track (in milliseconds)
- `no_replace` when specified, the server will ignore this request if a track is already playing

```json
{
  "op": 4,
  "d": {
    "guild_id": "...",
    "track": "...",
    "end_time": 30000,
    "start_time": 130000,
    "no_replace": false
  }
}
```

#### Stop Track

```json
{
  "op": 4,
  "d": {
    "guild_id": "..."
  }
}
```

#### Pause

- `state` whether playback should be paused.

```json
{
  "op": 6,
  "d": {
    "state": true
  }
}
```

#### Filters

*p.s. this operation overrides any filters that were previously configured, also I didn't really care to fill out the
example lol*

- `volume` the volume to set. `0.0` through `5.0` is accepted, where `1.0` is 100%
- `tremolo` creates a shuddering effect, where the volume quickly oscillates
  - `frequency` Effect frequency &bull; `0 < x`
  - `depth` Effect depth &bull; `0 < x ≤ 1`


- `equalizer` There are 15 bands (0-14) that can be configured. Each band has a gain and band field, band being the band
  number and gain being a number between `-0.25` and `1.0`
  `-0.25` means the band is completed muted and `0.25` meaning it's doubled


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


- `rotation` This filter simulates an audio source rotating around the listener
  - `rotation_hz` The frequency the audio should rotate around the listener, in Hertz


- `low_pass` Higher frequencies get suppressed, while lower frequencies pass through this filter, thus the name low pass
  - `smoothing` Smoothing to use. 20 is the default

```json
{
  "op": 7,
  "d": {
    "guild_id": "...",
    "volume": 1.0,
    "timescale": {},
    "karaoke": {},
    "channel_mix": {},
    "vibrato": {},
    "rotation": {},
    "low_pass": {},
    "tremolo": {},
    "equalizer": {
      "bands": []
    }
  }
}
```

#### Setup Resuming

- `key` Resume key
- `timeout` Resume timeout in milliseconds

```json
{
  "op": 10,
  "d": {
    "key": "d9qd02hbd190bd801",
    "timeout": 60000
  }
}
```

#### Setup Dispatch Buffer

- `timeout` Buffer timeout in milliseconds

```json
{
  "op": 11,
  "d": {
    "timeout": 60000
  }
}
```
