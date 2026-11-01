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

/** Server-authoritative instant rod removal from the client-side visual hit target. */
public record BreakHydraulicRodPayload(BlockPos pos, @Nullable UUID subLevelId) implements CustomPacketPayload {
    public static final Type<BreakHydraulicRodPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeroUniversalJointMod.MOD_ID, "break_hydraulic_rod"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BreakHydraulicRodPayload> STREAM_CODEC = StreamCodec.of(
            BreakHydraulicRodPayload::encode, BreakHydraulicRodPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, BreakHydraulicRodPayload payload) {
        buffer.writeBlockPos(payload.pos());
        buffer.writeBoolean(payload.subLevelId() != null);
        if (payload.subLevelId() != null) buffer.writeUUID(payload.subLevelId());
    }

    private static BreakHydraulicRodPayload decode(RegistryFriendlyByteBuf buffer) {
        return new BreakHydraulicRodPayload(buffer.readBlockPos(),
                buffer.readBoolean() ? buffer.readUUID() : null);
    }

    @Override
    public Type<BreakHydraulicRodPayload> type() {
        return TYPE;
    }

    public static void handleServer(BreakHydraulicRodPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            BlockEntity blockEntity = SubLevelReferenceHelper.resolveBlockEntityFast(player.level(), payload.pos(), payload.subLevelId());
            if (!(blockEntity instanceof HydraulicConnectionHeadBlockEntity head)
                    || !head.hasLink() || !HydraulicRodInteraction.isTarget(player, head)) {
                return;
            }
            head.detachLink();
        });
    }
}
