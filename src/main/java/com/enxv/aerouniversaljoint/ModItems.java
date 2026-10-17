package com.enxv.aerouniversaljoint;

import com.enxv.aerouniversaljoint.content.UniversalJointBlockItem;
import com.enxv.aerouniversaljoint.content.UniversalJointRodItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AeroUniversalJointMod.MOD_ID);

    public static final DeferredItem<UniversalJointBlockItem> BRASS_UNIVERSAL_JOINT = ITEMS.register(
            "brass_universal_joint",
            () -> new UniversalJointBlockItem(ModBlocks.BRASS_UNIVERSAL_JOINT.get(), new Item.Properties()));

    public static final DeferredItem<UniversalJointRodItem> UNIVERSAL_JOINT_ROD = ITEMS.register(
            "universal_joint_rod",
            () -> new UniversalJointRodItem(new Item.Properties().stacksTo(16), true));

    public static final DeferredItem<UniversalJointRodItem> ANDESITE_UNIVERSAL_JOINT_ROD = ITEMS.register(
            "universal_joint_rod2",
            () -> new UniversalJointRodItem(new Item.Properties().stacksTo(16), false));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
