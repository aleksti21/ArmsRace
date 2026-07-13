package aleksti21.armsrace

import dev.architectury.event.events.common.CommandRegistrationEvent

object ArmsRaceMod {
    const val MOD_ID = "armsrace"
    fun init () {
        println("[ArmsRace] Initialized")
        CommandRegistrationEvent.EVENT.register { dispatcher, registryContext, environment ->
            ArmsRaceCommand.register(dispatcher)
        }

        ConfigManager.loadConfigs()
        GameEvents.register()
    }
}