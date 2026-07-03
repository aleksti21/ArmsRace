package aleksti21.armsrace

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import java.util.Optional
import net.minecraft.network.chat.numbers.BlankFormat

object ScoreboardManager {
    private val objectiveName = "armsrace_board"

    private fun getDummyObjective(title: Component): Objective {
        return Objective(
            Scoreboard(),
            objectiveName,
            ObjectiveCriteria.DUMMY,
            title,
            ObjectiveCriteria.RenderType.INTEGER,
            false,
            BlankFormat.INSTANCE
        )
    }

    fun initScoreboard(player: ServerPlayer, title: Component) {
        val objective = getDummyObjective(title)
        player.connection.send(ClientboundSetObjectivePacket(objective, 0)) // 0 = Создать
        player.connection.send(ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective))
    }

    fun updateScoreboard(player: ServerPlayer, lobby: LobbyInstance) {
        var lineId = 0
        fun sendLine(text: Component, score: Int) {
            val packet = ClientboundSetScorePacket(
                "line_${lineId++}",
                objectiveName,
                score,
                Optional.of(text),
                Optional.empty()
            )
            player.connection.send(packet)
        }
        var lineScore = 99
        if (lobby.state == GameState.WAITING) {
            sendLine(
                Component.translatable("armsrace.scoreboard.state")
                    .append(Component.literal(": "))
                    .append(Component.translatable("armsrace.scoreboard.waiting").withStyle(ChatFormatting.YELLOW)),
                lineScore--
            )
            sendLine(
                Component.translatable("armsrace.scoreboard.players")
                    .append(Component.literal(": "))
                    .append(Component.literal(lobby.players.size.toString()).withStyle(ChatFormatting.GREEN)),
                lineScore--
            )
            if (lobby.warmupTicks > 0) {
                sendLine(
                    Component.translatable("armsrace.scoreboard.starting_in")
                        .append(Component.literal(": "))
                        .append(
                            Component.translatable("armsrace.scoreboard.seconds", lobby.warmupTicks / 20)
                                .withStyle(ChatFormatting.RED)
                        ),
                    lineScore--
                )
            }
        } else if (lobby.state == GameState.PLAYING) {
            val map = Component.literal(lobby.currentMap.name).withStyle(ChatFormatting.GOLD)
            sendLine(
                Component.translatable("armsrace.scoreboard.arena")
                    .append(Component.literal(": "))
                    .append(map),
                lineScore--
            )
            sendLine(Component.literal("------------------").withStyle(ChatFormatting.GRAY), lineScore--)

            val sortedPlayers = lobby.players.keys.sortedByDescending { LobbyManager.playerLevels[it.uuid] ?: 0 }

            for ((index, p) in sortedPlayers.withIndex()) {
                val playerTeamId = lobby.players[p] ?: ""
                val team = lobby.currentMap.teams.find { it.teamId == playerTeamId }
                val teamColor = team?.colorCode?.let { ChatFormatting.getByName(it) } ?: ChatFormatting.WHITE

                val kills = Component.literal("${LobbyManager.playerLevels[p.uuid] ?: 0}").withStyle(ChatFormatting.GOLD)
                val nameComp = Component.literal(p.name.string).withStyle(teamColor)
                val finalLine = Component.empty()
                    .append(nameComp)
                    .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                    .append(kills)

                sendLine(finalLine, lineScore--)

                if (index >= 4) break
            }
            sendLine(Component.literal("------------------ ").withStyle(ChatFormatting.GRAY), lineScore)
        }
    }

    fun removeScoreboard(player: ServerPlayer) {
        val objective = getDummyObjective(Component.empty())
        player.connection.send(ClientboundSetObjectivePacket(objective, 1)) // 1 = Удалить
    }
}