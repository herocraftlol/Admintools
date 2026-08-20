package fr.veloadmin.bridge;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.Messenger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Forwards each player's real Bukkit OP status to the Velocity proxy, so
 * VeloAdmin can grant its admin permissions to actual server operators
 * without needing a permissions plugin.
 */
public class OpSyncListener implements Listener {

    private final VeloAdminBridgePlugin plugin;

    public OpSyncListener(VeloAdminBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sendOpStatus(plugin, event.getPlayer());
    }

    public static void sendOpStatus(VeloAdminBridgePlugin plugin, Player player) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("OP");
            out.writeUTF(player.getUniqueId().toString());
            out.writeBoolean(player.isOp());
            player.sendPluginMessage(plugin, VeloAdminBridgePlugin.CHANNEL, bytes.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'envoyer le statut OP: " + e.getMessage());
        }
    }
}
