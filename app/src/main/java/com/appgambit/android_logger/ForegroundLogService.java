package com.appgambit.android_logger;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ForegroundLogService extends Service {
//    private static final Build.VERSION_CODES VERSION_CODES = ;
    private Thread logcatThread;
    private static final int NOTIFICATION_ID = 1;
    private String lastLogTimestamp = "";
    private static final int SERVER_PORT = 12990;
    private ServerSocket serverSocket;
    private boolean isServerRunning;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Intent serviceIntent2;
    // Define the number of days after which log files should be deleted.
    private static final int DELETE_LOGS_OLDER_THAN_DAYS = 2;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(NOTIFICATION_ID, createNotification());
        startLogcatUpdaterThread();
        scheduleLogDeletionTask();

        return START_STICKY;
    }

    private void startLogcatUpdaterThread() {
        Intent serviceIntent2 = new Intent(ForegroundLogService.this, WebServerService.class);
        startService(serviceIntent2);
        logcatThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File root = new File(getExternalFilesDir(null), "Log_Datas");
                    if (!root.exists()) {
                        root.mkdirs();
                    }

                    File logFile = new File(root, "logg.txt");

                    double fileSizeInBytes = 0;
                    double fileSizeInKB = 0;
                    double fileSizeInMB = 0;

                    while (!Thread.currentThread().isInterrupted()) {
                        Process process = Runtime.getRuntime().exec("logcat -d");

                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.setLength(0);
                        String line;

                        while ((line = bufferedReader.readLine()) != null) {
                            if (isNewLog(line)) {
                                stringBuilder.append(line).append("\n");
                            }
                        }

                        if (!stringBuilder.toString().isEmpty()) {
                            try (FileWriter writer = new FileWriter(logFile, true)) {
                                writer.append(stringBuilder.toString());
                            }

                            fileSizeInBytes = logFile.length();
                            fileSizeInKB = fileSizeInBytes / 1024;
                            fileSizeInMB = fileSizeInKB / 1024;

                            if (fileSizeInMB >= 1.0) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                                String newFileName = "log_" + sdf.format(new Date()) + ".txt";
                                logFile = new File(root, newFileName);

                                fileSizeInBytes = 0;
                                fileSizeInKB = 0;
                                fileSizeInMB = 0;
                            }
                        }

                        System.out.println(logFile);
                        System.out.println(fileSizeInMB);
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        });

        logcatThread.start();
    }

    // Schedule a periodic task to delete old log files every 24 hours.
    private void scheduleLogDeletionTask() {
        long interval = TimeUnit.HOURS.toMillis(24); // 24 hours
        long initialDelay = TimeUnit.MINUTES.toMillis(1); // 1 minute (to start after service is launched)

        Intent intent = new Intent(this, LogDeletionReceiver.class);
        // Add FLAG_IMMUTABLE to the PendingIntent to comply with Android 12+ requirements.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + initialDelay, interval, pendingIntent);
    }
    private boolean isNewLog(String logLine) {
        if (!logLine.contains(" ")) {
            return false;
        }

        if (logLine.contains("---------")) {
            return false;
        }

        String timestamp = extractTimestamp(logLine);

        if (timestamp != null && !timestamp.equals(lastLogTimestamp)) {
            lastLogTimestamp = timestamp;
            return true;
        }

        return false;
    }

    // Rest of the code as previously discussed...
    private String extractTimestamp(String logLine) {
        String[] parts = logLine.split(" ");
        if (parts.length >= 2) {
            String timestamp = parts[0] + " " + parts[1];
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);
            try {
                Date date = sdf.parse(timestamp);
                return sdf.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }
    // Define a BroadcastReceiver for log deletion.
    public static class LogDeletionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Call a method to delete old log files.
            deleteOldLogFiles(context);
        }

        private void deleteOldLogFiles(Context context) {
            File root = new File(context.getExternalFilesDir(null), "Log_Datas");

            if (root.exists() && root.isDirectory()) {
                File[] files = root.listFiles();

                for (File file : files) {
                    long fileAgeInMillis = System.currentTimeMillis() - file.lastModified();
                    long maxAgeInMillis = TimeUnit.DAYS.toMillis(DELETE_LOGS_OLDER_THAN_DAYS);

                    if (fileAgeInMillis >= maxAgeInMillis) {
                        if (file.delete()) {
                            Log.d("LogCaptureService", "Deleted old log file: " + file.getName());
                        } else {
                            Log.e("LogCaptureService", "Failed to delete old log file: " + file.getName());
                        }
                    }
                }
            }
        }
        
    }
    public Notification createNotification() {
        String notificationChannelId = "LOGCAT LOGGER CHANNEL";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                    notificationChannelId,
                    "Logcat logger notifications channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Service is running...");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            notificationManager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(this, notificationChannelId);
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        return builder
                .setContentTitle("Logcat Logger Service")
                .setContentText("Service is running...")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher) // Replace with your icon resource
                .setTicker("Ticker text")
                .setPriority(Notification.PRIORITY_HIGH)
                .build();
    }
}
