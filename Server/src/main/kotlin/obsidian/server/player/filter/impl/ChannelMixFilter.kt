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

package obsidian.server.player.filter.impl

import com.github.natanbc.lavadsp.channelmix.ChannelMixPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.Serializable
import obsidian.server.player.filter.Filter

@Serializable
data class ChannelMixFilter(
  val leftToLeft: Float = 1f,
  val leftToRight: Float = 0f,
  val rightToRight: Float = 0f,
  val rightToLeft: Float = 1f,
) : Filter {
  override val enabled: Boolean
    get() = Filter.isSet(leftToLeft, 1.0f) || Filter.isSet(leftToRight, 0.0f) ||
      Filter.isSet(rightToLeft, 0.0f) || Filter.isSet(rightToRight, 1.0f);

  override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter =
    ChannelMixPcmAudioFilter(downstream).also {
      it.leftToLeft = leftToLeft
      it.leftToRight = leftToRight
      it.rightToRight = rightToRight
      it.rightToLeft = rightToLeft
    }
}