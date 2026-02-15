package com.watones.watoneshubactionbar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WatonesHubActionBar v2.1.0
 *
 * - ActionBar con rotacion de mensajes.
 * - BossBar con rotacion y animacion de progreso.
 * - Comandos: /whab reload|on|off|status
 * - Una sola task global y cancelable.
 */
public final class WatonesHubActionBar extends JavaPlugin implements Listener {

    private boolean pluginEnabled;

    private boolean actionBarEnabled;
    private List<Component> actionBarMessages;
    private int actionBarIndex = 0;
    private long actionBarInterval;

    private boolean bossBarEnabled;
    private List<Component> bossBarMessages;
    private int bossBarIndex = 0;
    private long bossBarRotationInterval;
    private boolean bossBarProgressEnabled;
    private double bossBarProgress;
    private double bossBarProgressStep;
    private double bossBarProgressDirection;
    private boolean bossBarProgressPingPong;
    private double bossBarProgressInitial;
    private BossBar bossBar;

    private final Set<UUID> bossbarViewers = new HashSet<>();

    private Set<String> targetWorlds;
    private long tickCounter = 0L;
    private BukkitTask tickerTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        reloadSettings();
        applyRuntimeState();

        getLogger().info("WatonesHubActionBar enabled. State=" + (pluginEnabled ? "ON" : "OFF")
                + " ActionBar=" + actionBarEnabled + " interval=" + actionBarInterval + "t");
    }

    @Override
    public void onDisable() {
        stopTask();
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        bossbarViewers.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("whab")) {
            return false;
        }

        if (!sender.hasPermission("watoneshubactionbar.reload")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: " + ChatColor.WHITE + "/whab <reload|on|off|status>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload":
                reloadConfig();
                reloadSettings();
                applyRuntimeState();
                sender.sendMessage(ChatColor.GREEN + "WatonesHubActionBar recargado. Estado: "
                        + (pluginEnabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                return true;

            case "on":
                if (pluginEnabled) {
                    sender.sendMessage(ChatColor.YELLOW + "WatonesHubActionBar ya esta encendido.");
                    return true;
                }
                setEnabledInConfig(true);
                sender.sendMessage(ChatColor.GREEN + "WatonesHubActionBar encendido.");
                return true;

            case "off":
                if (!pluginEnabled) {
                    sender.sendMessage(ChatColor.YELLOW + "WatonesHubActionBar ya esta apagado.");
                    return true;
                }
                setEnabledInConfig(false);
                sender.sendMessage(ChatColor.RED + "WatonesHubActionBar apagado.");
                return true;

            case "status":
                sender.sendMessage(ChatColor.GRAY + "Estado: " + (pluginEnabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")
                        + ChatColor.GRAY + " | ActionBar: " + (actionBarEnabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")
                        + ChatColor.GRAY + " | BossBar: " + (bossBarEnabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                return true;

            default:
                sender.sendMessage(ChatColor.YELLOW + "Uso: " + ChatColor.WHITE + "/whab <reload|on|off|status>");
                return true;
        }
    }

    private void setEnabledInConfig(boolean enabled) {
        getConfig().set("plugin.enabled", enabled);
        saveConfig();
        reloadConfig();
        reloadSettings();
        applyRuntimeState();
    }

    private void reloadSettings() {
        FileConfiguration cfg = getConfig();

        pluginEnabled = cfg.getBoolean("plugin.enabled", true);

        actionBarIndex = 0;
        bossBarIndex = 0;
        tickCounter = 0L;
        bossBarProgressDirection = 1.0D;

        actionBarEnabled = cfg.getBoolean("actionbar.enabled", true);

        List<String> abList = cfg.getStringList("actionbar.messages");
        if (abList.isEmpty()) {
            String single = cfg.getString("actionbar.message", "&7mc.watones.net");
            actionBarMessages = Collections.singletonList(parseText(single));
        } else {
            actionBarMessages = abList.stream().map(this::parseText).collect(Collectors.toList());
        }

        actionBarInterval = cfg.getLong("actionbar.interval", 40L);
        if (actionBarInterval <= 0L) {
            actionBarInterval = 20L;
        }

        targetWorlds = cfg.getStringList("worlds").stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        bossBarEnabled = pluginEnabled && cfg.getBoolean("bossbar.enabled", false);

        if (bossBarEnabled) {
            List<String> bbList = cfg.getStringList("bossbar.messages");
            if (bbList.isEmpty()) {
                String single = cfg.getString("bossbar.message", "&f25% de &dDESCUENTO &fen tienda.watones.net");
                bossBarMessages = Collections.singletonList(parseText(single));
            } else {
                bossBarMessages = bbList.stream().map(this::parseText).collect(Collectors.toList());
            }

            bossBarRotationInterval = cfg.getLong("bossbar.rotation-interval", 200L);
            if (bossBarRotationInterval < 20L) {
                bossBarRotationInterval = 20L;
            }

            BarColor color = parseBarColor(cfg.getString("bossbar.color", "PINK"));
            BarStyle style = parseBarStyle(cfg.getString("bossbar.style", "SOLID"));

            String title = componentToLegacy(bossBarMessages.get(bossBarIndex));

            if (bossBar == null) {
                bossBar = Bukkit.createBossBar(title, color, style);
            } else {
                bossBar.setTitle(title);
                bossBar.setColor(color);
                bossBar.setStyle(style);
                bossBar.removeAll();
            }

            syncBossbarViewers();

            bossBarProgressEnabled = cfg.getBoolean("bossbar.progress.enabled", true);
            bossBarProgressStep = cfg.getDouble("bossbar.progress.step", 0.01D);
            if (bossBarProgressStep <= 0D) {
                bossBarProgressStep = 0.01D;
            }

            bossBarProgress = cfg.getDouble("bossbar.progress.initial", 1.0D);
            if (bossBarProgress < 0D) {
                bossBarProgress = 0D;
            } else if (bossBarProgress > 1D) {
                bossBarProgress = 1D;
            }

            bossBarProgressInitial = bossBarProgress;
            bossBarProgressPingPong = cfg.getBoolean("bossbar.progress.pingpong", true);
            bossBarProgressDirection = -1.0D;
            bossBar.setProgress(bossBarProgress);
        } else {
            if (bossBar != null) {
                bossBar.removeAll();
                bossBar = null;
            }

            bossBarProgressEnabled = false;
            bossBarProgressPingPong = false;
            bossBarProgressInitial = 1.0D;
            bossbarViewers.clear();
        }
    }

    private BarColor parseBarColor(String value) {
        try {
            return BarColor.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            getLogger().warning("Invalid bossbar color in config.yml, using PINK.");
            return BarColor.PINK;
        }
    }

    private BarStyle parseBarStyle(String value) {
        try {
            return BarStyle.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            getLogger().warning("Invalid bossbar style in config.yml, using SOLID.");
            return BarStyle.SOLID;
        }
    }

    private Component parseText(String raw) {
        if (raw == null) {
            raw = "";
        }

        if (raw.contains("&")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        }

        return MiniMessage.miniMessage().deserialize(raw);
    }

    private String componentToLegacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!bossBarEnabled || bossBar == null) {
            return;
        }

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (bossbarViewers.remove(id)) {
            bossBar.removePlayer(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!pluginEnabled || !bossBarEnabled || bossBar == null) {
            return;
        }
        updateBossbarViewer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (!pluginEnabled || !bossBarEnabled || bossBar == null) {
            return;
        }
        updateBossbarViewer(event.getPlayer());
    }

    private void syncBossbarViewers() {
        if (!bossBarEnabled || bossBar == null) {
            bossbarViewers.clear();
            return;
        }

        bossBar.removeAll();
        bossbarViewers.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (shouldShow(player.getWorld())) {
                UUID id = player.getUniqueId();
                bossbarViewers.add(id);
                bossBar.addPlayer(player);
            }
        }
    }

    private void updateBossbarViewer(Player player) {
        UUID id = player.getUniqueId();
        boolean inTarget = shouldShow(player.getWorld());

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

    private void applyRuntimeState() {
        if (pluginEnabled) {
            ensureTaskRunning();
        } else {
            stopTask();
            if (bossBar != null) {
                bossBar.removeAll();
            }
            bossbarViewers.clear();
        }
    }

    private void ensureTaskRunning() {
        if (tickerTask != null && !tickerTask.isCancelled()) {
            return;
        }

        tickerTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!pluginEnabled) {
                return;
            }

            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            if (players.isEmpty()) {
                return;
            }

            tickCounter++;

            boolean sendActionBarNow = actionBarEnabled && (tickCounter % actionBarInterval == 0);

            Component currentActionBar = null;
            if (sendActionBarNow) {
                currentActionBar = actionBarMessages.get(actionBarIndex);
                if (actionBarMessages.size() > 1) {
                    actionBarIndex = (actionBarIndex + 1) % actionBarMessages.size();
                }
            }

            if (bossBarEnabled && bossBar != null && bossBarMessages.size() > 1
                    && (tickCounter % bossBarRotationInterval == 0)) {
                bossBarIndex = (bossBarIndex + 1) % bossBarMessages.size();
                bossBar.setTitle(componentToLegacy(bossBarMessages.get(bossBarIndex)));
            }

            if (bossBarEnabled && bossBar != null && bossBarProgressEnabled) {
                bossBarProgress += bossBarProgressStep * bossBarProgressDirection;

                if (bossBarProgress >= 1.0D) {
                    bossBarProgress = 1.0D;
                    bossBarProgressDirection = bossBarProgressPingPong ? -1.0D : 1.0D;
                } else if (bossBarProgress <= 0.0D) {
                    if (bossBarProgressPingPong) {
                        bossBarProgress = 0.0D;
                        bossBarProgressDirection = 1.0D;
                    } else {
                        bossBarProgress = bossBarProgressInitial;
                        bossBarProgressDirection = -1.0D;
                    }
                }

                bossBar.setProgress(bossBarProgress);
            }

            if (sendActionBarNow && currentActionBar != null) {
                for (Player player : players) {
                    if (shouldShow(player.getWorld())) {
                        player.sendActionBar(currentActionBar);
                    }
                }
            }
        }, 0L, 1L);
    }

    private void stopTask() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
    }

    private boolean shouldShow(World world) {
        return targetWorlds.isEmpty() || targetWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }
}
