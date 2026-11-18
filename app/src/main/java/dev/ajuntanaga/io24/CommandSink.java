package dev.ajuntanaga.io24;

import java.util.List;

/** UI callback that accepts only validated controller commands. */
interface CommandSink {
    default void setAutoGain(int channel, boolean on) { }

    default boolean autoGainEnabled(int channel) { return false; }

    void dispatch(Io24Command command);

    void dispatchAll(List<Io24Command> commands, String label);
}
