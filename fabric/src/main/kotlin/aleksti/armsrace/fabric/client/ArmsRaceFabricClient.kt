package aleksti.armsrace.fabric.client

import aleksti.armsrace.ArmsRaceMod
import net.fabricmc.api.ClientModInitializer

class ArmsRaceFabricClient: ClientModInitializer {
    override fun onInitializeClient() {
        println("[ArmsRace] Fabric client version")
        ArmsRaceMod.init()
    }
}