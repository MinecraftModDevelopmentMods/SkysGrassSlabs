package zone.moddev.mc.skysgrassslabs.world;

public final class SmoothingDecision {
    public static boolean shouldPlace(int lowerSurfaceY, int neighbourSurfaceY,
            boolean lowerIsNaturalGrass, boolean neighbourIsNaturalGrass,
            boolean targetClear, boolean targetDry, boolean supported,
            boolean targetHasBlockEntity) {
        return isOneBlockGrassTransition(lowerSurfaceY, neighbourSurfaceY,
                neighbourIsNaturalGrass) && isEligibleTarget(lowerIsNaturalGrass,
                targetClear, targetDry, supported, targetHasBlockEntity);
    }

    static boolean isEligibleTarget(boolean lowerIsNaturalGrass, boolean targetClear,
            boolean targetDry, boolean supported, boolean targetHasBlockEntity) {
        return lowerIsNaturalGrass && targetClear && targetDry && supported &&
                !targetHasBlockEntity;
    }

    static boolean isOneBlockGrassTransition(int lowerSurfaceY, int neighbourSurfaceY,
            boolean neighbourIsNaturalGrass) {
        return neighbourSurfaceY == lowerSurfaceY + 1 && neighbourIsNaturalGrass;
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

    private SmoothingDecision() {
    }
}
