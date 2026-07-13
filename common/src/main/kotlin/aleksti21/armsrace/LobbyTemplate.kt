package aleksti21.armsrace
import kotlinx.serialization.Serializable

enum class GameState {
    WAITING,
    PLAYING,
    LOBBY,
    FINISHED
}

interface ConfigItem {
    val id: String
    val nbt: String?
    val enchantments: List<EnchantData>
        get() = emptyList()
    val unbreakable: Boolean?
        get() = true
}

@Serializable
data class SpawnPoint(
    val x: Double,
    val y: Double,
    val z: Double,
    val world: String = "minecraft:overworld",
    val yRot: Double? = null,
    val xRot: Double? = null,
)
@Serializable
data class TeamTemplate(
    val teamId: String,
    val colorCode: String = "§f",
    val spawns: List<SpawnPoint>
)

@Serializable
data class MapTemplate(
    val id: String,
    val name: String = id,
    val teams: List<TeamTemplate>,
)

@Serializable
data class AdditionalItem(
    override val id: String,
    override val enchantments: List<EnchantData> = emptyList(),
    override val nbt: String? = null,
    override val unbreakable: Boolean? = null,
    val count: Int = 1,
    val slot: Int,
    val ammoData: AmmoData? = null,
): ConfigItem

@Serializable
data class Armor(
    val helmet: ArmorPiece? = null,
    val chestplate: ArmorPiece? = null,
    val leggings: ArmorPiece? = null,
    val boots: ArmorPiece? = null,
    val shield: ArmorPiece? = null,
    val replacePreviousOnEmpty: Boolean = false,
    val teamId: String? = null,
)

@Serializable
data class ArmorPiece(
    override val id: String,
    override val enchantments: List<EnchantData> = emptyList(),
    override val nbt: String? = null,
    override val unbreakable: Boolean? = true,
): ConfigItem

@Serializable
data class ArmorPool(
    val options: List<Armor>,
)

@Serializable
data class Weapon(
    override val id: String,
    override val enchantments: List<EnchantData> = emptyList(),
    override val nbt: String? = null,
    override val unbreakable: Boolean? = true,
    val taczData: TaczData? = null,
    val additionalItems: List<AdditionalItem> = emptyList(),
    val teamId: String? = null,
): ConfigItem

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
    val maps: List<MapTemplate>,
    val instantRespawn: Boolean = true,
    val allowBlockBreaking: Boolean = false,
    val weapons: List<WeaponPool>,
    val armor: List<ArmorPool> = emptyList(),
    val additionalItems: List<AdditionalItem> = emptyList(),
    val minPlayers: Int,
    val maxPlayers: Int,
    val warmupTime: Int = 60,
    val warmup: Boolean = true,
    val lobbyCoord: SpawnPoint,
    val allowItemToss: Boolean = false,
    val infinityFood: Boolean = true,
    val gamemode: String = "adventure",
    val regeneration: Boolean = false,
)
