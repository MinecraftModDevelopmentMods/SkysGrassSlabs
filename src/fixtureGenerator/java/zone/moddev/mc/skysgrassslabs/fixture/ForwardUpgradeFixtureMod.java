package zone.moddev.mc.skysgrassslabs.fixture;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Creates the immutable Minecraft 1.10.2 input used by forward-upgrade tests. */
@Mod(modid = ForwardUpgradeFixtureMod.MOD_ID, name = "Sky's Grass Slabs Forward Upgrade Fixture",
        version = "1", dependencies = "required-after:skysgrassslabs")
public final class ForwardUpgradeFixtureMod {
    public static final String MOD_ID = "skysgrassslabsforwardfixture";
    private static final BlockPos ORIGIN = new BlockPos(8, 65, 8);

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) throws Exception {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer world = server.worldServerForDimension(0);
        world.getGameRules().setOrCreateGameRule("randomTickSpeed", "0");
        world.getWorldInfo().setRaining(false);
        world.getWorldInfo().setThundering(false);
        world.setSpawnPoint(ORIGIN);

        clearFixtureArea(world);
        placeSlab(world, ORIGIN, ModBlocks.DIRT_SLAB, BlockSlab.EnumBlockHalf.TOP);
        placeSlab(world, ORIGIN.east(), ModBlocks.DIRT_SLAB, BlockSlab.EnumBlockHalf.BOTTOM);
        placeSlab(world, ORIGIN.east(2), ModBlocks.GRASS_SLAB, BlockSlab.EnumBlockHalf.TOP);
        placeSlab(world, ORIGIN.east(3), ModBlocks.GRASS_SLAB, BlockSlab.EnumBlockHalf.BOTTOM);
        placeSlab(world, ORIGIN.east(4), ModBlocks.PATH_SLAB, BlockSlab.EnumBlockHalf.TOP);
        placeSlab(world, ORIGIN.east(5), ModBlocks.PATH_SLAB, BlockSlab.EnumBlockHalf.BOTTOM);
        support(world, ORIGIN.east(6));
        world.setBlockState(ORIGIN.east(6), defaultState(ModBlocks.TURF), 3);

        BlockPos chestPos = ORIGIN.south(2);
        support(world, chestPos);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 3);
        TileEntityChest chest = (TileEntityChest) world.getTileEntity(chestPos);
        ItemStack grassStack = new ItemStack(ModBlocks.GRASS_SLAB, 3, 0);
        NBTTagCompound grassData = new NBTTagCompound();
        grassData.setString("fixture", "retained");
        grassStack.setTagCompound(grassData);
        chest.setInventorySlotContents(0, new ItemStack(ModBlocks.DIRT_SLAB, 2, 0));
        chest.setInventorySlotContents(1, grassStack);
        chest.setInventorySlotContents(2, new ItemStack(ModBlocks.PATH_SLAB, 4, 0));
        chest.setInventorySlotContents(3, new ItemStack(ModBlocks.TURF, 5, 0));
        chest.markDirty();

        EntityItem dropped = new EntityItem(world, ORIGIN.getX() + 4.5D,
                ORIGIN.getY() + 1.0D, ORIGIN.getZ() + 2.5D,
                new ItemStack(ModBlocks.GRASS_SLAB, 6, 0));
        dropped.setNoPickupDelay();
        world.spawnEntity(dropped);

        ModWorldState state = ModWorldState.get(world);
        state.recordChunk();
        state.recordChunk();
        state.recordGrassBlocks(3, 0);
        state.recordGrassBlocks(5, 1);
        state.recordDirtBlocks(7, 0);
        state.recordDirtBlocks(11, 1);
        state.recordGrassItems(13);
        state.recordDirtItems(17);
        state.recordUnsupported("fixture:unsupported_shape", 19);

        File worldDirectory = world.getSaveHandler().getWorldDirectory();
        Files.write(new File(worldDirectory, "skysgrassslabs-forward-fixture.properties").toPath(),
                Arrays.asList(
                        "source_minecraft=1.10.2",
                        "source_mod_version=1.0.0.110021",
                        "source_jar_sha256=2030960E217C3F61AE4919C91058696B02F9FAE570BE1CD7B698696EA7BEB861",
                        "origin=8,65,8",
                        "expected_blocks=7",
                        "expected_chest_stacks=4",
                        "expected_entity_stacks=1",
                        "schema_version=1"), StandardCharsets.UTF_8);

        world.saveAllChunks(true, null);
        server.initiateShutdown();
    }

    private static void placeSlab(WorldServer world, BlockPos pos,
            net.minecraft.block.Block block, BlockSlab.EnumBlockHalf half) {
        support(world, pos);
        IBlockState state = block.getDefaultState().withProperty(BlockSlab.HALF, half);
        world.setBlockState(pos, state, 3);
    }

    private static void support(WorldServer world, BlockPos pos) {
        world.setBlockState(pos.down(), Blocks.DIRT.getDefaultState(), 3);
    }

    private static IBlockState defaultState(net.minecraft.block.Block block) {
        return block.getDefaultState();
    }

    private static void clearFixtureArea(WorldServer world) {
        for (int y = ORIGIN.getY() - 1; y <= ORIGIN.getY() + 3; ++y) {
            for (int z = ORIGIN.getZ() - 2; z <= ORIGIN.getZ() + 4; ++z) {
                for (int x = ORIGIN.getX() - 2; x <= ORIGIN.getX() + 8; ++x) {
                    world.setBlockToAir(new BlockPos(x, y, z));
                }
            }
        }
    }
}
