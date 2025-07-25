package lu.allandemiranda.tpms.util;

public final class UiLogger {

    private static Sink sink;

    private UiLogger() {
    }

    public static void setSink(Sink s) {
        sink = s;
    }

    public static void log(String msg) {
        if (sink != null) sink.log(msg);
    }

    public interface Sink {
        void log(String msg);
    }
}
