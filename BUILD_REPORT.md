# Android controller build report

## 0.3.7 repair build — 2026-11-17

This build responds to the user's phone test: HPF bypass, missing dynamics and
EQ controls, automatic hardware readback, shared-reverb routing, and FAT Channel
navigation. It has not been installed on the physical phone.

### Artifact and verification

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- SHA-256: `b0a490b59c30d6bb1792d1b15b099f629f228e8b90b98e9b45b8ce82b60904fa`
- Size: 1,154,344 bytes
- Version: `0.3.7` (`versionCode` 12), Android debug signing
- Java unit tests: 96 passed, zero failed or skipped
- Host repository contracts: 25 passed
- Android lint: zero errors; existing `OldTargetApi` and `GradleDependency` warnings
- Offline debug APK assembly: passed; one Gradle worker, two active JVM processors

The EQ tests include 808 independent coefficient fixtures derived from the
Linux UC instruction interpreter. Standard, Passive, and Vintage coefficients
and four-rate preset layouts are covered, including the Vintage preset band
ordering. Tube and FET compressor live commands and four-rate preset records
match the Linux reference. Model changes retain each model's controls.

### Emulator checks

The repair APK installed and launched on the API 35 emulator at 720 × 1600,
320 dpi, using two CPU cores and two software-renderer threads. Visual checks
covered compact input switches, one-rack FAT navigation, all three EQ and
compressor models, lower controls, 130% text size, and landscape scrolling.
Passive EQ retained its edited boost after switching to Vintage and back;
Tube retained Peak Reduction after a FET switch. The Passive response graph
changed with its controls. Labels and selectors remained readable in the
checked layouts. The crash log was empty. Display settings were restored and
the emulator was stopped after verification. These checks used the simulated
device; they do not establish audio behavior on the physical io24.

### Repairs

- HPF power now bypasses the programmable filter while retaining its cutoff.
  Explicit HPF edits clear the old scalar HPF so the two filters do not cascade.
  Connection and automatic polling do not change the HPF.
- Gate exposes Expander; limiter exposes Release.
- Compressor exposes Standard, Tube, and FET with their own controls.
- EQ exposes Standard, Passive, and Vintage with their own controls and actual
  coefficient response graphs. Standard retains draggable frequency/gain nodes.
- FAT Channel module buttons show one rack at a time; input switches use compact
  label spacing.
- Foreground polling replaces Refresh, coalesces reads, and protects local edits
  from stale replies. Hardware following covers fields available in the state
  reply; it cannot infer all effect parameters or the selected DSP model.
- Enabling block-202 reverb restores the shared FX return to MAIN while retaining
  its trim, mute, solo, and assignment state. Parameter edits do not overwrite
  input mixes. All six block-201 Voice FX encodings match the Linux reference;
  Transformer's coupled low/private-reverb values are intentional in that mapping.

### Publication audit — 2026-11-17

A bounded code audit found no blocking defects in DSP command encoding, HPF
bypass, effect-return routing, compressor/EQ model selection and preset layouts,
or in foreground polling, lifecycle handling, stale-reply protection, and
active-edit protection. The audit was read-only and did not access hardware.
The 25 host repository checks passed again before publication; the existing
96-test, lint, APK, and emulator results above remain the build verification.
Patch details are recorded in `CHANGELOG.md` under 0.3.7.

### Remaining physical checks

HPF audibility, shared block-202 reverb, hardware knob following, and switching
alternate models back to Standard need physical acceptance. The reported
Standard-compressor distortion has no isolated reproduction yet; this build
makes no claim to repair its audible cause. The phone sends control messages,
while audio processing runs on the io24. AutoGain remains a Linux host feedback
loop and has not been ported to Android.

## Historical 0.3.6 report

The record below describes the previous build. Its Standard-only model limits,
manual Refresh workflow, and HPF cutoff behavior are superseded by 0.3.7 above.

Clean offline build completed on 2026-11-03.

## Artifact

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- SHA-256: `c7ad0f4d50dee04f48c6a6fd65cb02713aea637ece9d69b20b9499762825ff72`
- Size: 1,013,072 bytes
- Package: `dev.ajuntanaga.io24`
- Version: `0.3.6` (`versionCode` 11)
- Android: API 23 minimum, API 35 target
- Signing: Android debug certificate

## Automated verification

- Standalone source, protocol, toolchain, and publication contracts: 25 passed
- Java unit tests: 74 passed, 0 failed, 0 skipped
- Android lint: zero errors; existing SDK currency warnings `OldTargetApi` and
  `GradleDependency`
- Clean debug APK assembly: passed
- Toolchain: Gradle 9.7.1, Android Gradle Plugin 9.4.1, Temurin 17
- Manifest: USB host feature only; no requested permissions
- USB entry: launcher only; no automatic `USB_DEVICE_ATTACHED` activity

The native byte suite covers scalar state/write framing, ACK parsing, mixer
blocks, both Voice FX input assignments, all six Voice FX models, reverb,
variable HPF, gate, Standard compressor, limiter, Standard EQ, UID wrap, scene
validation, mutual Voice FX exclusion, exact two-block addressing, complete
native `MemP/Stat` record generation, either-block replacement on both inputs,
shared-reverb Off wet muting with semantic mix retention, per-connection rate
confirmation, and the
88.2/96 kHz device-Delay safe-off/settle sequence. A rate confirmation at any
rate leaves a selected Delay explicitly off until the user enables it again.
Scenes cannot confirm a remembered rate or move a front-panel block, and active
Delay is refused in every device-resident block. The flat and fully edited
Standard native records match the Linux builder byte for byte. HPF power is one
shared channel state across Mixer, Fat Channel, scenes, and native presets.
Cutoff edits program the selected coefficients even while that shared power is
off, and power cycling retains the cutoff.

The controller tests also prove that Disconnect publishes immediately while
USB release drains on the serial worker, queued writes are discarded after a
disconnect, and an interrupted scene reports its confirmed progress plus the
possibility of a partially changed device.

## Android audit follow-up

The code audit repaired non-touch slider commits in ParameterSlider and the
three mixer control classes. Voice FX selection now reconciles with confirmed
state when an operation finishes instead of permanently retaining a local
selection. Redundant model and input picker callbacks no longer send commands.

The emulator installed and launched the APK and displayed the disconnected
Mixer correctly. After simulated Connect, QEMU failed to create a thread and
exited under the workstation process limit. The new interaction fixes and
remaining screens therefore still need emulator interaction checks; the
automated suite does not substitute for those checks. No physical device was
accessed.

## Resource-limited continuation (2026-11-03)

A second attempt installed the updated APK and invoked verified simulation
using one guest CPU and two host CPUs. Session thread usage nevertheless rose
to 382 of 384 slots. The screenshot command did not produce a usable capture
before the session was interrupted. This attempt adds no visual or interaction
acceptance evidence. A subsequent process check found no audit emulator left
running.

At the user’s request, leave the emulator stopped and avoid another launch or
full build while resource capacity remains unresolved. The next UI check should
verify non-touch slider commits and Voice FX state reconciliation first, then
finish the contrast and navigation checks described above. Existing automated
results remain valid for the unchanged application source; physical-device
acceptance remains separate.

## Completed emulator continuation (2026-11-03)

The user authorized additional resource headroom. Runtime task limits were
raised to 1,536 for this session and 2,048 for its parent Codex slice. The
session memory ceiling remained 10 GB. One emulator ran with two guest cores,
two host CPUs, two software-renderer threads, and 2.5 GB guest RAM; no build
ran alongside it. During interaction, session usage was about 323 tasks and
4.5 GB RAM, with zero current memory pressure. At completion, all session
memory-limit and OOM event counters were zero. The emulator was then stopped.
These results resolve the pending portrait interaction and contrast checks
from the two interrupted attempts above.

The existing 0.3.6 APK was installed and checked on Android 15 at 720x1600,
320 dpi. Application source and APK were unchanged, so the preceding complete
verification results still apply. This pass confirmed:

- Keyboard gain adjustment from 0 to +3 dB survives Refresh.
- Keyboard cutoff adjustment from 80 to 96 Hz survives Refresh.
- Selecting De-Tuner updates both picker and rack; both remain consistent
  after Refresh and disconnect/reconnect.
- An injected gain readback failure retains the confirmed 0 dB value, shows
  `Simulated state readback failed`, disables writes, and leaves Connect
  available while Refresh and Disconnect are disabled.
- At 96 kHz, the selected Delay rack cannot be enabled and explains the
  sample-rate restriction.
- Mixer, Fat Channel, Effects, Presets, and Device navigation works. Portrait
  screenshots show legible selectors and sliders, separate rack ears, and
  accessible bottom navigation. HPF, gate, compressor, limiter, Standard EQ,
  Voice FX, and the parameter-driven reverb mesh were visually inspected.
- The emulator crash buffer was empty after the checks.

This pass used only simulation. It does not establish physical-phone USB,
call/Bluetooth stability, audible processing, or device-resident persistence.
Landscape and enlarged system-font layouts were not checked in this pass.

## Rack update

Ivory powder-coated panels and orange controls now cover Fat Channel and Voice
FX. Rack screws stay on separate ears. Selectors retain dark recessed surfaces
for legible text, and slider tracks use stronger contrast on ivory. Graph blur
was removed. The reverb mesh is a bounded parameter illustration, not a live
audio analyzer; it has no continuous animation timer. Native control semantics
and Standard-only EQ support remain unchanged.

## Emulator acceptance

For the 2026-11-02 rack update, screenshots at 720x1600 / 320 dpi on
Android 15 confirmed HPF, gate, compressor, and reverb panel layout. The
reverb mesh and separate rack ears were visible without label overlaps.
That inspection found low-contrast selector text and slider tracks, which
were corrected and passed the full clean verification run. The rebuilt APK
installed and launched in the simulator. The workstation process/thread
limit interrupted the final screenshot recheck; the emulator was stopped.
Final portrait visual confirmation of those contrast corrections passed in the
2026-11-03 continuation above.
No physical phone or io24 was accessed.


The preceding 2026-10-12 0.3.6 candidate was inspected at 720x1600 and 320 dpi on Android
15. The run verified:

- Mixer, Fat Channel, Effects, Presets, and Device navigation and layout;
- the shared Mixer/Fat Channel HPF power state and retained 406 Hz cutoff after
  an Off/On cycle;
- a Standard EQ graph drag applying frequency and gain as one recoverable batch
  while remaining connected;
- system-bar insets and unobstructed bottom navigation;
- small, fine-grained Effects scrolling and access to shared device reverb;
- parameter-driven Voice FX graphs and disabled unknown/high-rate Delay controls;
- phone-scene save/load across process restart;
- exact Input 1/2 and Block 1/2 save controls with confirmation and honest
  `WRITE_SENT_UNVERIFIED` labeling;
- serialized scene apply;
- injected readback failure releases control, preserves the last confirmed
  state, displays the exact error, and leaves only Connect enabled.

## Proof boundary

No physical phone installation, USB transfer, io24 command, firmware action,
or audible test occurred during this 0.3.6 build. Earlier physical trials prove
the direct Android transport and USB-audio coexistence, plus application of the
Input 2 mute write. The expanded mixer, Fat Channel, Voice FX, reverb, scene,
and high-rate paths are ready for, but not promoted beyond, the next physical
acceptance run.

## Large-text and landscape follow-up (2026-11-03)

Repaired portrait heading/status placement at 150% text size. At 200%, action
and navigation rows scroll horizontally rather than breaking button labels,
and input switches stack vertically so their labels remain intact.

Device settings now ignores delayed programmatic spinner callbacks. Opening
Device while disconnected no longer triggers a false connection error. In the
verified simulator, selecting Headphones: Mix A still updates state and survives
Refresh. Final screenshots were inspected at 150% and 200% portrait and normal
landscape. The horizontally scrolling navigation reaches Device successfully.

The final clean offline verification passed all 25 contracts, 74 Java tests,
lint, and APK assembly. The emulator used two cores and 2560 MB; build and
emulator work ran separately. Cold boot displayed a System UI ANR that settled
after Wait; this was not an app crash. Font scale and rotation settings were
restored and the emulator stopped after inspection. These checks do not verify
physical-phone behavior or live io24 USB/audio operation.
