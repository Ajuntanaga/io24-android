package dev.ajuntanaga.io24;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

/** Serial lifecycle and command owner between Android UI and one io24 session. */
public final class Io24Controller implements AutoCloseable {
    public interface Listener {
        void onState(Io24State state);
    }

    public interface SessionFactory {
        Io24Control open() throws Io24UsbProbe.ProbeException;
    }

    private final Executor worker;
    private final Executor callbacks;
    private volatile Io24State state = Io24State.defaults();
    private volatile Listener listener;
    private Io24Control control;
    private long sessionEpoch;
    private long pendingPollEpoch = -1;
    private long commandRevision;
    private final boolean[] autoGainOn = new boolean[2];
    private final AutoGain[] autoGain = {new AutoGain(), new AutoGain()};

    public synchronized boolean autoGainEnabled(int channel) {
        return autoGainOn[channel - 1];
    }

    public synchronized void setAutoGain(int channel, boolean on) {
        if (channel < 1 || channel > 2) throw new IllegalArgumentException("Input must be 1 or 2");
        autoGainOn[channel - 1] = on;
        if (state.linked()) autoGainOn[2 - channel] = on;
        for (AutoGain gain : autoGain) gain.reset();
        publish(state);
    }

    public synchronized void stopAutoGain() {
        autoGainOn[0] = autoGainOn[1] = false;
        for (AutoGain gain : autoGain) gain.reset();
    }

    private void applyAutoGain(Io24State fresh) {
        double now = System.nanoTime() / 1e9;
        boolean linked = fresh.linked();
        for (int i = 0; i < 2; i++) {
            if (!autoGainOn[i]) continue;
            float level = fresh.input(i + 1).level();
            if (linked) level = Math.max(fresh.input(1).level(), fresh.input(2).level());
            Float gain = autoGain[i].step(now, level, fresh.input(i + 1).gainDb());
            if (gain != null) {
                List<Io24Command> commands = new ArrayList<>();
                commands.add(Io24Command.setGain(i + 1, gain));
                if (linked) commands.add(Io24Command.setGain(2 - i, gain));
                dispatchAll(commands, "AutoGain");
            }
            if (linked) break;
        }
    }


    public Io24Controller(Executor worker, Executor callbacks) {
        if (worker == null || callbacks == null) {
            throw new IllegalArgumentException("Worker and callback executors are required");
        }
        this.worker = worker;
        this.callbacks = callbacks;
    }

    public void setListener(Listener value) {
        listener = value;
    }

    public Io24State state() {
        return state;
    }

    public void connect(SessionFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("Session factory is required");
        }
        final long requestedEpoch;
        final Io24Control previous;
        synchronized (this) {
            requestedEpoch = ++sessionEpoch;
            previous = control;
            control = null;
        }
        Io24State pending = state.buildUpon()
                .setConnection(
                        Io24State.ConnectionStatus.CONNECTING,
                        "Connecting to io24…",
                        state.simulated())
                .setBusy(true)
                .build();
        publish(pending);
        worker.execute(() -> {
            // Release the previous USB claim before opening its replacement,
            // even if this connection request has already been superseded.
            if (previous != null) {
                previous.close();
            }
            if (!isCurrentEpoch(requestedEpoch)) {
                return;
            }
            try {
                Io24Control opened = factory.open();
                synchronized (this) {
                    if (sessionEpoch != requestedEpoch) {
                        opened.close();
                        return;
                    }
                    control = opened;
                }
                publish(opened.currentState().buildUpon()
                        .setBusy(false)
                        .build());
            } catch (Io24UsbProbe.ProbeException error) {
                if (isCurrentEpoch(requestedEpoch)) {
                    publishError(state, error);
                }
            }
        });
    }

    public void dispatch(Io24Command command) {
        if (command != null && (command.kind() == Io24Command.Kind.SET_GAIN
                || command.kind() == Io24Command.Kind.SET_LINK)) stopAutoGain();
        if (command == null) {
            throw new IllegalArgumentException("Command is required");
        }
        Io24Control active;
        long operationEpoch;
        synchronized (this) {
            active = control;
            operationEpoch = sessionEpoch;
            commandRevision++;
            for (AutoGain gain : autoGain) gain.reset();
        }
        if (active == null || !active.isConnected()) {
            publish(state.buildUpon()
                    .setConnection(
                            Io24State.ConnectionStatus.ERROR,
                            "Connect the io24 before changing a control",
                            state.simulated())
                    .setBusy(false)
                    .build());
            return;
        }
        Io24State confirmedBefore = state;
        publish(state.buildUpon()
                .setConnection(
                        Io24State.ConnectionStatus.CONNECTED,
                        "Applying " + friendly(command.kind()) + "…",
                        state.simulated())
                .setBusy(true)
                .build());
        worker.execute(() -> {
            if (!isCurrent(active, operationEpoch)) {
                return;
            }
            try {
                CommandResult result = active.execute(command);
                if (!isCurrent(active, operationEpoch)) {
                    return;
                }
                publish(result.state().buildUpon()
                        .setBusy(false)
                        .build());
            } catch (Io24UsbProbe.ProbeException error) {
                if (clearIfCurrent(active, operationEpoch)) {
                    active.close();
                    publishError(confirmedBefore, error);
                }
            }
        });
    }

    /** Applies an ordered scene as one serialized operation and one busy state. */
    public void dispatchAll(List<Io24Command> commands, String label) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        List<Io24Command> ordered = new ArrayList<>(commands);
        for (Io24Command command : ordered) {
            if (command == null) {
                throw new IllegalArgumentException("Scene command is required");
            }
        }
        Io24Control active;
        long operationEpoch;
        synchronized (this) {
            active = control;
            operationEpoch = sessionEpoch;
            commandRevision++;
            for (AutoGain gain : autoGain) gain.reset();
        }
        if (active == null || !active.isConnected()) {
            publish(state.buildUpon()
                    .setConnection(
                            Io24State.ConnectionStatus.ERROR,
                            "Connect the io24 before loading a scene",
                            state.simulated())
                    .setBusy(false)
                    .build());
            return;
        }
        Io24State confirmedBefore = state;
        publish(state.buildUpon()
                .setConnection(
                        Io24State.ConnectionStatus.CONNECTED,
                        "Applying " + (label == null ? "scene" : label) + "…",
                        state.simulated())
                .setBusy(true)
                .build());
        worker.execute(() -> {
            if (!isCurrent(active, operationEpoch)) {
                return;
            }
            int completed = 0;
            Io24State lastConfirmed = confirmedBefore;
            try {
                CommandResult result = null;
                for (Io24Command command : ordered) {
                    if (!isCurrent(active, operationEpoch)) {
                        return;
                    }
                    result = active.execute(command);
                    lastConfirmed = result.state();
                    completed++;
                }
                if (!isCurrent(active, operationEpoch)) {
                    return;
                }
                Io24State finalState = result == null
                        ? active.currentState()
                        : result.state();
                publish(finalState.buildUpon().setBusy(false).build());
            } catch (Io24UsbProbe.ProbeException error) {
                if (clearIfCurrent(active, operationEpoch)) {
                    active.close();
                    String scene = label == null ? "Scene" : label;
                    publishError(lastConfirmed, new Io24UsbProbe.ProbeException(
                            scene + " stopped after " + completed + " of "
                                    + ordered.size()
                                    + " confirmed controls; the device may be "
                                    + "partially changed. Reconnect "
                                    + "before continuing. " + error.getMessage(),
                            error));
                }
            }
        });
    }

    /** Coalesced readback: never blocks the UI or queues behind pending writes. */
    public void poll() {
        final Io24Control active;
        final long epoch;
        final long revision;
        synchronized (this) {
            active = control;
            epoch = sessionEpoch;
            revision = commandRevision;
            if (active == null || !active.isConnected() || state.busy()
                    || pendingPollEpoch == epoch) {
                return;
            }
            pendingPollEpoch = epoch;
        }
        worker.execute(() -> {
            try {
                synchronized (this) {
                    if (!isCurrent(active, epoch) || state.busy()
                            || commandRevision != revision) {
                        return;
                    }
                }
                Io24State refreshed = active.refresh();
                synchronized (this) {
                    // A gesture may have queued a write while the read was in flight.
                    if (isCurrent(active, epoch) && !state.busy()
                            && commandRevision == revision) {
                        publish(refreshed.buildUpon().setBusy(false).build());
                        applyAutoGain(refreshed);
                    }
                }
            } catch (Io24UsbProbe.ProbeException error) {
                if (clearIfCurrent(active, epoch)) {
                    active.close();
                    publishError(state, error);
                }
            } finally {
                synchronized (this) {
                    if (pendingPollEpoch == epoch) {
                        pendingPollEpoch = -1;
                    }
                }
            }
        });
    }

    public void refresh() {
        Io24Control active;
        long operationEpoch;
        synchronized (this) {
            active = control;
            operationEpoch = sessionEpoch;
            commandRevision++;
            for (AutoGain gain : autoGain) gain.reset();
        }
        if (active == null || !active.isConnected()) {
            return;
        }
        Io24State confirmedBefore = state;
        publish(state.buildUpon()
                .setConnection(
                        Io24State.ConnectionStatus.CONNECTED,
                        "Refreshing io24 state…",
                        state.simulated())
                .setBusy(true)
                .build());
        worker.execute(() -> {
            if (!isCurrent(active, operationEpoch)) {
                return;
            }
            try {
                Io24State refreshed = active.refresh();
                if (isCurrent(active, operationEpoch)) {
                    publish(refreshed.buildUpon().setBusy(false).build());
                }
            } catch (Io24UsbProbe.ProbeException error) {
                if (clearIfCurrent(active, operationEpoch)) {
                    active.close();
                    publishError(confirmedBefore, error);
                }
            }
        });
    }

    public void disconnect() {
        Io24Control active;
        synchronized (this) {
            sessionEpoch++;
            active = control;
            control = null;
        }
        publish(state.buildUpon()
                .setConnection(
                        Io24State.ConnectionStatus.DISCONNECTED,
                        "Control released",
                        state.simulated())
                .setBusy(false)
                .build());
        if (active != null) {
            worker.execute(active::close);
        }
    }

    @Override
    public void close() {
        disconnect();
        listener = null;
    }

    private void publishError(
            Io24State lastConfirmed,
            Exception error) {
        publish(lastConfirmed.buildUpon()
                .setConnection(
                        Io24State.ConnectionStatus.ERROR,
                        error.getMessage(),
                        lastConfirmed.simulated())
                .setBusy(false)
                .build());
    }

    private synchronized void publish(Io24State next) {
        if (next.connectionStatus() != Io24State.ConnectionStatus.CONNECTED) stopAutoGain();
        state = next;
        Listener current = listener;
        if (current != null) {
            callbacks.execute(() -> {
                if (listener == current && state == next) {
                    current.onState(next);
                }
            });
        }
    }

    private synchronized boolean isCurrentEpoch(long epoch) {
        return sessionEpoch == epoch;
    }

    private synchronized boolean isCurrent(
            Io24Control expected,
            long epoch) {
        return control == expected && sessionEpoch == epoch;
    }

    private synchronized boolean clearIfCurrent(
            Io24Control expected,
            long epoch) {
        if (control != expected || sessionEpoch != epoch) {
            return false;
        }
        control = null;
        sessionEpoch++;
        return true;
    }

    private static String friendly(Io24Command.Kind kind) {
        return kind.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
