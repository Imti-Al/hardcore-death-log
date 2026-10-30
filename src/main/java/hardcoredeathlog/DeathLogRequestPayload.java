package hardcoredeathlog;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DeathLogRequestPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DeathLogRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(HardcoreDeathLog.id("request_death_log"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeathLogRequestPayload> CODEC =
            StreamCodec.unit(new DeathLogRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
