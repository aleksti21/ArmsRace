package aleksti21.armsrace.mixin;

import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FoodData.class)
public class FoodDataMixin {
    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z")
    )
    private boolean redirectNaturalRegeneration(GameRules instance, GameRules.Key<GameRules.BooleanValue> key, Player player) {
        if (key == GameRules.RULE_NATURAL_REGENERATION) {
            if (player instanceof ServerPlayer) {
                if (aleksti21.armsrace.FunctionsKt.shouldDisableRegen((ServerPlayer) player)) {
                    return false;
                }
            }
        }
        return instance.getBoolean(key);
    }
}