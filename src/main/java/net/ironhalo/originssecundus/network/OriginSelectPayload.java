package net.ironhalo.originssecundus.network;

import io.netty.buffer.ByteBuf;
import net.ironhalo.originssecundus.OriginsSecundus;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OriginSelectPayload(String originId, String customizationJson) implements CustomPacketPayload {
    public static final Type<OriginSelectPayload> TYPE = new Type<>(OriginsSecundus.id("select_origin"));
    public static final StreamCodec<ByteBuf, OriginSelectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OriginSelectPayload::originId,
            ByteBufCodecs.STRING_UTF8,
            OriginSelectPayload::customizationJson,
            OriginSelectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
