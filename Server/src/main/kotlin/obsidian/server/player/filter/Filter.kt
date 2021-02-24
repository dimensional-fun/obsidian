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

package obsidian.server.player.filter

import com.sedmelluq.discord.lavaplayer.filter.AudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlin.math.abs

interface Filter {
  /**
   * Whether this filter is enabled.
   */
  val enabled: Boolean

  /**
   * Builds this filter's respective [AudioFilter]
   *
   * @param format The audio data format.
   * @param downstream The audio filter used as the downstream.
   *
   * @return null, if this filter isn't compatible with the provided format or if this filter isn't enabled.
   */
  fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter?

  companion object {
    /**
     * Minimum absolute difference for floating point values. Values whose difference to the default
     * value are smaller than this are considered equal to the default.
     */
    const val MINIMUM_FP_DIFF = 0.01f

    /**
     * Returns true if the difference between [value] and [default]
     * is greater or equal to [MINIMUM_FP_DIFF]
     *
     * @param value The value to check
     * @param default Default value.
     *
     * @return true if the difference is greater or equal to the minimum.
     */
    fun isSet(value: Float, default: Float): Boolean =
      abs(value - default) >= MINIMUM_FP_DIFF
  }
}