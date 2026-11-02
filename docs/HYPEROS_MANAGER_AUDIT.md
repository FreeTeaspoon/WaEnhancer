# HyperOS manager audit and fixes

Updated 2026-11-02 against the `hyperos-android-app-builder` skill. This report
supersedes the October parity report. It covers the 71 source findings from the
November audit, including workflow bugs, data recovery, accessibility and UI
contract differences. Source changes do not establish device or visual parity.

The starting application revision was `f4dcf9c5e870e0244db1549c074f290baf62096e`.
The Miuix source profile remains `2afdbb39f1aac5747165cc354cafd4b918fa55a5`, based
on v0.9.4 with reference pager fixes. This task does not upgrade dependencies
or claim this profile is the newest stable Miuix release.

## Findings addressed

Paths below refer to `app/src/main/java/com/freeteaspoon/wppenhacer/ui/miuix/`
unless a different directory is named.

| ID | Finding | Implementation |
| --- | --- | --- |
| 1 | Missing Magisk DenyList check | `utils/RootDiagnostics.kt` includes the check in collected diagnostics. |
| 2 | Missing editor image import | `SpecialScreens.kt` restores image selection, PNG naming and replacement confirmation copy; `ManagerFileWorkflowViewModel.kt` decodes and atomically writes the image. |
| 3 | Contact selection chooses the first host | `PreferenceScreens.kt` offers an explicit WhatsApp/Business selector. |
| 4 | Contact-picker failures escape | Picker construction and launch failures report feedback. |
| 5 | Download switches bypass permissions | The page requests all-files access; `PreferenceController.kt` also rejects enabling without access. |
| 6 | File MIME filters are discarded | Page-owned launchers retain each preference's configured types. |
| 7 | Unchecked XML import | `ManagerFileRepository.kt` validates and normalizes bounded XML, rejects document types and external entities before committing. |
| 8 | Unusable directory paths accepted | Paths are checked off the UI thread for existence, readability and an actual write/delete probe before saving. |
| 9 | Contact summaries expose raw identifiers | Summaries use a localized selected-contact count or empty label. |
| 10 | Call-blocking controls remain on the parent | `ManagerModels.kt` and `PreferenceScreens.kt` group all four controls on one feature route. |
| 11 | Theme scans race mutations | Theme scans and file operations share a mutex; confirmed mutations publish reconciled lists. |
| 12 | Theme archive replacement can corrupt existing data | `ManagerFileRepository.kt` stages archives, journals same-filesystem folder renames, and recovers interrupted batches. Recovery is repeatable and preserves transaction-like user theme names. |
| 13 | Unreadable themes appear empty | A failed directory listing becomes an error while cached content remains available. |
| 14 | Unsaved CSS is lost on process recreation | Private atomic draft files restore unsaved input; accepted writes finish when the route leaves. |
| 15 | Growing lists compose eagerly | `managerGroupedCardItems` emits independent lazy rows, including themes, recordings, contacts and search results. |
| 16 | Growing rows lack lazy identity | Every row has a stable group/row key and row content type. Search positioning counts real lazy items. |
| 17 | Growing rows lack mutation motion | Each row uses Miuix spring insertion, removal and placement animation. |
| 18 | Installed-app queries run in composition | `ManagerRestartAction.kt` loads and caches installed packages off the UI thread. |
| 19 | Editor assets load during view creation | The model reads the template on the IO dispatcher before WebView creation. |
| 20 | Theme-name validation performs synchronous IO | Validation uses the loaded theme snapshot; creation checks the real filesystem under the operation lock. |
| 21 | Message states are exactly centered | `ManagerPageStates.kt` uses `BiasAlignment(0f, -0.12f)`. |
| 22 | Message states reserve extra vertical space | Empty/error states use no vertical reserve; loading keeps 56 dp. |
| 23 | Messages have no width cap | The group is capped at 320 dp. |
| 24 | Empty artwork uses the old tile | The shared state uses a bare 36 dp muted Notes icon. |
| 25 | Empty title has the wrong style | The title is 15 sp, medium, onSurface. |
| 26 | Empty hints are visually absent | Useful hints render at 13 sp with a 7 dp gap. |
| 27 | Error artwork has the wrong size/color | Error uses the same bare 36 dp muted icon treatment. |
| 28 | Error title has the wrong style | Error uses the shared 15 sp medium title. |
| 29 | Error hint has the wrong style/gap | Error uses 13 sp regular muted text and a 7 dp gap. |
| 30 | Retry action has excess separation | Hint-to-action spacing is 10 dp. |
| 31 | Retry is a filled primary button | Retry uses transparent normal/disabled backgrounds, 13 sp primary text, 16/8 dp margins and native minimum touch height. |
| 32 | Retry merges with message semantics | Retry is a separate accessible action outside the message group. |
| 33 | Empty/error refresh has no scroll path | `ManagerDesignSystem.kt` retains a scrollable lazy host and viewport-sized state item for empty/error results. |
| 34 | Search is pinned | `ManagerSearchContent.kt` puts search in the same lazy host as its results. |
| 35 | Search-to-result spacing is 18 dp | The control owns the single 6 dp gap; page entry remains 12 dp. |
| 36 | Interrupted navigation can draw two scrims | The Miuix patch assigns ownership to the last visible entry by identity. A desktop pixel regression exercises pop-then-push interruption. |
| 37 | Wide windows use device corner radius | `navigation/ManagerStackNavigator.kt` uses device corners below 600 dp and zero above. |
| 38 | Liquid Glass bottom offsets differ | `liquid/LiquidGlassNavigationBar.kt` uses 12 dp plus the actual inset, including zero. |
| 39 | Snackbar events serialize | The collector launches a child coroutine for each native snackbar. |
| 40 | Snackbars can overlap the IME | Root and shell scaffold insets include the IME. |
| 41 | Ordinary prompts use separate windows | Prompts use native `OverlayDialog`; the patch supplies dialog semantics. |
| 42 | Destructive confirmations use filled actions | Shared actions support neutral error-colored destructive text beside Cancel. |
| 43 | Blocking popup geometry differs | `ManagerOperation.kt` provides a titleless 36 dp overlay, horizontal contrasting 24 dp indicator, 8 dp gap and regular body1 status. |
| 44 | Locked popup moves under predictive Back | The Miuix dialog collector only applies gesture translation/dimming when dismissal is available. Back remains consumed for locked popups. |
| 45 | Work starts while confirmation exits | Configuration, theme, repair, picker and deletion handoffs run from dismissal completion. |
| 46 | Busy ownership ends before popup exit | `ManagerOperation` keeps ownership until exit completion, including queued operations and host recreation. |
| 47 | Blocking operations have no progress host | File preferences, recordings and editor operations use the shared host; operations queue under mutexes. |
| 48 | Percent suffix lacks the required tint | `ManagerInputDialog.kt` uses onSurfaceVariantActions. |
| 49 | Interface-scale summary differs | The resource uses the contract wording and 80% - 110% range. |
| 50 | Invalid inputs lack accessible errors | Validation publishes error semantics and a polite live announcement. |
| 51 | Restart is a Tools preference row | Relevant settings pages and Tools use a bare top-bar Refresh action. |
| 52 | Restart lacks confirmation | The prompt names the affected installed hosts and explains the consequence. |
| 53 | Missing-host restart rows lack disabled semantics | Those rows are removed; restart appears when installed hosts are available. |
| 54 | Unnecessary page introductions remain | Configuration, Themes and Credits introductory cards are removed. Real error feedback remains. |
| 55 | Diagnostics uses extra centered repair buttons | Repairs move to top-bar overflow; the result has one full-width Back action. |
| 56 | Repair adds a spinner to the result | Repairs use the operation overlay while retaining the result. |
| 57 | Failed diagnostics still claims to be checking | Failure uses the shared load-failed title. |
| 58 | Failed diagnostics refresh has no feedback | Refresh retains content and reports the error through a snackbar. |
| 59 | Raw diagnostic output is proportional | Raw entries are explicitly marked and rendered in monospace. |
| 60 | Call-recording summary overpromises controls | The resource describes recording mode and root availability. |
| 61 | WebView renderer death has no recovery | `ManagerEditorAssets.kt` handles renderer loss; the editor releases the view and offers Retry with a new instance. |
| 62 | Editor textarea has no name | The bundled document receives a localized accessible name. |
| 63 | Editor is permanently dark | The document follows resolved light/dark mode, surface/text colors and a light syntax palette. |
| 64 | About stage intervals are halved | `HyperOsAboutBackground.kt` uses the full 5/8 second presets. |
| 65 | Hidden About background still advances | Visibility changes cancel the pending timer/animation until visible again. |
| 66 | About title alpha recomposes the bar | Alpha is consumed in the title graphics layer. |
| 67 | About mark duplicates the app name | Decorative artwork has no content description. |
| 68 | About credits an unused library | RemotePreferences is removed from acknowledgements. |
| 69 | Crash-report Share is a row | `MiuixCrashReportActivity.kt` places Share in the top bar; Copy remains a row. |
| 70 | Injected labels use middle-dot separators | `xposed/features/others/GoogleTranslateChatUi.kt` uses colons. |
| 71 | Audit claims superseded patterns are required | This report replaces the stale compliance claims and separates source fixes from runtime evidence. |

## Reproducible library changes

Run `./scripts/apply-miuix-patches.sh` after initializing submodules. The original
held-swipe patch is preserved. `third_party/patches/miuix-dialog-navigation.patch`
adds the scrim, locked-dialog and overlay semantics changes. The script skips
already applied patches individually; Gradle checks for all required fixes.
Both patches were checked against files extracted from the pinned revision,
then applied and reverse-checked. No submodule commit or dependency pin changes
are required.

## Verification

Passed for the final source changes:

- Both app flavors compile and pass 35 unit tests each, with no failures or skips.
- Both debug APKs and both instrumentation APKs assemble, including all five new Compose regressions.
- Both debug APKs pass the libxposed API 101 metadata verifier.
- Miuix navigation passes 194 desktop tests; Miuix UI passes three desktop tests.
- Both modified Miuix modules pass `spotlessCheck`.
- Root and Miuix worktrees pass `git diff --check`; patch-script syntax and idempotency checks pass.

Both flavors pass lint against the existing baseline, with no unfiltered
errors. Existing warnings and hints remain. The first lint run identified three
new errors, which were corrected without changing the baseline.

The first combined validation attempt exhausted the 512 MiB JVM metaspace
budget. The failed build's orphaned daemon was terminated after confirming it
belonged to this task and had requested shutdown. A fresh normal-wrapper run
completed compilation, tests and packaging within the existing host limits.
No other build or emulator was stopped.

Reproduce the checks using the normal Gradle wrappers:

```sh
./scripts/apply-miuix-patches.sh
./gradlew :app:testWhatsappDebugUnitTest :app:testBusinessDebugUnitTest \
  :app:assembleWhatsappDebug :app:assembleBusinessDebug \
  :app:assembleWhatsappDebugAndroidTest :app:assembleBusinessDebugAndroidTest
./gradlew :app:lintWhatsappDebug :app:lintBusinessDebug
(cd third_party/miuix && ./gradlew :miuix-nav:desktopTest :miuix-ui:desktopTest \
  :miuix-nav:spotlessCheck :miuix-ui:spotlessCheck)
python3 scripts/verify-xposed-apk.py <whatsapp-debug-apk> <business-debug-apk>
git diff --check
git -C third_party/miuix diff --check
```

The desktop interrupted-navigation regression passed with the fix. Running it
against the original renderer failed with the expected duplicate-dim assertion,
then the fixed source was restored. This negative control establishes that the
test exercises the original bug.

Device verification is unavailable. The dedicated audit emulator failed to boot
twice with exit code 1. No other session's device was taken over. No current
instrumentation runs, screenshots, gesture recordings or live hook checks are
claimed. New Compose regressions cover lazy row composition and anchor retention,
separate retry semantics, and initial-loading versus empty-result refresh.
They still need a working device to run. Search focus across empty results and
consecutive instant operation completion also have regression coverage.

Runtime checks remain for cold start/insets; IME and all dialog dismissal paths;
held, cancelled and committed predictive Back; interrupted navigation on Android;
empty/error/held/deep refresh; row motion and scroll retention; stacked snackbars;
About timing and title alignment; editor image import, renderer loss and process
recreation; compact/wide, landscape, RTL, font and interface scaling; blur and
contrast; and large-list release performance. Root diagnostics, permissions,
contacts, restart and actual WhatsApp/Business hooks need a suitably configured
rooted device with the host apps and framework.

## Preserved compatibility decisions

- Green remains the default accent to preserve the upstream/persisted preference default.
- Existing credential storage remains compatible with Xposed readers. Secret manager fields remain masked.
- Home retains its WeKit dashboard and artwork as a product choice.
- minSdk 33 remains required by the pinned blur module.
- Legacy Activities, Fragments, XML and Material dependencies remain for upstream merge compatibility. Injected WhatsApp UI follows host styling.
- No onboarding, OTP, credential, FAB or other unrelated workflow was invented.

These are recorded exceptions or scope boundaries, not claims of standard parity.
