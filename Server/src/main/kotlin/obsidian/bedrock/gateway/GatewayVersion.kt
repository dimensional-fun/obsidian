package obsidian.bedrock.gateway

import obsidian.bedrock.MediaConnection
import obsidian.bedrock.VoiceServerInfo

typealias MediaGatewayConnectionFactory = (mediaConnection: MediaConnection, voiceServerInfo: VoiceServerInfo) -> MediaGatewayConnection

enum class GatewayVersion(private val factory: MediaGatewayConnectionFactory) {
  V4({ mc, vsi -> MediaGatewayV4Connection(mc, vsi) });

  /**
   * Creates a new [MediaGatewayConnection]
   *
   * @param connection The media connection.
   * @param voiceServerInfo The voice server information.
   */
  fun createConnection(connection: MediaConnection, voiceServerInfo: VoiceServerInfo): MediaGatewayConnection =
    factory.invoke(connection, voiceServerInfo)
}