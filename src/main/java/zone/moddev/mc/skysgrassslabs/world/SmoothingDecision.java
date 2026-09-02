package zone.moddev.mc.skysgrassslabs.world;

public final class SmoothingDecision {
    public static boolean shouldPlace(int lowerSurfaceY, int neighbourSurfaceY,
            boolean lowerIsNaturalGrass, boolean neighbourIsNaturalGrass,
            boolean targetClear, boolean targetDry, boolean supported,
            boolean targetHasBlockEntity) {
        return neighbourSurfaceY == lowerSurfaceY + 1 && lowerIsNaturalGrass &&
                neighbourIsNaturalGrass && targetClear && targetDry && supported &&
                !targetHasBlockEntity;
    }

    private SmoothingDecision() {
    }
}
