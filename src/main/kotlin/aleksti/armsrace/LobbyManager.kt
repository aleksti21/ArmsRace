package aleksti.armsrace

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import java.util.UUID
import kotlin.collections.get

private var nextLobbyId = 1
object LobbyManager {
    val activeLobbies = mutableMapOf<Int, LobbyInstance>()
    val playerLevels = mutableMapOf<UUID, Int>()
    val inventories = mutableMapOf<UUID, List<ItemStack>>()

    private fun success(message: String) = "§a[ArmsRace] $message"
    private fun error(message: String) = "§c[ArmsRace] $message"
    private fun neutral(message: String) = "[ArmsRace] $message"

    fun createLobby(template_id: String): String {
        val id = nextLobbyId++
        val template = ConfigManager.templates.find { it.templateId == template_id } ?: return error("Arena not found!")
        activeLobbies[id] = LobbyInstance(id, template)
        return success("Lobby created successfully: $template_id - $id")
    }

    fun findLobbyByPlayer(player: ServerPlayer): LobbyInstance? {
        for (instance in activeLobbies.values) {
            if (instance.players.contains(player)) {
                return instance
            }
        }
        return null
    }

    fun deleteLobby(lobbyID: Int?): String {
        val lobby = activeLobbies[lobbyID] ?: return error("Lobby not found")
        lobby.state = GameState.LOBBY
        for (player in lobby.players.keys.toList()) removePlayer(player)
        activeLobbies.remove(lobbyID)
        return success("Lobby deleted")
    }

    fun addPlayer(player: ServerPlayer, id: Int? = null): String {
        if (findLobbyByPlayer(player) != null) {
            return error("You are already in a lobby")
        }
        if (id == null) {
            for (lobby in activeLobbies.values) {
                val totalSpawns = lobby.template.teams.sumOf { it.spawns.size }
                if (lobby.state != GameState.PLAYING && lobby.players.size < totalSpawns) {
                    lobby.players[player] = ""
                    playerLevels[player.uuid] = 0
                    lobby.checkWarmup()
                    ScoreboardManager.initScoreboard(player, Component.literal(lobby.template.displayName))
                    return success("You joined lobby ${lobby.id}")
                }
            }
            return error("No available lobby")
        } else {
            val lobby = activeLobbies[id] ?: return error("Lobby not found")
            lobby.players[player] = ""
            playerLevels[player.uuid] = 0
            lobby.checkWarmup()
            ScoreboardManager.updateScoreboard(player, lobby)
            return success("You joined lobby $id")
        }
    }

    fun removePlayer(player: ServerPlayer): String {
        val lobby = findLobbyByPlayer(player) ?: return error("You are not in a lobby")
        val spawn = lobby.template.lobbyCoord
        player.inventory.clearContent()
        val savedItems = inventories.remove(player.uuid)
        savedItems?.forEachIndexed { index, itemStack ->
            player.inventory.setItem(index, itemStack)
        }
        lobby.players.remove(player)
        playerLevels.remove(player.uuid)
        nametags(player, NametagsFunType.SHOW)
        ScoreboardManager.removeScoreboard(player)
        if (lobby.state != GameState.LOBBY) lobby.checkWarmup()
        player.teleportTo(player.serverLevel(), spawn.x, spawn.y, spawn.z, player.yRot, player.xRot)
        player.health = player.maxHealth
        return neutral("You left the game")
    }

    fun startCommand(lobbyID: Int?): String {
        val lobby = activeLobbies[lobbyID] ?: return error("Lobby not found")
        if (lobby.state == GameState.PLAYING) return error("Game is already running")
        if (lobby.state == GameState.WAITING) {
            for (player in lobby.players.keys) playerLevels[player.uuid] = 0
            return lobby.start(GameState.PLAYING)
        } else return lobby.start(GameState.WAITING)
    }
}
