package com.appgambit.android_logger;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

//import com.vizabli.android_logger.R;

public class MainActivity extends AppCompatActivity {
    private TextView t1;
    private Intent serviceIntent1;
    private Intent serviceIntent2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context=getApplicationContext();

        // Start the LogCaptureService
//        serviceIntent2 = new Intent(this, LogCaptureService.class);
//        startService(serviceIntent2);

//
//        // Check if the LogCaptureService is running

        Intent serviceIntent = new Intent(this, ForegroundLogService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        if (isServiceRunning(ForegroundLogService.class)) {
            Toast.makeText(this, "LogCaptureService is running", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "LogCaptureService is not running", Toast.LENGTH_SHORT).show();
        }



    }



    // Function to check if a service is running
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }
}
