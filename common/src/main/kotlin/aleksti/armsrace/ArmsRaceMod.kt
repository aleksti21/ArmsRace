package aleksti.armsrace

import dev.architectury.event.events.common.CommandRegistrationEvent

object ArmsRaceMod {
    const val MOD_ID = "armsrace"
    fun init () {
        println("[ArmsRace] Initialized")
        CommandRegistrationEvent.EVENT.register { dispatcher, registryContext, environment ->
            // dispatcher — это тот самый CommandDispatcher, который нужен для регистрации
            // Вызываем твой метод регистрации команды:
            ArmsRaceCommand.register(dispatcher)
        }

        ConfigManager.loadConfigs()
        GameEvents.register()
    }
}