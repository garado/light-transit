
# light-sdk-patch

Apply patches to the `light-sdk` submodule at build time.

## Creating/updating a patch

```sh
# Edit the file directly inside the submodule
nvim light-sdk/sdk/ui/src/main/kotlin/com/thelightphone/sdk/ui/LightBottomBar.kt

# Generate diff
git -C light-sdk diff -- <relative/path/to/file> > light-sdk-patch/<name>.patch

# Restore the original submodule state
git -C light-sdk checkout -- <relative/path/to/file>
```

## Patches
- Cosmetic
    - `light-bottom-bar-margin.patch`
        - There is an annoying 1dp margin above the bottom bar; it looks weird and asymmetric. I patch it to 0. I'll open a PR for it upstream probably
- Dependencies
    - `allow-snakeyaml-dependency.patch`
        - Added `org.yaml:snakeyaml` to `ALLOWED_DEPENDENCIES` for parsing Transitous's `config.yml` GTFS source catalog
- Permissions
    - `allow-read-logs-permission.patch`
        - added `android.permission.READ_LOGS` to `ALLOWED_PERMISSIONS` so `lighttool.toml` can declare it.
        - Needed to fetch LightOS's geolocation log lines. This is a stopgap til light-sdk has a proper location API.
        - This still requires manually granting log permissions: `adb shell pm grant dev.garado.transit android.permission.READ_LOGS`
        - This functionality is gated behind a developer settings toggle.
