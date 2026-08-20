package fr.veloadmin.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.veloadmin.VeloAdminPlugin;
import fr.veloadmin.util.OpCache;
import fr.veloadmin.util.PermissionUtil;
import fr.veloadmin.util.SuggestionsUtil;
import fr.veloadmin.util.VanishManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Duration;
import java.util.Optional;

/**
 * /tpto <joueur> — teleports the executor to the target, connecting to the
 * target's server first if needed, then asking the VeloAdminBridge plugin on
 * that server to perform the actual in-world teleport once both players are
 * present.
 */
public final class TpToCommand {

    private TpToCommand() {}

    public static BrigadierCommand create(ProxyServer server, VeloAdminPlugin plugin, VanishManager vanishManager, OpCache opCache) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("tpto")
                .requires(source -> PermissionUtil.has(source, "veloadmin.tpgui", opCache))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("joueur", StringArgumentType.word())
                        .suggests((ctx, builder) -> SuggestionsUtil.suggestPlayers(ctx, builder, server, vanishManager))
                        .executes(ctx -> {
                            CommandSource source = ctx.getSource();
                            if (!(source instanceof Player executor)) {
                                source.sendMessage(Component.text("Seuls les joueurs peuvent utiliser cette commande.", NamedTextColor.RED));
                                return 0;
                            }
                            String targetName = StringArgumentType.getString(ctx, "joueur");
                            teleport(server, plugin, executor, targetName);
                            return 1;
                        })
                )
                .build();

        return new BrigadierCommand(node);
    }

    private static void teleport(ProxyServer server, VeloAdminPlugin plugin, Player executor, String targetName) {
        Optional<Player> targetOpt = server.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            executor.sendMessage(Component.text("Joueur introuvable ou hors ligne.", NamedTextColor.RED));
            return;
        }
        Player target = targetOpt.get();

        if (target.getUniqueId().equals(executor.getUniqueId())) {
            executor.sendMessage(Component.text("Tu ne peux pas te téléporter à toi-même.", NamedTextColor.RED));
            return;
        }

        Optional<RegisteredServer> targetServerOpt = target.getCurrentServer().map(sc -> sc.getServer());
        if (targetServerOpt.isEmpty()) {
            executor.sendMessage(Component.text("Ce joueur n'est connecté à aucun serveur.", NamedTextColor.RED));
            return;
        }
        RegisteredServer targetServer = targetServerOpt.get();

        boolean alreadyThere = executor.getCurrentServer()
                .map(sc -> sc.getServer().getServerInfo().getName().equals(targetServer.getServerInfo().getName()))
                .orElse(false);

        if (alreadyThere) {
            sendTeleportRequest(executor, target, targetServer);
            executor.sendMessage(Component.text("Téléportation vers " + target.getUsername() + " en cours...", NamedTextColor.GREEN));
            return;
        }

        executor.createConnectionRequest(targetServer).connect().thenAccept(result -> {
            if (result.isSuccessful()) {
                server.getScheduler().buildTask(plugin, () -> sendTeleportRequest(executor, target, targetServer))
                        .delay(Duration.ofMillis(500))
                        .schedule();
                executor.sendMessage(Component.text("Connexion à " + targetServer.getServerInfo().getName()
                        + " puis téléportation vers " + target.getUsername() + "...", NamedTextColor.GREEN));
            } else {
                executor.sendMessage(Component.text("Impossible de rejoindre le serveur de " + target.getUsername()
                        + " : " + result.getReasonComponent().map(Object::toString).orElse("raison inconnue"), NamedTextColor.RED));
            }
        });
    }

    private static void sendTeleportRequest(Player executor, Player target, RegisteredServer targetServer) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("TP");
        out.writeUTF(executor.getUniqueId().toString());
        out.writeUTF(target.getUniqueId().toString());
        targetServer.sendPluginMessage(VeloAdminPlugin.CHANNEL, out.toByteArray());
    }
}
