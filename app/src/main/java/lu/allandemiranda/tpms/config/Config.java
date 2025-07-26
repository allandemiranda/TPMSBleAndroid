package lu.allandemiranda.tpms.config;

public final class Config {
    // Fixed MAC addresses of each sensor
    public static final String FRONT_MAC = "DC:32:62:31:9D:6A";
    public static final String REAR_MAC = "DC:32:62:31:93:BA";
    // Pressure limits (kPa)
    public static final int FRONT_MIN_KPA = 200;
    public static final int FRONT_MAX_KPA = 260;
    public static final int REAR_MIN_KPA = 225;
    public static final int REAR_MAX_KPA = 310;
    // Timeout – if no packet for this period the signal is considered lost
    public static final long SIGNAL_TIMEOUT_MIN = 5; // 5 minutes
    public static final boolean DEBUG_UI = true;

    private Config() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
