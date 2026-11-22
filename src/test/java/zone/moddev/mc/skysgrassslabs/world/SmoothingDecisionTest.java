package zone.moddev.mc.skysgrassslabs.world;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class SmoothingDecisionTest {
    @Test
    void acceptsOnlyAOneBlockNaturalGrassTransition() {
        assertTrue(SmoothingDecision.shouldPlace(63, 64,
                true, true, true, true, true, false));
        assertFalse(SmoothingDecision.shouldPlace(63, 63,
                true, true, true, true, true, false));
        assertFalse(SmoothingDecision.shouldPlace(63, 65,
                true, true, true, true, true, false));
    }

    @Test
    void rejectsEveryProtectedTargetCondition() {
        assertFalse(SmoothingDecision.shouldPlace(63, 64,
                false, true, true, true, true, false));
        assertFalse(SmoothingDecision.shouldPlace(63, 64,
                true, false, true, true, true, false));
        assertFalse(SmoothingDecision.shouldPlace(63, 64,
                true, true, false, true, true, false));
        assertFalse(SmoothingDecision.shouldPlace(63, 64,
                true, true, true, false, true, false));
        assertFalse(SmoothingDecision.shouldPlace(63, 64,
                true, true, true, true, false, false));
        assertFalse(SmoothingDecision.shouldPlace(63, 64,
                true, true, true, true, true, true));
    }

    @Test
    void fixedSeedAreaIsStableAcrossSeveralChunkOrders() {
        SyntheticArea area = new SyntheticArea(0x534759534c414253L);
        int[] forward = chunkOrder(false);
        int[] reverse = chunkOrder(true);
        int[] shuffled = forward.clone();
        Random random = new Random(0x112021L);
        for (int index = shuffled.length - 1; index > 0; --index) {
            int swap = random.nextInt(index + 1);
            int value = shuffled[index];
            shuffled[index] = shuffled[swap];
            shuffled[swap] = value;
        }

        boolean[] expected = smooth(area, forward);
        assertArrayEquals(expected, smooth(area, reverse));
        assertArrayEquals(expected, smooth(area, shuffled));
    }

    @Test
    void benchmarksTheTwoHundredAndFiftySixColumnDecisionPass() {
        SyntheticArea area = new SyntheticArea(0x112021L);
        int[] order = chunkOrder(false);
        int checksum = 0;
        long started = System.nanoTime();
        for (int pass = 0; pass < 20000; ++pass) {
            checksum += smoothChunk(area, order[pass % order.length], null);
        }
        long elapsed = System.nanoTime() - started;
        long nanosPerPass = elapsed / 20000L;
        System.out.println("256-column smoothing decision pass: " + nanosPerPass + " ns");

        assertTrue(checksum > 0, "Synthetic benchmark did not exercise an eligible slope");
        assertTrue(nanosPerPass < 5_000_000L,
                "Smoothing decision pass exceeded the 5 ms regression ceiling");
    }

    private static int[] chunkOrder(boolean reverse) {
        int[] order = new int[81];
        for (int index = 0; index < order.length; ++index) {
            order[index] = reverse ? order.length - index - 1 : index;
        }
        return order;
    }

    private static boolean[] smooth(SyntheticArea area, int[] order) {
        boolean[] decisions = new boolean[SyntheticArea.COLUMNS * SyntheticArea.COLUMNS];
        for (int chunk : order) {
            smoothChunk(area, chunk, decisions);
        }
        return decisions;
    }

    private static int smoothChunk(SyntheticArea area, int chunk, boolean[] decisions) {
        int chunkX = chunk % SyntheticArea.CHUNKS;
        int chunkZ = chunk / SyntheticArea.CHUNKS;
        int accepted = 0;
        for (int localZ = 0; localZ < 16; ++localZ) {
            for (int localX = 0; localX < 16; ++localX) {
                int x = (chunkX << 4) + localX;
                int z = (chunkZ << 4) + localZ;
                int index = z * SyntheticArea.COLUMNS + x;
                boolean place = area.eligible(index) && hasHigherNeighbour(
                        area, x, z, localX, localZ);
                if (place) {
                    ++accepted;
                    if (decisions != null) decisions[index] = true;
                }
            }
        }
        return accepted;
    }

    private static boolean hasHigherNeighbour(SyntheticArea area, int x, int z,
            int localX, int localZ) {
        int surface = area.height(x, z);
        if (localX > 0 && area.higherGrass(x - 1, z, surface)) return true;
        if (localZ > 0 && area.higherGrass(x, z - 1, surface)) return true;
        if (x + 1 < SyntheticArea.COLUMNS && area.higherGrass(x + 1, z, surface)) {
            return true;
        }
        return z + 1 < SyntheticArea.COLUMNS && area.higherGrass(x, z + 1, surface);
    }

    private static final class SyntheticArea {
        private static final int CHUNKS = 9;
        private static final int COLUMNS = CHUNKS * 16;

        private final int[] heights = new int[COLUMNS * COLUMNS];
        private final boolean[] grass = new boolean[heights.length];
        private final boolean[] clear = new boolean[heights.length];
        private final boolean[] dry = new boolean[heights.length];
        private final boolean[] supported = new boolean[heights.length];
        private final boolean[] blockEntity = new boolean[heights.length];

        private SyntheticArea(long seed) {
            Random random = new Random(seed);
            for (int index = 0; index < heights.length; ++index) {
                heights[index] = 62 + random.nextInt(4);
                grass[index] = random.nextInt(10) != 0;
                clear[index] = random.nextInt(20) != 0;
                dry[index] = random.nextInt(24) != 0;
                supported[index] = random.nextInt(20) != 0;
                blockEntity[index] = random.nextInt(100) == 0;
            }
        }

        private int height(int x, int z) {
            return heights[z * COLUMNS + x];
        }

        private boolean higherGrass(int x, int z, int lowerHeight) {
            int index = z * COLUMNS + x;
            return SmoothingDecision.shouldPlace(lowerHeight, heights[index],
                    true, grass[index], true, true, true, false);
        }

        private boolean eligible(int index) {
            return grass[index] && clear[index] && dry[index] && supported[index] &&
                    !blockEntity[index];
        }
    }
}
