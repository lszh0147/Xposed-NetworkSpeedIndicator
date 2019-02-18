package me.seasonyuu.xposed.networkspeedindicator.h2os;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class StatusBarIconModeActivity extends Activity {
    private CheckBox cbLightStatusBar;
    private CheckBox cbLowProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status_bar_icon_mode);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        findViewById(R.id.light_status_bar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cbLightStatusBar.setChecked(!cbLightStatusBar.isChecked());
            }
        });
        findViewById(R.id.low_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cbLowProfile.setChecked(!cbLowProfile.isChecked());
            }
        });
        cbLightStatusBar = findViewById(R.id.cb_light_status_bar);
        cbLowProfile = findViewById(R.id.cb_low_profile);

        cbLightStatusBar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    View decorView = getWindow().getDecorView();
                    int flags = decorView.getSystemUiVisibility();
                    if (isChecked)
                        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    else
                        flags = flags ^ View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    decorView.setSystemUiVisibility(flags);
                }
            }
        });

        cbLowProfile.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                View decorView = getWindow().getDecorView();
                int flags = decorView.getSystemUiVisibility();
                if (isChecked)
                    flags |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                else
                    flags = flags ^ View.SYSTEM_UI_FLAG_LOW_PROFILE;
                decorView.setSystemUiVisibility(flags);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        View decorView = getWindow().getDecorView();
        int flags = decorView.getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            cbLightStatusBar.setChecked((flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) != 0);
        else
            cbLightStatusBar.setEnabled(false);
        cbLightStatusBar.setChecked((flags & ~View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }
}
