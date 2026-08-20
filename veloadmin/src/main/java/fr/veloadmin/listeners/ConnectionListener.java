package fr.veloadmin.listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import fr.veloadmin.VeloAdminPlugin;
import fr.veloadmin.storage.Database;
import fr.veloadmin.util.DurationParser;
import fr.veloadmin.util.OpCache;
import fr.veloadmin.util.VanishManager;
import fr.veloadmin.util.VanishTabListUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class ConnectionListener {

    private final Database database;
    private final VanishManager vanishManager;
    private final OpCache opCache;
    private final ProxyServer server;
    private final VeloAdminPlugin plugin;

    public ConnectionListener(Database database, VanishManager vanishManager, OpCache opCache, ProxyServer server, VeloAdminPlugin plugin) {
        this.database = database;
        this.vanishManager = vanishManager;
        this.opCache = opCache;
        this.server = server;
        this.plugin = plugin;
    }

    /** Blocks login entirely for network-wide (ALL) bans. */
    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        Database.BanEntry ban = database.getActiveBan(player.getUsername(), "ALL");
        if (ban != null && ban.server().equals("ALL")) {
            event.setResult(LoginEvent.ComponentResult.denied(kickMessage(ban)));
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

    /** New player on the proxy: hide already-vanished players from their tab list. */
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        VanishTabListUtil.applyCurrentVanishState(server, vanishManager, event.getPlayer());
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

    /** Receives OP status pushed by VeloAdminBridge from the backend the player is on. */
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        ChannelIdentifier identifier = event.getIdentifier();
        if (!identifier.equals(VeloAdminPlugin.CHANNEL)) return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String type = in.readUTF();

        if (type.equals("OP")) {
            UUID uuid = UUID.fromString(in.readUTF());
            boolean isOp = in.readBoolean();
            opCache.setOp(uuid, isOp);
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        opCache.remove(player.getUniqueId());
        if (vanishManager.isVanished(player.getUniqueId())) {
            vanishManager.toggle(player.getUniqueId());
        }
    }

    private Component kickMessage(Database.BanEntry ban) {
        String scope = ban.server().equals("ALL") ? "du réseau" : ("de " + ban.server());
        long remaining = ban.end() - System.currentTimeMillis();
        return Component.text("Tu es banni " + scope + "\n", NamedTextColor.RED)
                .append(Component.text("Raison : " + ban.reason() + "\n", NamedTextColor.GRAY))
                .append(Component.text("Temps restant : " + DurationParser.humanize(Math.max(remaining, 0)), NamedTextColor.GRAY));
    }
}
