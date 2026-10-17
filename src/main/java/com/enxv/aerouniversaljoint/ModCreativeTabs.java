package com.enxv.aerouniversaljoint;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AeroUniversalJointMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.aeronautics_utility_objects"))
                    .icon(() -> ModItems.UNIVERSAL_JOINT_ROD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BRASS_UNIVERSAL_JOINT.get());
                        output.accept(ModItems.UNIVERSAL_JOINT_ROD.get());
                        output.accept(ModItems.ANDESITE_UNIVERSAL_JOINT_ROD.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}

