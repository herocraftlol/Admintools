package fr.veloadmin.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.veloadmin.util.OpCache;
import fr.veloadmin.util.PermissionUtil;
import fr.veloadmin.util.SuggestionsUtil;
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
 * /tpgui <serveur>    -> list players on that server, click one to teleport
 */
public final class TpGuiCommand {

    private TpGuiCommand() {}

    public static BrigadierCommand create(ProxyServer server, VanishManager vanishManager, OpCache opCache) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("tpgui")
                .requires(source -> PermissionUtil.has(source, "veloadmin.tpgui", opCache))
                .executes(ctx -> {
                    requirePlayer(ctx.getSource(), viewer -> listServers(viewer, server));
                    return 1;
                })
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("serveur", StringArgumentType.word())
                        .suggests((ctx, builder) -> SuggestionsUtil.suggestServers(builder, server, false))
                        .executes(ctx -> {
                            String serverName = StringArgumentType.getString(ctx, "serveur");
                            requirePlayer(ctx.getSource(), viewer -> listPlayers(viewer, serverName, server, vanishManager));
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    private interface PlayerAction {
        void run(Player player);
    }

    private static void requirePlayer(CommandSource source, PlayerAction action) {
        if (source instanceof Player player) {
            action.run(player);
        } else {
            source.sendMessage(Component.text("Seuls les joueurs peuvent utiliser cette commande.", NamedTextColor.RED));
        }
    }

    private static void listServers(Player viewer, ProxyServer server) {
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

    private static void listPlayers(Player viewer, String serverName, ProxyServer server, VanishManager vanishManager) {
        var opt = server.getServer(serverName);
        if (opt.isEmpty()) {
            viewer.sendMessage(Component.text("Serveur inconnu : " + serverName, NamedTextColor.RED));
            return;
        }
        RegisteredServer rs = opt.get();
        Collection<Player> players = rs.getPlayersConnected();

        viewer.sendMessage(Component.text("──── Joueurs sur " + serverName + " ────", NamedTextColor.GOLD, TextDecoration.BOLD));
        boolean canSeeVanished = viewer.hasPermission("veloadmin.vanish.see");

        boolean any = false;
        for (Player p : players) {
            if (vanishManager.isVanished(p.getUniqueId()) && !canSeeVanished) continue;
            any = true;

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

        if (!any) {
            viewer.sendMessage(Component.text("Aucun joueur en ligne sur ce serveur.", NamedTextColor.GRAY));
        }
    }
}
