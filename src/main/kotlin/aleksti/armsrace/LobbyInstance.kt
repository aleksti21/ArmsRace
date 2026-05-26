package aleksti.armsrace

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import kotlin.collections.iterator

class LobbyInstance(val id: Int, val template: LobbyTemplate) {
    val players = mutableMapOf<ServerPlayer, String>()
    var state = GameState.LOBBY
    var warmupTicks = -1
    val matchWeapons = mutableListOf<Weapon>()
    val matchArmor = mutableListOf<Armor>()

    private fun success(message: String) = "§a[ArmsRace] $message"
    private fun error(message: String) = "§c[ArmsRace] $message"

    fun start(gameState: GameState): String {
        if (state != GameState.PLAYING && players.isNotEmpty()) {
            val availableTeams = template.teams.map { it.teamId }
            if (availableTeams.isEmpty()) return error("Error: no teams found in template!")
            state = gameState

            if (state == GameState.WAITING) {
                for (pool in template.weapons) matchWeapons.add(pool.options.random())
                for (pool in template.armor) matchArmor.add(pool.options.random())
            }

            for ((index, player) in players.keys.toList().withIndex()) {
                if (gameState == GameState.WAITING) {
                    val assignedTeamId = availableTeams[index % availableTeams.size]
                    players[player] = assignedTeamId
                    LobbyManager.inventories[player.uuid] = player.inventory.items.map  {it.copy()}
                }
                LobbyManager.playerLevels[player.uuid] = 0
                teleportPlayerToSpawn(player)
                player.inventory.clearContent()
                ScoreboardManager.removeScoreboard(player)
                ScoreboardManager.updateScoreboard(player, this)
                given(0, player)
                for (i in template.additionalItems) player.inventory.setItem(i.slot, getAdditionalItem(i))
            }
        } else return error("Not enough players or the game is already running")
        return success("Game started")
    }

    // Функция сама узнает команду игрока и телепортирует его куда надо
    fun teleportPlayerToSpawn(player: ServerPlayer) {
        // 1. Узнаем, за какую команду играет этот игрок (читаем из нашей Мапы)
        val teamId = players[player] ?: return

        // 2. Ищем настройки этой команды в шаблоне
        val teamData = template.teams.find { it.teamId == teamId } ?: return

        // 3. Берем случайный спавн и телепортируем
        if (teamData.spawns.isNotEmpty()) {
            val spawn = teamData.spawns.random()
            player.health = player.maxHealth
            if (template.instantRespawn == false) given(LobbyManager.playerLevels[player.uuid] ?: 0, player)
            player.teleportTo(spawn.x, spawn.y, spawn.z)
            player.addEffect(MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 255, false, false))
        }
    }

    fun checkWarmup() {
        if (template.warmup == true) {
            if (players.size == template.maxPlayers) start(GameState.PLAYING)
            else if (players.size >= template.minPlayers && state != GameState.WAITING) {
                start(GameState.WAITING)
                warmupTicks = template.warmupTime * 20
            } else if (players.size < template.minPlayers && state != GameState.LOBBY) {
//                warmupTicks = -1
                for (p in players.keys) {
                    p.sendSystemMessage(Component.literal("§cМатч отменен: недостаточно игроков!"))
                }
                LobbyManager.deleteLobby(id)
            }
        } else {
            if (players.size == template.maxPlayers) start(GameState.PLAYING)
            else warmupTicks = -1
        }
    }

    fun tick() {
        // Разрешаем тикать и в WAITING, и в FINISHED
        if ((state != GameState.WAITING && state != GameState.FINISHED) || warmupTicks < 0) return

        warmupTicks-- // Отнимаем 1 тик

        if (warmupTicks % 20 == 0) {
            for (player in players.keys) {
                ScoreboardManager.updateScoreboard(player, this)
            }
        }

        // Если время вышло - стартуем!
        if (warmupTicks == 0) {
            if (state == GameState.FINISHED) {
                LobbyManager.deleteLobby(id)
            } else {
                start(GameState.PLAYING)
            }
        }

        // (Для красоты) Если число делится на 20 без остатка (прошла ровно 1 секунда)
        // можно выводить сообщение в ActionBar или чат, если осталось 5, 4, 3...
    }
    
    fun given(level: Int, player: ServerPlayer) {
        player.inventory.selected = 0
        if (matchWeapons[level].taczData != null) player.setItemSlot(EquipmentSlot.MAINHAND, taczItem(matchWeapons[level]))
        else player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack(getItemFromString(matchWeapons[level].item)))
        
        val armorData = matchArmor.getOrNull(level)
        if (armorData != null) {
            val armorMap = mapOf(
                EquipmentSlot.HEAD to armorData.helmet,
                EquipmentSlot.CHEST to armorData.chestplate,
                EquipmentSlot.LEGS to armorData.leggings,
                EquipmentSlot.FEET to armorData.boots,
                EquipmentSlot.OFFHAND to armorData.shield
            )
            
            for ((slot, itemString) in armorMap) {
                if (itemString != null) {
                    player.setItemSlot(slot, ItemStack(getItemFromString(itemString)))
                } else if (armorData.replacePreviousOnEmpty) {
                    player.setItemSlot(slot, ItemStack.EMPTY)
                }
            }
        }
        for (i in matchWeapons[level].additionalItems) player.inventory.setItem(i.slot, getAdditionalItem(i))
    }

    fun getAdditionalItem(itemConfig: Item): ItemStack {
        return if (itemConfig.ammoData != null) AmmoBox(itemConfig) else ItemStack(getItemFromString(itemConfig.item),itemConfig.count)
    }
}