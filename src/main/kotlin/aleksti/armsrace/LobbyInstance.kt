package aleksti.armsrace

import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
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
    lateinit var currentMap: MapTemplate

    fun start(gameState: GameState): MutableComponent {
        if (state != GameState.PLAYING && players.isNotEmpty()) {
            state = gameState
            if (state == GameState.WAITING) {
                for (pool in template.weapons) matchWeapons.add(pool.options.random())
                for (pool in template.armor) matchArmor.add(pool.options.random())
                currentMap = template.maps.random()
            }
            val availableTeams = currentMap.teams.map { it.teamId }
            if (availableTeams.isEmpty()) return text("no_teams", TextType.ERROR)

            for (p in players.keys.toList()) LobbyManager.playerLevels[p.uuid] = 0
            for ((index, player) in players.keys.toList().withIndex()) {
                if (gameState == GameState.WAITING) {
                    val assignedTeamId = availableTeams[index % availableTeams.size]
                    players[player] = assignedTeamId
                    LobbyManager.inventories[player.uuid] = player.inventory.items.map  {it.copy()}
                    nametags(player, NametagsFunType.HIDE)
                }
                ScoreboardManager.updateScoreboard(player, this)
                teleportPlayerToSpawn(player)
                player.inventory.clearContent()
                given(0, player)
                for (i in template.additionalItems) player.inventory.setItem(i.slot, buildAndEnchantItem(i, player))
            }
        } else return text("alredy_run", TextType.ERROR)
        return text("game_start", TextType.SUCCESS)
    }

    // Функция сама узнает команду игрока и телепортирует его куда надо
    fun teleportPlayerToSpawn(player: ServerPlayer) {
        // 1. Узнаем, за какую команду играет этот игрок (читаем из нашей Мапы)
        val teamId = players[player] ?: return

        // 2. Ищем настройки этой команды в шаблоне
        val teamData = currentMap.teams.find { it.teamId == teamId } ?: return

        // 3. Берем случайный спавн и телепортируем
        if (teamData.spawns.isNotEmpty()) {
//            val spawn = teamData.spawns.random()
            val index = players.filterValues { it == teamId }.keys.toList().indexOf(player) % teamData.spawns.size
            val spawn = teamData.spawns[index]
            val world = player.serverLevel().server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(spawn.world))) ?: player.serverLevel()
            player.health = player.maxHealth
            if (template.instantRespawn == false) given(LobbyManager.playerLevels[player.uuid] ?: 0, player)
            if (spawn.xRot != null) player.xRot = spawn.xRot.toFloat()
            if (spawn.yRot != null) player.yRot = spawn.yRot.toFloat()
            player.teleportTo(world, spawn.x, spawn.y, spawn.z, player.yRot, player.xRot)
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
        player.setItemSlot(EquipmentSlot.MAINHAND, buildAndEnchantItem(matchWeapons[level], player))
        
        val armorData = matchArmor.getOrNull(level)
        if (armorData != null) {
            val armorMap = mapOf(
                EquipmentSlot.HEAD to armorData.helmet,
                EquipmentSlot.CHEST to armorData.chestplate,
                EquipmentSlot.LEGS to armorData.leggings,
                EquipmentSlot.FEET to armorData.boots,
                EquipmentSlot.OFFHAND to armorData.shield
            )
            
            for ((slot, armorItem) in armorMap) {
                if (armorItem != null) {
                    player.setItemSlot(slot, buildAndEnchantItem(armorItem, player))
                } else if (armorData.replacePreviousOnEmpty) {
                    player.setItemSlot(slot, ItemStack.EMPTY)
                }
            }
        } else {
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY)
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY)
            player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY)
            player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY)
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY)
        }
        for (i in matchWeapons[level].additionalItems) player.inventory.setItem(i.slot, buildAndEnchantItem(i,player))
    }
}