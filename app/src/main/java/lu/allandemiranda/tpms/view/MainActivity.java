package lu.allandemiranda.tpms.view;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lu.allandemiranda.tpms.R;
import lu.allandemiranda.tpms.config.Config;
import lu.allandemiranda.tpms.controller.TpmsScanner;
import lu.allandemiranda.tpms.model.TireState;
import lu.allandemiranda.tpms.model.Tpms;
import lu.allandemiranda.tpms.util.NotificationHelper;
import lu.allandemiranda.tpms.util.UiLogger;
import lu.allandemiranda.tpms.util.UnitConverter;

public class MainActivity extends AppCompatActivity implements UiLogger.Sink {

    private static final int REQ_PERMS = 42;
    private static final long REFRESH_INTERVAL_MS = 1000L;

    private final TireState frontState = new TireState();
    private final TireState rearState = new TireState();

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private ScheduledExecutorService scheduler;
    private TpmsScanner tpmsScanner;

    private TextView tvFrontTime, tvFrontPressure, tvFrontTemp, tvFrontRssi;
    private ImageView dotFront;
    private TextView tvRearTime, tvRearPressure, tvRearTemp, tvRearRssi;
    private ImageView dotRear;
    private TextView tvDebug;

    private int lastFrontColor = -1;
    private int lastRearColor = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UiLogger.setSink(this);
        keepScreenOn();
        applyWindowInsets();
        bindViews();

        tpmsScanner = new TpmsScanner(this);

        if (!isBleReady()) {
            log("BLE não suportado ou Bluetooth desligado.");
            return;
        }

        requestAllPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasAllPermissions()) {
            startReadingLoop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopReadingLoop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UiLogger.setSink(null);
        stopReadingLoop();
    }

    private void bindViews() {
        View root = findViewById(R.id.rootLayout);
        tvDebug = findViewById(R.id.tvDebug);
        if (Config.DEBUG_UI) {
            findViewById(R.id.debugScroll).setVisibility(View.VISIBLE);
        }
        findViewById(R.id.btnInfo).setOnClickListener(v -> startActivity(new Intent(this, lu.allandemiranda.tpms.view.InfoActivity.class)));

        tvFrontTime = findViewById(R.id.tvFrontTime);
        tvFrontPressure = findViewById(R.id.tvFrontPressure);
        tvFrontTemp = findViewById(R.id.tvFrontTemp);
        tvFrontRssi = findViewById(R.id.tvFrontRssi);
        dotFront = findViewById(R.id.dotFront);

        tvRearTime = findViewById(R.id.tvRearTime);
        tvRearPressure = findViewById(R.id.tvRearPressure);
        tvRearTemp = findViewById(R.id.tvRearTemp);
        tvRearRssi = findViewById(R.id.tvRearRssi);
        dotRear = findViewById(R.id.dotRear);
    }

    private void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.rootLayout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void startReadingLoop() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::pollTpms, 0, REFRESH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopReadingLoop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void pollTpms() {
        Tpms[] tpms = tpmsScanner.getTpms(Config.FRONT_MAC, Config.REAR_MAC);
        uiHandler.post(() -> updateTireState(tpms));
    }

    private void updateTireState(Tpms[] tpms) {
        if (tpms[0] != null) {
            updateSingleTire(tpms[0], frontState, tvFrontTime, tvFrontPressure, tvFrontTemp, tvFrontRssi, dotFront, true, Config.FRONT_MIN_KPA, Config.FRONT_MAX_KPA);
        }
        if (tpms[1] != null) {
            updateSingleTire(tpms[1], rearState, tvRearTime, tvRearPressure, tvRearTemp, tvRearRssi, dotRear, false, Config.REAR_MIN_KPA, Config.REAR_MAX_KPA);
        }
    }

    private void updateSingleTire(@NonNull Tpms sensor, @NonNull TireState state, @NonNull TextView tvTime, @NonNull TextView tvPress, @NonNull TextView tvTemp, @NonNull TextView tvRssi, @NonNull ImageView dot, boolean isFront, int minKpa, int maxKpa) {
        state.setLastUpdate(LocalDateTime.now());
        state.setRssi(sensor.rssi());
        state.setPressureKpa(UnitConverter.pressureTpmsToKpa(sensor.pressure()));
        state.setTemperatureC(sensor.temperature());

        tvTime.setText(state.getLastUpdate().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        tvTemp.setText(String.format(Locale.US, "%02d°C", state.getTemperatureC()));

        int kpa = state.getPressureKpa();
        double psi = UnitConverter.kpaToPsi(kpa);
        double bar = UnitConverter.kpaToBar(kpa);
        tvPress.setText(String.format(Locale.US, "%.2f bar (%.1f PSI)", bar, psi));
        tvRssi.setText(String.format(Locale.US, "%d dBm", state.getRssi()));

        int colorRes = state.getColor(minKpa, maxKpa);
        applyDotColorAndNotify(dot, colorRes, isFront, state, minKpa, maxKpa);
    }

    private void applyDotColorAndNotify(ImageView dot, @ColorRes int colorRes, boolean isFront, TireState state, int minKpa, int maxKpa) {
        int last = isFront ? lastFrontColor : lastRearColor;
        if (colorRes == last) return; // nada a fazer

        ImageViewCompat.setImageTintList(dot, ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));
        if (isFront) lastFrontColor = colorRes;
        else lastRearColor = colorRes;

        if (colorRes == R.color.yellow) {
            NotificationHelper.notifySignalLost(this, true);
            UiLogger.log((isFront ? "FRONT" : "REAR") + " SIGNAL LOST");
            return;
        }

        if (colorRes == R.color.orange) {
            NotificationHelper.notifyPressure(this, isFront, state.getPressureKpa(), minKpa, maxKpa);
        }
    }

    private boolean isBleReady() {
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm == null) return false;
        BluetoothAdapter adapter = bm.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    private void requestAllPermissions() {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQ_PERMS);
        } else {
            startReadingLoop();
        }
    }

    private boolean hasPermission(String perm) {
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return false;
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return false;
        }
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasPermission(Manifest.permission.POST_NOTIFICATIONS);
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_PERMS) return;

        boolean allGranted = true;
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i]);
                if (!showRationale) {
                    Toast.makeText(this, "Permissão permanentemente negada: " + permissions[i], Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
                allGranted = false;
            }
        }

        if (allGranted && hasAllPermissions()) {
            startReadingLoop();
        } else {
            Toast.makeText(this, "Permissões necessárias não concedidas.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void log(String msg) {
        if (!Config.DEBUG_UI) return;

        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        String logLine = time + "  " + msg + "\n";

        runOnUiThread(() -> {
            tvDebug.append(logLine);
            ScrollView parent = (ScrollView) tvDebug.getParent();
            parent.post(() -> parent.fullScroll(View.FOCUS_DOWN));
        });
    }
}
