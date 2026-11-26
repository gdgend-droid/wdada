package com.radar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

/**
 * Client-side radar. Every few ticks it collects the players your client currently
 * has loaded (so: players within your render/entity-tracking range) and POSTs their
 * names + coordinates to your Render website.
 */
public class PlayerRadarClient implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("playerradar");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private String url = "";
    private String apiKey = "";
    private String serverName = "Minecraft";
    private int interval = 20;
    private int ticks = 0;
    private boolean sentEmpty = false;
    private boolean warned = false;

    @Override
    public void onInitializeClient() {
        loadConfig();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void loadConfig() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("playerradar.properties");
        Properties p = new Properties();
        p.setProperty("url", "https://YOUR-APP.onrender.com");
        p.setProperty("api-key", "CHANGE_ME");
        p.setProperty("label", "My Radar");
        p.setProperty("interval-ticks", "20");
        try {
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) { p.load(in); }
            } else {
                try (OutputStream out = Files.newOutputStream(file)) {
                    p.store(out, "Player Radar config. url = your Render site, api-key = RADAR_API_KEY from Render.");
                }
            }
        } catch (IOException e) {
            LOG.warn("Could not read/write config: {}", e.getMessage());
        }
        url = p.getProperty("url", "").trim().replaceAll("/+$", "");
        apiKey = p.getProperty("api-key", "").trim();
        serverName = p.getProperty("label", "Minecraft");
        try { interval = Math.max(5, Integer.parseInt(p.getProperty("interval-ticks", "20").trim())); }
        catch (NumberFormatException e) { interval = 20; }
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String dimOf(ClientLevel level) {
        String d = level.dimension().toString();   // e.g. ResourceKey[minecraft:dimension / minecraft:the_nether]
        if (d.contains("the_nether")) return "nether";
        if (d.contains("the_end")) return "end";
        if (d.contains("overworld")) return "overworld";
        return "custom";
    }

    private void tick(Minecraft mc) {
        if (++ticks < interval) return;
        ticks = 0;

        if (url.isEmpty() || url.contains("YOUR-APP") || apiKey.isEmpty() || apiKey.equals("CHANGE_ME")) {
            if (!warned) { LOG.warn("Set url and api-key in config/playerradar.properties, then restart the game."); warned = true; }
            return;
        }

        ClientLevel level = mc.level;
        StringBuilder sb = new StringBuilder("{\"server\":\"").append(esc(serverName)).append("\",\"players\":[");

        if (level == null || mc.player == null) {
            if (sentEmpty) return;      // only send one "empty" update after leaving a world
            sentEmpty = true;
        } else {
            sentEmpty = false;
            String dim = dimOf(level);
            boolean first = true;
            for (Player pl : level.players()) {
                if (!first) sb.append(',');
                first = false;
                sb.append("{\"name\":\"").append(esc(pl.getName().getString()))
                  .append("\",\"uuid\":\"").append(pl.getUUID())
                  .append("\",\"world\":\"").append(dim)
                  .append("\",\"dim\":\"").append(dim)
                  .append("\",\"x\":").append((int) Math.floor(pl.getX()))
                  .append(",\"y\":").append((int) Math.floor(pl.getY()))
                  .append(",\"z\":").append((int) Math.floor(pl.getZ()))
                  .append(",\"yaw\":").append(Math.round(pl.getYRot())).append('}');
            }
        }
        sb.append("]}");
        send(sb.toString());
    }

    private void send(String json) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url + "/api/update"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        http.sendAsync(req, HttpResponse.BodyHandlers.discarding()).whenComplete((r, ex) -> {
            if (ex != null) LOG.debug("Radar push failed: {}", ex.getMessage());
            else if (r.statusCode() == 401) LOG.warn("Radar rejected the api-key (401). Check it matches Render.");
        });
    }
}
