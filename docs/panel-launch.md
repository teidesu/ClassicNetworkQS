# Internet panel launch investigation

Checked AOSP main sources and Android 17/API 37 emulator, 2026-09-17.

## Collapse is not inherent to tile clicks

`CustomTile.handleClick()` invokes `mService.onClick(mToken)` without collapsing.
`TileService.startActivityAndCollapse(PendingIntent)` calls SystemUI's TileServices
binder, which forwards to `CustomTile.startActivityAndCollapse()`. That calls
`ActivityStarterImpl.startPendingIntentMaybeDismissingKeyguard()`, which supplies
`dismissShade=true`; ActivityStarterInternalImpl collapses immediately or at the
end of the launch animation. `Tile.setActivityLaunchForClick()` uses the same path.
`TileServices.onShowDialog()` also calls `forceCollapsePanels()`.

## An alternative exists

Settings' `PanelFeatureProviderImpl.getPanel()` handles the public Internet panel
activity action by sending a foreground broadcast with the SAME action targeted
to `com.android.systemui`, then returning null. PanelFragment finishes the activity.

SystemUI `NetworkControllerImpl.registerListeners()` registers that action through
BroadcastDispatcher with default RECEIVER_EXPORTED and null sender permission.
Its receiver posts `InternetDialogManager.create(true, canConfigMobileData,
canConfigWifi, null)`. InternetDialogManager shows a SystemUIDialog directly,
without invoking the activity-start or collapse path.

`TilePanelLaunchTest` sends that broadcast using the target app context/UID,
without privileged permissions, hidden APIs, or connectivity toggles. With QS
expanded, it opened the system Internet dialog while QS remained expanded behind
it. Screenshot: artifacts/broadcast-panel.png. This is a manual experiment;
the test sends the broadcast, while the result was visually inspected.

Public SDK methods/constants are used, but BROADCAST semantics for this action
and the SystemUI receiver/package are undocumented implementation details.
The SDK documents ACTION_INTERNET_CONNECTIVITY as an activity action. Sending a
broadcast has no portable success/failure acknowledgement, so no reliable
automatic fallback can detect an OEM ignoring it. With user approval, production short-tap now sends this broadcast directly.
Long-tap now opens system Internet settings via public ACTION_WIFI_SETTINGS;
AOSP maps that action to NetworkProviderSettings. Its activity launch still
collapses QS before our code executes.

Sources:
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/SystemUI/src/com/android/systemui/qs/external/CustomTile.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/SystemUI/src/com/android/systemui/qs/external/TileServices.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/SystemUI/src/com/android/systemui/statusbar/phone/ActivityStarterImpl.kt
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/SystemUI/src/com/android/systemui/statusbar/phone/ActivityStarterInternalImpl.kt
- https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/src/com/android/settings/panel/PanelFeatureProviderImpl.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/SystemUI/src/com/android/systemui/statusbar/connectivity/NetworkControllerImpl.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/SystemUI/src/com/android/systemui/broadcast/BroadcastDispatcher.kt
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/SystemUI/src/com/android/systemui/qs/tiles/dialog/InternetDialogManager.kt

Production gesture verification on Android 17: short-tap opens the dialog with
QS expanded; long-tap opens the same dialog but QS is collapsed before its
preferences activity runs. SystemUI dumps report expanded=true and false
respectively. Screenshots: artifacts/short-tap-panel.png and long-tap-panel.png.

## Mobile switch startup animation

Android 17 source: InternetDetailsContentController initializes its cached
mIsMobileDataEnabled=false. Its TelephonyCallback.DataEnabledListener replaces
that with the real value asynchronously. InternetDetailsContentManager and the
legacy delegate set the mobile switch checked state from that cache as content
updates arrive. Their switch lives entirely in SystemUI, not this app.

The stock tile passes an Expandable to InternetDialogManager, which uses
DialogTransitionAnimator.show(). The broadcast passes null and calls
SystemUIDialog.show() directly. That different launch timing can expose the
switch's initial off-to-on checked-state animation instead of hiding it within
the stock reveal. This is a source-supported explanation; no captured timing
trace on the user's device establishes exactly which initialization callback
accounts for the observed animation. Opening the dialog does not call the
mobile-data setter; the setter is wired to switch clicks.

No intent extra controls the initial checked state or supplies the private
Expandable/animation controller. We cannot preinitialize the SystemUI switch,
call jumpDrawablesToCurrentState() on it, or change its animation using this
broadcast contract. No production workaround was added.

Android 17 sources:
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/packages/SystemUI/src/com/android/systemui/qs/tiles/dialog/InternetDetailsContentController.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/packages/SystemUI/src/com/android/systemui/qs/tiles/dialog/InternetDetailsContentManager.kt
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/packages/SystemUI/src/com/android/systemui/qs/tiles/dialog/InternetDialogManager.kt

The long-press relay uses Theme.NoDisplay and an empty task affinity. Its temporary
task finishes in onCreate, preserving the foreground task instead of surfacing an
existing MainActivity task. It remains excluded from recents and has no history.

Long-press behavior updated: the NoDisplay relay now launches public Wi-Fi/Internet
settings in the system Settings task, rather than opening the dialog.
