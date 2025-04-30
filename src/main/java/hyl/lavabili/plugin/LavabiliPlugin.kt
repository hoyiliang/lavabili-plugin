package hyl.lavabili.plugin

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import dev.arbjerg.lavalink.api.AudioPlayerManagerConfiguration
import hyl.lavabili.source.BilibiliAudioSourceManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LavabiliPlugin(private val config: PluginConfig) : AudioPlayerManagerConfiguration {
    init {
        log.info("START: lavabili-plugin:v0.1 by Stevehyl.")
    }

    override fun configure(manager: AudioPlayerManager): AudioPlayerManager {
        if (config.activeSources.contains("bilibili")) {
            manager.registerSourceManager(
                BilibiliAudioSourceManager()
                    .setPlaylistPageCount(config.lavabiliConfig.playlistPageCount)
            )
            log.info("Registered Bilibili source manager.")
        }
        return manager
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(LavabiliPlugin::class.java)
    }
}
