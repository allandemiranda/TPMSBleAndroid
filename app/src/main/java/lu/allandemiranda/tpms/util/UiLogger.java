package lu.allandemiranda.tpms.util;

public final class UiLogger {

    private static Sink sink;

    private UiLogger() {
    }

    /**
     * MainActivity chama aqui para registrar‑se como console de log.
     */
    public static void setSink(Sink s) {
        sink = s;
    }

    /**
     * Qualquer classe do app chama aqui para registrar uma linha.
     */
    public static void log(String msg) {
        if (sink != null) sink.log(msg);
    }

    public interface Sink {
        void log(String msg);
    }
}
