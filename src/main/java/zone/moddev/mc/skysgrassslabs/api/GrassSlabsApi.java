package zone.moddev.mc.skysgrassslabs.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zone.moddev.mc.skysgrassslabs.block.GrassSpread;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;

/** Optional content integration. Registration closes after mod initialization. */
public final class GrassSlabsApi {
    public static final int VERSION = 1;
    private static final Map<IBlockState, IBlockState> GRASS = new LinkedHashMap<>();
    private static final Map<IBlockState, IBlockState> DIRT = new LinkedHashMap<>();
    private static final java.util.Set<IBlockState> NON_SPREADING = new java.util.HashSet<>();
    private static final Map<IBlockState,IBlockState> SUPPORTS = new LinkedHashMap<>();
    private static final Map<String,java.util.function.BiFunction<IBlockState,IBlockState,IBlockState>> SPREAD_RULES = new LinkedHashMap<>();
    private static String smoothingOwner;
    private static boolean ownerEnabled, configurationLoaded, frozen;

    private GrassSlabsApi() { }

    /** Claim during startup. The owner suppresses our pass even when its own generation is off. */
    public static void claimSmoothing(String owner, boolean enabled) {
        if (configurationLoaded || owner == null || owner.isEmpty() || smoothingOwner != null)
            throw new IllegalStateException("Smoothing ownership must be claimed once before configuration loads");
        smoothingOwner = owner;
        ownerEnabled = enabled;
    }

    public static boolean hasSmoothingOwner() { return smoothingOwner != null; }
    public static boolean ownerAllowsGeneration() { return smoothingOwner == null || ownerEnabled; }
    public static boolean grassGenerationAllowed() { return SkysGrassSlabsConfig.isSmoothingActive(); }

    /** Register each ordinary dirt/grass orientation, never coarse dirt or podzol. */
    public static void registerGrassForm(IBlockState dirt, IBlockState grass) {
        if (frozen) throw new IllegalStateException("Grass form registration is closed");
        if (dirt == null || grass == null || dirt.getBlock() == grass.getBlock() ||
                dirt.getBlock().hasTileEntity(dirt) || grass.getBlock().hasTileEntity(grass))
            throw new IllegalArgumentException("Grass forms need distinct non-tile dirt and grass states");
        dirt = canonical(dirt); grass = canonical(grass);
        if (DIRT.containsKey(dirt) || GRASS.containsKey(grass))
            throw new IllegalArgumentException("Conflicting grass form mapping");
        DIRT.put(dirt, grass); GRASS.put(grass, dirt);
    }

    public static boolean isGrassForm(IBlockState state) { return GRASS.containsKey(canonical(state)); }
    /** Additional grass skins can share a dirt form without changing its default regrowth. */
    public static void registerGrassVariant(IBlockState dirt, IBlockState grass, boolean spreads) {
        if(frozen)throw new IllegalStateException("Grass form registration is closed");
        if(dirt==null||grass==null||dirt.equals(grass)||dirt.getBlock().hasTileEntity(dirt)||grass.getBlock().hasTileEntity(grass))
            throw new IllegalArgumentException("Grass variants need distinct non-tile states");
        dirt=canonical(dirt);grass=canonical(grass);
        if(GRASS.containsKey(grass))throw new IllegalArgumentException("Conflicting grass variant mapping");
        GRASS.put(grass,dirt);if(!spreads)NON_SPREADING.add(grass);
    }
    public static boolean isSpreadingGrassForm(IBlockState state) { return isGrassForm(state)&&!NON_SPREADING.contains(canonical(state)); }
    public static IBlockState grassFor(IBlockState dirt) { return DIRT.get(canonical(dirt)); }
    /** Choose grass to match the source; return null to use the default dirt/grass pair. */
    public static IBlockState grassFor(IBlockState dirt,IBlockState source) {
        for(java.util.function.BiFunction<IBlockState,IBlockState,IBlockState> rule:SPREAD_RULES.values()) {
            IBlockState result=rule.apply(canonical(source),canonical(dirt));if(result!=null)return result;
        }
        return grassFor(dirt);
    }
    public static void registerSpreadRule(String owner,java.util.function.BiFunction<IBlockState,IBlockState,IBlockState> rule) {
        if(frozen)throw new IllegalStateException("Grass form registration is closed");
        if(owner==null||owner.isEmpty()||rule==null||SPREAD_RULES.containsKey(owner))throw new IllegalArgumentException("Invalid or conflicting grass spread rule");
        SPREAD_RULES.put(owner,rule);
    }
    public static void registerGrassSupport(IBlockState grass,IBlockState dirt) {
        if(frozen)throw new IllegalStateException("Grass form registration is closed");
        if(grass==null||dirt==null||grass.equals(dirt)||grass.getBlock().hasTileEntity(grass)||dirt.getBlock().hasTileEntity(dirt)||SUPPORTS.containsKey(canonical(grass)))
            throw new IllegalArgumentException("Invalid or conflicting grass support");
        SUPPORTS.put(canonical(grass),canonical(dirt));
    }
    public static IBlockState dirtFor(IBlockState grass) { return GRASS.get(canonical(grass)); }
    public static void configurationLoaded() { configurationLoaded = true; }
    public static void freeze() { frozen = true; }

    public static void tickDirt(World world, BlockPos pos, IBlockState state, Random random) {
        GrassSpread.tickDirtSlab(world, pos, state, random);
    }

    public static void tickGrass(World world, BlockPos pos, IBlockState state, Random random) {
        if (world.isRemote) return;
        repairSupport(world, pos);
        if (!GrassSpread.canRemainGrass(world, pos)) {
            IBlockState dirt = dirtFor(state);
            if (dirt != null) world.setBlockState(pos, dirt, 3);
        } else if(isSpreadingGrassForm(state)) GrassSpread.spreadFrom(world, pos, random, pos.down());
    }

    public static void repairSupport(World world, BlockPos pos) {
        if(world.isRemote||!world.isBlockLoaded(pos.down()))return;
        IBlockState lower=world.getBlockState(pos.down());
        IBlockState dirt=lower.getBlock()==Blocks.GRASS?Blocks.DIRT.getDefaultState():SUPPORTS.get(canonical(lower));
        if(dirt!=null)world.setBlockState(pos.down(),dirt,2);
    }

    private static IBlockState canonical(IBlockState state) {
        return state.getBlock().getStateFromMeta(state.getBlock().getMetaFromState(state));
    }
}
