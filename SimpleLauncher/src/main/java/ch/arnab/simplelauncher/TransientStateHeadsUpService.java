package ch.arnab.simplelauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.util.Calendar;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class TransientStateHeadsUpService extends Service {
    public static String APP_PACKAGE_NAME = "APP_PACKAGE_NAME";

    private String appPackageName;
    private WindowManager windowManager;
    private ImageView transientStateHead;
    WindowManager.LayoutParams params;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        appPackageName = intent.getStringExtra(APP_PACKAGE_NAME);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if(transientStateHead != null) {
            showTransientStateHead();
        }
    }

    private void showTransientStateHead() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        transientStateHead = new ImageView(this);
        transientStateHead.setImageResource(R.drawable.ic_launcher);

        params= new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        // Position the view on the top right side of the screen
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.x = 0;
        params.y = 200;

        transientStateHead.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            // Used to determine whether the view was clicked
            private static final int MAX_CLICK_DURATION = 200;
            private long startClickTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                        if (clickDuration < MAX_CLICK_DURATION) {
                            onTransientStateHeadClicked();
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX
                                + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY
                                + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(transientStateHead, params);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(transientStateHead, params);
    }

    @DebugLog
    private void onTransientStateHeadClicked() {
        if(appPackageName != null) {
            Timber.i(appPackageName);
            startLauncherApp();
            restartPackageProcess();
       }
    }

    private void startLauncherApp() {
        // Launch the home app
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        this.startActivity(intent);
    }

    /**
     * Restarting the background process via ActivityManager.killBackgroundProcess() destroys the
     * memory of the application similar to the case where the kernel is under extreme memory
     * pressure.
     */
    private void restartPackageProcess() {
        final Handler killProcessHandler = new Handler();
        killProcessHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                // Destroy the application process
                final Handler startAppHandler = new Handler();
                ActivityManager am = (ActivityManager)
                        TransientStateHeadsUpService.this.getApplicationContext().getSystemService(Activity.ACTIVITY_SERVICE);

                am.killBackgroundProcesses(appPackageName);

                // Restart the application now that the process has been destroyed
                startAppHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Start the application again
                        Intent intent = TransientStateHeadsUpService.this.getApplicationContext().getPackageManager().getLaunchIntentForPackage(appPackageName);
                        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                        if(transientStateHead != null) {
                            showTransientStateHead();
                        }
                    }
                }, 1000);
            }
        }, 2000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (transientStateHead != null)
            windowManager.removeView(transientStateHead);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
}
