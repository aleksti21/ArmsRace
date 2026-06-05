package aleksti.armsrace

import dev.architectury.event.EventResult
import dev.architectury.event.events.common.PlayerEvent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import dev.architectury.event.events.common.EntityEvent
import dev.architectury.event.events.common.InteractionEvent
import dev.architectury.event.events.common.TickEvent
import net.minecraft.world.damagesource.DamageSource

object GameEvents {

    fun register() {
        EntityEvent.LIVING_DEATH.register { entity: LivingEntity, damageSource: DamageSource ->

            runIfInGame(entity) { player, lobby ->
                val killer = damageSource.entity as? ServerPlayer
                val killerLobby = killer?.let { LobbyManager.findLobbyByPlayer(it) }
                val level = killer?.let { LobbyManager.playerLevels[it.uuid] }
                if (killer == null || killerLobby != lobby || level == null) {
                    return@runIfInGame EventResult.pass()
                }

                val newLevel = level + 1
                LobbyManager.playerLevels[killer.uuid] = newLevel

                if (lobby.template.weapons.getOrNull(newLevel) == null) {
                    player.health = player.maxHealth

                    if (lobby.state == GameState.PLAYING) {
                        for (p in lobby.players.keys) {
                            ScoreboardManager.removeScoreboard(p)
                            p.health = p.maxHealth
                            p.inventory.clearContent()
                            p.connection.send(ClientboundSetTitlesAnimationPacket(10, 60, 20))
                            p.connection.send(ClientboundSetTitleTextPacket(text("title_game_over", TextType.GAME).withStyle(ChatFormatting.BOLD)))
                            p.connection.send(ClientboundSetSubtitleTextPacket(
                                text("subtitle_game_over", TextType.GAME).withStyle(ChatFormatting.WHITE)
                                    .append(Component.literal(killer.displayName?.string ?: killer.name.string).withStyle(ChatFormatting.GREEN))
                            ))
                        }
                        lobby.state = GameState.FINISHED
                        lobby.warmupTicks = 100

                    } else if (lobby.state == GameState.WAITING) {
                        killer.displayClientMessage(text("last_weapon", TextType.GAME), true)
                        killer.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f)

                        if (lobby.template.instantRespawn == true) {
                            lobby.teleportPlayerToSpawn(player)
                        }
                    }
                    return@runIfInGame EventResult.interruptFalse()

                } else {
                    lobby.given(newLevel, killer)
                    killer.displayClientMessage(text("weapon", TextType.GAME, args = arrayOf(newLevel, lobby.template.weapons.size)), true)
                    killer.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f)

                    for (p in lobby.players.keys) ScoreboardManager.updateScoreboard(p, lobby)
                    if (lobby.state == GameState.LOBBY || lobby.template.instantRespawn == false) {
                        return@runIfInGame EventResult.pass() // Игрок умирает по-настоящему
                    }
                    player.health = player.maxHealth
                    lobby.teleportPlayerToSpawn(player)
                    return@runIfInGame EventResult.interruptFalse() // Отменяем смерть
                }
            }
        }

        PlayerEvent.PLAYER_RESPAWN.register { player, bool, reason ->
            runIfInGame(player) {player, lobby ->
                lobby.teleportPlayerToSpawn(player)
                EventResult.pass()
            }
        }

        InteractionEvent.LEFT_CLICK_BLOCK.register { player, hand, pos, direction ->
            runIfInGame(player, condition = {it.allowBlockBreaking == false}) {player, lobby ->
                EventResult.interruptFalse()
            }
        }

        InteractionEvent.RIGHT_CLICK_BLOCK.register { player, hand, pos, direction ->
            runIfInGame(player, condition = {it.allowBlockBreaking == false}) {player, lobby ->
                player.inventoryMenu.sendAllDataToRemote()
                EventResult.interruptFalse()
            }
        }

        EntityEvent.LIVING_HURT.register { entity, source, f ->
            runIfInGame(entity) {player, lobby ->
                if (lobby.players[player] == lobby.players[source as? ServerPlayer ?: return@register EventResult.pass()]) EventResult.interruptFalse()
                EventResult.pass()
            }
        }

        TickEvent.SERVER_POST.register { player ->
            for (lobby in LobbyManager.activeLobbies.values) {
                lobby.tick()

                if (lobby.state != GameState.LOBBY && lobby.template.infinityfood == true) {
                    for (p in lobby.players.keys) {
                        p.foodData.foodLevel = 20
                        p.foodData.setSaturation(5.0f)
                    }
                }
            }
        }

        PlayerEvent.PLAYER_QUIT.register { player ->
            runIfInGame(player) {p, lobby ->
                LobbyManager.removePlayer(p)
                EventResult.pass()
            }
        }


        PlayerEvent.DROP_ITEM.register { player, itemEntity ->
            runIfInGame(
                entity = player,
                // Код ниже (action) сработает ТОЛЬКО если выбрасывать предметы запрещено
                condition = { it.allowItemToss == false }
            ) { p, lobby ->
                // Возвращаем предмет и отменяем ивент
                p.inventory.add(itemEntity.item)
                p.inventoryMenu.broadcastChanges()
                EventResult.interruptFalse()
            }
        }
    }

    // Вспомогательная функция (где-то в твоих утилитах или Functions.kt)
    inline fun runIfInGame(
        entity: Entity?,
        // Добавили проверку конфига. По умолчанию она всегда выдает true
        condition: (LobbyTemplate) -> Boolean? = { true },
        action: (ServerPlayer, LobbyInstance) -> EventResult
    ): EventResult {
        // Проверяем, что это ServerPlayer
        val player = entity as? ServerPlayer ?: return EventResult.pass()

        // Ищем лобби
        val lobby = LobbyManager.findLobbyByPlayer(player) ?: return EventResult.pass()

        // Если игра идет (не в режиме ожидания) И наше кастомное условие выполнилось
        if (lobby.state != GameState.LOBBY && condition(lobby.template) == true) {
            // Выполняем то, что передали в action
            return action(player, lobby)
        }

        // Если игрок не в игре или условие не совпало — просто идем дальше
        return EventResult.pass()
    }
}