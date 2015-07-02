package org.idelgado.tslu;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;


public class HomeScreen extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homescreen);

        ((LauncherApplication)getApplication()).setHomeScreen(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_screen, menu);
        return true;
    }

    /**
     * Starts the application based on the specified package name
     * @param packageName
     */
    public void startApplication(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);

        if (intent != null) {
            startActivity(intent);

            // Start the transient state heads up service
            Intent serviceIntent = new Intent(this, TransientStateHeadsUpService.class);
            serviceIntent.putExtra(TransientStateHeadsUpService.APP_PACKAGE_NAME, packageName);

            startService(serviceIntent);
        }
    }

}
