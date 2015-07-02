# Transient State Launcher Utility

This Android launcher application provides a repeatable mechanism to test Android application for transient state bugs.

## Background
Transient state bugs are seldomly encountered during development but often appear in production. Transient state can occur for several reasons,
a configuration change occurs(screen orietnation, locale, etc.), the Activity is destroyed by Android due to inactivity, or the application process
is destroyed by the operating system due to "extreme memory pressure". In production environments users typically run numerous apps causing
individual activities or the entire application process to be destroyed leading to a higher prevelance of transient state scenarios.

The major challenge with testing transient state is that there is no simple mechanism to reliably and repeatably test for transient state bugs. The
most common recommendation is to invoke screen orientation changes with an emulator or to use Don't keep activities from the developer options. Both of
these mechanisms fall short of providing true transient state testing. In both cases, these options only test the transient state of the Activity. This
does not destroy the entire transient state of the application. 

## How It Works
This application uses a repeatable procedure to destroy the selected application process and re-launch the application to test for transient state bugs.

1. Use the Transient State Launcher Utility as the default Home application.
2. Launch the application under test.
3. Navigate through the application to arrive at the transient state you are interested in testing.
4. Tap on the heads up display window overlay to start the following automated procedure.
  1. The application is placed into the background by re-launching the default Home application
  2. The launcher application tracks which package was last launched. It destroys the application corresponding to this package by calling ``` ActivityManager.killBackgroundProcesses(String packageName) ```
  3. Re-launch the application after a few seconds
  4. See if this results in unexpected behavior or an application crash.
5. Repeat steps 3-4 as necessary.

## FAQ

*Why did you decide to use a Launcher app?*

Prior to Lollipop, it was possible to determine the last active application by querying getRunningTasks. Unfortunately, this mechanism is now deprecated
and there are no reliable api mechanisms to determine which package was last launched. Alternatively, by writing the Launcher app it is possible to track
which app was last launched and use that information later to destroy the process and re-launch the application.

*Are there other ways to test for transient state bugs?*

Yes, if your device is rooted you can put the application in the background, manually kill the process via adb ``` adb shell pkill your.package.name ````, and then re-launch the application. This should produce the same behavior as described above.

