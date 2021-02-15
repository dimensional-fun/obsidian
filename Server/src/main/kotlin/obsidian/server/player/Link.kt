package obsidian.server.player

import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import obsidian.server.io.MagmaClient

class Link(
  private val client: MagmaClient,
  private val guildId: Long
) : AudioEventAdapter() {



}