# WaEnhancer

An Xposed module for WhatsApp and WhatsApp Business, with a Miuix settings manager.

Download signed APKs from [GitHub Releases](https://github.com/FreeTeaspoon/WaEnhancer/releases/latest).

Requires Android 13 or later and an Xposed framework supporting libxposed API 101, such as Vector or LSPosed IT. The current supported WhatsApp and WhatsApp Business versions are 2.26.38.xx through 2.26.43.xx.

This fork preserves its own package IDs, signing identity, settings recovery, deleted-message backup workflows and update URLs. Upstream is [Dev4Mod/WaEnhancer](https://github.com/Dev4Mod/WaEnhancer).

For a fresh checkout, initialize submodules recursively and run `./scripts/apply-miuix-patches.sh` before building. The Miuix source profile is based on stable 0.9.4 with pager and dialog fixes. Signing properties belong in a private Gradle properties file.

See [changelog.txt](changelog.txt) for release notes. Local documentation under `docs/` is ignored by Git. Required third-party notices remain in [third_party/licenses](third_party/licenses).
