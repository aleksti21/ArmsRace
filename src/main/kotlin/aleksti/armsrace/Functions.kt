package aleksti.armsrace

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team

enum class NametagsFunType {
    SHOW,
    HIDE
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

        tacz.scope?.let { tag.put("AttachmentSCOPE", createAttachmentTag(it)) }
        tacz.muzzle?.let { tag.put("AttachmentMUZZLE", createAttachmentTag(it)) }
        tacz.laser?.let { tag.put("AttachmentLASER", createAttachmentTag(it)) }
        tacz.grip?.let { tag.put("AttachmentGRIP", createAttachmentTag(it)) }
        tacz.stock?.let { tag.put("AttachmentSTOCK", createAttachmentTag(it)) }
        tacz.extendedMag?.let { tag.put("AttachmentEXTENDED_MAG", createAttachmentTag(it)) }

        val customData = CustomData.of(tag)
        stack.set(DataComponents.CUSTOM_DATA, customData)
    }

    return stack
}

fun AmmoBox(itemConfig: aleksti.armsrace.Item): ItemStack {
    // 1. Создаем базовый предмет
    val item = getItemFromString(itemConfig.item)
    val stack = ItemStack(item, itemConfig.count)

    // 2. Если это ящик с патронами (есть ammoData)
    itemConfig.ammoData?.let { ammo ->
        val tag = CompoundTag()

        // СЛУЧАЙ 1: Бесконечный ящик для ВСЕХ патронов (isCreative = true, ammoId = нет)
        if (ammo.isCreative && ammo.ammoId.isNullOrEmpty()) {
            tag.putBoolean("AllTypeCreative", true)
        }
        // ОСТАЛЬНЫЕ СЛУЧАИ (ammoId точно указан)
        else if (!ammo.ammoId.isNullOrEmpty()) {
            tag.putString("AmmoId", ammo.ammoId)

            if (ammo.isCreative) {
                // СЛУЧАЙ 2: Бесконечный ящик под КОНКРЕТНЫЙ патрон
                tag.putBoolean("Creative", true)
            } else {
                // СЛУЧАЙ 3: Обычный железный ящик
                tag.putInt("AmmoCount", ammo.ammoCount)
                tag.putInt("Level", ammo.level)
            }
        }

        // Запаковываем в предмет
        val customData = CustomData.of(tag)
        stack.set(DataComponents.CUSTOM_DATA, customData)
    }

    return stack
}

fun applyEnchantments(stack: ItemStack, player: ServerPlayer, enchantments: List<EnchantData>): ItemStack {
    if (enchantments != emptyList()) {
        val enchRegistry = player.server.registryAccess().registryOrThrow(Registries.ENCHANTMENT)

        for ((enchId, level) in enchantments.map { it.id to it.level }) {
            val location = ResourceLocation.parse(enchId)
            val enchKey = ResourceKey.create(Registries.ENCHANTMENT, location)
            val holder = enchRegistry.getHolder(enchKey)

            holder.ifPresent { ench ->
                stack.enchant(ench, level)
            }
        }

        return stack
    } else return stack
}

fun nametags(player: ServerPlayer, type: NametagsFunType) {
    // 1. Создаем фейковую команду
    val lobby = LobbyManager.findLobbyByPlayer(player) ?: return
    val team = PlayerTeam(Scoreboard(), "hidden_${lobby.id}")

    if (type == NametagsFunType.HIDE) {
        team.nameTagVisibility = Team.Visibility.NEVER
        player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true))
        lobby.players.keys.forEach { p ->
            player.connection.send(
                ClientboundSetPlayerTeamPacket.createPlayerPacket(
                    team,
                    p.scoreboardName,
                    ClientboundSetPlayerTeamPacket.Action.ADD
                )
            )
        }
    } else if (type == NametagsFunType.SHOW) player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(team))
}
