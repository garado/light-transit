
# light-sdk-patch

These are patches made to specific files in `light-sdk` and their justification.

At build time, these overwrite the `light-sdk` submodule contents.

## Patches
- Cosmetic
    - `LightBottomBar.kt` - there is an annoying 1dp margin above the bottom bar; it looks weird and asymmetric. I patch it to 0. I'll open a PR for it upstream probably
