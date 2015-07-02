package org.idelgado.tslu;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Calendar;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class TransientStateHeadsUpService extends Service {
    public static String APP_PACKAGE_NAME = "APP_PACKAGE_NAME";
    public static String HIDE_HUD = "HIDE_HUD";

    public static final String SHARED_PREFS = "transient_state_heads_up_location";

    private String appPackageName;
    private WindowManager windowManager;
    private ImageView transientStateHeadView;
    WindowManager.LayoutParams params;

    @DebugLog
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getBooleanExtra(HIDE_HUD, false)) {
            Timber.i("HIDE_HUD");
            intent.removeExtra(HIDE_HUD);
            if(transientStateHeadView != null && windowManager != null) {
                removeTransientStateHead();
           }
        } else {
            appPackageName = intent.getStringExtra(APP_PACKAGE_NAME);
            showTransientStateHead();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void showTransientStateHead() {
        if(transientStateHeadView != null) {
            showIcon();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        transientStateHeadView = new ImageView(this);
        showIcon();

        params= new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);


        // Check for last known location
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        int x = prefs.getInt("x", -1);
        int y = prefs.getInt("y", -1);

        if(x != -1 && y != -1) {
            // Position the view on the top right side of the screen
            params.gravity = Gravity.TOP | Gravity.LEFT;

            params.x = x;
            params.y = y;
        } else {
            // Position the view on the top right side of the screen
            params.gravity = Gravity.TOP | Gravity.LEFT;

            // Set the view in the left middle part of the screen
            Display display = windowManager.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int height = size.y;
            params.x = 0;
            params.y = height/2;
        }

        transientStateHeadView.setOnTouchListener(new View.OnTouchListener() {
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

                        // Save the last location
                        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit();
                        editor.putInt("x", params.x);
                        editor.putInt("y", params.y);
                        editor.apply();
                        windowManager.updateViewLayout(transientStateHeadView, params);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(transientStateHeadView, params);
    }

    private void showIcon() {
        if(appPackageName != null) {
            try {
                Drawable icon = getApplication().getPackageManager().getApplicationIcon(appPackageName);
                transientStateHeadView.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeTransientStateHead() {
        try {
            windowManager.removeView(transientStateHeadView);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            transientStateHeadView = null;
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
                        /* Start the application from the context of the activity to avoid
                         * having to call it from the service since it requires the
                         * Intent.FLAG_ACTIVITY_NEW_TASK flag. This flag causes some apps to create
                         * a new activity stack on some device/api levels.
                         */
                        LauncherApplication launcherApplication = (LauncherApplication)getApplication();
                        HomeScreen homeScreen = launcherApplication.getHomeScreen();
                        if(homeScreen != null) {
                            homeScreen.startApplication(appPackageName);
                        } else {
                            Toast.makeText(getApplicationContext(), "Unable to relaunch " + appPackageName, Toast.LENGTH_LONG).show();
                        }
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
