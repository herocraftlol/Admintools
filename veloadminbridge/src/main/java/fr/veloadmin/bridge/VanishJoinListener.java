package fr.veloadmin.bridge;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class VanishJoinListener implements Listener {

    private final VeloAdminBridgePlugin plugin;

    public VanishJoinListener(VeloAdminBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();

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
}
