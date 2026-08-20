package fr.veloadmin.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

public class BridgeMessageListener implements PluginMessageListener {

    private final VeloAdminBridgePlugin plugin;

    public BridgeMessageListener(VeloAdminBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player receiver, byte[] message) {
        if (!channel.equals(VeloAdminBridgePlugin.CHANNEL)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String type = in.readUTF();

            switch (type) {
                case "TP" -> handleTeleport(in);
                case "VANISH" -> handleVanish(in);
                default -> plugin.getLogger().warning("Type de message VeloAdmin inconnu: " + type);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Erreur en lisant un message VeloAdmin: " + e.getMessage());
        }
    }

    private void handleTeleport(DataInputStream in) throws IOException {
        UUID executorId = UUID.fromString(in.readUTF());
        UUID targetId = UUID.fromString(in.readUTF());

        // Run a few ticks later in case the player just switched servers and hasn't fully spawned.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player executor = Bukkit.getPlayer(executorId);
            Player target = Bukkit.getPlayer(targetId);
            if (executor == null || target == null) return;
            executor.teleport(target.getLocation());
            executor.sendMessage("§aTéléporté vers " + target.getName() + " !");
        }, 5L);
    }

    private void handleVanish(DataInputStream in) throws IOException {
        UUID targetId = UUID.fromString(in.readUTF());
        boolean nowVanished = in.readBoolean();

        if (nowVanished) {
            plugin.getVanished().add(targetId);
        } else {
            plugin.getVanished().remove(targetId);
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null) return;

        // Physical invisibility, on top of hidePlayer (belt and suspenders: hidePlayer stops
        // interaction/collision/rendering, the potion effect covers edge cases like armor stands
        // shooting at the player, mob targeting, or plugins that bypass the visibility API).
        if (nowVanished) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false));
        } else {
            target.removePotionEffect(PotionEffectType.INVISIBILITY);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(target)) continue;
            boolean bypass = online.hasPermission("veloadmin.vanish.see");
            if (nowVanished && !bypass) {
                online.hidePlayer(plugin, target);
            } else {
                online.showPlayer(plugin, target);
            }
        }
    }
}
