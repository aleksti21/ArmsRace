package aleksti.armsrace

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.neoforged.fml.loading.FMLPaths
import java.io.File

object ConfigManager {

    // 1. Настраиваем "Переводчик" JSON
    private val jsonFormat = Json {
        prettyPrint = true // Делает JSON красивым (с переносами строк и отступами), чтобы админу было удобно читать
        ignoreUnknownKeys = true // Если админ напишет в конфиге отсебятину, мод не крашнется, а просто проигнорирует её
    }

    // 2. Указываем путь к файлу: папка_сервера/config/armsrace_arenas.json
    private val configFile: File = FMLPaths.CONFIGDIR.get().resolve("armsrace_arenas.json").toFile()

    // Здесь мы будем хранить загруженные арены в оперативной памяти
    var templates = listOf<LobbyTemplate>()

    // 3. Главная функция. Её нужно будет вызвать ОДИН РАЗ в FMLCommonSetupEvent
    fun loadConfigs() {
        try {
            // ПРОВЕРКА: Существует ли файл на жестком диске?
            if (!configFile.exists()) {

                // --- РЕЖИМ СОЗДАТЕЛЯ (Файла нет) ---
                println("[ArmsRace] Конфиг не найден. Создаю базовый шаблон...")

                // Создаем болванку для примера
                val defaultTemplate = LobbyTemplate(
                    templateId = "vanilla",
                    teams = listOf(TeamTemplate("1", "§b",listOf(SpawnPoint(143.0, -57.0, 28.0))), TeamTemplate("2", "§a",listOf(SpawnPoint(80.0, -60.0, 8.0)))),
                    weapons = listOf(Weapon("minecraft:wooden_sword"), Weapon("minecraft:iron_sword", additionalItems = listOf(Item("minecraft:grass_block", 3, 2))),
                        Weapon("minecraft:diamond_sword")),
                    additionalItems = listOf(Item("minecraft:cobblestone", 54, 7)),
                    armor = listOf(Armor(helmet = "minecraft:iron_helmet", level = 0), Armor(chestplate = "minecraft:iron_chestplate", level = 1)),
                    maxPlayers = 10,
                    warmupTime = 60,
                    lobbyCoord = SpawnPoint(137.0, -54.0, 0.0),
                    minPlayers = 2
                )
                val taczTemplate = LobbyTemplate(
                    templateId = "tacz",
                    teams = listOf(TeamTemplate("1", "§b",listOf(SpawnPoint(143.0, -57.0, 28.0))), TeamTemplate("2", "§a",listOf(SpawnPoint(80.0, -60.0, 8.0)))),
                    maxPlayers = 10,
                    warmupTime = 60,
                    lobbyCoord = SpawnPoint(137.0, -54.0, 0.0),
                    minPlayers = 2,
                    additionalItems = listOf(Item(item = "tacz:ammo_box", slot = 8, ammoData = AmmoData(isCreative = true))),
                    weapons = listOf(Weapon(item = "tacz:modern_kinetic_gun", taczData = TaczData(gunId="tacz:scar_h", ammo = 20, fireMode = "SEMI", laser = "tacz:laser_compact")),
                        Weapon(item = "tacz:modern_kinetic_gun", taczData = TaczData(gunId="tacz:ak47", ammo = 20, fireMode = "AUTO")),
                        Weapon(item = "tacz:modern_kinetic_gun", taczData = TaczData(gunId="tacz:deagle", ammo = 10)))
                )
                val defaultList = listOf(defaultTemplate, taczTemplate)

                // МАГИЯ 1: Превращаем наши объекты Котлина в текст формата JSON
                val jsonText = jsonFormat.encodeToString(defaultList)

                // Записываем этот текст в новый файл
                configFile.writeText(jsonText)

                // Сохраняем в память
                templates = defaultList

            } else {

                // --- РЕЖИМ ЧИТАТЕЛЯ (Файл уже есть) ---
                println("[ArmsRace] Чтение конфига ArmsRace...")

                // Читаем весь текст из файла
                val jsonText = configFile.readText()

                // МАГИЯ 2: Превращаем текст обратно в объекты Котлина
                templates = jsonFormat.decodeFromString(jsonText)

                println("[ArmsRace] Успешно загружено арен: ${templates.size}")
            }

        } catch (e: Exception) {
            // Если админ забыл поставить кавычку в JSON, мы поймаем ошибку здесь!
            println("[ArmsRace] КРИТИЧЕСКАЯ ОШИБКА В КОНФИГЕ: ${e.message}")
            // Чтобы мод не сломался полностью, выдадим пустой список
            templates = emptyList()
        }
    }
}