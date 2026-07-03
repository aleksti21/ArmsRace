package aleksti21.armsrace.fabric

import net.fabricmc.api.ModInitializer
import aleksti21.armsrace.ArmsRaceMod

class ArmsRaceFabric: ModInitializer {
    override fun onInitialize() {
        ArmsRaceMod.init()
    }
}