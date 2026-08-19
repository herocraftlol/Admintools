package fr.veloadmin.listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import fr.veloadmin.VeloAdminPlugin;
import fr.veloadmin.storage.Database;
import fr.veloadmin.util.DurationParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ConnectionListener {

    private final Database database;
    private final fr.veloadmin.util.VanishManager vanishManager;
    private final VeloAdminPlugin plugin;

    public ConnectionListener(Database database, fr.veloadmin.util.VanishManager vanishManager, VeloAdminPlugin plugin) {
        this.database = database;
        this.vanishManager = vanishManager;
        this.plugin = plugin;
    }

    /** Blocks login entirely for network-wide (ALL) bans. */
    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        Database.BanEntry ban = database.getActiveBan(player.getUsername(), "ALL");
        if (ban != null && ban.server().equals("ALL")) {
            event.setResult(com.velocitypowered.api.event.connection.LoginEvent.ComponentResult.denied(kickMessage(ban)));
        }
    }

    /** Blocks connecting to a specific backend server if banned there (or globally). */
    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        String serverName = event.getOriginalServer().getServerInfo().getName();
        Database.BanEntry ban = database.getActiveBan(player.getUsername(), serverName);
        if (ban != null) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(kickMessage(ban));
        }
    }

    /** Re-applies vanish state on the newly joined backend server. */
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        if (!vanishManager.isVanished(player.getUniqueId())) return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("VANISH");
        out.writeUTF(player.getUniqueId().toString());
        out.writeBoolean(true);
        event.getServer().sendPluginMessage(VeloAdminPlugin.CHANNEL, out.toByteArray());
    }

    private Component kickMessage(Database.BanEntry ban) {
        String scope = ban.server().equals("ALL") ? "du réseau" : ("de " + ban.server());
        long remaining = ban.end() - System.currentTimeMillis();
        return Component.text("Tu es banni " + scope + "\n", NamedTextColor.RED)
                .append(Component.text("Raison : " + ban.reason() + "\n", NamedTextColor.GRAY))
                .append(Component.text("Temps restant : " + DurationParser.humanize(Math.max(remaining, 0)), NamedTextColor.GRAY));
    }
}
