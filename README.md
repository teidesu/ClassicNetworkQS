# Classic Network QS

a minimal app that implements a unified **Internet** QS tile, reproducing the one shipped in android 12, and removed in android 17. 

in other words, this app brings back the unified internet tile to android 17+.

full disclosure: this entire thing was vibe-coded with codex (5.6 sol)

## install

you can install the app from the github releases page.

updates will most likely be rare, but you can also install it from github
through obtainium to get automatic updates.

## build

while this app primarily targets android 17, it is possible to use it on older versions of android (but why?).

just open the repo in android studio, or run:

```sh
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## license

this project is licensed under the MIT license.

icons from aosp (`third_party/aosp` and some `app/src/main/res/drawable`) are licensed under the Apache 2.0 license.