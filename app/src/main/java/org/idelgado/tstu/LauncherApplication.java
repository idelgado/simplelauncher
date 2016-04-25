package org.idelgado.tstu;

import android.app.Application;

import org.idelgado.tstu.activity.HomeScreenActivity;

import timber.log.Timber;

public class LauncherApplication extends Application {

	private HomeScreenActivity homeScreenActivity;

	@Override
	public void onCreate() {
		super.onCreate();

		// Setup logging
		if (BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree());
		}
	}

	public void setHomeScreenActivity(HomeScreenActivity homeScreenActivity) {
		this.homeScreenActivity = homeScreenActivity;
	}

	public HomeScreenActivity getHomeScreenActivity() {
		return homeScreenActivity;
	}
}
