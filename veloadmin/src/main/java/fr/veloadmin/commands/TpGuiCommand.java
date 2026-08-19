package fr.veloadmin.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.veloadmin.util.VanishManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Collection;

/**
 * Chat-based "GUI" (Velocity has no inventory access):
 * /tpgui              -> list servers with online counts, click one to see its players
 * /tpgui <server>     -> list players on that server, click one to teleport
 */
public class TpGuiCommand implements SimpleCommand {

    private final ProxyServer server;
    private final VanishManager vanishManager;

    public TpGuiCommand(ProxyServer server, VanishManager vanishManager) {
        this.server = server;
        this.vanishManager = vanishManager;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        if (!(source instanceof Player viewer)) {
            source.sendMessage(Component.text("Seuls les joueurs peuvent utiliser cette commande.", NamedTextColor.RED));
            return;
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            listServers(viewer);
        } else {
            listPlayers(viewer, args[0]);
        }
    }

    private void listServers(Player viewer) {
        viewer.sendMessage(Component.text("──── Serveurs ────", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (RegisteredServer rs : server.getAllServers()) {
            String name = rs.getServerInfo().getName();
            int count = rs.getPlayersConnected().size();
            Component line = Component.text("▶ " + name + " ", NamedTextColor.AQUA)
                    .append(Component.text("(" + count + " joueurs)", NamedTextColor.GRAY))
                    .clickEvent(ClickEvent.runCommand("/tpgui " + name))
                    .hoverEvent(HoverEvent.showText(Component.text("Cliquer pour voir les joueurs de " + name)));
            viewer.sendMessage(line);
        }
    }

    private void listPlayers(Player viewer, String serverName) {
        var opt = server.getServer(serverName);
        if (opt.isEmpty()) {
            viewer.sendMessage(Component.text("Serveur inconnu : " + serverName, NamedTextColor.RED));
            return;
        }
        RegisteredServer rs = opt.get();
        Collection<Player> players = rs.getPlayersConnected();

        viewer.sendMessage(Component.text("──── Joueurs sur " + serverName + " ────", NamedTextColor.GOLD, TextDecoration.BOLD));
        if (players.isEmpty()) {
            viewer.sendMessage(Component.text("Aucun joueur en ligne sur ce serveur.", NamedTextColor.GRAY));
            return;
        }

        boolean canSeeVanished = viewer.hasPermission("veloadmin.vanish.see");

        for (Player p : players) {
            if (vanishManager.isVanished(p.getUniqueId()) && !canSeeVanished) continue;

            Component tag = vanishManager.isVanished(p.getUniqueId())
                    ? Component.text(" [Vanish]", NamedTextColor.DARK_GRAY)
                    : Component.empty();

            Component line = Component.text("▶ ", NamedTextColor.GREEN)
                    .append(Component.text(p.getUsername(), NamedTextColor.WHITE))
                    .append(tag)
                    .clickEvent(ClickEvent.runCommand("/tpto " + p.getUsername()))
                    .hoverEvent(HoverEvent.showText(Component.text("Cliquer pour te téléporter à " + p.getUsername())));
            viewer.sendMessage(line);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("veloadmin.tpgui");
    }
}
