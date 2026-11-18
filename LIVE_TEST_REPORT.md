# Android physical test record

## User report — 2026-11-17

The user tested the installed app on their phone and reported that it works,
including audible Transformer reverb. They also reported:

- HPF cutoff remained effective with rack power off.
- Gate lacked an Expander switch, and limiter lacked Release.
- Compressor and EQ exposed only Standard, with the alternate models missing.
- Standard compressor distortion changed with Attack and improved with a
  longer Release. No captured audio or isolated reproduction is available.
- Shared block-202 reverb controls produced no audible change.
- Hardware edits needed manual refresh, input switches had excessive label
  spacing, and FAT Channel needed one selectable rack at a time.

The exact installed APK hash was not captured with this report. It is user
observation, not an instrumented test. Transformer has its own private reverb
core, which is separate from shared block 202.

The 0.3.7 changes address the confirmed control, routing, and UI defects and add
regression tests. It adds Tube/FET compression and Passive/Vintage EQ with
model-specific controls, live encoders, response graphs, and preset support.
This repair build has not been installed on the phone or
used with the physical io24. HPF bypass, block-202 audibility, automatic
readback, alternate-model switching (including alternate EQ back to Standard),
and the compressor issue still need physical acceptance. AutoGain
remains unavailable; no inactive firmware control was added in its place.

## Compressor breakup follow-up — 2026-11-17

The user clarified that the sound may be buffering/stuttering rather than
conventional distortion. No new live hardware operation or audio capture was
performed during this investigation.

Source tracing found:

- The app owns vendor control interface 5, not an audio stream. Its code does
  not use AudioTrack or AudioRecord. Phone audio-path buffering, if involved,
  is outside this controller's sample processing.
- Touch-slider movement previews locally; release commits one parameter edit.
  This behavior also existed in the previously published 0.3.6 source.
- A Standard compressor edit writes two complete CPXT records, indices 0 and
  1. Each waits for an optional SetP acknowledgement with a 1,600 ms response
  deadline. Absent replies can therefore add approximately 3.2 seconds of
  acknowledgement waiting, excluding write time. This is a possible control
  delay, not measured audio-buffer underrun evidence. The timeout also existed
  in 0.3.6. It has not been shortened without measuring reply behavior.
- The complete records use the app's retained compressor state. JaSt readback
  does not supply all compressor settings, so an edit after an externally
  loaded preset can also send the app's other compressor values. This is a
  possible first-edit discontinuity, not an established cause of continued
  breakup at a fixed setting.
- The Linux reference uses the same two-record sequence; its historical
  optional acknowledgement timeout is 2,000 ms, versus Android's 1,600 ms.
  Android is not uniquely introducing that wait.
- More specifically, Android's Standard record fixes hard knee, identity
  sidechain filter, and key listen off, whereas the Linux encoder supports
  soft knee, Auto mode, sidechain filtering, and key listen. A full-record
  Attack edit can therefore replace those non-default preset settings too.
  This is a concrete control-parity gap; its audible contribution is untested.
- Existing minimum/custom timing byte fixtures cover the 0.2 ms Attack and
  2.5 ms Release endpoints as well as non-default timings. No unit-conversion
  defect was identified in this trace.

The user subsequently clarified that breakup occurs approximately at the
lowest Attack settings and smooths out with longer Release. This makes rapid
gain modulation the leading explanation: gain can change appreciably within
individual waveform cycles and introduce harmonic or intermodulation
distortion. This is a supported working explanation, not a measured diagnosis.
It does not establish phone CPU overload, buffer underruns, bit-depth reduction,
or phase incoherence, and it need not be confined to a transient's first cycle.

No DSP timing range or USB timeout was changed. The existing minimum/custom
encoding fixtures already cover the relevant values; this follow-up does not
change the encoder. The separate preset-state/control-parity limitations above
remain open and should not be conflated with distortion at settled settings.

If further physical investigation is needed, the smallest useful comparison is
the same steady source and monitoring path at minimum versus longer Attack,
then minimum versus longer Release, with Threshold, Ratio, and Makeup held
fixed and levels matched for listening. Record audio after each control has
settled, paired with control-write/acknowledgement timestamps, sample rate, and
monitoring path. Persistence at a fixed setting would distinguish the symptom
from an edit-time discontinuity; audio capture is still needed to distinguish
harmonic distortion from dropouts. No additional hardware test was performed.

The earlier instrumented transport results follow.

## Setup

- Phone: BLU 1660V
- Device: PreSonus Revelator io24 (`194f:0422`)
- Signal: patch-cable signal into io24 Input 2

## Control and audio coexistence

The 0.1.0 app claimed vendor interface 5 without forced driver detachment and
reported protocol 1, 2048-byte command and response limits, a complete
2048-byte `GetP / Appl / JaSt` reply, and 503 state slots.

While the app retained interface 5, a separate Android recorder captured five
seconds from io24 Input 2. The patch-cable signal was loud and clear, and
playback returned through the io24 output. Returning to the controller still
showed it connected. Disconnect then released the control interface.

This proves that direct native vendor control can coexist with Android USB
audio input and output on this phone.

## Manual launch and power observation

The 0.2.1 build removed automatic USB-attach launch. After a phone system
update and a transient USB power incident, the user manually opened the app
and completed a fresh connection. The io24 then remained powered and reported
Input 2 mute Off. The timing does not establish that the APK caused or fixed
the earlier phone-side power cycling.

## Input 2 mute and short reply

The user tapped **Mute input 2** in 0.2.1. The app reported that the following
state reply was too short and released control. A fresh connection then read
Input 2 mute On. This proves that `Pari` parameter 7, channel index 1 applied,
and that the old verifier had consumed the firmware's short `SetP` reply as if
it were the next `JaSt` response.

The user did not hear attenuation on the observed path. That run did not
isolate whether monitoring was before the mute point or routed independently,
so audible mute is not claimed.

The matching Unmute attempt produced the same old verifier error. Its final
physical state was not read back in that run.

## 0.3.5 boundary

Version 0.3.5 drains and validates each optional short write reply before any
fresh state request. It also adds the full typed mixer, Fat Channel, Voice FX,
native reverb, scene, settings, and exact front-panel block architecture. The
native block body is byte-matched to the Linux encoder, but neither that send
nor its recall has been attempted from this APK. Exact byte vectors, state
handling, failure recovery, and emulator workflows pass offline, but those
expanded writes still require a new physical acceptance run before they are
described as device-confirmed, audible, or power-cycle persistent.
