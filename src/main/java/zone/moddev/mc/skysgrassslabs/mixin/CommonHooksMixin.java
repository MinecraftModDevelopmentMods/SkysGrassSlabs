package zone.moddev.mc.skysgrassslabs.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;

/** Captures NeoForge's legacy registry snapshot before the loader discards it. */
@Mixin(value = CommonHooks.class, remap = false)
abstract class CommonHooksMixin {
    @Inject(method = "readAdditionalLevelSaveData", at = @At("HEAD"))
    private static void skysgrassslabs$captureLegacyLevelData(CompoundTag root,
            LevelStorageSource.LevelDirectory levelDirectory, CallbackInfo callback) {
        LegacyWorldDataHook.captureLegacyLevelData(root, levelDirectory);
    }
}
