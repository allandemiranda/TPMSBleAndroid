package lu.allandemiranda.tpms.view;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import lu.allandemiranda.tpms.R;
import lu.allandemiranda.tpms.config.Config;

public class InfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        TextView tv = findViewById(R.id.tvInfo);
        String txt = "Static configuration:\n"
                + "Front: " + Config.FRONT_MAC + " min " + Config.FRONT_MIN_KPA + "kPa max " + Config.FRONT_MAX_KPA + "kPa\n"
                + "Rear : " + Config.REAR_MAC + " min " + Config.REAR_MIN_KPA + "kPa max " + Config.REAR_MAX_KPA + "kPa";
        tv.setText(txt);
    }
}
