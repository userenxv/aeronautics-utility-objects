package com.enxv.aerouniversaljoint.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AeroUniversalJointNetwork {
    private AeroUniversalJointNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(AeroUniversalJointNetwork::registerPayloads);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToServer(SetJointSpeedRatioPayload.TYPE, SetJointSpeedRatioPayload.STREAM_CODEC, SetJointSpeedRatioPayload::handleServer);
    }
}
