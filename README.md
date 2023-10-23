# LogcatLogger

An Android utility app to save and download device logs easily.

## Basic Features

- Keeps the logs for the last 2 days
- Logs are accessible by an HTTP server built inside the service
- Runs in the background to avoid MDM interruption
- Force Stop stops the log collection

## Usage

Use ADB to grant `android.permission.READ_LOGS` to LogcatLogger.

If you don't grant permission, it only collects logs from the LogcatLogger app.

Once the permission is granted, it starts collecting full Android OS logs.

### Steps

- Install the App
- Connect Android device over ADB
- Run the following ADB command

```sh
adb shell "pm grant com.appgambit.android_logger android.permission.READ_LOGS && am force-stop com.appgambit.android_logger"
```

- Open app and it will start collecting logs
