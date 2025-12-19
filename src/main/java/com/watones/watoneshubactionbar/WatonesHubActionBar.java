package com.watones.watoneshubactionbar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WatonesHubActionBar v2.1
 *
 * - ActionBar permanente con rotación de mensajes (toggleable).
 * - BossBar con rotación de mensajes y animación de progreso.
 * - Comando /whab reload para recargar config.
 * - Task única y ligera.
 */
public final class WatonesHubActionBar extends JavaPlugin {

    // ─────────── ActionBar ───────────
    private boolean actionBarEnabled;
    private List<Component> actionBarMessages;
    private int actionBarIndex = 0;
    private long actionBarInterval;          // cada cuántos ticks se envía

    // ─────────── BossBar ───────────
    private boolean bossBarEnabled;
    private List<Component> bossBarMessages;
    private int bossBarIndex = 0;
    private long bossBarRotationInterval;    // cada cuántos ticks cambia el mensaje
    private boolean bossBarProgressEnabled;
    private double bossBarProgress;          // 0.0 - 1.0
    private double bossBarProgressStep;      // cuanto avanza por tick
    private double bossBarProgressDirection; // 1 sube, -1 baja
    private BossBar bossBar;

    // Optimización: membership O(1) para no usar bossBar.getPlayers().contains() cada tick
    private final Set<UUID> bossbarViewers = new HashSet<>();

    // ─────────── Mundos y tick global ───────────
    private Set<String> targetWorlds;
    private long tickCounter = 0L;

    // ───────────────── Ciclo de vida ─────────────────

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();
        startTask();
        getLogger().info("WatonesHubActionBar habilitado. ActionBar: " + actionBarEnabled
                + " | Interval ActionBar: " + actionBarInterval + " ticks.");
    }

    @Override
    public void onDisable() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        bossbarViewers.clear();
    }

    // ───────────────── Comando /whab ─────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("whab")) return false;

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("§eUso: §f/whab reload");
            return true;
        }

        if (!sender.hasPermission("watoneshubactionbar.reload")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        reloadConfig();
        reloadSettings();
        sender.sendMessage("§aWatonesHubActionBar recargado correctamente.");
        return true;
    }

    // ───────────────── Configuración ─────────────────

    private void reloadSettings() {
        FileConfiguration cfg = getConfig();

        // Reset de índices y ticks
        actionBarIndex = 0;
        bossBarIndex = 0;
        tickCounter = 0L;
        bossBarProgressDirection = 1.0D;

        // ---------- ActionBar ----------
        actionBarEnabled = cfg.getBoolean("actionbar.enabled", true);

        List<String> abList = cfg.getStringList("actionbar.messages");
        if (abList == null || abList.isEmpty()) {
            String single = cfg.getString("actionbar.message", "&7ᴍᴄ.ᴡᴀᴛᴏɴᴇꜱ.ɴᴇᴛ");
            actionBarMessages = Collections.singletonList(parseText(single));
        } else {
            actionBarMessages = abList.stream()
                    .map(this::parseText)
                    .collect(Collectors.toList());
        }

        actionBarInterval = cfg.getLong("actionbar.interval", 40L);
        if (actionBarInterval <= 0) {
            actionBarInterval = 20L; // mínimo 1s para evitar spam absurdo
        }

        // ---------- Mundos ----------
        targetWorlds = cfg.getStringList("worlds").stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // ---------- BossBar ----------
        bossBarEnabled = cfg.getBoolean("bossbar.enabled", false);

        if (bossBarEnabled) {
            // mensajes
            List<String> bbList = cfg.getStringList("bossbar.messages");
            if (bbList == null || bbList.isEmpty()) {
                String single = cfg.getString("bossbar.message",
                        "&f25% de &dDESCUENTO &fen tienda.watones.net");
                bossBarMessages = Collections.singletonList(parseText(single));
            } else {
                bossBarMessages = bbList.stream()
                        .map(this::parseText)
                        .collect(Collectors.toList());
            }

            bossBarRotationInterval = cfg.getLong("bossbar.rotation-interval", 200L); // 10s
            if (bossBarRotationInterval < 20L) {
                bossBarRotationInterval = 20L; // mínimo 1s entre cambios
            }

            // color y estilo
            String colorName = cfg.getString("bossbar.color", "PINK").toUpperCase();
            String styleName = cfg.getString("bossbar.style", "SOLID").toUpperCase();

            BarColor color;
            BarStyle style;

            try {
                color = BarColor.valueOf(colorName);
            } catch (IllegalArgumentException ex) {
                color = BarColor.PURPLE;
                getLogger().warning("Color de bossbar inválido en config.yml, usando PURPLE.");
            }

            try {
                style = BarStyle.valueOf(styleName);
            } catch (IllegalArgumentException ex) {
                style = BarStyle.SOLID;
                getLogger().warning("Estilo de bossbar inválido en config.yml, usando SOLID.");
            }

            // título inicial
            Component titleComponent = bossBarMessages.get(bossBarIndex);
            String title = componentToLegacy(titleComponent);

            if (bossBar == null) {
                bossBar = Bukkit.createBossBar(title, color, style);
            } else {
                bossBar.setTitle(title);
                bossBar.setColor(color);
                bossBar.setStyle(style);
                bossBar.removeAll();
            }

            // IMPORTANT: resetea viewers para que se vuelva a añadir correctamente por mundo
            bossbarViewers.clear();

            // progreso y animación
            bossBarProgressEnabled = cfg.getBoolean("bossbar.progress.enabled", true);
            bossBarProgressStep = cfg.getDouble("bossbar.progress.step", 0.01D);
            if (bossBarProgressStep <= 0) {
                bossBarProgressStep = 0.01D;
            }

            bossBarProgress = cfg.getDouble("bossbar.progress.initial", 1.0D);
            if (bossBarProgress < 0) bossBarProgress = 0;
            if (bossBarProgress > 1) bossBarProgress = 1;

            boolean pingPong = cfg.getBoolean("bossbar.progress.pingpong", true);
            bossBarProgressDirection = pingPong ? -1.0D : -1.0D; // empezamos bajando desde 1.0
            bossBar.setProgress(bossBarProgress);

        } else {
            if (bossBar != null) {
                bossBar.removeAll();
                bossBar = null;
            }
            bossBarProgressEnabled = false;
            bossbarViewers.clear();
        }
    }

    private Component parseText(String raw) {
        if (raw == null) raw = "";
        if (raw.contains("&")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        }
        return MiniMessage.miniMessage().deserialize(raw);
    }

    private String componentToLegacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    // ───────────────── Tarea principal ─────────────────

    private void startTask() {
        // Una sola task cada 1 tick.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            tickCounter++;

            // ¿Toca enviar ActionBar este tick?
            boolean sendActionBarNow = actionBarEnabled && (tickCounter % actionBarInterval == 0);

            // Mensaje actual del ActionBar (si toca enviar)
            Component currentActionBar = null;
            if (sendActionBarNow) {
                currentActionBar = actionBarMessages.get(actionBarIndex);
                if (actionBarMessages.size() > 1) {
                    actionBarIndex = (actionBarIndex + 1) % actionBarMessages.size();
                }
            }

            // Rotación de mensaje de BossBar
            if (bossBarEnabled && bossBar != null && bossBarMessages.size() > 1
                    && (tickCounter % bossBarRotationInterval == 0)) {
                bossBarIndex = (bossBarIndex + 1) % bossBarMessages.size();
                bossBar.setTitle(componentToLegacy(bossBarMessages.get(bossBarIndex)));
            }

            // Animación de progreso de BossBar
            if (bossBarEnabled && bossBar != null && bossBarProgressEnabled) {
                bossBarProgress += bossBarProgressStep * bossBarProgressDirection;

                if (bossBarProgress >= 1.0D) {
                    bossBarProgress = 1.0D;
                    bossBarProgressDirection = -1.0D;
                } else if (bossBarProgress <= 0.0D) {
                    bossBarProgress = 0.0D;
                    bossBarProgressDirection = 1.0D;
                }

                bossBar.setProgress(bossBarProgress);
            }

            // Recorrido de jugadores UNA sola vez por tick
            for (Player player : Bukkit.getOnlinePlayers()) {
                boolean inTarget = shouldShow(player.getWorld());

                // ActionBar solo si toca y si está en mundo objetivo
                if (sendActionBarNow && inTarget && currentActionBar != null) {
                    player.sendActionBar(currentActionBar);
                }

                // BossBar: añadir / remover según mundo (O(1) con set)
                if (bossBarEnabled && bossBar != null) {
                    UUID id = player.getUniqueId();

                    if (inTarget) {
                        if (bossbarViewers.add(id)) {
                            bossBar.addPlayer(player);
                        }
                    } else {
                        if (bossbarViewers.remove(id)) {
                            bossBar.removePlayer(player);
                        }
                    }
                }
            }

        }, 0L, 1L); // periodo 1 tick
    }

    private boolean shouldShow(World world) {
        return targetWorlds.isEmpty() || targetWorlds.contains(world.getName().toLowerCase());
    }
}
