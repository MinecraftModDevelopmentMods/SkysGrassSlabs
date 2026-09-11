package zone.moddev.mc.skysgrassslabs.compat;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Runtime checks for the optional Grass Slabs content migration. */
@GameTestHolder(value = SkysGrassSlabs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GrassSlabsMigrationGameTests {
    private GrassSlabsMigrationGameTests() {
    }

    @GameTest(template = "empty", batch = "grassslabscompat001")
    public static void slabStatesConvertWithoutLosingNativeProperties(GameTestHelper helper) {
        require(helper, GrassSlabsCompat.hasLegacyAliases(),
                "the absent source mod did not register compatibility holders");

        BlockState grass = GrassSlabsCompat.grassSlab().defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.TOP)
                .setValue(SlabBlock.WATERLOGGED, true);
        BlockState convertedGrass = GrassSlabsMigrationHandler.replacement(grass,
                GrassSlabsMigrationHandler.LegacyKind.GRASS_SLAB,
                Blocks.DIRT.defaultBlockState());
        require(helper, convertedGrass.is(ModBlocks.GRASS_SLAB.get())
                        && convertedGrass.getValue(SlabBlock.TYPE) == SlabType.TOP
                        && convertedGrass.getValue(SlabBlock.WATERLOGGED)
                        && !convertedGrass.getValue(SnowyDirtBlock.SNOWY),
                "grass slab state was not preserved");

        BlockState dirt = GrassSlabsCompat.dirtSlab().defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState convertedDirt = GrassSlabsMigrationHandler.replacement(dirt,
                GrassSlabsMigrationHandler.LegacyKind.DIRT_SLAB,
                Blocks.DIRT.defaultBlockState());
        require(helper, convertedDirt.is(ModBlocks.DIRT_SLAB.get())
                        && convertedDirt.getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                "dirt slab state was not preserved");

        BlockState path = GrassSlabsCompat.pathSlab().defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.TOP)
                .setValue(SlabBlock.WATERLOGGED, true);
        BlockState convertedPath = GrassSlabsMigrationHandler.replacement(path,
                GrassSlabsMigrationHandler.LegacyKind.PATH_SLAB,
                Blocks.DIRT.defaultBlockState());
        require(helper, convertedPath.is(ModBlocks.PATH_SLAB.get())
                        && convertedPath.getValue(SlabBlock.TYPE) == SlabType.TOP
                        && convertedPath.getValue(SlabBlock.WATERLOGGED),
                "path slab state was not preserved");

        for (GrassSlabsMigrationHandler.LegacyKind kind : List.of(
                GrassSlabsMigrationHandler.LegacyKind.GRASS_SLAB,
                GrassSlabsMigrationHandler.LegacyKind.DIRT_SLAB,
                GrassSlabsMigrationHandler.LegacyKind.PATH_SLAB)) {
            BlockState source = switch (kind) {
                case GRASS_SLAB -> GrassSlabsCompat.grassSlab().defaultBlockState();
                case DIRT_SLAB -> GrassSlabsCompat.dirtSlab().defaultBlockState();
                case PATH_SLAB -> GrassSlabsCompat.pathSlab().defaultBlockState();
                case GRASS_CARPET -> throw new IllegalStateException();
            };
            BlockState full = GrassSlabsMigrationHandler.replacement(
                    source.setValue(SlabBlock.TYPE, SlabType.DOUBLE), kind,
                    Blocks.DIRT.defaultBlockState());
            require(helper, full.is(kind.fullBlock()), "double slab did not normalize: " + kind);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "grassslabscompat002")
    public static void carpetAndItemConversionRespectTheSafeBoundary(GameTestHelper helper) {
        BlockState carpet = GrassSlabsCompat.grassCarpet().defaultBlockState();
        BlockState safe = GrassSlabsMigrationHandler.replacement(carpet,
                GrassSlabsMigrationHandler.LegacyKind.GRASS_CARPET,
                Blocks.DIRT.defaultBlockState());
        BlockState unsafe = GrassSlabsMigrationHandler.replacement(carpet,
                GrassSlabsMigrationHandler.LegacyKind.GRASS_CARPET,
                Blocks.STONE.defaultBlockState());
        require(helper, safe != null && safe.is(ModBlocks.TURF.get()),
                "dirt-supported grass carpet did not become turf");
        require(helper, unsafe == null, "unsafe grass carpet was not preserved");

        ListTag items = new ListTag();
        for (String id : List.of("grass_slab", "dirt_slab", "dirt_path_slab",
                "grass_carpet")) {
            CompoundTag item = new CompoundTag();
            item.putString("id", GrassSlabsCompat.MOD_ID + ":" + id);
            item.putByte("Count", (byte) 3);
            CompoundTag custom = new CompoundTag();
            custom.putString("migration_test", id);
            item.put("tag", custom);
            items.add(item);
        }
        require(helper, GrassSlabsMigrationHandler.migrateStacksInNbt(items, null),
                "supported item stacks were not migrated");
        List<String> expected = List.of("skysgrassslabs:grass_slab",
                "skysgrassslabs:dirt_slab", "skysgrassslabs:path_slab",
                "skysgrassslabs:turf");
        for (int index = 0; index < items.size(); ++index) {
            CompoundTag item = items.getCompound(index);
            require(helper, expected.get(index).equals(item.getString("id")),
                    "wrong migrated item ID at index " + index);
            require(helper, item.getByte("Count") == 3,
                    "item count changed at index " + index);
            require(helper, item.getCompound("tag").contains("migration_test"),
                    "custom item data was lost at index " + index);
        }
        helper.succeed();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
