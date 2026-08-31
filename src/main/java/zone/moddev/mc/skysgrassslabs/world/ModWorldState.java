package zone.moddev.mc.skysgrassslabs.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Minimal permanent schema marker for future cross-version migrations. */
public final class ModWorldState extends SavedData {
    public static final String DATA_NAME = "skysgrassslabs_world_state";
    public static final int SCHEMA_VERSION = 1;

    private int schemaVersion;

    private ModWorldState(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public static ModWorldState get(ServerLevel level) {
        ModWorldState state = level.getDataStorage().computeIfAbsent(ModWorldState::load,
                () -> new ModWorldState(SCHEMA_VERSION), DATA_NAME);
        if (state.schemaVersion < SCHEMA_VERSION) {
            state.schemaVersion = SCHEMA_VERSION;
        }
        state.setDirty();
        return state;
    }

    private static ModWorldState load(CompoundTag tag) {
        return new ModWorldState(tag.getInt("schema_version"));
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("schema_version", schemaVersion);
        return tag;
    }
}
