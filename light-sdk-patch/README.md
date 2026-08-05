
# light-sdk-patch

These are patches made to specific files in `light-sdk` and their justification.

At build time, these overwrite the `light-sdk` submodule contents.

## Patches
- Cosmetic
    - `LightBottomBar.kt` - there is an annoying 1dp margin above the bottom bar; it looks weird and asymmetric. I patch it to 0. I'll open a PR for it upstream probably
- Functional
    - Allow reading logs.
        - There is no support for getting current location within the SDK currently. However, LightOS does log locations (see adb logcat output below).
        - LightTransit implements an interface for retrieving the current location, which internally reads logs. Once official location support is added, I'll swap out this out.

```
08-05 10:39:58.812  8073  8073 D LightOSEventManager: Sending event LightOSGeolocationModule:didReceiveLocationUpdate{"altitude":22.06256103515625,"realAccuracy":12.098625183105469,"provider":gps,"latitude":...,"accuracy":0.0,"technology":gnss,"speed":0.0,"timestamp":1785951598799,"longitude":...}
```
