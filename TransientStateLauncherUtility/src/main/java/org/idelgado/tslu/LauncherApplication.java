package org.idelgado.tslu;

import android.app.Application;

import timber.log.Timber;

public class LauncherApplication extends Application {

	private HomeScreen homeScreen;

	@Override
	public void onCreate() {
		super.onCreate();

		// Setup our logging
		if (BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree());
		}
	}

	public void setHomeScreen(HomeScreen homeScreen) {
		this.homeScreen = homeScreen;
	}

	public HomeScreen getHomeScreen() {
		return homeScreen;
	}
}
