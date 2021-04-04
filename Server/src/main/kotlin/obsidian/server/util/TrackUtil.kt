/*
 * Copyright 2021 MixtapeBot and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package obsidian.server.util

import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import obsidian.server.Obsidian.playerManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

object TrackUtil {
  /**
   * Decodes a base64 encoded string into a usable [AudioTrack]
   *
   * @param encodedTrack The base64 encoded string.
   *
   * @return The decoded [AudioTrack]
   */
  fun decode(encodedTrack: String): AudioTrack {
    val decoded = Base64.getDecoder()
      .decode(encodedTrack)

    val inputStream = ByteArrayInputStream(decoded)
    val track = playerManager.decodeTrack(MessageInput(inputStream))!!.decodedTrack

    inputStream.close()
    return track
  }

  /**
   * Encodes a [AudioTrack] into a base64 encoded string.
   *
   * @param track The audio track to encode.
   *
   * @return The base64 encoded string
   */
  fun encode(track: AudioTrack): String {
    val outputStream = ByteArrayOutputStream()
    playerManager.encodeTrack(MessageOutput(outputStream), track)

    val encoded = Base64.getEncoder()
      .encodeToString(outputStream.toByteArray())

    outputStream.close()
    return encoded
  }
}