package lu.allandemiranda.tpms.util;

import java.util.Arrays;

public final class TpmsDecoder {

    public static TpmsDecoder.TPMS decode(byte[] data) {
        if (data == null || data.length < 12) {
            UiLogger.log("Dados de fabricante inválidos ou muito curtos: " + Arrays.toString(data));
            return null;
        }

        int expectedLength = data[0] & 0xFF;

        if (expectedLength != data.length && expectedLength != 0x1E) {
            UiLogger.log("Tamanho inesperado do pacote: declarado " + expectedLength + " bytes, mas recebido " + data.length);
            return null;
        }

        int temperature = data[1]; // temperatura em °C
        int pressureRaw = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF); // pressão absoluta em kPa (incluir pressão atmosferica)

        int status = data[5] & 0xFF;
        StringBuilder mac = new StringBuilder();
        for (int i = 6; i <= 11; i++) {
            mac.append(String.format("%02X", data[i]));
            if (i < 11) mac.append(":");
        }

        //UiLogger.log("TPMS encontrado – Temp: " + temperature + "°C, Pressão: " + pressureRaw + " raw, Status: " + status + ", MAC: " + mac);

        return new TpmsDecoder.TPMS(temperature, pressureRaw);
    }

    public record TPMS(int temperature, int pressure) {
    }
}
