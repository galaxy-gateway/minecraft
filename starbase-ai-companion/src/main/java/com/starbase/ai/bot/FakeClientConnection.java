package com.starbase.ai.bot;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;

public class FakeClientConnection extends ClientConnection {
    public FakeClientConnection() {
        super(NetworkSide.SERVERBOUND);
        this.channel = new EmbeddedChannel();
    }

    @Override
    public void handleDisconnection() {
        // No-op
    }
}
