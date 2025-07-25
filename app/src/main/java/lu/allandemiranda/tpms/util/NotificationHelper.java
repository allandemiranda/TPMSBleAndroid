package lu.allandemiranda.tpms.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public final class NotificationHelper {

    private static final String CHANNEL_ID = "tpms_alerts";
    private static final int PRIO = NotificationCompat.PRIORITY_HIGH;

    private NotificationHelper() {
    }

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "TPMS alerts", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Pressure out‑of‑range or signal lost");
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    public static void notifyPressure(Context ctx, boolean front, int kpa, int min, int max) {
        String tyre = front ? "Front" : "Rear";
        String title = "Pressure alert – " + tyre;
        String msg = kpa < min ? String.format("Current %d kPa < Min %d kPa", kpa, min) : String.format("Current %d kPa > Max %d kPa", kpa, max);

        Notification n = new NotificationCompat.Builder(ctx, CHANNEL_ID).setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle(title).setContentText(msg).setPriority(PRIO).setAutoCancel(true).build();
        NotificationManagerCompat.from(ctx).notify(tyre.hashCode(), n);
    }

    public static void notifySignalLost(Context ctx, boolean front) {
        String tyre = front ? "Front" : "Rear";
        Notification n = new NotificationCompat.Builder(ctx, CHANNEL_ID).setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle("Signal lost – " + tyre).setContentText("No packets for 5 minutes.").setPriority(PRIO).setAutoCancel(true).build();
        NotificationManagerCompat.from(ctx).notify((tyre + "_lost").hashCode(), n);
    }
}
