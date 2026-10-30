package hardcoredeathlog;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardcoreDeathLog implements ModInitializer {
    public static final String MOD_ID = "hardcore-death-log";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        DeathLogNetworking.initialize();

        ServerLifecycleEvents.SERVER_STARTED.register(DeathLog::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> DeathLog.save());

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                DeathLog.recordDeath(player, damageSource);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));

        LOGGER.info("Hardcore Death Log initialized.");
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deaths")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    DeathLogNetworking.sendTo(player);
                    return 1;
                })
                .then(Commands.literal("stats")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            DeathLogNetworking.sendTo(player);
                            return 1;
                        }))
                .then(Commands.literal("list")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            DeathLogNetworking.sendTo(player);
                            return 1;
                        })));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
