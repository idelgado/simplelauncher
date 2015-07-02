package ch.arnab.simplelauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.IBinder;
import android.view.Display;
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
    public static String HIDE_HUD = "HIDE_HUD";

    private String appPackageName;
    private WindowManager windowManager;
    private ImageView transientStateHead;
    WindowManager.LayoutParams params;

    @DebugLog
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getBooleanExtra(HIDE_HUD, false)) {
            Timber.i("HIDE_HUD");
            intent.removeExtra(HIDE_HUD);
            if(transientStateHead != null && windowManager != null) {
                removeTransientStateHead();
           }
        } else {
            appPackageName = intent.getStringExtra(APP_PACKAGE_NAME);
            showTransientStateHead();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void showTransientStateHead() {
        if(transientStateHead != null) {
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        transientStateHead = new ImageView(this);
        transientStateHead.setImageResource(R.drawable.ic_launcher);

        params= new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        // Position the view on the top right side of the screen
        params.gravity = Gravity.TOP | Gravity.LEFT;

        // Set the view in the left middle part of the screen
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height = size.y;
        params.x = 0;
        params.y = height/2;

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

    private void removeTransientStateHead() {
        try {
            windowManager.removeView(transientStateHead);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            transientStateHead = null;
        }
    }

    @DebugLog
    private void onTransientStateHeadClicked() {
        if(appPackageName != null) {
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

                        showTransientStateHead();
                    }
                }, 1000);
            }
        }, 2000);
    }

    @DebugLog
    @Override
    public void onDestroy() {
        super.onDestroy();
        removeTransientStateHead();
   }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
}
