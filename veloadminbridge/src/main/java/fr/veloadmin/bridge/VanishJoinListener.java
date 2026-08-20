package fr.veloadmin.bridge;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class VanishJoinListener implements Listener {

    private final VeloAdminBridgePlugin plugin;

    public VanishJoinListener(VeloAdminBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();

        // If the player who just joined is themselves (re)connecting while flagged vanished
        // (e.g. proxy-side reconnect), suppress their own join broadcast.
        if (plugin.getVanished().contains(joined.getUniqueId())) {
            event.setJoinMessage(null);
        }

        // Hide any already-vanished players from the newcomer, unless they can bypass.
        boolean bypass = joined.hasPermission("veloadmin.vanish.see");
        if (!bypass) {
            for (var vanishedId : plugin.getVanished()) {
                var vanishedPlayer = plugin.getServer().getPlayer(vanishedId);
                if (vanishedPlayer != null) {
                    joined.hidePlayer(plugin, vanishedPlayer);
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getVanished().contains(event.getPlayer().getUniqueId())) {
            event.setQuitMessage(null);
        }
    }
}
