package zone.moddev.mc.skysgrassslabs.integrationtest;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Runs against the released jar first, then checks the same save with its successor. */
final class ReleasedUpgradeChecks {
    private ReleasedUpgradeChecks() { }
    static void run(WorldServer world,boolean seed) {
        world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        Block[] blocks={ModBlocks.DIRT_SLAB,ModBlocks.GRASS_SLAB,ModBlocks.PATH_SLAB,ModBlocks.TURF};
        int number=0;
        for(Block block:blocks)for(int metadata=0;metadata<(block==ModBlocks.TURF?1:2);metadata++) {
            BlockPos pos=new BlockPos(160+number++,100,160);
            IBlockState expected=block.getStateFromMeta(metadata);
            if(seed){world.setBlockState(pos.down(),Blocks.DIRT.getDefaultState(),2);world.setBlockToAir(pos.up());world.setBlockState(pos,expected,2);}
            else require(world.getBlockState(pos).equals(expected),"Released block orientation: "+block.getRegistryName()+" / "+metadata);
        }
        BlockPos chestPos=new BlockPos(160,101,164);
        if(seed)world.setBlockState(chestPos,Blocks.CHEST.getDefaultState(),2);
        TileEntityChest chest=(TileEntityChest)world.getTileEntity(chestPos);
        require(chest!=null,"Released container survived");
        for(int slot=0;slot<blocks.length;slot++) {
            ItemStack expected=new ItemStack(blocks[slot],11+slot,blocks[slot]==ModBlocks.TURF?0:1);
            NBTTagCompound data=new NBTTagCompound();data.setString("upgrade_note","Saved custom item data");data.setInteger("fixture_slot",slot);expected.setTagCompound(data);
            if(seed)chest.setInventorySlotContents(slot,expected);
            else require(ItemStack.areItemStacksEqual(chest.getStackInSlot(slot),expected),"Released item identity, count and NBT: "+slot);
        }
        if(seed) {
            EntityItem dropped=new EntityItem(world,164.5,101,164.5,new ItemStack(ModBlocks.GRASS_SLAB,7,1));
            dropped.getEntityItem().setTagCompound(new NBTTagCompound());dropped.getEntityItem().getTagCompound().setString("upgrade_entity","Retained");
            require(world.spawnEntity(dropped),"Released entity seeded");
            ModWorldState state=ModWorldState.get(world);state.recordChunk();state.recordGrassBlocks(13,0);state.recordDirtBlocks(17,1);state.recordGrassItems(19);state.recordDirtItems(23);
        } else {
            boolean found=false;
            for(net.minecraft.util.ClassInheritanceMultiMap<Entity> section:world.getChunkFromBlockCoords(chestPos).getEntityLists())for(Entity entity:section)
                if(entity instanceof EntityItem) {
                    ItemStack stack=((EntityItem)entity).getEntityItem();
                    if(stack.getTagCompound()!=null&&stack.getTagCompound().hasKey("upgrade_entity")) {
                        require(stack.getItem()==net.minecraft.item.Item.getItemFromBlock(ModBlocks.GRASS_SLAB)&&stack.stackSize==7&&stack.getMetadata()==1&&stack.getTagCompound().getString("upgrade_entity").equals("Retained"),"Released dropped item and NBT");found=true;
                    }
                }
            require(found,"Released entity survived");
            ModWorldState state=ModWorldState.get(world);
            require(state.migratedChunks()==1&&state.migratedGrassBlocks()==13&&state.migratedGrassBlocksTop()==13&&state.migratedDirtBlocks()==17&&state.migratedDirtBlocksBottom()==17&&state.migratedGrassItems()==19&&state.migratedDirtItems()==23,"Released schema-1 counters survived unchanged");
        }
        System.out.println("GRASS_SLABS_RELEASED_UPGRADE_PASS seed="+seed+" states="+number);
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
