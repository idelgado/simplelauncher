package org.idelgado.tslu;

import android.app.Application;

import timber.log.Timber;

public class LauncherApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		// Setup our logging
		if (BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree());
		}
	}

}
