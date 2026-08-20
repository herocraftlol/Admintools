package fr.veloadmin.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.veloadmin.storage.Database;
import fr.veloadmin.util.DurationParser;
import fr.veloadmin.util.OpCache;
import fr.veloadmin.util.PermissionUtil;
import fr.veloadmin.util.SuggestionsUtil;
import fr.veloadmin.util.VanishManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
public final class TempBanCommand {

    private TempBanCommand() {}

    public static BrigadierCommand create(Database database, ProxyServer server, VanishManager vanishManager, OpCache opCache) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("tempban")
                .requires(source -> PermissionUtil.has(source, "veloadmin.admin.ban", opCache))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("joueur", StringArgumentType.word())
                        .suggests((ctx, builder) -> SuggestionsUtil.suggestPlayers(ctx, builder, server, vanishManager))
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("duree", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String s : List.of("30m", "1h", "6h", "1d", "3d", "7d")) {
                                        if (s.startsWith(builder.getRemaining())) builder.suggest(s);
                                    }
                                    return builder.buildFuture();
                                })
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("serveur", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SuggestionsUtil.suggestServers(builder, server, true))
                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("raison", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String targetName = StringArgumentType.getString(ctx, "joueur");
                                                    String durationRaw = StringArgumentType.getString(ctx, "duree");
                                                    String serverArg = StringArgumentType.getString(ctx, "serveur");
                                                    String reason = StringArgumentType.getString(ctx, "raison");
                                                    execute(ctx.getSource(), database, server, targetName, durationRaw, serverArg, reason);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
                .build();

        return new BrigadierCommand(node);
    }

    private static void execute(CommandSource source, Database database, ProxyServer server,
                                 String targetName, String durationRaw, String serverArg, String reason) {

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

        String finalServerName = serverName;
        online.ifPresent(p -> {
            boolean currentlyOnBannedServer = finalServerName.equals("ALL") || p.getCurrentServer()
                    .map(sc -> sc.getServer().getServerInfo().getName().equalsIgnoreCase(finalServerName))
                    .orElse(false);
            if (currentlyOnBannedServer) {
                Component kickMsg = Component.text("Tu as été banni " + (finalServerName.equals("ALL") ? "du réseau" : "de " + finalServerName) + "\n", NamedTextColor.RED)
                        .append(Component.text("Raison : " + reason + "\n", NamedTextColor.GRAY))
                        .append(Component.text("Durée : " + DurationParser.humanize(durationMillis), NamedTextColor.GRAY));
                p.disconnect(kickMsg);
            }
        });
    }
}
