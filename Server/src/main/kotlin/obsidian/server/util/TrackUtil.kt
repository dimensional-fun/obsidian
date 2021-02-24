/*
 * Obsidian
 * Copyright (C) 2021 Mixtape-Bot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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