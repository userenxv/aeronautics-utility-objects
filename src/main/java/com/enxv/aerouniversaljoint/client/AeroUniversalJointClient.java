package com.enxv.aerouniversaljoint.client;

import com.enxv.aerouniversaljoint.ModBlockEntities;
import com.enxv.aerouniversaljoint.ModMenuTypes;
import com.enxv.aerouniversaljoint.content.ponder.AeroUniversalJointPonderPlugin;
import com.enxv.aerouniversaljoint.content.UniversalJointRenderer;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class AeroUniversalJointClient {
    private AeroUniversalJointClient() {
    }

    public static void init(IEventBus modBus) {
        PonderIndex.addPlugin(new AeroUniversalJointPonderPlugin());
        AeroUniversalJointPartials.init();
        modBus.addListener(AeroUniversalJointClient::registerRenderers);
        modBus.addListener(AeroUniversalJointClient::registerScreens);
        NeoForge.EVENT_BUS.addListener(ConnectionPreviewRenderer::render);
        ToolgunCompatibilityNotice.init();
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.UNIVERSAL_JOINT.get(), UniversalJointRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.UNIVERSAL_JOINT.get(), UniversalJointScreen::new);
    }

}
