package aleksti21.armsrace.neoforge

import aleksti21.armsrace.ArmsRaceMod
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod

@Mod(ArmsRaceMod.MOD_ID)
class ArmsRaceForge(modEventBus: IEventBus, modContainer: ModContainer) {
    init {
        ArmsRaceMod.init()
    }
}