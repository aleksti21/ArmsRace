package aleksti21.armsrace

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
                    ctx.source.sendSuccess({ text("command_armsrace", TextType.ERROR) }, false)
                    1
                }
                .then(
                    Commands.literal("create")
                        .requires { sourceStack -> sourceStack.hasPermission(2) }
                        .executes { ctx ->
                            ctx.source.sendSuccess({ text("command_create", TextType.ERROR) }, false)
                            1
                        }

                        .then(
                            Commands.argument("template_id", StringArgumentType.word())
                                .suggests { context, builder ->
                                    val availableIds = ConfigManager.templates.map { it.templateId }
                                    SharedSuggestionProvider.suggest(availableIds, builder)
                                }
                                .executes { ctx ->
                                    ctx.source.sendSuccess({LobbyManager.createLobby(StringArgumentType.getString(ctx, "template_id"))}, false)
                                    1
                                }
                        ))
                .then(
                    Commands.literal("join")
                        .executes { ctx ->
                            ctx.source.sendSuccess({LobbyManager.addPlayer(ctx.source.playerOrException) }, false)
                            1
                        }
                        .then(
                            Commands.argument("lobby_id", IntegerArgumentType.integer(1))
                                .executes {ctx ->
                                    ctx.source.sendSuccess({
                                        LobbyManager.addPlayer(ctx.source.playerOrException,
                                            IntegerArgumentType.getInteger(ctx, "lobby_id")) }, false)
                                    1
                                })
                )
                .then(
                    Commands.literal("start")
                        .requires { sourceStack -> sourceStack.hasPermission(2) }
                        .executes { ctx ->
                            ctx.source.sendSuccess({LobbyManager.startCommand(LobbyManager.findLobbyByPlayer(ctx.source.playerOrException)?.id)}, false)
                            1
                        }
                        .then(
                            Commands.argument("start_id", IntegerArgumentType.integer(1))
                                .executes {ctx ->
                                    ctx.source.sendSuccess({LobbyManager.startCommand(IntegerArgumentType.getInteger(ctx, "start_id"))}, false)
                                    1
                                })
                )
                .then(
                    Commands.literal("leave")
                        .executes { ctx ->
                            ctx.source.sendSuccess({LobbyManager.removePlayer(ctx.source.playerOrException)}, false)
                            1
                        }
                )
                .then(
                    Commands.literal("stop")
                        .requires { sourceStack -> sourceStack.hasPermission(2) }
                        .executes { ctx ->
                            ctx.source.sendSuccess({LobbyManager.deleteLobby(LobbyManager.findLobbyByPlayer(ctx.source.playerOrException)?.id)}, false)
                            1
                        }
                        .then(
                            Commands.argument("stop_id", IntegerArgumentType.integer(1))
                                .executes { ctx ->
                                    ctx.source.sendSuccess({ LobbyManager.deleteLobby(IntegerArgumentType.getInteger(ctx, "stop_id"))}, false)
                                    1
                                })
                )
                .then(
                    Commands.literal("setteam")
                        .requires { sourceStack -> sourceStack.hasPermission(2) }
                        .then(
                            Commands.argument("target", EntityArgument.player())
                                .then(
                                    Commands.argument("team_id", StringArgumentType.word())
                                        .executes { ctx ->
                                            val targetPlayer = EntityArgument.getPlayer(ctx, "target")
                                            val newTeamId = StringArgumentType.getString(ctx, "team_id")

                                            // Ищем лобби этого игрока
                                            val lobby = LobbyManager.findLobbyByPlayer(targetPlayer)
                                            if (lobby == null) {
                                                ctx.source.sendFailure(text("player_not_in_lobby", TextType.ERROR, arrayOf(targetPlayer.name.string)))
                                                return@executes 0
                                            }

                                            val teamExists = lobby.currentMap.teams.any { it.teamId == newTeamId }
                                            if (!teamExists) {
                                                ctx.source.sendFailure(text("team_is_not_exist", TextType.ERROR, arrayOf(newTeamId)))
                                                return@executes 0
                                            }
                                            lobby.players[targetPlayer] = newTeamId
                                            lobby.teleportPlayerToSpawn(targetPlayer)
                                            for (p in lobby.players.keys) {
                                                ScoreboardManager.updateScoreboard(p, lobby)
                                            }

                                            ctx.source.sendSuccess({ text("set_player_team", TextType.SUCCESS, arrayOf(targetPlayer.name.string, newTeamId)) }, true)
                                            1
                                        }
                                )
                        )
                )
                .then(
                    Commands.literal("reload")
                        .requires { sourceStack -> sourceStack.hasPermission(2) } // Только для админов
                        .executes { ctx ->
                            ConfigManager.loadConfigs()
                            ctx.source.sendSuccess({ text("reload", TextType.SUCCESS) }, true)
                            1
                        }
                )
        )
    }
}