package aleksti.armsrace

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
    fun updateScoreboard(player: ServerPlayer, lobby: LobbyInstance) {
        val objectiveName = "armsrace_board"
        val scoreboard = Scoreboard()

        val objective = Objective(
            scoreboard,
            objectiveName,
            ObjectiveCriteria.DUMMY,
            Component.literal(lobby.template.displayName),
            ObjectiveCriteria.RenderType.INTEGER,
            false,
            BlankFormat.INSTANCE,
        )

        // МЫ БОЛЬШЕ НЕ УДАЛЯЕМ ПАНЕЛЬ КАЖДЫЙ РАЗ!
        // Просто шлём пакет на её создание (клиент сам разберется, если она уже есть)
        player.connection.send(ClientboundSetObjectivePacket(objective, 0))
        player.connection.send(ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective))

        // Вот новый sendLine:
        var lineId = 0 // Порядковый номер строки
        fun sendLine(text: String, score: Int) {
            val packet = ClientboundSetScorePacket(
                "line_${lineId++}", // Владелец теперь ВСЕГДА одинаковый для этой строчки! (line_0, line_1...)
                objectiveName,
                score,
                Optional.of(Component.literal(text)), // А сам текст (Ники, киллы) кладем сюда!
                Optional.empty(),
            )
            player.connection.send(packet)
        }

        // --- ДАЛЬШЕ ТВОЯ ЛОГИКА ---
        var lineScore = 99
        if (lobby.state == GameState.WAITING) {
            sendLine("§fСостояние: §eОжидание", lineScore--)
            sendLine("§fИгроков: §a${lobby.players.size}", lineScore--)
            if (lobby.warmupTicks > 0) {
                // Делим тики на 20, чтобы получить секунды
                sendLine("§fСтарт через: §c${lobby.warmupTicks / 20} сек", lineScore--)
            }
        } else if (lobby.state == GameState.PLAYING) {
            sendLine("§fАрена: §eАрена 1", lineScore--)
            sendLine("§7------------------", lineScore--)

            // Сортируем игроков по фрагам (от лидера к отстающим)
            val sortedPlayers = lobby.players.keys.sortedByDescending { LobbyManager.playerLevels[it.uuid] ?: 0 }

            for ((index, p) in sortedPlayers.withIndex()) {
                val playerTeamId = lobby.players[p] ?: ""

// 2. Ищем эту команду в шаблоне и берем её цвет
                val teamColor = lobby.template.teams.find { it.teamId == playerTeamId }?.colorCode ?: "§f"

// 3. Выводим ник покрашенным!
                val kills = LobbyManager.playerLevels[p.uuid] ?: 0
                sendLine("$teamColor${p.name.string}§f: §a$kills киллов", lineScore--)

                // Выводим только Топ-5 игроков, чтобы панель не уехала в пол
                if (index >= 4) break
            }
            // Пробел в конце строки важен! Если отправить две одинаковые строки "---", игра их склеит.
            sendLine("§7------------------ ", lineScore)
        }
    }

    // Функция для удаления панели (когда игра закончилась)
    fun removeScoreboard(player: ServerPlayer) {
        val scoreboard = Scoreboard()
        val objective = Objective(
            scoreboard,
            "armsrace_board",
            ObjectiveCriteria.DUMMY,
            Component.literal(""),
            ObjectiveCriteria.RenderType.INTEGER,
            false,
            null
        )
        player.connection.send(ClientboundSetObjectivePacket(objective, 1))
    }
}