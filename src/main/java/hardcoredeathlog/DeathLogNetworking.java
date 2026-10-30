package hardcoredeathlog;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class DeathLogNetworking {
    private DeathLogNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(DeathLogPayload.TYPE, DeathLogPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DeathLogRequestPayload.TYPE, DeathLogRequestPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(DeathLogRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ServerPlayNetworking.send(player, new DeathLogPayload(DeathLog.toJson()));
        });
    }

    public static void sendTo(ServerPlayer player) {
        ServerPlayNetworking.send(player, new DeathLogPayload(DeathLog.toJson()));
    }
}
