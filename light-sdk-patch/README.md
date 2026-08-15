
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
    - `light-bottom-bar-margin.patch` (`LightBottomBar.kt`) - there is an annoying 1dp margin above the bottom bar; it looks weird and asymmetric. I patch it to 0. I'll open a PR for it upstream probably
- Dependencies
    - `allow-snakeyaml-dependency.patch` (`LightSdkPlugin.kt`) - added `org.yaml:snakeyaml` to `ALLOWED_DEPENDENCIES` for parsing Transitous's `config.yml` GTFS source catalog
