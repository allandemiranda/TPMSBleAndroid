package lu.allandemiranda.tpms.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public final class NotificationHelper {

    private static final String CH_ID_ALERTS = "tpms_alerts";
    private static final int ID_FRONT_PRESSURE = 1001;
    private static final int ID_REAR_PRESSURE = 1002;
    private static final int ID_FRONT_LOST = 2001;
    private static final int ID_REAR_LOST = 2002;

    private NotificationHelper() {
    }

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(CH_ID_ALERTS) == null) {
                NotificationChannel ch = new NotificationChannel(CH_ID_ALERTS, "TPMS Alerts", NotificationManager.IMPORTANCE_HIGH   // heads‑up / banner
                );
                ch.setDescription("Pressure and signal warnings");
                ch.enableVibration(true);
                ch.enableLights(true);
                nm.createNotificationChannel(ch);
            }
        }
    }

    public static void notifyPressure(Context ctx, boolean front, int currentKpa, int minKpa, int maxKpa) {
        String tire = front ? "Front tire" : "Rear tire";
        String msg = currentKpa < minKpa ? String.format("Low pressure: %d kPa (min %d)", currentKpa, minKpa) : String.format("High pressure: %d kPa (max %d)", currentKpa, maxKpa);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CH_ID_ALERTS).setSmallIcon(android.R.drawable.stat_sys_warning) // ícone existente no framework
                .setContentTitle(tire + " pressure alert").setContentText(msg).setStyle(new NotificationCompat.BigTextStyle().bigText(msg)).setPriority(NotificationCompat.PRIORITY_HIGH) // pré-O
                .setAutoCancel(true);

        NotificationManagerCompat.from(ctx).notify(front ? ID_FRONT_PRESSURE : ID_REAR_PRESSURE, b.build());
    }

    public static void notifySignalLost(Context ctx, boolean front) {
        String tire = front ? "Front tire" : "Rear tire";
        String msg = tire + " signal lost (5+ min without packets)";

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CH_ID_ALERTS).setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle("TPMS signal lost").setContentText(msg).setStyle(new NotificationCompat.BigTextStyle().bigText(msg)).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true);

        NotificationManagerCompat.from(ctx).notify(front ? ID_FRONT_LOST : ID_REAR_LOST, b.build());
    }
}
