package aleksti.armsrace

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component

object ArmsRaceCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("armsrace")
                .executes { ctx ->
                    // Это сработает, если игрок введет просто /armsrace
                    ctx.source.sendSuccess({ text("command_armsrace", TextType.ERROR) }, false)
                    1
                }
//                .then(
//                    Commands.literal("create")
//                        .requires { sourceStack -> sourceStack.hasPermission(2) }
//                        .executes { ctx ->
//                            ctx.source.sendSuccess({ text("command_create", TextType.ERROR) }, false)
//                            1
//                        }
//
//                        .then(
//                            Commands.argument("template_id", StringArgumentType.word())
//                                .suggests { context, builder ->
//                                    // Берем список всех загруженных шаблонов и достаем из них template_id
//                                    val availableIds = ConfigManager.templates.map { it.templateId }
//                                    // Отдаем их Майнкрафту, чтобы он показал их в чате
//                                    SharedSuggestionProvider.suggest(availableIds, builder)
//                                }
//                                .executes { ctx ->
//                                    ctx.source.sendSuccess({LobbyManager.createLobby(StringArgumentType.getString(ctx, "template_id"))}, false)
//                                    1
//                                }
//                        ))
//                .then(
//                    Commands.literal("join")
//                        .executes { ctx ->
//                            ctx.source.sendSuccess({LobbyManager.addPlayer(ctx.source.playerOrException) }, false)
//                            1
//                        }
//                        .then(
//                            Commands.argument("lobby_id", IntegerArgumentType.integer(1))
//                                .executes {ctx ->
//                                    ctx.source.sendSuccess({
//                                        LobbyManager.addPlayer(ctx.source.playerOrException,
//                                            IntegerArgumentType.getInteger(ctx, "lobby_id")) }, false)
//                                    1
//                                })
//                )
//                .then(
//                    Commands.literal("start")
//                        .requires { sourceStack -> sourceStack.hasPermission(2) }
//                        .executes { ctx ->
//                            ctx.source.sendSuccess({LobbyManager.startCommand(LobbyManager.findLobbyByPlayer(ctx.source.playerOrException)?.id)}, false)
//                            1
//                        }
//                        .then(
//                            Commands.argument("start_id", IntegerArgumentType.integer(1))
//                                .executes {ctx ->
//                                    ctx.source.sendSuccess({LobbyManager.startCommand(IntegerArgumentType.getInteger(ctx, "start_id"))}, false)
//                                    1
//                                })
//                )
//                .then(
//                    Commands.literal("leave")
//                        .executes { ctx ->
//                            ctx.source.sendSuccess({LobbyManager.removePlayer(ctx.source.playerOrException)}, false)
//                            1
//                        }
//                )
//                .then(
//                    Commands.literal("stop")
//                        .requires { sourceStack -> sourceStack.hasPermission(2) }
//                        .executes { ctx ->
//                            ctx.source.sendSuccess({LobbyManager.deleteLobby(LobbyManager.findLobbyByPlayer(ctx.source.playerOrException)?.id)}, false)
//                            1
//                        }
//                        .then(
//                            Commands.argument("stop_id", IntegerArgumentType.integer(1))
//                                .executes { ctx ->
//                                    ctx.source.sendSuccess({ LobbyManager.deleteLobby(IntegerArgumentType.getInteger(ctx, "stop_id"))}, false)
//                                    1
//                                })
//                )
//                .then(
//                    Commands.literal("setteam")
//                        .requires { sourceStack -> sourceStack.hasPermission(2) } // Только для админов
//                        .then(
//                            Commands.argument("target", EntityArgument.player()) // Аргумент 1: ИГРОК
//                                .then(
//                                    Commands.argument("team_id", StringArgumentType.word()) // Аргумент 2: КОМАНДА
//                                        .executes { ctx ->
//                                            // Получаем введенные данные
//                                            val targetPlayer = EntityArgument.getPlayer(ctx, "target")
//                                            val newTeamId = StringArgumentType.getString(ctx, "team_id")
//
//                                            // Ищем лобби этого игрока
//                                            val lobby = LobbyManager.findLobbyByPlayer(targetPlayer)
//                                            if (lobby == null) {
//                                                ctx.source.sendFailure(Component.literal("Этот игрок не находится в лобби!"))
//                                                return@executes 0
//                                            }
//
//                                            // Проверяем, существует ли такая команда в шаблоне лобби
//                                            val teamExists = lobby.currentMap.teams.any { it.teamId == newTeamId }
//                                            if (!teamExists) {
//                                                ctx.source.sendFailure(Component.literal("Команды $newTeamId не существует в этой арене!"))
//                                                return@executes 0
//                                            }
//
//                                            // --- САМА ЛОГИКА ---
//                                            // 1. Меняем команду в мапе
//                                            lobby.players[targetPlayer] = newTeamId
//
//                                            // 2. Телепортируем на новую базу
//                                            lobby.teleportPlayerToSpawn(targetPlayer)
//
//                                            // 3. Обновляем скорборд, чтобы цвет ника изменился
//                                            for (p in lobby.players.keys) {
//                                                ScoreboardManager.updateScoreboard(p, lobby)
//                                            }
//
//                                            ctx.source.sendSuccess({ Component.literal("§aИгрок ${targetPlayer.name.string} переведен в команду $newTeamId!") }, true)
//                                            1
//                                        }
//                                )
//                        )
//                )
//                .then(
//                    Commands.literal("reload")
//                        .requires { sourceStack -> sourceStack.hasPermission(2) } // Только для админов
//                        .executes { ctx ->
//                            // Вызываем ту самую функцию, которая читает JSON
//                            ConfigManager.loadConfigs()
//
//                            ctx.source.sendSuccess({ Component.literal("§a[ArmsRace] Конфиги успешно перезагружены!") }, true)
//                            1
//                        }
//                )
        )
    }
}