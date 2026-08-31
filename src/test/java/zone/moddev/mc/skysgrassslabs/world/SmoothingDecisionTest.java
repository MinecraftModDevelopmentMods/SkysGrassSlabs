package zone.moddev.mc.skysgrassslabs.world;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class SmoothingDecisionTest {
    @Test
    public void oneBlockGrassRisePlacesSlab() {
        assertTrue(SmoothingDecision.shouldPlace(64, 65, 64, 64, 64,
                true, true, true, true, true, true, true, true));
    }

    @Test
    public void rejectsFlatCliffWetOccupiedAndUnsupportedPatterns() {
        assertFalse(SmoothingDecision.shouldPlace(64, 64, 64, 64, 64,
                true, true, true, true, true, true, true, true));
        assertFalse(SmoothingDecision.shouldPlace(64, 66, 64, 64, 64,
                true, true, true, true, true, true, true, true));
        assertFalse(SmoothingDecision.shouldPlace(64, 65, 64, 64, 64,
                true, true, true, true, true, true, false, true));
        assertFalse(SmoothingDecision.shouldPlace(64, 65, 64, 64, 64,
                true, true, true, true, true, false, true, true));
        assertFalse(SmoothingDecision.shouldPlace(64, 65, 64, 64, 64,
                true, true, true, true, true, true, true, false));
    }

    @Test
    public void higherSurfaceMustAlsoBeNaturalGrass() {
        assertFalse(SmoothingDecision.shouldPlace(64, 65, 64, 64, 64,
                true, false, true, true, true, true, true, true));
    }

    @Test
    public void fixedSeedNineByNineResultIgnoresGenerationOrder() {
        List<Integer> rowMajor = new ArrayList<>();
        for (int index = 0; index < 81; index++) {
            rowMajor.add(index);
        }
        List<Integer> shuffled = new ArrayList<>(rowMajor);
        Collections.shuffle(shuffled, new Random(0x5A17BEEFL));
        List<Integer> reverse = new ArrayList<>(rowMajor);
        Collections.reverse(reverse);

        assertEquals(plan(rowMajor), plan(shuffled));
        assertEquals(plan(rowMajor), plan(reverse));
    }

    @Test
    public void benchmarkHotDecisionAcrossManyChunks() {
        long start = System.nanoTime();
        int placements = 0;
        for (int chunk = 0; chunk < 4096; chunk++) {
            for (int column = 0; column < 256; column++) {
                int y = 60 + (column & 3);
                if (SmoothingDecision.shouldPlace(y, y, y + 1, y, y,
                        true, true, true, true, true, true, true, true)) {
                    placements++;
                }
            }
        }
        long elapsed = System.nanoTime() - start;
        assertEquals(4096 * 256, placements);
        System.out.println("smoothing-decision-benchmark-ns=" + elapsed
                + ", columns=" + (4096 * 256));
    }

    private static List<Integer> plan(List<Integer> chunkOrder) {
        List<Integer> placements = new ArrayList<>();
        for (int chunk : chunkOrder) {
            int chunkX = chunk / 9;
            int chunkZ = chunk % 9;
            for (int column = 0; column < 256; column++) {
                int y = 62 + Math.floorMod(chunkX * 31 + chunkZ * 17 + column, 3);
                if (SmoothingDecision.shouldPlace(y, y, y + 1, y, y,
                        true, true, true, true, true, true, true, true)) {
                    placements.add(chunk * 256 + column);
                }
            }
        }
        Collections.sort(placements);
        return placements;
    }
}
