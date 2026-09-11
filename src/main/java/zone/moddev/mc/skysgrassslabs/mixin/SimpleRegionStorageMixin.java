package zone.moddev.mc.skysgrassslabs.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;

/** Wraps vanilla data fixing so recovered legacy slab states survive conversion. */
@Mixin(SimpleRegionStorage.class)
abstract class SimpleRegionStorageMixin {
    @Inject(
            method = "upgradeChunkTag(Lnet/minecraft/nbt/CompoundTag;ILnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("HEAD"))
    private void skysgrassslabs$prepareLegacyChunk(CompoundTag chunk,
            int fallbackDataVersion, @Nullable CompoundTag context,
            CallbackInfoReturnable<CompoundTag> callback) {
        LegacyWorldDataHook.prepareLegacyChunk(chunk);
    }

    @Inject(
            method = "upgradeChunkTag(Lnet/minecraft/nbt/CompoundTag;ILnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"), cancellable = true)
    private void skysgrassslabs$finalizeLegacyChunk(CompoundTag chunk,
            int fallbackDataVersion, @Nullable CompoundTag context,
            CallbackInfoReturnable<CompoundTag> callback) {
        callback.setReturnValue(LegacyWorldDataHook.finalizeLegacyChunk(
                callback.getReturnValue()));
    }
}
