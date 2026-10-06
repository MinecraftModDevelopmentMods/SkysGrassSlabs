package zone.moddev.mc.skysgrassslabs.integrationtest;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.util.EnumFacing;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import zone.moddev.mc.skysgrassslabs.compat.*;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Mixed permanent/historical identities, including an already migrated chunk. */
public final class AliasRecoveryChecks {
    private static BlockPos pos(int x){return new BlockPos(4096+x,80,4096);}
    public static void fresh(WorldServer world) {
        Block[] aliases=BuildingBricksCompat.legacyAliases();require(aliases.length==3,"three distinct holders");
        NBTTagCompound before=state(world);
        for(int i=0;i<3;i++)for(int orientation=0;orientation<2;orientation++) {
            Block alias=aliases[i];require(alias!=ModBlocks.GRASS_SLAB&&alias!=ModBlocks.DIRT_SLAB,"holder identity must not collide with permanent IDs");
            world.setBlockState(pos(i*2+orientation),alias.getStateFromMeta(orientation),2);
        }
        world.setBlockState(pos(6),sky(true).getStateFromMeta(0),2);
        world.setBlockState(pos(7),sky(false).getStateFromMeta(1),2);
        world.setBlockState(pos(8),Blocks.CHEST.getDefaultState(),2);
        TileEntityChest chest=(TileEntityChest)world.getTileEntity(pos(8));
        for(int i=0;i<3;i++)chest.setInventorySlotContents(i,stack(aliases[i],i+7));
        // A real entity join exercises the early live-item path without a counter reset.
        EntityItem dropped=new EntityItem(world,4096,83,4096,stack(aliases[2],11));
        new LegacyMigrationHandler().joinEntity(new EntityJoinWorldEvent(dropped,world));
        require(dropped.getEntityItem().getItem()==Item.getItemFromBlock(ModBlocks.GRASS_SLAB)&&dropped.getEntityItem().stackSize==11&&dropped.getEntityItem().getTagCompound().getString("custom").equals("preserved"),"dropped stack recovery");
        LegacyMigrationHandler events=new LegacyMigrationHandler();
        EntityItemFrame frame=new EntityItemFrame(world,pos(9),EnumFacing.NORTH);
        frame.setDisplayedItem(stack(aliases[1],1));
        events.joinEntity(new EntityJoinWorldEvent(frame,world));
        checkStack(frame.getDisplayedItem(),ModBlocks.DIRT_SLAB,1,"frame");
        FakePlayer player=FakePlayerFactory.get(world,new GameProfile(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),"SlabRecoveryTest"));
        player.inventory.setInventorySlotContents(0,stack(aliases[0],13));
        player.getInventoryEnderChest().setInventorySlotContents(0,stack(aliases[1],17));
        events.playerLogin(new PlayerEvent.PlayerLoggedInEvent(player));
        checkStack(player.inventory.getStackInSlot(0),ModBlocks.GRASS_SLAB,13,"player");
        checkStack(player.getInventoryEnderChest().getStackInSlot(0),ModBlocks.DIRT_SLAB,17,"ender chest");
        // Queue a loaded chest, then put a historical stack through its live handler.
        world.setBlockState(pos(10),Blocks.CHEST.getDefaultState(),2);
        events.loadChunk(new ChunkDataEvent.Load(world.getChunkFromBlockCoords(pos(10)),new NBTTagCompound()));
        TileEntityChest handlerChest=(TileEntityChest)world.getTileEntity(pos(10));
        handlerChest.setInventorySlotContents(0,stack(aliases[2],19));
        events.migrateLoadedHandlers(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        checkStack(handlerChest.getStackInSlot(0),ModBlocks.GRASS_SLAB,19,"live item handler");
        events.playerLogin(new PlayerEvent.PlayerLoggedInEvent(player));
        checkStack(player.inventory.getStackInSlot(0),ModBlocks.GRASS_SLAB,13,"second login");
        require(before.equals(state(world)),"absent recovery preserves historical counters");
        System.out.println("SKY_ALIAS_FIXTURE_SEEDED historical_and_permanent_ids=true player_ender_frame_handler=true");
    }
    public static void reload(WorldServer world) {
        NBTTagCompound before=state(world);
        for(int i=0;i<3;i++)for(int orientation=0;orientation<2;orientation++) {
            Block expected=i==1?ModBlocks.DIRT_SLAB:ModBlocks.GRASS_SLAB;
            require(world.getBlockState(pos(i*2+orientation)).equals(expected.getStateFromMeta(orientation)),"alias block orientation "+i+" / "+orientation);
        }
        require(world.getBlockState(pos(6)).equals(sky(true).getStateFromMeta(0))&&world.getBlockState(pos(7)).equals(sky(false).getStateFromMeta(1)),"permanent mixed content unchanged");
        TileEntityChest chest=(TileEntityChest)world.getTileEntity(pos(8));
        for(int i=0;i<3;i++){ItemStack s=chest.getStackInSlot(i);require(s!=null&&s.stackSize==i+7&&s.getItem()==Item.getItemFromBlock(i==1?ModBlocks.DIRT_SLAB:ModBlocks.GRASS_SLAB)&&s.getTagCompound().getString("custom").equals("preserved"),"chest count and NBT");}
        // Revisit a marker that predates the remaining historical content.
        Chunk chunk=world.getChunkFromBlockCoords(pos(0));Block alias=BuildingBricksCompat.grassSlab();world.setBlockState(pos(0),alias.getStateFromMeta(0),2);
        NBTTagCompound root=new NBTTagCompound(),level=new NBTTagCompound(),section=new NBTTagCompound(),marker=new NBTTagCompound();root.setTag("Level",level);marker.setInteger("buildingbricks_migration_version",1);marker.setString("future_field","preserved");root.setTag("skysgrassslabs",marker);
        byte[] low=new byte[4096],high=new byte[2048],data=new byte[2048];int id=Block.getIdFromBlock(alias);low[0]=(byte)id;high[0]=(byte)(id>>8);section.setByte("Y",(byte)5);section.setByteArray("Blocks",low);section.setByteArray("Add",high);section.setByteArray("Data",data);NBTTagList sections=new NBTTagList();sections.appendTag(section);level.setTag("Sections",sections);
        LegacyMigrationHandler events=new LegacyMigrationHandler();events.loadChunk(new ChunkDataEvent.Load(chunk,root));
        require(world.getBlockState(pos(0)).equals(sky(true).getStateFromMeta(0)),"old marker cannot skip remaining recovery");
        NBTTagCompound saved=new NBTTagCompound();events.saveChunk(new ChunkDataEvent.Save(chunk,saved));require(saved.getCompoundTag("skysgrassslabs").equals(marker),"complete old marker preserved");
        require(before.equals(state(world)),"reload preserves counters");
        System.out.println("SKY_ALIAS_RECOVERY_PASS orientations=2 aliases=3 counts_and_nbt=true old_marker=true");
    }
    private static ItemStack stack(Block alias,int count){ItemStack s=new ItemStack(alias,count,0);NBTTagCompound tag=new NBTTagCompound();tag.setString("custom","preserved");s.setTagCompound(tag);return s;}
    private static void checkStack(ItemStack stack,Block block,int count,String context){require(stack!=null&&stack.getItem()==Item.getItemFromBlock(block)&&stack.stackSize==count&&stack.getTagCompound().getString("custom").equals("preserved"),context+" identity/count/NBT");}
    private static NBTTagCompound state(WorldServer world){return ((net.minecraft.world.WorldSavedData)ModWorldState.get(world)).writeToNBT(new NBTTagCompound());}
    private static Block sky(boolean grass){return grass?ModBlocks.GRASS_SLAB:ModBlocks.DIRT_SLAB;}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
