package hardcoredeathlog;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DeathLogPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DeathLogPayload> TYPE =
            new CustomPacketPayload.Type<>(HardcoreDeathLog.id("death_log"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeathLogPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(1_000_000),
                    DeathLogPayload::json,
                    DeathLogPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
