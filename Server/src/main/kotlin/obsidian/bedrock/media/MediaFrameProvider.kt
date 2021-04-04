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

package obsidian.bedrock.media

import io.netty.buffer.ByteBuf
import obsidian.bedrock.codec.Codec

/**
 * Base interface for media frame providers. Note that Bedrock doesn't handle stuff such as speaking state, silent frames
 * or etc., these are implemented by codec-specific frame provider classes.
 *
 * @see OpusAudioFrameProvider for Opus audio codec specific implementation that handles speaking state and etc.
 */
interface MediaFrameProvider {
  /**
   * Frame interval between polling attempts or sets the delay between polling attempts.
   */
  var frameInterval: Int

  /**
   * Called when this [MediaFrameProvider] should clean up it's event handlers and etc.
   */
  fun dispose()

  /**
   * @return If true, Bedrock will request media data for given [Codec] by calling [retrieve] method.
   */
  fun canSendFrame(codec: Codec): Boolean

  /**
   * If [canSendFrame] returns true, Bedrock will attempt to retrieve an media frame encoded with specified [Codec] type, by calling this method with target [ByteBuf] where the data should be written to.
   * Do not call [ByteBuf.release] - memory management is already handled by Bedrock itself. In case if no data gets written to the buffer, audio packet won't be sent.
   *
   * Do not let this method block - all data should be queued on another thread or pre-loaded in memory - otherwise it will very likely have significant impact on application performance.
   *
   * @param codec     [Codec] type this handler was registered with.
   * @param buf       [ByteBuf] the buffer where the media data should be written to.
   * @param timestamp [IntReference] reference to current frame timestamp, which must be updated with timestamp of written frame.
   *
   * @return If true, Bedrock will immediately attempt to poll a next frame, this is meant for video transmissions.
   */
  suspend fun retrieve(codec: Codec?, buf: ByteBuf?, timestamp: IntReference?): Boolean
}

