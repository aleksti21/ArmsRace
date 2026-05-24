package aleksti.armsrace
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class GameState {
    WAITING,
    PLAYING,
    LOBBY,
    FINISHED
}

@Serializable
data class SpawnPoint(
    val x: Double,
    val y: Double,
    val z: Double,
//    val world: String
)
@Serializable
data class TeamTemplate(
    val teamId: String, // Например: "red", "blue" или "terrorists"
    val colorCode: String = "§f",
    val spawns: List<SpawnPoint>
)

@Serializable
data class Item(
    val item: String,
    val count: Int = 1,
//    val enchantments: List<String>,
    val slot: Int,
    val level: Int? = null,
    val ammoData: AmmoData? = null,
)

// Общий интерфейс для элементов списка брони
@Serializable
sealed interface ArmorEntry

@Serializable
@SerialName("armor")
data class Armor(
    val helmet: String? = null,
    val chestplate: String? = null,
    val leggings: String? = null,
    val boots: String? = null,
    val shield: String? = null,
    val level: Int? = null,
    val replacePreviousOnEmpty: Boolean = true,
//    val enchantments: List<String>,
) : ArmorEntry

@Serializable
@SerialName("group")
data class ArmorGroup(
    val armors: List<Armor>,
    val randomArmor: Boolean = true,
) : ArmorEntry

// Общий интерфейс для элементов списка оружия
@Serializable
sealed interface WeaponEntry

@Serializable
@SerialName("weapon")
data class Weapon(
    val item: String,
//    val level: Int,
//    val enchantments: List<String>,
    val taczData: TaczData? = null,
    val additionalItems: List<Item> = emptyList(),
) : WeaponEntry

@Serializable
@SerialName("group")
data class WeaponGroup(
    val weapons: List<Weapon>,
    val additionalItems: List<Item> = emptyList(),
    val randomWeapons: Boolean = true,
) : WeaponEntry

@Serializable
data class TaczData(
    val gunId: String,
    val ammo: Int? = null,
    val fireMode: String? = null,
    val scope: String? = null,
    val muzzle: String? = null,
    val laser: String? = null,
    val grip: String? = null,
    val stock: String? = null,
    val extendedMag: String? = null,
)

@Serializable
data class AmmoData(
    val ammoId: String? = null,
    val ammoCount: Int = 1,
    val isCreative: Boolean = false,
    val level: Int = 0,
)

@Serializable
data class LobbyTemplate(
    val templateId: String,
    val displayName: String = "§6§lГОНКА ВООРУЖЕНИЙ",
    val teams: List<TeamTemplate>,
    val instantRespawn: Boolean = true,
    val allowBlockBreaking: Boolean = false,
    val randomWeapons: Boolean = false,
    val weapons: List<WeaponEntry>,
    val armor: List<ArmorEntry> = emptyList(),
    val additionalItems: List<Item> = emptyList(),
    val minPlayers: Int,
    val maxPlayers: Int,
    val warmupTime: Int = 60,
    val warmup: Boolean = true,
    val lobbyCoord: SpawnPoint,
)
