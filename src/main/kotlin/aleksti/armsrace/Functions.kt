package aleksti.armsrace

import net.minecraft.ChatFormatting
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
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team

enum class NametagsFunType {
    SHOW,
    HIDE
}

enum class TextType(val prefix: String, val color: ChatFormatting) {
    INFO("info", ChatFormatting.WHITE),
    SUCCESS("success", ChatFormatting.GREEN),
    ERROR("error", ChatFormatting.RED),
    WARNING("warning", ChatFormatting.YELLOW),
    GAME("game", ChatFormatting.GOLD),
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

private fun taczItem(stack: ItemStack, taczData: TaczData): ItemStack {
    val tag = CompoundTag()
    tag.putString("GunId", taczData.gunId)
    tag.putByte("HasBulletInBarrel", 1)

    if (taczData.ammo != null) {
        tag.putInt("GunCurrentAmmoCount", taczData.ammo)
    }
    if (taczData.fireMode != null) {
        tag.putString("GunFireMode", taczData.fireMode)
    }

    taczData.scope?.let { tag.put("AttachmentSCOPE", createAttachmentTag(it)) }
    taczData.muzzle?.let { tag.put("AttachmentMUZZLE", createAttachmentTag(it)) }
    taczData.laser?.let { tag.put("AttachmentLASER", createAttachmentTag(it)) }
    taczData.grip?.let { tag.put("AttachmentGRIP", createAttachmentTag(it)) }
    taczData.stock?.let { tag.put("AttachmentSTOCK", createAttachmentTag(it)) }
    taczData.extendedMag?.let { tag.put("AttachmentEXTENDED_MAG", createAttachmentTag(it)) }

    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
    return stack
}

private fun AmmoBox(stack: ItemStack, AmmoData: AmmoData): ItemStack {
    val tag = CompoundTag()
    if (AmmoData.isCreative && AmmoData.ammoId.isNullOrEmpty()) {
        tag.putBoolean("AllTypeCreative", true)
    }
    // ОСТАЛЬНЫЕ СЛУЧАИ (ammoId точно указан)
    else if (!AmmoData.ammoId.isNullOrEmpty()) {
        tag.putString("AmmoId", AmmoData.ammoId)

        if (AmmoData.isCreative) {
            // СЛУЧАЙ 2: Бесконечный ящик под КОНКРЕТНЫЙ патрон
            tag.putBoolean("Creative", true)
        } else {
            // СЛУЧАЙ 3: Обычный железный ящик
            tag.putInt("AmmoCount", AmmoData.ammoCount)
            tag.putInt("Level", AmmoData.level)
        }
    }
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
    return stack
}

private fun applyEnchantments(stack: ItemStack, player: ServerPlayer, enchantments: List<EnchantData>): ItemStack {
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
}

fun buildAndEnchantItem(config: ConfigItem, player: ServerPlayer): ItemStack {
    // 1. Создаем базовый предмет
    val mcItem = getItemFromString(config.id)
    // У AdditionalItem может быть count, у Weapon его нет. Берем 1 по умолчанию.
    val count = if (config is AdditionalItem) config.count else 1
    val stack = ItemStack(mcItem, count)

    // 2. Если это Weapon — накидываем NBT от TaC:Z
    if (config is Weapon && config.taczData != null) {
        // Твоя логика из taczItem, но применяем её к уже созданному stack
        taczItem(stack, config.taczData)
    }
    // 3. Если это AdditionalItem с патронами (твой бывший AmmoBox)
    else if (config is AdditionalItem && config.ammoData != null) {
        AmmoBox(stack, config.ammoData)
    }

    // 4. Накидываем чары для ЛЮБОГО предмета (Оружие или Вещь)
    if (config.enchantments.isNotEmpty()) {
        applyEnchantments(stack, player, config.enchantments)
    }

    return stack
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

fun text(key: String, type: TextType = TextType.INFO, vararg args: Any): MutableComponent = if (type != TextType.GAME) Component.literal("[ArmsRace] ").append(Component.translatable("${type.prefix}.armsrace.$key", *args).withStyle(type.color)) else Component.translatable("${type.prefix}.armsrace.$key", *args).withStyle(type.color)
