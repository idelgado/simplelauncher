# Transient State Testing Utility

This launcher application provides a simple repeatable way of testing for transient state bugs in your Android app.

## How-To

1. Set the launcher application as your default Home app
2. Launch the app you want to test for transient state bugs
3. Navigate to an Activity you want to test
4. Tap the overlay icon that appears on the screen and wait several seconds for it to re-appear
5. If your app crashes or if the state of your Activity is not restored correctly you have a transient state bug!

## Background

Transient state bugs are rarely encountered during development but often appear in production. Transient state loss can occur for several reasons:

1. A configuration change occurs as a result of a screen orientation change, locale change, etc.
2. The Activity is destroyed. This is equivalent to setting `Don't keep activities` in developer options.
3. The application process is killed due to inactivity.

The challenge of testing transient state loss is that there are no simple repeatable ways to test it without jumping through a few hoops. The most common recommendation is to invoke screen orientation changes with an emulator or to use Don't keep Activities from the developer options. Both of these options fall short of providing complete transient state testing coverage because they only destroy the Activity and do not destroy the entire Application process which can happen during inactivity.

## How It Works

When an app is started from the TSTU launcher app, the package name of the app is recorded. Whenever the overlay icon is pressed once inside the app, the app is placed in the background allowing the laucher app to call ```ActivityManager.killBackgroundProcesses(String appPackageName)```. This kills the app process entirely. After a few seconds, the app is re-launched. Since the ```Recents Activity``` is still present, the app will attempt to restore its state. This allows you to test for transient state bugs.

## FAQ

*Why is this a launcher app?*

Prior to Lollipop, it was possible to determine the last active application by querying getRunningTasks. Unfortunately, this mechanism is now deprecated
and there are no reliable api mechanisms to determine which package was last launched. By using a launcher app it is possible to track
which app was last launched and use this information later to destroy the application process and re-launch the application.

*Are there other ways to test for transient state bugs?*

Yes, if your device is rooted you can put the application in the background, manually kill the process via adb ``` adb shell pkill your.package.name ```, and then re-launch the application. This is the same behavior provided by this launcher utility.


## Credits

The original launcher app was forked from [simplelauncher](https://github.com/arnabc/simplelauncher) and modified to implement the transient state utility.
