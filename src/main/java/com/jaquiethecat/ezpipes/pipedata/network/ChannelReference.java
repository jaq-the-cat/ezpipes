package com.jaquiethecat.ezpipes.pipedata.network;

import java.util.UUID;

public class ChannelReference {
    public final UUID id;
    public boolean isInput;

    public ChannelReference(UUID id) {
        this.id = id;
    }

    public ChannelReference(UUID id, boolean isInput) {
        this(id);
        this.isInput = isInput;
    }
}
