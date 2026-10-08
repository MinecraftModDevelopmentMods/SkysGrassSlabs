package zone.moddev.mc.skysgrassslabs.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.nbt.*;

import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import zone.moddev.mc.skysgrassslabs.MinecraftTestBootstrap;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

class MigrationMappingTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.registerVanilla();
    }

    @Test
    void buildingBricksMetadataIsPreservedForBothSupportedBlocks() {
        for (boolean grass : new boolean[] {false, true}) {
            for (int metadata = 0; metadata <= 1; ++metadata) {
                IBlockState migrated = LegacyMigrationHandler.skyStateFor(grass, metadata);
                assertSame(grass ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB,
                        migrated.getBlock());
                assertEquals(metadata == 0 ? BlockSlab.EnumBlockHalf.TOP
                        : BlockSlab.EnumBlockHalf.BOTTOM,
                        migrated.getValue(BlockSlab.HALF));
            }
        }
    }

    @Test
    void onlyTheOrientationBitIsAcceptedFromHistoricalMetadata() {
        assertEquals(BlockSlab.EnumBlockHalf.TOP,
                LegacyMigrationHandler.skyStateFor(true, 2).getValue(BlockSlab.HALF));
        assertEquals(BlockSlab.EnumBlockHalf.BOTTOM,
                LegacyMigrationHandler.skyStateFor(false, 3).getValue(BlockSlab.HALF));
    }

    @Test void holdersRetainBothOrientationsWithoutPlayableEntries() {
        for(boolean grass:new boolean[]{false,true}) {
            LegacySlabAliasBlock holder=new LegacySlabAliasBlock(grass);
            assertNull(holder.getCreativeTabToDisplayOn());assertFalse(holder.hasTileEntity(holder.getDefaultState()));
            for(int metadata=0;metadata<16;metadata++)assertEquals(metadata&1,holder.getMetaFromState(holder.getStateFromMeta(metadata)));
        }
    }

    @Test void historicalStacksKeepCountsAndUnrelatedNestedData() {
        if(ModBlocks.GRASS_SLAB.getRegistryName()==null)ModBlocks.GRASS_SLAB.setRegistryName("skysgrassslabs","grass_slab");
        if(ModBlocks.DIRT_SLAB.getRegistryName()==null)ModBlocks.DIRT_SLAB.setRegistryName("skysgrassslabs","dirt_slab");
        for(String id:new String[]{"buildingbricks:grass_slab","buildingbricks:dirt_slab","buildingbrickscompatvanilla:grass_slab"}) {
            NBTTagCompound stack=new NBTTagCompound(),tag=new NBTTagCompound();stack.setString("id",id);stack.setByte("Count",(byte)47);stack.setShort("Damage",(short)13);
            tag.setString("custom_name","Original");tag.setInteger("RepairCost",9);tag.setString("material","minecraft:dirt");stack.setTag("tag",tag);
            NBTTagCompound caps=new NBTTagCompound();caps.setString("other_capability","preserved");stack.setTag("ForgeCaps",caps);
            assertTrue(LegacyMigrationHandler.migrateStacksInNbt(stack,null));
            assertEquals(id.contains("dirt_slab")?"skysgrassslabs:dirt_slab":"skysgrassslabs:grass_slab",stack.getString("id"));
            assertEquals(47,stack.getByte("Count"));assertEquals(tag,stack.getCompoundTag("tag"));assertEquals(caps,stack.getCompoundTag("ForgeCaps"));
            NBTTagCompound converted=stack.copy();assertFalse(LegacyMigrationHandler.migrateStacksInNbt(stack,null));assertEquals(converted,stack);
        }
    }
}
