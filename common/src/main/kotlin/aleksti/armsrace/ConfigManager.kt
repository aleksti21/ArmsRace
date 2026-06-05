package aleksti.armsrace

import dev.architectury.platform.Platform
import kotlinx.serialization.json.Json
import java.io.File

object ConfigManager {

    private val jsonFormat = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val configFile: File = Platform.getConfigFolder().resolve("armsrace_arenas.json").toFile()
    var templates = listOf<LobbyTemplate>()

    fun loadConfigs() {
        try {
            if (!configFile.exists()) {
                println("[ArmsRace] Config not found. Create a config using the official wiki page.")
                configFile.createNewFile()

            } else {
                val jsonText = configFile.readText()
                templates = jsonFormat.decodeFromString(jsonText)

                println("[ArmsRace] Arenas loaded successfully: ${templates.size}")
            }

        } catch (e: Exception) {
            println("[ArmsRace] CRITICAL ERROR IN THE CONFIG: ${e.message}")
            templates = emptyList()
        }
    }
}