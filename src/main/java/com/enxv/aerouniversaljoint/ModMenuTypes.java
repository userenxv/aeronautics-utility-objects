package com.enxv.aerouniversaljoint;

import com.enxv.aerouniversaljoint.content.UniversalJointMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, AeroUniversalJointMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<UniversalJointMenu>> UNIVERSAL_JOINT = MENU_TYPES.register(
            "universal_joint",
            () -> IMenuTypeExtension.create(UniversalJointMenu::new));

    private ModMenuTypes() {
    }

    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}
