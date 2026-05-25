package net.ironhalo.originssecundus.network;

import io.netty.buffer.ByteBuf;
import net.ironhalo.originssecundus.OriginsSecundus;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OriginSyncPayload(String originId, String customizationJson) implements CustomPacketPayload {
    public static final Type<OriginSyncPayload> TYPE = new Type<>(OriginsSecundus.id("sync_origin"));
    public static final StreamCodec<ByteBuf, OriginSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OriginSyncPayload::originId,
            ByteBufCodecs.STRING_UTF8,
            OriginSyncPayload::customizationJson,
            OriginSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
