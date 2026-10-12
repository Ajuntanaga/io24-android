package dev.ajuntanaga.io24;

import java.util.List;

/** UI callback that accepts only validated controller commands. */
interface CommandSink {
    void dispatch(Io24Command command);

    void dispatchAll(List<Io24Command> commands, String label);
}
