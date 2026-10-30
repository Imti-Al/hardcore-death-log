package hardcoredeathlog.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import hardcoredeathlog.DeathLogPayload;

public class HardcoreDeathLogClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                DeathLogPayload.TYPE,
                (payload, context) ->
                        context.client().execute(() ->
                                context.client().setScreenAndShow(
                                        new DeathLogScreen(payload.json())
                                )
                        )
        );
    }
}