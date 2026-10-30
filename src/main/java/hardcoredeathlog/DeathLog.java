package hardcoredeathlog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class DeathLog {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final String FILE_NAME = "hardcore-deaths.json";
    private static final String WORLD_STATE_FILE_NAME = "hardcore-death-log-state.json";

    private static Data data = new Data();
    private static Path file;
    private static Path worldStateFile;
    private static boolean firstDeathRecorded;

    private DeathLog() {
    }

    public static void load(MinecraftServer server) {
        file = server.getServerDirectory()
                .resolve("config")
                .resolve(FILE_NAME);

        // This state belongs to the current world, not the persistent death history.
        // A new world has no state file, so its first death can be counted.
        worldStateFile = server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(WORLD_STATE_FILE_NAME);

        firstDeathRecorded = loadWorldState();

        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            HardcoreDeathLog.LOGGER.error("Could not create the config directory for the death log.", e);
            return;
        }

        if (!Files.exists(file)) {
            data = new Data();
            save();
            HardcoreDeathLog.LOGGER.info("Created new death log at {}", file.toAbsolutePath());
            return;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            data = loaded != null ? loaded : new Data();

            if (data.deaths == null) {
                data.deaths = new ArrayList<>();
            }

            HardcoreDeathLog.LOGGER.info("Loaded {} death records from {}", data.deaths.size(), file.toAbsolutePath());
        } catch (IOException | JsonIOException | JsonSyntaxException e) {
            HardcoreDeathLog.LOGGER.error("Could not load the death log. Starting with an empty log; the existing file was not overwritten.", e);
            data = new Data();
        }
    }

    public static void recordDeath(ServerPlayer player, DamageSource source) {
        if (file == null || worldStateFile == null) {
            HardcoreDeathLog.LOGGER.warn("Tried to record a death before the death log was loaded.");
            return;
        }

        // Hardcore rule: only the first player death in a world counts.
        // Later deaths in the same world are deliberately ignored.
        if (firstDeathRecorded) {
            HardcoreDeathLog.LOGGER.info("Ignoring death for {} because this world's first death has already been recorded.", player.getName().getString());
            return;
        }

        String message = source.getLocalizedDeathMessage(player).getString();
        DeathRecord record = new DeathRecord(
                player.getUUID(),
                player.getName().getString(),
                Instant.now().toString(),
                message
        );

        data.deaths.add(record);
        firstDeathRecorded = true;
        saveWorldState();
        save();

        int deathNumber = getDeathCount(player.getUUID());
        HardcoreDeathLog.LOGGER.info("{} died (death #{}): {}", record.name(), deathNumber, message);
    }

    public static int getDeathCount(UUID uuid) {
        int count = 0;

        for (DeathRecord record : data.deaths) {
            if (record.uuid().equals(uuid)) {
                count++;
            }
        }

        return count;
    }

    public static int getTotalDeaths() {
        return data.deaths.size();
    }

    public static List<DeathRecord> getDeaths() {
        return Collections.unmodifiableList(data.deaths);
    }

    public static String toJson() {
        return GSON.toJson(data);
    }

    private static boolean loadWorldState() {
        if (worldStateFile == null || !Files.exists(worldStateFile)) {
            return false;
        }

        try (Reader reader = Files.newBufferedReader(worldStateFile)) {
            WorldState state = GSON.fromJson(reader, WorldState.class);
            return state != null && state.firstDeathRecorded;
        } catch (IOException | JsonIOException | JsonSyntaxException e) {
            HardcoreDeathLog.LOGGER.error("Could not load the current world's death state. Treating it as a new run.", e);
            return false;
        }
    }

    private static void saveWorldState() {
        if (worldStateFile == null) {
            return;
        }

        try {
            Files.createDirectories(worldStateFile.getParent());
            try (Writer writer = Files.newBufferedWriter(
                    worldStateFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                GSON.toJson(new WorldState(firstDeathRecorded), writer);
            }
        } catch (IOException | JsonIOException e) {
            HardcoreDeathLog.LOGGER.error("Could not save the current world's death state.", e);
        }
    }

    public static void save() {
        if (file == null) {
            return;
        }

        Path tempFile = file.resolveSibling(FILE_NAME + ".tmp");

        try (Writer writer = Files.newBufferedWriter(
                tempFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            GSON.toJson(data, writer);
        } catch (IOException | JsonIOException e) {
            HardcoreDeathLog.LOGGER.error("Could not save the death log.", e);
            return;
        }

        try {
            Files.move(
                    tempFile,
                    file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException atomicMoveFailure) {
            try {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                HardcoreDeathLog.LOGGER.error("Could not replace the death log file.", e);
            }
        }
    }

    private static class Data {
        private List<DeathRecord> deaths = new ArrayList<>();
    }

    private static class WorldState {
        private boolean firstDeathRecorded;

        private WorldState(boolean firstDeathRecorded) {
            this.firstDeathRecorded = firstDeathRecorded;
        }
    }
}
