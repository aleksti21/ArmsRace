package aleksti21.armsrace

import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
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

    private fun processPlayerJoin(player: ServerPlayer, lobby: LobbyInstance) {
        // 1. Базовые вещи: ставим пустую команду, обнуляем уровень, даем скорборд
        lobby.players[player] = ""
        playerLevels[player.uuid] = 0
        ScoreboardManager.initScoreboard(player, Component.literal(lobby.template.displayName))

        // 2. Если игра УЖЕ на разминке (WAITING)
        if (lobby.state == GameState.WAITING) {
            // Находим команду, в которой меньше всего игроков (балансировщик)
            val teamCounts = lobby.currentMap.teams.associate { it.teamId to 0 }.toMutableMap()
            lobby.players.values.filter { it.isNotEmpty() }.forEach { teamId ->
                teamCounts[teamId] = teamCounts.getOrDefault(teamId, 0) + 1
            }
            val bestTeamId = teamCounts.minByOrNull { it.value }?.key ?: lobby.currentMap.teams.first().teamId

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

    fun createLobby(template_id: String): MutableComponent {
        val id = nextLobbyId++
        val template = ConfigManager.templates.find { it.templateId == template_id } ?: return text("no_lobby",
            TextType.ERROR)
        activeLobbies[id] = LobbyInstance(id, template)
        return text("lobby_created", TextType.SUCCESS,template_id, id)
    }

    fun findLobbyByPlayer(player: ServerPlayer): LobbyInstance? {
        for (instance in activeLobbies.values) {
            if (instance.players.contains(player)) {
                return instance
            }
        }
        return null
    }

    fun deleteLobby(lobbyID: Int?): MutableComponent {
        val lobby = activeLobbies[lobbyID] ?: return text("no_lobby", TextType.ERROR)
        lobby.state = GameState.LOBBY
        for (player in lobby.players.keys.toList()) removePlayer(player)
        activeLobbies.remove(lobbyID)
        return text("lobby_deleted", TextType.SUCCESS)
    }

    fun addPlayer(player: ServerPlayer, id: Int? = null): MutableComponent {
        if (findLobbyByPlayer(player) != null) return text("already_in_lobby", TextType.ERROR)

        if (id == null) {
            // Подключение в ЛЮБОЕ свободное лобби
            for (lobby in activeLobbies.values) {
                // Проверяем, что игра не началась и есть места
                if ((lobby.state == GameState.LOBBY || lobby.state == GameState.WAITING) && lobby.players.size < lobby.template.maxPlayers) {
                    processPlayerJoin(player, lobby)
                    return text("add_player", TextType.SUCCESS)
                }
            }
            return text("no_lobby", TextType.ERROR)
        } else {
            // Подключение по ID
            val lobby = activeLobbies[id] ?: return text("no_lobby", TextType.ERROR)

            // Та же самая проверка на стадию игры и количество мест
            if ((lobby.state == GameState.LOBBY || lobby.state == GameState.WAITING) && lobby.players.size < lobby.template.maxPlayers) {
                processPlayerJoin(player, lobby)
                return text("add_player", TextType.SUCCESS)
            } else {
                // Если лобби заполнено или игра уже идет (PLAYING/FINISHED)
                return text("no_lobby", TextType.ERROR) // Можешь потом заменить на текст "Лобби заполнено"
            }
        }
    }

    fun removePlayer(player: ServerPlayer): MutableComponent {
        val lobby = findLobbyByPlayer(player) ?: return text("no_lobby", TextType.ERROR)
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
        return text("remove_player", TextType.SUCCESS)
    }

    fun startCommand(lobbyID: Int?): MutableComponent {
        val lobby = activeLobbies[lobbyID] ?: return text("no_lobby", TextType.ERROR)
        if (lobby.state == GameState.PLAYING) return text("already_run", TextType.ERROR)
        if (lobby.state == GameState.WAITING) {
            for (player in lobby.players.keys) playerLevels[player.uuid] = 0
            return lobby.start(GameState.PLAYING)
        } else return lobby.start(GameState.WAITING)
    }
}
