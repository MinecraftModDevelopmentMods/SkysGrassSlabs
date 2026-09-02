package zone.moddev.mc.skysgrassslabs.compat;

/** Internal decisions shared by compatibility event handlers and focused tests. */
final class BuildingBricksPolicy {
    static boolean shouldArbitrateWorldgen(boolean installed, boolean skyWorldgenEnabled) {
        return installed && skyWorldgenEnabled;
    }

    static boolean shouldReplaceSlabs(boolean installed, boolean forceReplacementEnabled) {
        return installed && forceReplacementEnabled;
    }

    private BuildingBricksPolicy() {
    }
}
