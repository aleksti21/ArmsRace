package aleksti.armsrace

import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
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

    private fun processPlayerJoin(player: ServerPlayer, lobby: LobbyInstance) {
        // 1. Базовые вещи: ставим пустую команду, обнуляем уровень, даем скорборд
        lobby.players[player] = ""
        playerLevels[player.uuid] = 0
        ScoreboardManager.initScoreboard(player, Component.literal(lobby.template.displayName))

        // 2. Если игра УЖЕ на разминке (WAITING)
        if (lobby.state == GameState.WAITING) {
            // Находим команду, в которой меньше всего игроков (балансировщик)
            val teamCounts = lobby.template.teams.associate { it.teamId to 0 }.toMutableMap()
            lobby.players.values.filter { it.isNotEmpty() }.forEach { teamId ->
                teamCounts[teamId] = teamCounts.getOrDefault(teamId, 0) + 1
            }
            val bestTeamId = teamCounts.minByOrNull { it.value }?.key ?: lobby.template.teams.first().teamId

            // Выдаем игроку эту команду
            lobby.players[player] = bestTeamId

            // Делаем всё то же самое, что в start(WAITING)
            inventories[player.uuid] = player.inventory.items.map { it.copy() }
            // Вызови свою функцию прятанья ников (у тебя вроде nametags())
            nametags(player, NametagsFunType.HIDE)
            lobby.teleportPlayerToSpawn(player)
        } else {
            // Если игра еще в LOBBY (ожидание первого/второго игрока), проверяем вармап
            lobby.checkWarmup()
        }

        // 3. Обновляем скорборд (чтобы появилась статистика)
        ScoreboardManager.updateScoreboard(player, lobby)
    }

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
        if (findLobbyByPlayer(player) != null) return "error.armsrace.already_in_lobby" // Я сразу подкинул идею под локализацию 😉

        if (id == null) {
            for (lobby in activeLobbies.values) {
                val totalSpawns = lobby.template.teams.sumOf { it.spawns.size }
                // Пускаем либо в LOBBY, либо в WAITING (если есть места)
                if (lobby.state != GameState.PLAYING && lobby.players.size < totalSpawns) {
                    processPlayerJoin(player, lobby)
                    return "success.armsrace.joined"
                }
            }
            return "error.armsrace.no_lobby"
        } else {
            val lobby = activeLobbies[id] ?: return "error.armsrace.lobby_not_found"

            // Тут тоже проверяем, что игра еще не началась и есть места
            val totalSpawns = lobby.template.teams.sumOf { it.spawns.size }
            if (lobby.state == GameState.PLAYING || lobby.players.size >= totalSpawns) {
                return "error.armsrace.lobby_full_or_playing"
            }

            processPlayerJoin(player, lobby)
            return "success.armsrace.joined"
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
        val world = player.serverLevel().server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(spawn.world))) ?: player.serverLevel().server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("minecraft:overworld")))
        if (spawn.xRot != null) player.xRot = spawn.xRot.toFloat()
        if (spawn.yRot != null) player.yRot = spawn.yRot.toFloat()
        player.teleportTo(world, spawn.x, spawn.y, spawn.z, player.yRot, player.xRot)
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
