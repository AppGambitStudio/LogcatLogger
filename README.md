# LogcatLogger

A utility app to save and download Android device logs easily

## Usage

Use ADB to grant `android.permission.READ_LOGS` to LogcatLogger.

If you don't grant permission, it collects logs from the LogcatLogger app only.

After permission is granted, it starts collecting all the logs.

### Steps

- Install App.
- Connect android device to computer.
- Run below command in Terminal or CMD.

```sh
adb shell "pm grant com.appgambit.android_logger android.permission.READ_LOGS && am force-stop com.appgambit.android_logger"
```

- Open app and it will starts collecting logs.
