package zone.moddev.mc.skysgrassslabs.mixin;

import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;

/** Captures Forge's legacy registry snapshot before Forge discards it. */
@Mixin(value = ForgeHooks.class, remap = false)
abstract class ForgeHooksMixin {
    @Inject(method = "readAdditionalLevelSaveData", at = @At("HEAD"))
    private static void skysgrassslabs$captureLegacyLevelData(
            LevelStorageSource.LevelStorageAccess access,
            LevelStorageSource.LevelDirectory levelDirectory, CallbackInfo callback) {
        LegacyWorldDataHook.captureLegacyLevelData(access, levelDirectory);
    }
}
