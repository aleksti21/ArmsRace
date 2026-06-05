package aleksti.armsrace.neoforge

import aleksti.armsrace.ArmsRaceMod
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod

@Mod(ArmsRaceMod.MOD_ID)
class ArmsRaceForge(modEventBus: IEventBus, modContainer: ModContainer) {
    init {
        println("🔨 [ArmsRace] Загрузка NeoForge версии...")
        ArmsRaceMod.init()
    }
}