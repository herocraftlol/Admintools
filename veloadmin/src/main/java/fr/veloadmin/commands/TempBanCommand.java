package fr.veloadmin.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.veloadmin.storage.Database;
import fr.veloadmin.util.DurationParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /tempban <joueur> <durée> <serveur|ALL> <raison...>
 * Exemples :
 *   /tempban Steve 1d2h ALL Insultes envers un joueur
 *   /tempban Steve 30m survie AFK farm suspecte
 */
public class TempBanCommand implements SimpleCommand {

    private final Database database;
    private final ProxyServer server;

    public TempBanCommand(Database database, ProxyServer server) {
        this.database = database;
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 4) {
            source.sendMessage(Component.text("Usage: /tempban <joueur> <durée ex: 1d2h30m> <serveur|ALL> <raison>", NamedTextColor.RED));
            return;
        }

        String targetName = args[0];
        String durationRaw = args[1];
        String serverArg = args[2];
        String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        long durationMillis = DurationParser.parseToMillis(durationRaw);
        if (durationMillis <= 0) {
            source.sendMessage(Component.text("Durée invalide. Exemple valide: 1d2h30m, 45m, 3h", NamedTextColor.RED));
            return;
        }

        String serverName;
        if (serverArg.equalsIgnoreCase("all")) {
            serverName = "ALL";
        } else {
            var found = server.getServer(serverArg);
            if (found.isEmpty()) {
                List<String> known = server.getAllServers().stream()
                        .map(s -> s.getServerInfo().getName())
                        .collect(Collectors.toList());
                source.sendMessage(Component.text("Serveur inconnu : " + serverArg + ". Serveurs connus : " + known, NamedTextColor.RED));
                return;
            }
            serverName = found.get().getServerInfo().getName();
        }

        // Resolve UUID: prefer the online player, otherwise fall back to offline placeholder.
        Optional<Player> online = server.getPlayer(targetName);
        UUID uuid = online.map(Player::getUniqueId).orElseGet(() ->
                UUID.nameUUIDFromBytes(("OfflinePlayer:" + targetName).getBytes()));

        long start = System.currentTimeMillis();
        long end = start + durationMillis;
        String bannedBy = source instanceof Player p ? p.getUsername() : "CONSOLE";

        database.addBan(uuid, targetName, reason, bannedBy, start, end, serverName);

        String scope = serverName.equals("ALL") ? "tout le réseau" : "le serveur " + serverName;
        source.sendMessage(Component.text(targetName + " banni de " + scope + " pour " + DurationParser.humanize(durationMillis)
                + ". Raison : " + reason, NamedTextColor.GREEN));

        // Kick immediately if relevant and currently connected.
        online.ifPresent(p -> {
            boolean currentlyOnBannedServer = serverName.equals("ALL") || p.getCurrentServer()
                    .map(sc -> sc.getServer().getServerInfo().getName().equalsIgnoreCase(serverName))
                    .orElse(false);
            if (currentlyOnBannedServer) {
                Component kickMsg = Component.text("Tu as été banni " + (serverName.equals("ALL") ? "du réseau" : "de " + serverName) + "\n", NamedTextColor.RED)
                        .append(Component.text("Raison : " + reason + "\n", NamedTextColor.GRAY))
                        .append(Component.text("Durée : " + DurationParser.humanize(durationMillis), NamedTextColor.GRAY));
                p.disconnect(kickMsg);
            }
        });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("veloadmin.admin.ban");
    }
}
