package net.ironhalo.originssecundus.network;

import io.netty.buffer.ByteBuf;
import net.ironhalo.originssecundus.OriginsSecundus;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ActivePowerPayload(String key) implements CustomPacketPayload {
    public static final Type<ActivePowerPayload> TYPE = new Type<>(OriginsSecundus.id("active_power"));
    public static final StreamCodec<ByteBuf, ActivePowerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ActivePowerPayload::key,
            ActivePowerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
