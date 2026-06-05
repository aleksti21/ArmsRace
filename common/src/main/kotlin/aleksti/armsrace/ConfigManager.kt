package aleksti.armsrace

import kotlinx.serialization.json.Json
import net.minecraft.nbt.NbtUtils.prettyPrint
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
                println("[ArmsRace] Config not found. Create a config using the official wiki page.")
                configFile.createNewFile()

            } else {

                // --- РЕЖИМ ЧИТАТЕЛЯ (Файл уже есть) ---
//                println("[ArmsRace] Чтение конфига ArmsRace...")

                // Читаем весь текст из файла
                val jsonText = configFile.readText()

                // МАГИЯ 2: Превращаем текст обратно в объекты Котлина
                templates = jsonFormat.decodeFromString(jsonText)

                println("[ArmsRace] Arenas loaded successfully: ${templates.size}")
            }

        } catch (e: Exception) {
            // Если админ забыл поставить кавычку в JSON, мы поймаем ошибку здесь!
            println("[ArmsRace] CRITICAL ERROR IN THE CONFIG: ${e.message}")
            // Чтобы мод не сломался полностью, выдадим пустой список
            templates = emptyList()
        }
    }
}