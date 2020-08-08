package com.frekis.sdk.sample;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.frekis.sdk.Frekis;
import com.frekis.sdk.listener.BleListener;
import com.frekis.sdk.listener.BleStatusListener;
import com.frekis.sdk.listener.ResponseListener;
import com.frekis.sdk.listener.SessionConnectionListener;
import com.frekis.sdk.model.lock.Asset;
import com.frekis.sdk.model.ride.LatLng;
import com.google.android.material.progressindicator.ProgressIndicator;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, BleListener, SessionConnectionListener {

    private static final int REQUEST_CODE_PERMISSION = 101;

//    private static final String API_SECRET_KEY = "0a02a1ed-b9ce-4500-aec4-7a48db13eba3";

    private static final String[] PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private Frekis frekis;
    private TextView txt_status, txt_detail;
    private EditText edt_id, edt_token;
    private Button btn_init, btn_connect, btn_unlock, btn_alarm;
    private ProgressIndicator progressIndicator;
    private LinearLayout layout_scan, layout_token;

    private boolean is_connected = false;
    private boolean is_unlocked = false;
    private boolean is_alarm_enable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapping();

        edt_token.setText("Your API token here");
        edt_id.setText("07873CA3089E277D");
    }

    private void mapping() {
        txt_status = findViewById(R.id.txt_status);
        txt_detail = findViewById(R.id.txt_detail);
        edt_token = findViewById(R.id.edt_token);
        edt_id = findViewById(R.id.edt_id);
        btn_init = findViewById(R.id.btn_init);
        btn_connect = findViewById(R.id.btn_connect);
        btn_unlock = findViewById(R.id.btn_unlock);
        btn_alarm = findViewById(R.id.btn_alarm);
        progressIndicator = findViewById(R.id.progressIndicator);
        layout_scan = findViewById(R.id.layout_scan);
        layout_token = findViewById(R.id.layout_token);
    }


    @Override
    public void onSessionConnectionSuccess() {
        progressIndicator.setVisibility(View.GONE);
        layout_scan.setVisibility(View.VISIBLE);
        txt_status.setText(R.string.session_connected);
        layout_token.setVisibility(View.GONE);
        layout_scan.setVisibility(View.VISIBLE);
        btn_init.setEnabled(true);
    }

    @Override
    public void onSessionConnectionError(int code, String error) {
        progressIndicator.setVisibility(View.GONE);
        layout_scan.setVisibility(View.GONE);
        txt_status.setText(error);
        btn_init.setEnabled(true);
    }


    @Override
    public void onClick(View view) {
        String lock_id = edt_id.getText().toString().trim();
        switch (view.getId()) {
            case R.id.btn_init:
                if (edt_token.getText().toString().trim().isEmpty()) {
                    edt_token.setError("Please enter valid secret token");
                }
                progressIndicator.setVisibility(View.VISIBLE);
                btn_init.setEnabled(false);

//               SDK will authorise the token and give response to user
//               for success or error callback SessionConnectionListener.java
                frekis = Frekis.getInstance(this);
                frekis.setSessionConnectionListener(this);
                frekis.setBleConnectionListener(this);
                frekis.init(edt_token.getText().toString().trim());
                break;
            case R.id.btn_connect:
                if (lock_id.isEmpty()) {
                    edt_id.setError("Please enter lock id");
                    return;
                }

                if (!hasPermission()) return;
                if (is_connected) disconnect();
                else connect();
                break;
            case R.id.btn_unlock:
                LatLng latLng = new LatLng(23.0969506, 72.5849778);
                if (is_unlocked) {
                    progressIndicator.setVisibility(View.VISIBLE);
                    frekis.lock(lock_id, latLng, new ResponseListener() {
                        @Override
                        public void onSuccess() {
                            is_unlocked = false;
                            txt_status.setText(R.string.unlock);
                            btn_unlock.setText(R.string.unlock);
                            progressIndicator.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError(int code, String error) {
                            txt_status.setText(error);
                            progressIndicator.setVisibility(View.GONE);
                        }
                    });
                    return;
                }

                progressIndicator.setVisibility(View.VISIBLE);
                frekis.unlock(lock_id, latLng, new ResponseListener() {
                    @Override
                    public void onSuccess() {
                        is_unlocked = true;
                        txt_status.setText(R.string.lock);
                        btn_unlock.setText(R.string.lock);
                        progressIndicator.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(int code, String error) {
                        txt_status.setText(error);
                        progressIndicator.setVisibility(View.GONE);
                    }
                });
                break;
            case R.id.btn_alarm:
                if (lock_id.isEmpty()) {
                    edt_id.setError("Please enter lock id");
                    return;
                }

                if (!is_connected) return;
                frekis.setVibration(lock_id, !is_alarm_enable, new BleStatusListener() {
                    @Override
                    public void onBleStatusUpdateSuccess(Asset asset) {
                        if (asset == null) return;
                        if (is_alarm_enable == asset.isVibrationEnabled()) {
                            toast("Unable to update the alarm");
                        }
                        is_alarm_enable = asset.isVibrationEnabled();
                        btn_alarm.setText(is_alarm_enable ? "Disable Alarm" : "Enable Alarm");
                    }

                    @Override
                    public void onBleStatusUpdateError(int code, String message) {
                        toast(message);
                    }
                });
                break;
        }
    }

    @AfterPermissionGranted(REQUEST_CODE_PERMISSION)
    private void connect() {
        progressIndicator.setVisibility(View.VISIBLE);
        frekis.connect(edt_id.getText().toString());
        btn_connect.setEnabled(false);
    }

    private void disconnect() {
        frekis.disconnect(edt_id.getText().toString());
        btn_unlock.setVisibility(View.GONE);
        btn_connect.setEnabled(true);
        btn_connect.setText(R.string.connect);
    }

    private boolean hasPermission() {
        if (EasyPermissions.hasPermissions(this, PERMISSIONS)) return true;
        EasyPermissions.requestPermissions(this, "Permission are required to access bluetooth of the device", REQUEST_CODE_PERMISSION, PERMISSIONS);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(REQUEST_CODE_PERMISSION, permissions, grantResults, this);
    }

    @Override
    public void onBleUpdate(String status, Asset asset) {
        txt_status.setText(status);
        if (asset == null) {
            is_alarm_enable = false;
            btn_alarm.setVisibility(View.GONE);
            txt_detail.setText("Asset detail not found");
        } else {
            is_alarm_enable = asset.isVibrationEnabled();
            btn_alarm.setText(is_alarm_enable ? "Disable Alarm" : "Enable Alarm");
            btn_alarm.setVisibility(View.VISIBLE);
            txt_detail.setText(asset.toString());
        }
    }

    @Override
    public void onBleConnected() {
        is_connected = true;
        btn_connect.setText(R.string.disconnect);
        btn_unlock.setVisibility(View.VISIBLE);
        progressIndicator.setVisibility(View.GONE);
        btn_connect.setEnabled(true);
        btn_alarm.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBleConnectionError(int code, String error) {
        btn_connect.setEnabled(true);
        is_connected = false;
        txt_status.setText(error);
        progressIndicator.setVisibility(View.GONE);
        btn_alarm.setVisibility(View.GONE);
    }

    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}