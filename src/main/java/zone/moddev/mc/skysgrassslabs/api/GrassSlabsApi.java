package zone.moddev.mc.skysgrassslabs.api;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.RandomSource;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import zone.moddev.mc.skysgrassslabs.block.GrassSpread;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;

/** Optional grass and terrain integration. Register forms before load completion. */
public final class GrassSlabsApi {
    public static final int VERSION = 1;

    private static final Map<BlockState, BlockState> GRASS = new LinkedHashMap<>();
    private static final Map<BlockState, BlockState> DIRT = new LinkedHashMap<>();
    private static final Set<BlockState> NON_SPREADING = new HashSet<>();
    private static final Map<BlockState, BlockState> SUPPORTS = new LinkedHashMap<>();
    private static final Map<String, BiFunction<BlockState, BlockState, BlockState>> SPREAD_RULES =
            new LinkedHashMap<>();

    private static String smoothingOwner;
    private static BooleanSupplier ownerEnabled = () -> true;
    private static boolean configurationLoaded;
    private static boolean frozen;

    private GrassSlabsApi() {
    }

    /** Claim once during construction, before common setup closes ownership registration. */
    public static void claimSmoothing(String owner, boolean enabled) {
        claimSmoothing(owner, () -> enabled);
    }

    /** Forge loads configuration after construction, so owners may supply their setting lazily. */
    public static synchronized void claimSmoothing(String owner, BooleanSupplier enabled) {
        if (configurationLoaded || frozen || owner == null || owner.isEmpty()
                || enabled == null || smoothingOwner != null) {
            throw new IllegalStateException("Smoothing must be claimed once before common setup");
        }

        smoothingOwner = owner;
        ownerEnabled = enabled;
    }

    public static boolean hasSmoothingOwner() {
        return smoothingOwner != null;
    }

    public static boolean ownerAllowsGeneration() {
        return smoothingOwner == null || ownerEnabled.getAsBoolean();
    }

    /** Respect the player's grass switch even when another mod owns terrain generation. */
    public static boolean grassGenerationAllowed() {
        return SkysGrassSlabsConfig.isSmoothingActive();
    }

    /** Register each orientation and water state; snow is a derived visual property. */
    public static synchronized void registerGrassForm(BlockState dirt, BlockState grass) {
        checkRegistrationOpen();
        checkForms(dirt, grass);
        dirt = canonical(dirt);
        grass = canonical(grass);
        if (DIRT.containsKey(dirt) || GRASS.containsKey(grass)) {
            throw new IllegalArgumentException("Conflicting grass form mapping");
        }

        DIRT.put(dirt, grass);
        GRASS.put(grass, dirt);
    }

    /** Additional skins can share dirt without replacing its ordinary regrowth. */
    public static synchronized void registerGrassVariant(BlockState dirt, BlockState grass,
            boolean spreads) {
        checkRegistrationOpen();
        checkForms(dirt, grass);
        dirt = canonical(dirt);
        grass = canonical(grass);
        if (GRASS.containsKey(grass)) {
            throw new IllegalArgumentException("Conflicting grass variant mapping");
        }

        GRASS.put(grass, dirt);
        if (!spreads) {
            NON_SPREADING.add(grass);
        }
    }

    public static boolean isGrassForm(BlockState state) {
        return state != null && GRASS.containsKey(canonical(state));
    }

    public static boolean isSpreadingGrassForm(BlockState state) {
        return isGrassForm(state) && !NON_SPREADING.contains(canonical(state))
                && !isWet(state);
    }

    public static BlockState grassFor(BlockState dirt) {
        return dirt == null ? null : DIRT.get(canonical(dirt));
    }

    /** Give source-aware rules first choice, then use the default grass for this dirt. */
    public static BlockState grassFor(BlockState dirt, BlockState source) {
        if (dirt == null) {
            return null;
        }
        for (BiFunction<BlockState, BlockState, BlockState> rule : SPREAD_RULES.values()) {
            BlockState result = rule.apply(canonical(source), canonical(dirt));
            if (result != null) {
                return result;
            }
        }

        return grassFor(dirt);
    }

    public static BlockState dirtFor(BlockState grass) {
        return grass == null ? null : GRASS.get(canonical(grass));
    }

    public static synchronized void registerSpreadRule(String owner,
            BiFunction<BlockState, BlockState, BlockState> rule) {
        checkRegistrationOpen();
        if (owner == null || owner.isEmpty() || rule == null || SPREAD_RULES.containsKey(owner)) {
            throw new IllegalArgumentException("Invalid or conflicting grass spread rule");
        }

        SPREAD_RULES.put(owner, rule);
    }

    /** Register native grass that may need dirtifying beneath a full-cover piece. */
    public static synchronized void registerGrassSupport(BlockState grass, BlockState dirt) {
        checkRegistrationOpen();
        checkForms(dirt, grass);
        if (SUPPORTS.containsKey(canonical(grass))) {
            throw new IllegalArgumentException("Conflicting grass support mapping");
        }

        SUPPORTS.put(canonical(grass), canonical(dirt));
    }

    public static synchronized void configurationLoaded() {
        configurationLoaded = true;
    }

    public static synchronized void freeze() {
        frozen = true;
    }

    public static void tickDirt(Level world, BlockPos position, BlockState state, RandomSource random) {
        GrassSpread.tickDirtSlab(world, position, state, random);
    }

    /** Retain the registered shape and water state when grass turns back into dirt. */
    public static void tickGrass(Level world, BlockPos position, BlockState state, RandomSource random) {
        if (world.isClientSide) {
            return;
        }

        repairSupport(world, position);
        if (isWet(state)
                || !GrassSpread.canRemainGrass(world, position)) {
            BlockState dirt = dirtFor(state);
            if (dirt != null) {
                world.setBlock(position, dirt, 3);
            }
        } else if (isSpreadingGrassForm(state)) {
            GrassSpread.spreadFrom(world, position, random,
                    coversSupport(state, world, position) ? position.below() : null);
        }
    }

    public static void repairSupport(Level world, BlockPos position) {
        if (world.isClientSide || !world.isLoaded(position.below())
                || !coversSupport(world.getBlockState(position), world, position)) {
            return;
        }

        BlockState support = world.getBlockState(position.below());
        BlockState dirt = support.getBlock() == Blocks.GRASS_BLOCK
                ? Blocks.DIRT.defaultBlockState() : SUPPORTS.get(canonical(support));
        if (dirt != null) {
            // The support keeps its full shape. Send its new appearance to clients,
            // but do not let neighbour-shape updates load chunks across a generation border.
            world.setBlock(position.below(), dirt, 2 | 16);
        }
    }

    /** Steps and corners leave grass exposed; a slab or turf covers the whole footprint. */
    public static boolean coversSupport(BlockState piece, BlockGetter world, BlockPos position) {
        VoxelShape shape = piece.getShape(world, position);
        if (shape.isEmpty()) {
            return false;
        }

        AABB bounds = shape.bounds();
        return bounds.minX == 0 && bounds.maxX == 1 && bounds.minZ == 0 && bounds.maxZ == 1;
    }

    private static BlockState canonical(BlockState state) {
        return state != null && state.hasProperty(SnowyDirtBlock.SNOWY)
                ? state.setValue(SnowyDirtBlock.SNOWY, false) : state;
    }

    private static boolean isWet(BlockState state) {
        return state.hasProperty(SlabBlock.WATERLOGGED) && state.getValue(SlabBlock.WATERLOGGED)
                || !state.getFluidState().isEmpty();
    }

    private static void checkForms(BlockState dirt, BlockState grass) {
        if (dirt == null || grass == null || canonical(dirt).equals(canonical(grass))
                || dirt.hasBlockEntity() || grass.hasBlockEntity()) {
            throw new IllegalArgumentException("Grass forms need distinct states without block entities");
        }
    }

    private static void checkRegistrationOpen() {
        if (frozen) {
            throw new IllegalStateException("Grass form registration is closed");
        }
    }
}
