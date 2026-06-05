package aleksti.armsrace.fabric

import net.fabricmc.api.ModInitializer
import aleksti.armsrace.ArmsRaceMod

class ArmsRaceFabric: ModInitializer {
    override fun onInitialize() {
        println("[ArmsRace] Fabric version")
        ArmsRaceMod.init()
    }
}