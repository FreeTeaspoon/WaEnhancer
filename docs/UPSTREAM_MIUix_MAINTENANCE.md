# Maintaining the Miuix manager across upstream merges

WaEnhancer remains one `app` module and one APK per existing product flavor. The launcher points to
`com.wmods.wppenhacer.ui.miuix.MiuixMainActivity`; the retained Fragment, Activity, adapter, and XML
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

- SIM Hub: `a36b4c10b898e5a15c34efdb17e34eb5d6e6fcdc`
- HyperLPA: `9ad421e3a5f922474e426b39cfa84ce1b1f289f8`
- compose-miuix-ui 0.9.3: `c36fab72391801d1e3ea5a00f966bf16bac28d4c`
- Mishka: `edbf244688521b500cc405190166b91dc471111a`
- WeKit: `a55e0d4f7221c3bc3ee5b32f9b0bd62c1291381b`

## Direct source adaptations

WaEnhancer and both reference projects are GPL-3.0 projects. The manager's Appearance screen,
Miuix/Liquid Glass bottom-navigation setup, platform predictive-back toggle, and Liquid Glass
helpers track the pinned Mishka sources. The primary Home dashboard, feature search/category
presentation, and `ManagerStackNavigator` track the pinned WeKit sources. Keep the attribution
headers in adapted files and diff these paths against their reference revisions whenever either
reference pin changes.

For Liquid Glass, preserve the additional Apache-2.0 attribution to
`Kyant0/AndroidLiquidGlass`. In particular, keep `InnerShadow` density conversions outside the
graphics-layer recording scope; moving `Dp.toPx()` into that nested scope causes a main-thread
`StackOverflowError` on current Android Compose builds.

The Miuix 0.9.3 Android artifacts require compile SDK 37. This project therefore compiles against 37
while preserving min SDK 28 and target SDK 34; the blur library manifest is overridden exactly as in
the pinned reference apps and all runtime shader use remains API-gated with an opaque fallback.
