package hardcoredeathlog;

import java.util.UUID;

public class DeathRecord {
    private String uuid;
    private String name;
    private String timestamp;
    private String message;

    public DeathRecord() {
        // Required by Gson.
    }

    public DeathRecord(UUID uuid, String name, String timestamp, String message) {
        this.uuid = uuid.toString();
        this.name = name;
        this.timestamp = timestamp;
        this.message = message;
    }

    public UUID uuid() {
        return UUID.fromString(uuid);
    }

    public String name() {
        return name;
    }

    public String timestamp() {
        return timestamp;
    }

    public String message() {
        return message;
    }
}
