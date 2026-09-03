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

    private SmoothingDecision() {
    }
}
