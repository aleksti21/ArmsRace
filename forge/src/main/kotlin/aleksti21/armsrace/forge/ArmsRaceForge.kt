package aleksti21.armsrace.neoforge

import aleksti21.armsrace.ArmsRaceMod
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.ModContainer
import net.minecraftforge.fml.common.Mod

@Mod(ArmsRaceMod.MOD_ID)
class ArmsRaceForge(modEventBus: IEventBus, modContainer: ModContainer) {
    init {
        ArmsRaceMod.init()
    }
}