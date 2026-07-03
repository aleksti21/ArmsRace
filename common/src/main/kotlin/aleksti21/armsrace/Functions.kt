package aleksti21.armsrace

import aleksti21.nbttodata.applySNBT
import aleksti21.nbttodata.editData
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
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
    tag.putByte("Count", 1) // В 1.20.1 "Count" пишется с большой буквы и является байтом!

    val innerTag = CompoundTag()
    innerTag.putString("AttachmentId", attachmentId)

    tag.put("tag", innerTag) // В 1.20.1 вложения хранятся в старом теге "tag"
    return tag
}

fun buildAndEnchantItem(config: ConfigItem, player: ServerPlayer): ItemStack {
    val mcItem = getItemFromString(config.id)
    val count = if (config is AdditionalItem) config.count else 1
    val stack = ItemStack(mcItem, count)
    val registries = player.server.registryAccess()

    // 1. Используем наш editData DSL для сборки базовых свойств
    stack.editData(registries) {
        if (config.unbreakable == true) {
            unbreakable = true
        }

        // Зачарования применяются одной строчкой
        if (config.enchantments.isNotEmpty()) {
            enchantments {
                config.enchantments.forEach { data ->
                    data.id(data.level) // Вызываем перегрузку оператора "id"(level)
                }
            }
        }

        // Кастомные NBT оружия и патронов пишем в customData
        if (config is Weapon && config.taczData != null) {
            customData {
                val tData = config.taczData
                putString("GunId", tData.gunId)
                putByte("HasBulletInBarrel", 1)
                tData.ammo?.let { putInt("GunCurrentAmmoCount", it) }
                tData.fireMode?.let { putString("GunFireMode", it) }

                tData.scope?.let { put("AttachmentSCOPE", createAttachmentTag(it)) }
                tData.muzzle?.let { put("AttachmentMUZZLE", createAttachmentTag(it)) }
                tData.laser?.let { put("AttachmentLASER", createAttachmentTag(it)) }
                tData.grip?.let { put("AttachmentGRIP", createAttachmentTag(it)) }
                tData.stock?.let { put("AttachmentSTOCK", createAttachmentTag(it)) }
                tData.extendedMag?.let { put("AttachmentEXTENDED_MAG", createAttachmentTag(it)) }
            }
        } else if (config is AdditionalItem && config.ammoData != null) {
            customData {
                val aData = config.ammoData
                if (aData.isCreative && aData.ammoId.isNullOrEmpty()) {
                    putBoolean("AllTypeCreative", true)
                } else if (!aData.ammoId.isNullOrEmpty()) {
                    putString("AmmoId", aData.ammoId)
                    if (aData.isCreative) {
                        putBoolean("Creative", true)
                    } else {
                        putInt("AmmoCount", aData.ammoCount)
                        putInt("Level", aData.level)
                    }
                }
            }
        }
    }

    // 2. А теперь магия! Любой кастомный NBT-тег из конфига транслируется в компоненты!
    if (config.nbt != null) {
        stack.applySNBT(config.nbt!!, registries)
    }

    return stack
}

fun nametags(player: ServerPlayer, type: NametagsFunType) {
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
