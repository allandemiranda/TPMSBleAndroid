package lu.allandemiranda.tpms.model;

import java.time.LocalDateTime;

import lu.allandemiranda.tpms.R;

public class TireState {
    private LocalDateTime lastUpdate = LocalDateTime.MIN;
    private int rssi = 0;
    private int pressureKpa = 0;
    private int temperatureC = 0;

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public int getPressureKpa() {
        return pressureKpa;
    }

    public void setPressureKpa(int pressureKpa) {
        this.pressureKpa = pressureKpa;
    }

    public int getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(int temperatureC) {
        this.temperatureC = temperatureC;
    }

    public int getColor(int minKpa, int maxKpa) {
        if (this.lastUpdate == LocalDateTime.MIN) {
            return R.color.red;
        } else if (lastUpdate.isBefore(LocalDateTime.now().minusMinutes(5))) {
            return R.color.yellow;
        } else if (pressureKpa > maxKpa || pressureKpa < minKpa) {
            return R.color.orange;
        } else {
            return R.color.green;
        }
    }
}
