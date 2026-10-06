# Maintaining the Miuix manager across upstream merges

WaEnhancer remains one `app` module and one APK per existing product flavor. The launcher points to
`com.freeteaspoon.wppenhacer.ui.miuix.MiuixMainActivity`; the retained Fragment, Activity, adapter, and XML
manager sources are a compatibility reference and are not normal production routes. Xposed-injected
WhatsApp UI is deliberately outside this migration.

## Merge procedure

1. Merge upstream without moving or rewriting its legacy UI files solely for Miuix. Resolve hook,
   provider, native, manifest, permission, and preference changes in their original locations.
2. Run `testWhatsappDebugUnitTest testBusinessDebugUnitTest`. `PreferenceParityTest` parses all seven
   upstream preference XML files and intentionally fails for a new, removed, duplicated, unsupported,
   or type-incompatible setting.
3. Classify every new keyed preference in `PreferenceRegistry`. Keep the upstream key, storage type,
   default, dependency, summary, and Xposed read contract. Only standalone-manager presentation keys
   may use the `manager_ui_` prefix and must be added to the explicit allow-list test.
4. Port side effects into `PreferenceController`: warnings, mutual exclusion, permission gates,
   restart broadcasts, language restart, theme updates, and dependent enablement. Never move an
   injected WhatsApp control into the manager UI package.
5. If upstream changes import/export, recordings, root diagnostics, updates, call recording, theme
   archives/editor, or crash reporting, update the corresponding Miuix workflow and retain backwards
   compatibility with existing files and preferences.
6. Verify both `whatsapp` and `business` variants with compile, unit tests, debug assembly, lint, and
   `git diff --check`. Device-test both variants against existing preferences and LSPosed before calling
   runtime behavior verified.

## Reference pins

- HyperLPA: `e613bb84a9307c758afda057d28e570aaccb2d43`
- Language Selector: `1220669f6de59490f6a1caae105f8857c3a80348`
- compose-miuix-ui 0.9.4 source profile: `2afdbb39f1aac5747165cc354cafd4b918fa55a5`
- Mishka: `e855709c476c8f82635b3bb6f751975e1319f391`
- Mishka page spacing: `94573b0a0b11b624d75592558d34bc3f2e6ef451`
- WeKit's retained Home adaptation: `a55e0d4f7221c3bc3ee5b32f9b0bd62c1291381b`

After checking out submodules, run `./scripts/apply-miuix-patches.sh`. The tracked
patch contains the held-swipe fix and its desktop regression. Gradle rejects an
unpatched navigation checkout, and every CI workflow applies the patch before
building.

## Direct source adaptations

WaEnhancer, HyperLPA and Mishka are GPL-3.0 projects. Language Selector is Apache-2.0. The manager's Appearance screen,
Miuix/Liquid Glass bottom-navigation setup, platform predictive-back toggle, and Liquid Glass
helpers track the pinned Mishka sources. The primary Home dashboard, feature search/category
presentation, and `ManagerStackNavigator` track the pinned WeKit sources. Keep the attribution
headers in adapted files and diff these paths against their reference revisions whenever either
reference pin changes.

For Liquid Glass, preserve the additional Apache-2.0 attribution to
`Kyant0/AndroidLiquidGlass`. In particular, keep `InnerShadow` density conversions outside the
graphics-layer recording scope; moving `Dp.toPx()` into that nested scope causes a main-thread
`StackOverflowError` on current Android Compose builds.

## HyperOS manager parity audit, 2026-10-05

The manager was audited against `hyperos-android-app-builder` and its linked
contracts. The normal launcher is the Miuix Compose manager; legacy activities,
fragments, XML, and injected WhatsApp UI remain compatibility boundaries.

Audited source checkouts and revisions:

- WaEnhancer, current working tree on `master`.
- HyperLPA `e613bb84a9307c758afda057d28e570aaccb2d43`.
- Language Selector `1220669f6de59490f6a1caae105f8857c3a80348`.
- Mishka spacing reference `94573b0a0b11b624d75592558d34bc3f2e6ef451` and current source `e855709c476c8f82635b3bb6f751975e1319f391`.
- compose-miuix-ui source profile `2afdbb39f1aac5747165cc354cafd4b918fa55a5`, based on stable v0.9.4 `39c40f99844227b853f0049a0933b1f3ae6c00ba` plus the reference pager fixes.

The manager now uses shared Miuix cards, page states, dialogs, detail scaffolds,
progressive top-bar blur, PullToRefresh, typed child routes, adaptive layout,
NavigationEvent-compatible Activity dependencies, and the v0.9.4 pager spring
profile. Dependent feature groups open as child routes. Battery optimization is
a permission row with current state on resume. Long-running update, diagnostics,
recording, import, export, and theme operations retain state in models or locks.
The library swipe-dismiss implementation consumes active pointer changes while
the initiating pointer is held and releases ownership when that pointer lifts.

The pinned blur source declares Android API 33, so the manager's minimum SDK is
33. This is a deliberate compatibility change required by the adopted source
profile. No manifest override lowers that floor. The app still targets SDK 34
and compiles with SDK 37.

Adapted source keeps its original notices: Language Selector's About and
HyperOS shader files retain Apache-2.0 headers, while manager code adapted from
HyperLPA and Mishka retains GPL notices. `LICENSE` remains the product GPL-3.0
licence. The About screen links the GPL notice, source repository, support
channel, dependencies, and the complete contributor list.

## CSS editor

The Compose editor uses bundled Prism Live, Prism and Bliss modules loaded through
AndroidX WebKit 1.17.1 and `WebViewAssetLoader` at
`https://appassets.androidplatform.net/assets/`. File and content access remain
disabled. It keeps the existing JavaScript bridge, atomic writes, Apply, export,
and Clear confirmation without relying on remote scripts. The loader returns
null for the internal `data:` document supplied by `loadDataWithBaseURL`, lets
WebView render it, and serves bundled modules with the correct MIME type.
All other requests outside the asset origin receive 404. Bliss sources are
pinned at `4f38d6effbbf7084f83ed3b686e3f8222a23527a`; preserve the bundled MIT notice.
Keep the WebView's native layout parameters at `MATCH_PARENT`, as well as its
Compose viewport sizing. Chromium forces a zero CSS layout height when native
layout parameters use `WRAP_CONTENT`; `fillParentMaxSize` alone is insufficient.
The document's viewport sizing keeps the input and highlighting layers aligned.

`ManagerThemeEditorTest` requires all-files access set before instrumentation
starts. It verifies invalid names, visible highlighted text, unsaved draft
restoration after Activity recreation, atomic save, Apply,
reopening, and the Clear confirmation. The existing permission tests run in
both denied and granted states. Changing this appop while a suite runs kills
the process on Android.
