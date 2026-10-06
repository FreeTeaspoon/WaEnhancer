# Local Miuix patches

The `miuix` submodule is pinned to `2afdbb39f1aac5747165cc354cafd4b918fa55a5`.
`miuix-held-swipe.patch` fixes pointer ownership and includes regression tests.
Apply it after a fresh recursive checkout with `./scripts/apply-miuix-patches.sh`.
The script checks for an already-applied patch and refuses conflicting source.
