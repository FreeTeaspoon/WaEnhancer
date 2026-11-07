# Local Miuix patches

The `miuix` submodule is pinned to `2afdbb39f1aac5747165cc354cafd4b918fa55a5`,
a source profile based on stable v0.9.4 with pager fixes. UI, icons, preferences,
blur and navigation modules all resolve from this checkout.

`miuix-held-swipe.patch` fixes pointer ownership and includes regression tests.
`miuix-dialog-navigation.patch` fixes interrupted-navigation scrim ownership,
locked-dialog Back handling and dialog accessibility semantics.
`miuix-toolchain.patch` aligns AGP 9.5.1, Kotlin 2.4.21, Compose Multiplatform
1.12.2, NavigationEvent 1.2.0, serialization 1.12.0 and immutable collections
0.5.2. The vendored build includes library modules only, keeping unused demo
and documentation apps out of the app build.
Apply these patches after a fresh recursive checkout with `./scripts/apply-miuix-patches.sh`.
The script checks each patch independently and refuses conflicting source.
