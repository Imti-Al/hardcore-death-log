package hardcoredeathlog.client;

import com.google.gson.Gson;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import hardcoredeathlog.DeathRecord;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class DeathLogScreen extends Screen {
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("dd MMM yyyy • HH:mm", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());

    private static final int PANEL_MAX_WIDTH = 720;
    private static final int PANEL_MAX_HEIGHT = 520;
    private static final int PANEL_MARGIN = 20;
    private static final int HISTORY_ROW_HEIGHT = 70;

    private final List<DeathRecord> deaths;
    private final List<PlayerSummary> players;
    private UUID selectedPlayer;
    private double historyScroll;

    public DeathLogScreen(String json) {
        super(Component.literal("Hardcore Death Log"));
        Snapshot snapshot = GSON.fromJson(json, Snapshot.class);
        this.deaths = snapshot != null && snapshot.deaths != null
                ? new ArrayList<>(snapshot.deaths.reversed())
                : new ArrayList<>();
        this.players = buildPlayerSummaries(this.deaths);
    }

    private static List<PlayerSummary> buildPlayerSummaries(List<DeathRecord> records) {
        Map<UUID, PlayerSummary> byPlayer = new LinkedHashMap<>();

        for (DeathRecord record : records) {
            UUID uuid = record.uuid();
            PlayerSummary summary = byPlayer.get(uuid);

            if (summary == null) {
                summary = new PlayerSummary(uuid, record.name(), 0);
                byPlayer.put(uuid, summary);
            }

            summary.name = record.name();
            summary.count++;
        }

        List<PlayerSummary> result = new ArrayList<>(byPlayer.values());
        result.sort(Comparator.comparingInt((PlayerSummary p) -> p.count).reversed()
                .thenComparing(p -> p.name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    @Override
    protected void init() {
        historyScroll = 0;
    }

    private int panelWidth() {
        return Math.min(PANEL_MAX_WIDTH, Math.max(0, width - PANEL_MARGIN * 2));
    }

    private int panelHeight() {
        return Math.min(PANEL_MAX_HEIGHT, Math.max(240, height - PANEL_MARGIN));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(10, (height - panelHeight()) / 2);
    }

    private int panelRight() {
        return panelLeft() + panelWidth();
    }

    private int visiblePlayerCount(int panelHeight) {
        int maxRowsFromHeight = Math.max(1, (panelHeight - 180) / 17);
        return Math.min(players.size(), Math.min(7, maxRowsFromHeight));
    }

    private int historyTitleY(int top, int panelHeight) {
        int summaryTop = top + 58;
        int playerY = summaryTop + 22;
        int visiblePlayers = visiblePlayerCount(panelHeight);

        playerY += visiblePlayers * 17;
        if (players.size() > visiblePlayers) {
            playerY += 17;
        }

        return Math.max(playerY + 8, top + 190);
    }

    private int historyTop(int top, int panelHeight) {
        return historyTitleY(top, panelHeight) + 24;
    }

    private int historyBottom(int top, int panelHeight) {
        return top + panelHeight - 15;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = panelLeft();
        int top = panelTop();
        int right = panelRight();

        graphics.fill(left, top, right, top + panelHeight, 0xE6101010);
        graphics.outline(left, top, right, top + panelHeight, 0xFF555555);

        graphics.centeredText(font, title, width / 2, top + 14, 0xFFFFFFFF);
        graphics.text(font, "Total deaths: " + deaths.size(), left + 18, top + 34, 0xFFCCCCCC);

        int summaryTop = top + 58;
        graphics.text(font, "Players", left + 18, summaryTop, 0xFFFFFFFF);
        graphics.horizontalLine(left + 18, right - 18, summaryTop + 12, 0xFF555555);

        int playerY = summaryTop + 22;
        int visiblePlayers = visiblePlayerCount(panelHeight);

        for (int i = 0; i < visiblePlayers; i++) {
            PlayerSummary player = players.get(i);

            boolean selected = player.uuid.equals(selectedPlayer);
            boolean hovered = mouseX >= left + 14 && mouseX <= right - 14
                    && mouseY >= playerY - 3 && mouseY < playerY + 15;

            if (selected || hovered) {
                graphics.fill(left + 14, playerY - 4, right - 14, playerY + 13,
                        selected ? 0xFF303A4A : 0xFF252525);
            }

            graphics.text(font, player.name, left + 22, playerY, 0xFFFFFFFF);
            graphics.text(font, "☠ " + player.count, right - 75, playerY, 0xFFE0E0E0);

            playerY += 17;
        }

        if (players.size() > visiblePlayers) {
            graphics.text(font, "and " + (players.size() - visiblePlayers) + " more...",
                    left + 22, playerY, 0xFF888888);
        }

        int historyTitleY = historyTitleY(top, panelHeight);
        graphics.text(font,
                selectedPlayer == null ? "Death History" : selectedPlayerName() + "'s Deaths",
                left + 18, historyTitleY, 0xFFFFFFFF);

        if (selectedPlayer != null) {
            graphics.text(font, "[Click player again to show all]",
                    right - 195, historyTitleY, 0xFFAAAAAA);
        }

        graphics.horizontalLine(left + 18, right - 18, historyTitleY + 12, 0xFF555555);

        int historyTop = historyTop(top, panelHeight);
        int historyBottom = historyBottom(top, panelHeight);

        List<DeathRecord> visibleDeaths = getVisibleDeaths();
        int visibleRows = Math.max(1, (historyBottom - historyTop) / HISTORY_ROW_HEIGHT);
        int maxScroll = Math.max(0, visibleDeaths.size() - visibleRows);

        historyScroll = Math.max(0, Math.min(historyScroll, maxScroll));
        int start = (int) historyScroll;

        graphics.enableScissor(left + 14, historyTop - 2, right - 14, historyBottom);

        int y = historyTop;
        for (int i = start; i < visibleDeaths.size(); i++) {
            DeathRecord record = visibleDeaths.get(i);
            int deathNumber = deaths.size() - deaths.indexOf(record);

            graphics.text(font, "#" + deathNumber + "  " + record.name(),
                    left + 22, y, 0xFFFFFFFF);
            graphics.text(font, TIME_FORMAT.format(Instant.parse(record.timestamp())),
                    left + 22, y + 15, 0xFF999999);
            graphics.textWithWordWrap(font, Component.literal(record.message()),
                    left + 22, y + 29, right - left - 44, 0xFFCCCCCC);

            y += HISTORY_ROW_HEIGHT;
        }

        if (visibleDeaths.isEmpty()) {
            graphics.text(font, "No recorded deaths.",
                    left + 22, historyTop + 4, 0xFF888888);
        }

        graphics.disableScissor();

        // Draw a scrollbar only when the history actually overflows.
        if (maxScroll > 0) {
            int scrollbarX = right - 12;
            int trackTop = historyTop;
            int trackBottom = historyBottom;
            int trackHeight = trackBottom - trackTop;

            graphics.fill(scrollbarX, trackTop, scrollbarX + 4, trackBottom, 0xFF303030);

            float thumbRatio = (float) visibleRows / visibleDeaths.size();
            int thumbHeight = Math.max(18, (int) (trackHeight * thumbRatio));
            int travel = trackHeight - thumbHeight;
            int thumbY = trackTop + (travel == 0
                    ? 0
                    : (int) ((historyScroll / maxScroll) * travel));

            graphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    private List<DeathRecord> getVisibleDeaths() {
        if (selectedPlayer == null) {
            return deaths;
        }

        return deaths.stream()
                .filter(record -> record.uuid().equals(selectedPlayer))
                .toList();
    }

    private String selectedPlayerName() {
        return players.stream()
                .filter(player -> player.uuid.equals(selectedPlayer))
                .map(player -> player.name)
                .findFirst()
                .orElse("Player");
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }

        int panelHeight = panelHeight();
        int left = panelLeft();
        int top = panelTop();
        int right = panelRight();
        int summaryTop = top + 58;
        int playerY = summaryTop + 22;
        int visiblePlayers = visiblePlayerCount(panelHeight);

        for (int i = 0; i < visiblePlayers; i++) {
            if (event.x() >= left + 14 && event.x() <= right - 14
                    && event.y() >= playerY - 4 && event.y() < playerY + 13) {
                UUID clicked = players.get(i).uuid;
                selectedPlayer = clicked.equals(selectedPlayer) ? null : clicked;
                historyScroll = 0;
                return true;
            }

            playerY += 17;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        int panelHeight = panelHeight();
        int left = panelLeft();
        int top = panelTop();
        int right = panelRight();

        int historyTop = historyTop(top, panelHeight);
        int historyBottom = historyBottom(top, panelHeight);

        if (mouseX >= left + 14 && mouseX <= right - 14
                && mouseY >= historyTop && mouseY <= historyBottom) {
            List<DeathRecord> visibleDeaths = getVisibleDeaths();
            int visibleRows = Math.max(1, (historyBottom - historyTop) / HISTORY_ROW_HEIGHT);
            int maxScroll = Math.max(0, visibleDeaths.size() - visibleRows);

            historyScroll = Math.max(0,
                    Math.min(historyScroll - verticalAmount, maxScroll));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private static class Snapshot {
        private List<DeathRecord> deaths;
    }

    private static class PlayerSummary {
        private final UUID uuid;
        private String name;
        private int count;

        private PlayerSummary(UUID uuid, String name, int count) {
            this.uuid = uuid;
            this.name = name;
            this.count = count;
        }
    }
}
