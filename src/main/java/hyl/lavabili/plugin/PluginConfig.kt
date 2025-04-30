package hyl.lavabili.plugin

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "plugins.lavabili")
@Component
data class PluginConfig(
    val activeSources: ArrayList<String> = arrayListOf("bilibili"),
    val lavabiliConfig: LavabiliConfig = LavabiliConfig()
) {
    data class LavabiliConfig(var playlistPageCount: Int = -1)
}