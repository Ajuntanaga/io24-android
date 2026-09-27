# Changelog

## Unreleased

- Moved the Android controller into its own repository with independent CI,
  security scanning, dependency updates, and publication checks.
- Updated the pinned build toolchain to Gradle 9.7.1 and Android Gradle Plugin
  9.4.1, with a regression test that keeps every version reference aligned.

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
