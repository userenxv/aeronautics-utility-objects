package com.enxv.aerouniversaljoint.network;

import com.enxv.aerouniversaljoint.AeroUniversalJointMod;
import com.enxv.aerouniversaljoint.content.HydraulicConnectionHeadBlockEntity;
import com.enxv.aerouniversaljoint.content.HydraulicRodInteraction;
import com.enxv.aerouniversaljoint.util.SubLevelReferenceHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public record OpenHydraulicRodSettingsPayload(BlockPos pos, @Nullable UUID subLevelId)
        implements CustomPacketPayload {
    public static final Type<OpenHydraulicRodSettingsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeroUniversalJointMod.MOD_ID, "open_hydraulic_rod_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenHydraulicRodSettingsPayload> STREAM_CODEC = StreamCodec.of(
            OpenHydraulicRodSettingsPayload::encode, OpenHydraulicRodSettingsPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, OpenHydraulicRodSettingsPayload payload) {
        buffer.writeBlockPos(payload.pos());
        buffer.writeBoolean(payload.subLevelId() != null);
        if (payload.subLevelId() != null) buffer.writeUUID(payload.subLevelId());
    }

    private static OpenHydraulicRodSettingsPayload decode(RegistryFriendlyByteBuf buffer) {
        return new OpenHydraulicRodSettingsPayload(buffer.readBlockPos(),
                buffer.readBoolean() ? buffer.readUUID() : null);
    }

    @Override
    public Type<OpenHydraulicRodSettingsPayload> type() { return TYPE; }

    public static void handleServer(OpenHydraulicRodSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            BlockEntity blockEntity = SubLevelReferenceHelper.resolveBlockEntityFast(player.level(), payload.pos(), payload.subLevelId());
            if (!(blockEntity instanceof HydraulicConnectionHeadBlockEntity head)
                    || !head.hasLink()) {
                return;
            }
            if (!HydraulicRodInteraction.isTarget(player, head)) {
                return;
            }
            com.enxv.aerouniversaljoint.content.HydraulicConnectionHeadMenu.open(player, head, true);
        });
    }
}
