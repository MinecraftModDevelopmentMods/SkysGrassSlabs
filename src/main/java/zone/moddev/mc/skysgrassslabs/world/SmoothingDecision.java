package zone.moddev.mc.skysgrassslabs.world;

/** Pure, allocation-free terrain-pattern decision used by world generation and tests. */
public final class SmoothingDecision {
    private SmoothingDecision() {
    }

    public static boolean shouldPlace(int lowerY, int northY, int southY, int westY,
            int eastY, boolean lowerIsGrass, boolean northIsGrass, boolean southIsGrass,
            boolean westIsGrass, boolean eastIsGrass, boolean targetClear,
            boolean targetDry, boolean supported) {
        if (!lowerIsGrass || !targetClear || !targetDry || !supported) {
            return false;
        }
        int highY = lowerY + 1;
        return northIsGrass && northY == highY
                || southIsGrass && southY == highY
                || westIsGrass && westY == highY
                || eastIsGrass && eastY == highY;
    }
}
