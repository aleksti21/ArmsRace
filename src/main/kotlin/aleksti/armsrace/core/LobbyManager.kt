package aleksti.armsrace.core

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import java.util.UUID

object LobbyManager {
    val activeLobbies = mutableMapOf<Int, LobbyInstance>()
    val playerLevels = mutableMapOf<UUID, Int>()
    val inventories = mutableMapOf<UUID, List<ItemStack>>()

    private fun success(message: String) = "§a[ArmsRace] $message"
    private fun error(message: String) = "§c[ArmsRace] $message"
    private fun neutral(message: String) = "[ArmsRace] $message"

    fun createLobby(template_id: String): String {
        val id = activeLobbies.size + 1
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
                    ScoreboardManager.updateScoreboard(player, lobby)
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
        ScoreboardManager.removeScoreboard(player)
        if (lobby.state != GameState.LOBBY) lobby.checkWarmup()
        player.teleportTo(spawn.x, spawn.y, spawn.z)
        player.health = 20f
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

    fun getItemFromString(id: String?): Item {
        val location = ResourceLocation.parse(id)
        return BuiltInRegistries.ITEM.getOptional(location).orElse(Items.AIR)
    }

    private fun createAttachmentTag(attachmentId: String): CompoundTag {
        val tag = CompoundTag()
        tag.putString("id", "tacz:attachment")
        tag.putInt("count", 1)

        val customData = CompoundTag()
        customData.putString("AttachmentId", attachmentId)

        val components = CompoundTag()
        components.put("minecraft:custom_data", customData)

        tag.put("components", components)
        return tag
    }

    fun taczItem(weaponConfig: Weapon): ItemStack {
        // 1. Создаем базовый предмет (твоя старая функция getItemFromString)
        val item = getItemFromString(weaponConfig.item)
        val stack = ItemStack(item)

        // 2. Проверяем, есть ли настройки TaC:Z
        weaponConfig.taczData?.let { tacz ->
            // Создаем "коробку" для кастомных NBT данных
            val tag = CompoundTag()

            // Обязательный параметр: ID самой пушки (скин/модель)
            tag.putString("GunId", tacz.gunId)

            // Добавляем пулю в патронник, чтобы пушка сразу стреляла (как на твоем скрине)
            tag.putByte("HasBulletInBarrel", 1)

            // Опциональные параметры (если админ указал их в конфиге)
            if (tacz.ammo != null) {
                tag.putInt("GunCurrentAmmoCount", tacz.ammo)
            }
            if (tacz.fireMode != null) {
                tag.putString("GunFireMode", tacz.fireMode)
            }

            // --- МАГИЯ 1.21.1 ---
            // Засовываем наш NBT-тег внутрь компонента minecraft:custom_data
            val customData = CustomData.of(tag)
            stack.set(DataComponents.CUSTOM_DATA, customData)

            tacz.scope?.let { tag.put("AttachmentSCOPE", createAttachmentTag(it)) }
            tacz.muzzle?.let { tag.put("AttachmentMUZZLE", createAttachmentTag(it)) }
            tacz.laser?.let { tag.put("AttachmentLASER", createAttachmentTag(it)) }
            tacz.grip?.let { tag.put("AttachmentGRIP", createAttachmentTag(it)) }
            tacz.stock?.let { tag.put("AttachmentSTOCK", createAttachmentTag(it)) }
            tacz.extendedMag?.let { tag.put("AttachmentEXTENDED_MAG", createAttachmentTag(it)) }
        }

        return stack
    }

    fun AmmoBox(itemConfig: aleksti.armsrace.core.Item): ItemStack {
        // 1. Создаем базовый предмет
        val item = getItemFromString(itemConfig.item)
        val stack = ItemStack(item, itemConfig.count)

        // 2. Если это ящик с патронами (есть ammoData)
        itemConfig.ammoData?.let { ammo ->
            val tag = CompoundTag()

            // СЛУЧАЙ 1: Творческий бокс с любыми патронами (ammoId не указан)
            if (ammo.ammoId == null) {
                tag.putByte("AllTypeCreative", 1)
            }
            // СЛУЧАИ 2, 3 и 4: Боксы под конкретный патрон (ammoId указан)
            else {
                // Обязательные параметры для конкретного патрона
                tag.putString("AmmoId", ammo.ammoId)

                // Если бокс бесконечный (творческий)
                if (ammo.isCreative) {
                    tag.putByte("Creative", 1)
                }
                // Если бокс обычный (железный, на N патронов)
                else {
                    tag.putInt("AmmoCount", ammo.ammoCount)
                    tag.putInt("Level", ammo.level)
                }
            }

            // Запихиваем NBT-тег внутрь компонента minecraft:custom_data
            val customData = CustomData.of(tag)
            stack.set(DataComponents.CUSTOM_DATA, customData)
        }

        return stack
    }
}
