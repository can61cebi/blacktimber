package tr.cebi.blacktimber;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Anonymous, opt-out usage telemetry.
 *
 * Once every fifteen minutes the plugin sends a tiny ping to the BlackTimber stats
 * service: a random id this server generated once, its current online player count,
 * and its software and version strings. No IP, no player names, no UUIDs, nothing
 * personal ever leaves the server. The totals power the live chart in the README so
 * players and admins can see how widely the plugin is used.
 *
 * Server owners turn it off with a single line, {@code telemetry: false} in
 * {@code config.yml}. The work runs on the async scheduler, never on a region
 * thread, and a failure can never affect the server.
 */
public final class Telemetry {

    private static final String ENDPOINT = "https://cebi.tr/api/blacktimber/telemetry";
    private static final long INITIAL_DELAY_MINUTES = 2;
    private static final long PERIOD_MINUTES = 15;

    private final BlackTimber plugin;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String serverId;
    private String userAgent = "BlackTimber";

    public Telemetry(BlackTimber plugin) {
        this.plugin = plugin;
    }

    /** Starts reporting if the owner has left telemetry enabled. */
    public void start() {
        if (!plugin.getConfig().getBoolean("telemetry", true)) {
            return;
        }
        this.serverId = resolveServerId();
        this.userAgent = "BlackTimber/" + plugin.getPluginMeta().getVersion();
        plugin.getLogger().info("Anonymous usage stats are on: server and player counts only, "
                + "nothing personal. Turn it off with 'telemetry: false' in config.yml.");
        plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, task -> send(),
                INITIAL_DELAY_MINUTES, PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    /** A stable random id for this server, generated once and kept in config.yml. */
    private String resolveServerId() {
        String id = plugin.getConfig().getString("telemetry-server-id", "");
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            plugin.editConfig("telemetry-server-id", id);
        }
        return id;
    }

    private void send() {
        try {
            int players = plugin.getServer().getOnlinePlayers().size();
            String version = plugin.getPluginMeta().getVersion();
            String body = "{"
                    + "\"server_id\":\"" + serverId + "\","
                    + "\"players\":" + players + ","
                    + "\"software\":\"" + esc(software()) + "\","
                    + "\"plugin_version\":\"" + esc(version) + "\","
                    + "\"mc_version\":\"" + esc(plugin.getServer().getMinecraftVersion()) + "\""
                    + "}";
            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", userAgent)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            http.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // Telemetry is best effort and must never disturb the server.
        }
    }

    /** Folia reports itself as Paper through the Bukkit name, so detect it directly. */
    private String software() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return "Folia";
        } catch (ClassNotFoundException notFolia) {
            return plugin.getServer().getName();
        }
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
