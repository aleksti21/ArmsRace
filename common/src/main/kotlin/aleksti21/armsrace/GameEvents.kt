package aleksti21.armsrace

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
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffects

object GameEvents {

    fun register() {
        EntityEvent.LIVING_DEATH.register { entity: LivingEntity, damageSource: DamageSource ->

            runIfInGame(entity) { player, lobby ->
                val killer = damageSource.entity as? ServerPlayer
                val killerLobby = killer?.let { LobbyManager.findLobbyByPlayer(it) }
                val level = killer?.let { LobbyManager.playerLevels[it.uuid] }
                if (killer == null || killerLobby != lobby || level == null || killer == entity) {
                    if (lobby.template.instantRespawn == true) {
                        player.health = player.maxHealth
                        lobby.teleportPlayerToSpawn(player)
                        val victimLevel = LobbyManager.playerLevels[player.uuid] ?: 0
                        lobby.given(victimLevel, player)

                        return@runIfInGame EventResult.interruptFalse()
                    }
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

        PlayerEvent.PLAYER_RESPAWN.register { player, bool ->
            runIfInGame(player) {player, lobby ->
                lobby.teleportPlayerToSpawn(player)
                return@runIfInGame EventResult.pass()
            }
        }

        InteractionEvent.LEFT_CLICK_BLOCK.register { player, hand, pos, direction ->
            runIfInGame(player, condition = {it.allowBlockBreaking == false}) {player, lobby ->
                return@runIfInGame EventResult.interruptFalse()
            }
        }

        InteractionEvent.RIGHT_CLICK_BLOCK.register { player, hand, pos, direction ->
            runIfInGame(player, condition = {it.allowBlockBreaking == false}) {player, lobby ->
                player.inventoryMenu.sendAllDataToRemote()
                return@runIfInGame EventResult.interruptFalse()
            }
        }

        EntityEvent.LIVING_HURT.register { entity, source, f ->
            runIfInGame(entity) {player, lobby ->
                val attacker = source.entity as? ServerPlayer ?: return@runIfInGame EventResult.pass()
                if (lobby.players[player] == lobby.players[attacker]) return@runIfInGame EventResult.interruptFalse()
                return@runIfInGame EventResult.pass()
            }
        }

        TickEvent.SERVER_POST.register { server ->
            for (lobby in LobbyManager.activeLobbies.values) {
                lobby.tick()

                if (lobby.state != GameState.LOBBY && lobby.template.infinityFood == true) {
                    for (p in lobby.players.keys) {
                        p.foodData.foodLevel = 20
                        p.foodData.setSaturation(5.0f)
                    }
                }
            }
        }

        TickEvent.PLAYER_POST.register { player ->
            val p = player as? ServerPlayer ?: return@register
            if (p.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                val world = p.serverLevel()

                world.sendParticles(
                    ParticleTypes.END_ROD, // Тип частиц (парящие белые звёздочки)
                    p.x, p.y + 1.0, p.z,   // Спавним на высоте груди (1 метр над ногами)
                    1,                     // Всего 3 частицы за тик (чтобы вообще не нагружать сервер)
                    0.3, 0.5, 0.3,         // Небольшой разброс по размерам тела игрока
                    0.02                   // Минимальная скорость парения
                )
            }
        }


        PlayerEvent.DROP_ITEM.register { player, itemEntity ->
            runIfInGame(
                entity = player,
                condition = { it.allowItemToss == false }
            ) { p, lobby ->
                p.inventory.add(itemEntity.item)
                p.inventoryMenu.broadcastChanges()
                return@runIfInGame EventResult.interruptFalse()
            }
        }
    }

    private inline fun runIfInGame(
        entity: Entity?,
        condition: (LobbyTemplate) -> Boolean? = { true },
        action: (ServerPlayer, LobbyInstance) -> EventResult
    ): EventResult {
        val player = entity as? ServerPlayer ?: return EventResult.pass()
        val lobby = LobbyManager.findLobbyByPlayer(player) ?: return EventResult.pass()
        if (lobby.state != GameState.LOBBY && condition(lobby.template) == true) {
            return action(player, lobby)
        }
        return EventResult.pass()
    }
}