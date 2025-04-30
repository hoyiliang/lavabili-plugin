package hyl.lavabili.source

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import hyl.lavabili.plugin.LavabiliPlugin
import org.apache.http.client.methods.HttpGet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.DataInput
import java.io.DataOutput

class BilibiliAudioSourceManager : AudioSourceManager {
    val httpInterface: HttpInterface
    private var playlistPageCountConfig: Int = -1

    init {
        val httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()
        httpInterfaceManager.setHttpContextFilter(BilibiliHttpContextFilter())
        httpInterface = httpInterfaceManager.`interface`
    }

    override fun getSourceName(): String? {
        return "bilibili"
    }

    override fun loadItem(manager: AudioPlayerManager, reference: AudioReference): AudioItem? {
        val matcher = URL_PATTERN.matcher(reference.identifier)
        if (matcher.find()) {
            when (matcher.group("type")) {
                "video" -> {
                    val bvid = matcher.group("id")
                    val page = (matcher.group("page")?.toInt() ?: 1) - 1

                    val response = httpInterface.execute(HttpGet("${BASE_URL}x/web-interface/view?bvid=$bvid"))
                    val responseJson = JsonBrowser.parse(response.entity.content)

                    val statusCode = responseJson.get("code").`as`(Int::class.java)
                    if (statusCode != 0) {
                        return AudioReference.NO_TRACK
                    }

                    val trackData = responseJson.get("data")
                    return if (trackData.get("pages").values().size > 1) {
                        loadVideoAnthology(trackData, page)
                    } else {
                        loadVideo(trackData)
                    }
                }
                "audio" -> {
                    val type = when (matcher.group("audioType")) {
                        "am" -> "menu"
                        "au" -> "song"
                        else -> return AudioReference.NO_TRACK
                    }
                    val sid = matcher.group("audioId")

                    val response = httpInterface.execute(HttpGet("${BASE_URL}audio/music-service-c/web/$type/info?sid=$sid"))
                    val responseJson = JsonBrowser.parse(response.entity.content)

                    val statusCode = responseJson.get("code").`as`(Int::class.java)
                    if (statusCode != 0) {
                        return AudioReference.NO_TRACK
                    }

                    return when (type) {
                        "song" -> loadAudio(responseJson.get("data"))
                        "menu" -> loadAudioPlaylist(responseJson.get("data"))
                        else -> AudioReference.NO_TRACK
                    }
                }
            }
        }
        return null
    }

    fun setPlaylistPageCount(count: Int): BilibiliAudioSourceManager {
        playlistPageCountConfig = count
        return this
    }

    private fun loadVideo(trackData: JsonBrowser): AudioTrack {
        val bvid = trackData.get("bvid").`as`(String::class.java)

        val log: Logger = LoggerFactory.getLogger(LavabiliPlugin::class.java)
        log.atInfo().log("DEBUG: ${trackData.text()}")

        return BilibiliAudioTrack(
            AudioTrackInfo(
                trackData.get("title").`as`(String::class.java),
                trackData.get("owner").get("name").`as`(String::class.java),
                trackData.get("duration").asLong(0) * 1000,
                bvid,
                false,
                getVideoUrl(bvid),
                trackData.get("first_frame").`as`(String::class.java),
                ""
            ),
            BilibiliAudioTrack.TrackType.VIDEO,
            bvid,
            trackData.get("cid").asLong(0),
            this
        )
    }


    private fun loadVideoAnthology(trackData: JsonBrowser, page: Int): AudioPlaylist {
        val log: Logger = LoggerFactory.getLogger(LavabiliPlugin::class.java)
        log.atInfo().log("DEBUG: ${trackData.text()}")

        val playlistName = trackData.get("title").`as`(String::class.java)
        val author = trackData.get("owner").get("name").`as`(String::class.java)
        val bvid = trackData.get("bvid").`as`(String::class.java)

        val tracks = ArrayList<AudioTrack>()

        for (item in trackData.get("pages").values()) {
            log.atInfo().log("DEBUG: ${item.text()}")
            tracks.add(BilibiliAudioTrack(
                AudioTrackInfo(
                    item.get("part").`as`(String::class.java),
                    author,
                    item.get("duration").asLong(0) * 1000,
                    bvid,
                    false,
                    getVideoUrl(bvid, item.get("page").`as`(Int::class.java)),
                    item.get("first_frame").`as`(String::class.java),
                    ""
                ),
                BilibiliAudioTrack.TrackType.VIDEO,
                bvid,
                item.get("cid").asLong(0),
                this
            ))
        }

        return BasicAudioPlaylist(playlistName, tracks, tracks[page], false)
    }

    private fun loadAudio(trackData: JsonBrowser): AudioTrack {
        val sid = trackData.get("statistic").get("sid").asLong(0).toString()

        val log: Logger = LoggerFactory.getLogger(LavabiliPlugin::class.java)
        log.atInfo().log("DEBUG: ${trackData.text()}")

        return BilibiliAudioTrack(
            AudioTrackInfo(
                trackData.get("title").`as`(String::class.java),
                trackData.get("uname").`as`(String::class.java),
                trackData.get("duration").asLong(0) * 1000,
                "au$sid",
                false,
                getAudioUrl("au$sid")
            ),
            BilibiliAudioTrack.TrackType.AUDIO,
            sid,
            null,
            this
        )
    }

    private fun loadAudioPlaylist(playlistData: JsonBrowser): AudioPlaylist {
        val log: Logger = LoggerFactory.getLogger(LavabiliPlugin::class.java)
        log.atInfo().log("DEBUG: ${playlistData.text()}")

        val playlistName = playlistData.get("title").`as`(String::class.java)
        val sid = playlistData.get("statistic").get("sid").asLong(0).toString()

        val response = httpInterface.execute(HttpGet("${BASE_URL}audio/music-service-c/web/song/of-menu?sid=$sid&pn=1&ps=100"))
        val responseJson = JsonBrowser.parse(response.entity.content)

        val tracksData = responseJson.get("data").get("data").values()
        val tracks = ArrayList<AudioTrack>()

        var curPage = responseJson.get("data").get("curPage").`as`(Int::class.java)
        val pageCount = responseJson.get("data").get("pageCount").`as`(Int::class.java).let {
            if (playlistPageCountConfig == -1) it
            else if (it <= playlistPageCountConfig) it
            else playlistPageCountConfig
        }

        while (curPage <= pageCount) {
            val responsePage = httpInterface.execute(HttpGet("${BASE_URL}audio/music-service-c/web/song/of-menu?sid=$sid&pn=${++curPage}&ps=100"))
            val responseJsonPage = JsonBrowser.parse(responsePage.entity.content)
            tracksData.addAll(responseJsonPage.get("data").get("data").values())
        }

        for (track in tracksData) {
            tracks.add(loadAudio(track))
        }

        return BasicAudioPlaylist(playlistName, tracks, null, false)
    }

    override fun isTrackEncodable(track: AudioTrack): Boolean {
        return true
    }

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        track as BilibiliAudioTrack
        DataFormatTools.writeNullableText(output, track.type.toString())
        DataFormatTools.writeNullableText(output, track.id)
        DataFormatTools.writeNullableText(output, track.cid.toString())
    }

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        val log: Logger = LoggerFactory.getLogger(LavabiliPlugin::class.java)
        val inputString = DataFormatTools.readNullableText(input)
        log.atInfo().log("DEBUG: $inputString")
        val trackType: BilibiliAudioTrack.TrackType = when (inputString) {
            "VIDEO" -> {
                BilibiliAudioTrack.TrackType.VIDEO
            }
            "AUDIO" -> {
                BilibiliAudioTrack.TrackType.AUDIO
            }
            else -> {
                throw IllegalArgumentException("ERROR: Must be VIDEO or AUDIO")
            }
        }
        return BilibiliAudioTrack(trackInfo, trackType, DataFormatTools.readNullableText(input), DataFormatTools.readNullableText(input).toLong(), this)
    }

    override fun shutdown() {
        //
    }

    companion object {
        const val BASE_URL = "https://api.bilibili.com/"
        private val URL_PATTERN = Regex(
            "^https?:\\/\\/(?:(?:www|m)\\.)?bilibili\\.com\\/(?<type>video|audio)\\/(?<id>(?:(?<audioType>am|au)?(?<audioId>[0-9]+))|[A-Za-z0-9]+)\\/?(?:(?:\\?p=(?<page>[\\d]+)(?:&.+)?)?|(?:\\?.*)?)\$"
        ).toPattern()

        private fun getVideoUrl(id: String, page: Int? = null): String {
            return "https://www.bilibili.com/video/$id${if (page != null) "?p=$page" else ""}"
        }

        private fun getAudioUrl(id: String): String {
            return "https://www.bilibili.com/audio/$id"
        }
    }
}