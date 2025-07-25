package lu.allandemiranda.tpms.service;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import lu.allandemiranda.tpms.model.ManofactureData;
import lu.allandemiranda.tpms.util.UiLogger;

public class BleScanner {
    private final BluetoothLeScanner bleScanner;
    private final Context context;

    public BleScanner(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    public List<ManofactureData> getManufacturerData(int durationMillis) {
        List<ManofactureData> manufacturerDataList = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            UiLogger.log("Permissão de BLUETOOTH_SCAN não concedida – abortando scan.");
            return manufacturerDataList;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            UiLogger.log("Permissão de localização não concedida – abortando scan.");
            return manufacturerDataList;
        }

        CountDownLatch latch = new CountDownLatch(1);

        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                SparseArray<byte[]> md = Objects.requireNonNull(result.getScanRecord()).getManufacturerSpecificData();
                if (md != null) {
                    for (int i = 0; i < md.size(); i++) {
                        byte[] data = md.valueAt(i);
                        if (data != null) {
                            manufacturerDataList.add(new ManofactureData(result.getDevice().getAddress(), result.getRssi(), data));
                        }
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                UiLogger.log("Scan falhou: " + errorCode);
                latch.countDown();
            }
        };

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        bleScanner.startScan(null, settings, scanCallback);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            bleScanner.stopScan(scanCallback);
            latch.countDown();
        }, durationMillis);

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            UiLogger.log("Thread interrompida");
        }

        if (manufacturerDataList.isEmpty()) {
            UiLogger.log("Nenhum MD encontrado");
        } else {
            UiLogger.log(manufacturerDataList.size() + " MDs encontrado");
        }

        return manufacturerDataList;
    }
}
