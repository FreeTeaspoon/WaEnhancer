# HyperOS manager audit

Audited on 2026-10-05, completed on 2026-10-06 using `hyperos-android-app-builder`, including its design,
page spacing, Appearance/About and verification contracts. Scope is this
WaEnhancer checkout, both existing product flavors. The app remains an Xposed
module with the same application IDs, preference keys, file formats and hook
capabilities.

## Screen inventory

| Screen | Changes or preserved behavior |
| --- | --- |
| Home | Existing fork dashboard and module status retained; native card feedback and 6 dp card gaps; shared safe-area ownership. |
| Features and Search | Fixed search above results; remaining-viewport empty state accounts for IME; typed search destinations; native grouped cards. |
| General, Home screen, Conversation, Status, Privacy, Media, Customize | Registry still reads all seven upstream XML files; headings only when multiple groups; common cards, validated value dialogs, preserved dependency and mutual-exclusion behavior. |
| Dependent feature pages | Typed routes for color, wallpaper, filters/CSS, bubble colors, floating bar, bootloader customization, read receipt options, double-tap, video quality, call recording and transcription groups. Parent rows show On/Off. |
| File imports | Native storage-permission recovery before copying into the existing public folder; XML string imports need no shared-storage permission; failed copies preserve the previous file. |
| Tools | Native destination/action rows; battery permission state refreshes on resume. |
| Appearance | ThemeController, system bars, Monet conditions, full-width layout, validated 80–110 percent scale dialog, native menus and animated conditional rows. |
| About | Actual launcher silhouette; Language Selector OS3 background and measured fixed hero; progressive top bar; one glass dependency card. |
| Credits | Typed child route preserving all contributor links, GPL and support links. |
| Configuration | First tip, concise import/export rows, separate destructive Reset card, confirmations, ViewModel operation lock and snackbar feedback. |
| Updates | Complete asynchronous result, first-load/error states, native refresh retaining prior content. |
| Diagnostics | Whole result published after checks finish; centred result and full-width Back; refreshing preserves the previous result. |
| Call recording mode | Native choice menu; authoritative root check and serialized selection. |
| Recordings and contact detail | Storage permission recovery, retained refresh content, stable keys, top-bar share/delete/overflow, selection, confirmation and a real contact child route. |
| Themes | Storage recovery, first tip, native entity rows, validated creation, confirmed archive replacement, retained model state. |
| Theme editor | Real full-viewport WebView; Save and overflow Apply/Clear/Export; Clear confirmation, atomic writes, draft retained during activity recreation. |
| Crash report | Existing selectable report and sharing retained; native copy icon and snackbar. |

All scrolling child pages use the shared detail scaffold. It owns adaptive bars,
880 dp ordinary content width, horizontal cutout safety, Miuix overscroll and
haptics, page-start classification, refresh geometry and collapsed-title
correction. Appearance and About keep their explicit width/hero exceptions.
Native `IconButton` geometry is preserved.

The shared empty-state host uses the required 48 dp muted squircle, 12 dp
corners, 27 dp Notes icon, 18 dp gap and regular 15 sp title. The squircle
uses summary-color alpha 0.23 in light mode and 0.38 in dark mode. Explanations
remain in the merged accessibility description, while error pages retain
their separate Refresh icon, message and available retry action.

## Navigation and dependencies

The root handler observes the immediate pager target outside NavDisplay.
Programmatic navigation uses `springAnimateToPage`; user paging uses the same
spring fling profile, `pagerGestureOverride` and its nested-scroll connection.
Child routes use Miuix NavDisplay and the standard transitions with directional
swipe-back.

All Miuix modules resolve to the local source profile `2afdbb39`, including
icons, preferences and blur. Stable tag v0.9.4 resolves to `39c40f99`; the source
profile adds the reference pager fixes. The former checkout was `ac7802c7` on
0.9.3. Dialogs, dropdown wrappers, TextField, refresh and backdrop signatures
were inspected and the app wrappers compiled against the new profile.

Runtime dependency inspection resolves Activity 1.13.0 and NavigationEvent
1.1.2. `javap` confirms that ComponentActivity implements
NavigationEventDispatcherOwner. The manifest enables platform Back support.
The local nav patch consumes all changes during a claimed held swipe, releasing
ownership when its initiating pointer lifts. Its regression checks both held
secondary touches and interactive release settle.

The current blur module requires API 33. Minimum SDK therefore changes from 28
to 33; target SDK remains 34 and compile SDK remains 37. No manifest floor
override remains. Kotlin 2.4.20, AGP 9.4.1 and Gradle 9.7.0 match the source
profile. Compose instrumentation follows the resolved Compose 1.12.1 profile.

## References and notices

Exact revisions and patch application are in
[UPSTREAM_MIUix_MAINTENANCE.md](UPSTREAM_MIUix_MAINTENANCE.md).
Actual reference Kotlin was inspected, including HyperLPA's shared scaffolds,
dialogs and pager; Mishka's page-entry composition and current HintCard; and
Language Selector's About and shader implementation.

The adapted shader files retain their Apache copyright/SPDX notices. A full
Apache license and adaptation notice are included in the APK's `assets/licenses`
and in this repository. Existing GPL, WeKit and AndroidLiquidGlass notices
remain. New manager copy follows the existing manager's English resource
policy. Incomplete inherited translations fall back to the base resources and
are acknowledged per string in lint, without deleting existing translations.
Synthetic injected view IDs retain their values and carry a narrowly scoped
ResourceType annotation because they are not Android resource IDs.

## Product boundaries

Legacy Activities, Fragments, adapters and XML remain available for upstream
merges. They are not normal manager navigation. Xposed-injected WhatsApp views
retain the host app's UI and are deliberately outside the Miuix manager scope.
The fork's existing dashboard artwork and persisted appearance choices are
preserved. There is no new onboarding, credential form, OTP inbox or message
history workflow, so their conditional skill rules do not introduce new pages.

Runtime hook behavior requires WhatsApp and LSPosed, and successful root
checks require an authorized rooted device. An isolated emulator verifies the
manager and denied/unavailable states; it cannot establish those integrations.

## Preference compatibility exception

The fork's XML defaults `wae_color_preset` to `green`, so the manager retains
Green as its default accent instead of the skill's System accent default.
Existing Cyan selections also remain readable. These values belong to the
existing preference contract and are not reported as standard Appearance
parity. Theme mode, Monet, palette, blur, scale and navigation retain the
specified manager defaults.

The final review also corrected the theme manager's denied-storage title and
removed refresh where storage access is unavailable. Recording-mode requests
now retain the latest selection if a root check finishes after the user has
selected non-root mode.

Transcription credential rows now mask saved AssemblyAI and Groq keys, and their
editors use password keyboard and visual transformation. The existing storage
format remains compatible with the Xposed readers. Palette and accent labels
use resources; a retained Cyan selection appears accurately in the menu.
About combines horizontal system and cutout insets with their maximum, and its
top bar includes side navigation-bar safety.

Unavailable preference rows now expose disabled accessibility semantics. Miuix's
native disabled arrow row removes click handling but does not publish that state,
so the manager adds it without changing native row geometry or press feedback.

Predictive Back follows the persisted appearance state for switch changes,
configuration imports and resets. Applying a changed value updates the platform
flag and recreates the Activity without a transition, retaining its child route.

Safe legacy theme names longer than 64 characters remain readable and editable.
Only the new-name dialog applies the 64-character input limit; repository path
validation preserves existing themes while rejecting traversal and unsafe names.

The CSS editor retains Prism Live syntax highlighting with bundled Bliss
modules at revision `4f38d6effbbf7084f83ed3b686e3f8222a23527a` and the complete
MIT notice. It needs no remote scripts. The asset loader allows WebView to
handle its internal `data:` document; previously the external-request fallback
returned 404 for that document and showed a WebView error page. File/content
access remain disabled and unhandled external requests remain blocked.
The WebView has explicit `MATCH_PARENT` native layout parameters. Compose's
measured size alone did not prevent Chromium's wrap-content mode from forcing
its CSS layout height to zero, leaving the textarea only about 6 pixels tall.
The document now sizes its editor with viewport units rather than depending on
a chain of percentage heights. The sizing behavior was checked against
[Chromium's AwLayoutSizer](https://chromium.googlesource.com/chromium/src/+/HEAD/android_webview/java/src/org/chromium/android_webview/AwLayoutSizer.java).

## Verification

The final Gradle check ran these tasks with one worker, local JVM memory limits
and a disk-backed Java temporary directory:

```sh
./gradlew :app:assembleWhatsappDebug :app:assembleBusinessDebug \
  :app:assembleWhatsappDebugAndroidTest :app:assembleBusinessDebugAndroidTest \
  :app:testWhatsappDebugUnitTest :app:testBusinessDebugUnitTest \
  :app:lintWhatsappDebug :app:lintBusinessDebug --no-daemon --max-workers=1
```

Lint succeeds with the existing repository baseline. The final reports contain
412 WhatsApp warnings and 413 Business warnings.

Both `whatsapp` and `business` pass Kotlin compilation, debug assembly and
instrumentation APK assembly. Each flavor passes 24 unit tests and 18 permanent
device tests. Two file-import permission tests per flavor also pass with
all-files access granted,
after running the full suites with access denied. The desktop Miuix held-swipe
regression passed separately.

Permanent device coverage includes native menus and dialog Back, child routes,
rapid Back during tab motion and immediately after a pop, search positioning
within large grouped cards, restored scroll positions, collapsed-title retention,
file-import permissions, Liquid Glass drag reversal and release, and platform
Predictive Back changes after replacing settings.
The editor regression covers invalid theme names, visible syntax-highlighted
CSS containing HTML-sensitive characters, viewport geometry, unsaved draft
restoration after Activity recreation, exact saved bytes, Apply preference
values, reopening, Clear cancellation and confirmed draft clearing.

The earlier adaptive and motion captures used the isolated API 35 emulator
with host GPU rendering. Final permanent suites and editor/empty-state captures
used the same private AVD with SwiftShader rendering. Visual checks cover all
seven preference categories, card/heading/tip/input page starts, empty search
with the keyboard, masked transcription keys, denied and granted storage,
recordings empty state, diagnostics and refresh, live update results,
Appearance at 80/90/100/110 percent, dark Monet with pure black, opaque fallback,
About expanded/collapsed, and both floating navigation styles with labels or
icons only.

Adaptive captures passed at compact, wide and expanded sizes, forced RTL,
150 percent system font, landscape with a simulated tall cutout and three-button
navigation, and disabled system animations. Held pager swipes verify selection
before release and after reversal. A separate rotated-device capture confirms
three-button navigation mode `0`, the visible side buttons, and cutout safety.

All three animation scales were `1` for the native motion recording. That
recording runs outside the Compose test clock and verifies native page motion,
system Back and edge Back. Its frames
were extracted and inspected with FFmpeg. Display overrides, rotation, font,
RTL, overlays and animation scales were restored afterwards.

Adaptive and motion evidence is in
`/home/kerry/.local/share/waenhancer-audit/2026-10-05/`.
Final build, unit/lint and permanent device-suite evidence is in
`/home/kerry/.local/share/waenhancer-audit/2026-10-06/`.
Final suite logs are `whatsapp-instrumentation.log` and
`business-instrumentation.log`; permission rechecks are the corresponding
`storage-granted.log` files. Editor logs are the corresponding `theme-editor.log`
files. Both flavor `*-theme` folders contain visible CSS, restored draft,
selected theme and reopened editor captures. Light/dark empty recordings and
search with/without the keyboard are captured as `empty-*.png`.
The final Gradle log is `editor-native-viewport-build.log`, and
`verification-summary.json` records APK hashes and test counts.
Scenario folders in the October 5 evidence contain screenshots and their
passing instrumentation logs. `native-motion.mp4` records normal motion;
`waenhancer-native-motion-frames.png` contains the inspected frames.
Earlier failed collection attempts are retained under `prior-attempts`.
Final device logcat contains no fatal application exception or navigation
dispatcher/linkage exception.

The temporary instrumentation screenshot collector was removed from the
repository. Test APKs and seeded app state were cleared from the isolated
emulator, its original animation settings were restored, and it was stopped.
`cleanup.json` records those checks. The permanent tests remain in the repository.
