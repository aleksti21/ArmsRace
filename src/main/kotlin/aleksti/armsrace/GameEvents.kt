package aleksti.armsrace

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.item.ItemTossEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

object GameEvents {

    @SubscribeEvent
    fun onEntityDeath(event: LivingDeathEvent) {
        val entity = event.entity as? ServerPlayer ?: return
        val source = event.source.entity as? ServerPlayer ?: return // as? ServerPlayer ?: return
        val lobby = LobbyManager.findLobbyByPlayer(source) ?: return
        val lobby2 = LobbyManager.findLobbyByPlayer(entity) ?: return
        if (lobby != lobby2) return
        val level = LobbyManager.playerLevels[source.uuid] ?: return
        if (lobby.state == GameState.LOBBY) return
        val newLevel = level + 1
        LobbyManager.playerLevels[source.uuid] = newLevel
        if (lobby.matchWeapons.getOrNull(newLevel) == null) {
            event.isCanceled = true
            entity.health = entity.maxHealth
            if (lobby.state == GameState.PLAYING) {
                for (player in lobby.players.keys) {
                    ScoreboardManager.removeScoreboard(player)
                    player.health = player.maxHealth
                    player.inventory.clearContent()
                    player.connection.send(ClientboundSetTitlesAnimationPacket(10, 60, 20))
//                    player.connection.send(ClientboundSetTitleTextPacket(Component.literal("§6§lИГРА ОКОНЧЕНА")))
//                    player.connection.send(ClientboundSetSubtitleTextPacket(Component.literal("§fПобедил: §a${source.displayName?.string ?: source.name.string}")))
                    player.connection.send(ClientboundSetTitleTextPacket(text("title_game_over", TextType.GAME).withStyle(
                        ChatFormatting.BOLD)))
                    player.connection.send(ClientboundSetSubtitleTextPacket(text("subtitle_game_over", TextType.GAME).withStyle(
                        ChatFormatting.WHITE).append(Component.literal(source.displayName?.string ?: source.name.string).withStyle(
                        ChatFormatting.GREEN))))
                }
                lobby.state = GameState.FINISHED
                lobby.warmupTicks = 100 // 5 секунд = 5 * 20 тиков
            } else if (lobby.state == GameState.WAITING) {
                source.displayClientMessage(text("last_weapon", TextType.GAME), true)
                source.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f)
                if (lobby2.template.instantRespawn == false) return
                lobby2.teleportPlayerToSpawn(entity)
            }


        } else {
            lobby.given(newLevel, source)
//            source.displayClientMessage(Component.literal("§eОружие: ${newLevel}/${lobby.template.weapons.size}"), true)
            source.displayClientMessage(text("weapon", TextType.GAME, args = arrayOf(newLevel, lobby.template.weapons.size)), true)
            source.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f)
            for (player in lobby.players.keys) ScoreboardManager.updateScoreboard(player, lobby)
            if (lobby2.state == GameState.LOBBY) return
            if (lobby2.template.instantRespawn == false) return
            event.isCanceled = true
            lobby2.teleportPlayerToSpawn(entity)
        }
    }

    @SubscribeEvent
    fun onEntityDeathAndRespawn(event: PlayerEvent.PlayerRespawnEvent) = runIfInGame(event.entity) {player, lobby ->
        lobby.teleportPlayerToSpawn(player)
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        for (lobby in LobbyManager.activeLobbies.values) {
            lobby.tick()

            if (lobby.state != GameState.LOBBY) {
                for (player in lobby.players.keys) {
                    player.foodData.foodLevel = 20
                    player.foodData.setSaturation(5.0f)
                }
            }
        }
    }

    @SubscribeEvent
    fun onBlockBreak(event: PlayerInteractEvent.LeftClickBlock) = runIfInGame(event.entity) { player, lobby ->
        if (lobby.template.allowBlockBreaking == false) event.isCanceled = true
    }

    @SubscribeEvent
    fun onBlockPlace(event: PlayerInteractEvent.RightClickBlock) = runIfInGame(event.entity) { player, lobby ->
        if (lobby.template.allowBlockBreaking == false) {
            event.isCanceled = true
            event.cancellationResult = InteractionResult.FAIL
            // Принудительно обновляем инвентарь клиента, чтобы "исчезнувший" блок вернулся
            player.inventoryMenu.sendAllDataToRemote()
        }
    }

    @SubscribeEvent
    fun onPlayerDamage(event: LivingIncomingDamageEvent) = runIfInGame(event.entity) { player, lobby ->
        val source = event.source.entity as? ServerPlayer ?: return
        if (lobby.players[player] == lobby.players[source]) event.isCanceled = true
    }

    @SubscribeEvent
    fun onItemToss(event: ItemTossEvent) = runIfInGame(event.player) {player, lobby -> if (lobby.template.allowItemToss == false) event.isCanceled = true }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val lobby = LobbyManager.findLobbyByPlayer(player) ?: return
        LobbyManager.removePlayer(player)
    }

    private inline fun runIfInGame(entity: Entity?, action: (ServerPlayer, LobbyInstance) -> Unit) {
        val player = entity as? ServerPlayer ?: return
        val lobby = LobbyManager.findLobbyByPlayer(player) ?: return
        if (lobby.state != GameState.LOBBY) {
            action(player, lobby) // Вызываем переданную логику
        }
    }
}