package lu.allandemiranda.tpms.view;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private final TireState frontState = new TireState();
    private final TireState rearState = new TireState();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TpmsScanner tpmsScanner;
    private TextView tvFrontTime, tvFrontPressure, tvFrontTemp, tvFrontRssi;
    private ImageView dotFront;
    private TextView tvRearTime, tvRearPressure, tvRearTemp, tvRearRssi;
    private ImageView dotRear;
    private TextView tvDebug;
    private boolean isFrontSignalOut = false;
    private boolean isRearSignalOut = false;

    private void updateTireState(Tpms[] tpms) {
        int frontStateColorLast = frontState.getColor(Config.FRONT_MIN_KPA, Config.FRONT_MAX_KPA);
        int rearStateColorLast = rearState.getColor(Config.REAR_MIN_KPA, Config.REAR_MAX_KPA);

        if (tpms[0] != null) {
            frontState.setLastUpdate(LocalDateTime.now());
            frontState.setRssi(tpms[0].rssi());
            int pressureKpa = UnitConverter.pressureTpmsToKpa(tpms[0].pressure());
            frontState.setPressureKpa(pressureKpa);
            frontState.setTemperatureC(tpms[0].temperature());

            tvFrontTime.setText(frontState.getLastUpdate().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            tvFrontTemp.setText(String.format(Locale.US, "%02d°C", frontState.getTemperatureC()));
            double psi = UnitConverter.kpaToPsi(frontState.getPressureKpa());
            double bar = UnitConverter.kpaToBar(frontState.getPressureKpa());
            tvFrontPressure.setText(String.format(Locale.US, "%.2f bar (%.1f PSI)", bar, psi));
            tvFrontRssi.setText(String.format(Locale.US, "%d dBm", frontState.getRssi()));
        }
        if (tpms[1] != null) {
            rearState.setLastUpdate(LocalDateTime.now());
            rearState.setRssi(tpms[1].rssi());
            int pressureKpa = UnitConverter.pressureTpmsToKpa(tpms[1].pressure());
            rearState.setPressureKpa(pressureKpa);
            rearState.setTemperatureC(tpms[1].temperature());

            tvRearTime.setText(rearState.getLastUpdate().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            tvRearTemp.setText(String.format(Locale.US, "%02d°C", rearState.getTemperatureC()));
            double psi = UnitConverter.kpaToPsi(rearState.getPressureKpa());
            double bar = UnitConverter.kpaToBar(rearState.getPressureKpa());
            tvRearPressure.setText(String.format(Locale.US, "%.2f bar (%.1f PSI)", bar, psi));
            tvRearRssi.setText(String.format(Locale.US, "%d dBm", rearState.getRssi()));
        }

        int frontStateColor = frontState.getColor(Config.FRONT_MIN_KPA, Config.FRONT_MAX_KPA);
        dotFront.setColorFilter(ContextCompat.getColor(getMainActivity(), frontStateColor));
        if (R.color.blue == frontStateColor && !isFrontSignalOut) {
            NotificationHelper.notifySignalLost(getMainActivity(), true);
            UiLogger.log("XXX-- FRONT SIGNAL LOST --XXX");
            isFrontSignalOut = true;
        } else if (R.color.blue != frontStateColor && isFrontSignalOut) {
            UiLogger.log(">>>-- FRONT SIGNAL BACK --<<<");
            isFrontSignalOut = false;
        }
        if (frontStateColor != frontStateColorLast && R.color.orange == frontStateColor) {
            NotificationHelper.notifyPressure(getMainActivity(), true, frontState.getPressureKpa(), Config.FRONT_MIN_KPA, Config.FRONT_MAX_KPA);
        }


        int rearStateColor = rearState.getColor(Config.REAR_MIN_KPA, Config.REAR_MAX_KPA);
        dotRear.setColorFilter(ContextCompat.getColor(getMainActivity(), rearStateColor));
        if (R.color.blue == rearStateColor && !isRearSignalOut) {
            NotificationHelper.notifySignalLost(getMainActivity(), false);
            UiLogger.log("XXX-- REAR SIGNAL LOST --XXX");
            isRearSignalOut = true;
        } else if (R.color.blue != rearStateColor && isRearSignalOut) {
            UiLogger.log(">>>-- REAR SIGNAL BACK --<<<");
            isRearSignalOut = false;
        }
        if (rearStateColor != rearStateColorLast && R.color.orange == rearStateColor) {
            NotificationHelper.notifyPressure(getMainActivity(), false, rearState.getPressureKpa(), Config.REAR_MIN_KPA, Config.REAR_MAX_KPA);
        }
    }

    private boolean checkBleSupport() {
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm == null) return false;
        BluetoothAdapter adapter = bm.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UiLogger.setSink(this);
        NotificationHelper.ensureChannel(this);
        View root = findViewById(R.id.rootLayout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        tvDebug = findViewById(R.id.tvDebug);
        if (Config.DEBUG_UI) {
            findViewById(R.id.debugScroll).setVisibility(View.VISIBLE);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

        this.tpmsScanner = new TpmsScanner(this);

        if (!checkBleSupport()) {
            log("BLE não suportado ou Bluetooth desligado.");
            return;
        }

        requestAllPermissions();

        UiLogger.log("Starting...");
    }

    private MainActivity getMainActivity() {
        return this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UiLogger.setSink(null);
        handler.removeCallbacks(repetitiveTask);
    }

    @Override
    public void log(String msg) {
        if (!Config.DEBUG_UI) return;

        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        String logLine = time + "  " + msg + "\n";

        runOnUiThread(() -> {
            tvDebug.append(logLine);
            ((ScrollView) tvDebug.getParent()).post(() -> ((ScrollView) tvDebug.getParent()).fullScroll(View.FOCUS_DOWN));
        });
    }

    private void requestAllPermissions() {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQ_PERMS);
        } else {
            handler.post(repetitiveTask);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMS) {
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
                handler.post(repetitiveTask);
            } else {
                Toast.makeText(this, "Permissões necessárias não concedidas.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean hasAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return false;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return false;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    private final Runnable repetitiveTask = new Runnable() {
        @Override
        public void run() {
            new Thread(() -> {
                Tpms[] tpms = tpmsScanner.getTpms(Config.FRONT_MAC, Config.REAR_MAC);

                runOnUiThread(() -> {
                    updateTireState(tpms);
                    handler.postDelayed(repetitiveTask, 1000);
                });
            }).start();
        }
    };
}
