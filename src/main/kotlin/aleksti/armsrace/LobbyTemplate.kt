package aleksti.armsrace
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
    val id: String,
    val enchantments: List<EnchantData>
)

@Serializable
data class AdditionalItem(
    val item: String,
    val count: Int = 1,
    val enchantments: List<EnchantData> = emptyList(),
    val slot: Int,
    val level: Int? = null,
    val ammoData: AmmoData? = null,
)

@Serializable
data class Armor(
    val helmet: String? = null,
    val chestplate: String? = null,
    val leggings: String? = null,
    val boots: String? = null,
    val shield: String? = null,
    val level: Int,
    val replacePreviousOnEmpty: Boolean = true,
    val enchantments: List<EnchantData> = emptyList(),
)

@Serializable
data class ArmorPool(
    val options: List<Armor>,
)

@Serializable
data class Weapon(
    val item: String,
    val enchantments: List<EnchantData> = emptyList(),
    val taczData: TaczData? = null,
    val additionalItems: List<Item> = emptyList(),
)

@Serializable
data class WeaponPool(
    val options: List<Weapon>,
)

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
data class EnchantData(
    val id: String,
    val level: Int = 1,
)

@Serializable
data class LobbyTemplate(
    val templateId: String,
    val displayName: String = "§6§lГОНКА ВООРУЖЕНИЙ",
    val teams: List<TeamTemplate>,
    val instantRespawn: Boolean = true,
    val allowBlockBreaking: Boolean = false,
    val weapons: List<WeaponPool>,
    val armor: List<ArmorPool> = emptyList(),
    val additionalItems: List<Item> = emptyList(),
    val minPlayers: Int,
    val maxPlayers: Int,
    val warmupTime: Int = 60,
    val warmup: Boolean = true,
    val lobbyCoord: SpawnPoint,
)
