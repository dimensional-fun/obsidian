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

package obsidian.server.player.filter

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
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
