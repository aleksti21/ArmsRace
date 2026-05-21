package aleksti.armsrace

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData

fun getItemFromString(id: String?): Item {
    val location = ResourceLocation.parse(id)
    return BuiltInRegistries.ITEM.getOptional(location).orElse(Items.AIR)
}

private fun createAttachmentTag(attachmentId: String): CompoundTag {
    val tag = CompoundTag()
    tag.putString("id", "tacz:attachment")
    tag.putInt("count", 1)

    val customData = CompoundTag()
    customData.putString("AttachmentId", attachmentId)

    val components = CompoundTag()
    components.put("minecraft:custom_data", customData)

    tag.put("components", components)
    return tag
}

fun taczItem(weaponConfig: Weapon): ItemStack {
    // 1. Создаем базовый предмет (твоя старая функция getItemFromString)
    val item = getItemFromString(weaponConfig.item)
    val stack = ItemStack(item)

    // 2. Проверяем, есть ли настройки TaC:Z
    weaponConfig.taczData?.let { tacz ->
        // Создаем "коробку" для кастомных NBT данных
        val tag = CompoundTag()

        // Обязательный параметр: ID самой пушки (скин/модель)
        tag.putString("GunId", tacz.gunId)

        // Добавляем пулю в патронник, чтобы пушка сразу стреляла (как на твоем скрине)
        tag.putByte("HasBulletInBarrel", 1)

        // Опциональные параметры (если админ указал их в конфиге)
        if (tacz.ammo != null) {
            tag.putInt("GunCurrentAmmoCount", tacz.ammo)
        }
        if (tacz.fireMode != null) {
            tag.putString("GunFireMode", tacz.fireMode)
        }

        // --- МАГИЯ 1.21.1 ---
        // Засовываем наш NBT-тег внутрь компонента minecraft:custom_data

        tacz.scope?.let { tag.put("AttachmentSCOPE", createAttachmentTag(it)) }
        tacz.muzzle?.let { tag.put("AttachmentMUZZLE", createAttachmentTag(it)) }
        tacz.laser?.let { tag.put("AttachmentLASER", createAttachmentTag(it)) }
        tacz.grip?.let { tag.put("AttachmentGRIP", createAttachmentTag(it)) }
        tacz.stock?.let { tag.put("AttachmentSTOCK", createAttachmentTag(it)) }
        tacz.extendedMag?.let { tag.put("AttachmentEXTENDED_MAG", createAttachmentTag(it)) }

        val customData = CustomData.of(tag)
        stack.set(DataComponents.CUSTOM_DATA, customData)
    }

    return stack
}

fun AmmoBox(itemConfig: aleksti.armsrace.Item): ItemStack {
    // 1. Создаем базовый предмет
    val item = getItemFromString(itemConfig.item)
    val stack = ItemStack(item, itemConfig.count)

    // 2. Если это ящик с патронами (есть ammoData)
    itemConfig.ammoData?.let { ammo ->
        val tag = CompoundTag()

        // СЛУЧАЙ 1: Бесконечный ящик для ВСЕХ патронов (isCreative = true, ammoId = нет)
        if (ammo.isCreative && ammo.ammoId.isNullOrEmpty()) {
            tag.putBoolean("AllTypeCreative", true)
        }
        // ОСТАЛЬНЫЕ СЛУЧАИ (ammoId точно указан)
        else if (!ammo.ammoId.isNullOrEmpty()) {
            tag.putString("AmmoId", ammo.ammoId)

            if (ammo.isCreative) {
                // СЛУЧАЙ 2: Бесконечный ящик под КОНКРЕТНЫЙ патрон
                tag.putBoolean("Creative", true)
            } else {
                // СЛУЧАЙ 3: Обычный железный ящик
                tag.putInt("AmmoCount", ammo.ammoCount)
                tag.putInt("Level", ammo.level)
            }
        }

        // Запаковываем в предмет
        val customData = CustomData.of(tag)
        stack.set(DataComponents.CUSTOM_DATA, customData)
    }

    return stack
}