# Changelog

## 0.3.7 - 2026-11-17

### Phone-test repairs

- Fixed HPF bypass: power and cutoff now control the same FAT Channel filter.
  Explicit edits clear the legacy scalar HPF first; bypass retains the cutoff.
- Added the missing Gate/Expander switch and limiter Release control.
- Added Standard, Tube, and FET compressor selection with model-specific
  controls, retained settings, live commands, and four-rate native presets.
- Added Standard, Passive, and Vintage EQ selection, retained settings, native
  coefficients, response graphs, and four-rate preset support. Standard retains
  its draggable graph nodes.
- Restored FAT Channel module selection with one rack shown at a time and
  tightened input HPF, Mute, and 48V label spacing.
- Replaced manual Refresh with foreground polling every 500 ms. Reads coalesce,
  yield to writes, and discard stale replies; active slider edits are protected.
  Only fields exposed by the device state reply can follow hardware changes.
- Restored the shared FX Return route to Main when enabling block-202 reverb,
  preserving the app's remembered return level and bus settings. Later parameter
  edits leave mixer routing alone; input Processing mix is not changed.
- Checked all six block-201 Voice FX encodings against Linux. Transformer's
  coupled low/private-reverb parameters are part of the existing native mapping.

### Verification and remaining work

- 96 Java unit tests and 25 repository checks pass. EQ coverage includes 808
  independent coefficient fixtures. Lint has no errors and two existing SDK
  currency warnings; the debug APK builds successfully.
- Emulator checks cover all model panels, retained settings, response graphs,
  portrait, landscape, and 130% text size. No emulator crashes were recorded.
- This repair build has not been installed on the phone. Physical checks remain
  for HPF bypass, block-202 audibility, hardware following, and model transitions.
- The reported Standard-compressor distortion remains unresolved. Audio runs
  on the io24, not the phone CPU. AutoGain is still not ported from Linux.
- See [BUILD_REPORT.md](BUILD_REPORT.md) for the APK hash and
  [LIVE_TEST_REPORT.md](LIVE_TEST_REPORT.md) for the user observations.

### Earlier changes included in this release


- Fixed keyboard and accessibility slider changes failing to commit across mixer
  and parameter controls; touch drags still commit on release.
- Reconciled the Voice FX rack with confirmed state after commands, reconnects,
  and refreshes, and ignored redundant picker callbacks.

- Brought the Host ivory/orange rack styling to Android Fat Channel and Voice FX,
  with powder-coat stipple, separate rack ears, and crisp unblurred traces.
- Added a lightweight triangulated reverb illustration responding to room size,
  wet return, high-pass, pre-delay, and bypass; it is not an audio analyzer.
- Preserved the native control transport while updating the rack styling.

- Moved the Android controller into its own repository with independent CI,
  security scanning, dependency updates, and publication checks.
- Updated the pinned build toolchain to Gradle 9.7.1 and Android Gradle Plugin
  9.4.1, with a regression test that keeps every version reference aligned.

## 0.3.6 - 2026-10-12

- Added the complete four-band Standard EQ editor and response graph.
- Unified Mixer and Fat Channel HPF power, retained cutoff across power cycles,
  and ordered scene cutoff writes before HPF power changes.
- Made graph frequency/gain edits one recoverable controller batch and made
  private scene replacement atomic.

## 0.3.5 - 2026-09-27

- Made either front-panel block writable on either input after the existing
  replacement confirmation, including the block currently selected for use.
- Made shared Reverb Off send both the native Off word and a zero wet return,
  while keeping the chosen wet level ready for the next On action.

## 0.3.4 - 2026-09-24

- Added the five-screen native control surface for Mixer, Fat Channel,
  Effects, Presets, and Device settings.
- Added whole-phone scenes, explicit front-panel block operations, native
  Voice FX and reverb controls, channel 2 handling, and high-rate Delay safety.
- Added emulator acceptance coverage and retained the physically proved direct
  USB control and Android USB-audio coexistence path.
