package lu.allandemiranda.tpms.util;

public final class UnitConverter {

    private UnitConverter() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static double kpaToPsi(double kpa) {
        if (kpa == 0d) {
            return 0d;
        }
        return (kpa / 6.89476);
    }

    public static double kpaToBar(double kpa) {
        if (kpa == 0d) {
            return 0d;
        }
        return (kpa / 100);
    }

    public static int pressureTpmsToKpa(int pRaw) {
        int i = pRaw - 101;
        return Math.max(i, 0);
    }
}
