package aleksti.armsrace.neoforge

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import aleksti.armsrace.ArmsRaceMod

const val ID = "armsrace"
@Mod(ID)
class ArmsRaceForge(modEventBus: IEventBus) {

    init {
        println("[ArmsRace] Neoforge version")
        ArmsRaceMod.init()
    }
}