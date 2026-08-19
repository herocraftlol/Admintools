package fr.veloadmin.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.veloadmin.VeloAdminPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

/**
 * /tpto <joueur> — teleports the executor to the target, connecting to the
 * target's server first if needed, then asking the VeloAdminBridge plugin on
 * that server to perform the actual in-world teleport once both players are
 * present.
 */
public class TpToCommand implements SimpleCommand {

    private final ProxyServer server;
    private final VeloAdminPlugin plugin;

    public TpToCommand(ProxyServer server, VeloAdminPlugin plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        if (!(source instanceof Player executor)) {
            source.sendMessage(Component.text("Seuls les joueurs peuvent utiliser cette commande.", NamedTextColor.RED));
            return;
        }
        String[] args = invocation.arguments();
        if (args.length < 1) {
            executor.sendMessage(Component.text("Usage: /tpto <joueur>", NamedTextColor.RED));
            return;
        }

        Optional<Player> targetOpt = server.getPlayer(args[0]);
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
                // Give the backend a moment to fully spawn the player before teleporting.
                server.getScheduler().buildTask(plugin, () -> sendTeleportRequest(executor, target, targetServer))
                        .delay(java.time.Duration.ofMillis(500))
                        .schedule();
                executor.sendMessage(Component.text("Connexion à " + targetServer.getServerInfo().getName()
                        + " puis téléportation vers " + target.getUsername() + "...", NamedTextColor.GREEN));
            } else {
                executor.sendMessage(Component.text("Impossible de rejoindre le serveur de " + target.getUsername()
                        + " : " + result.getReasonComponent().map(Object::toString).orElse("raison inconnue"), NamedTextColor.RED));
            }
        });
    }

    private void sendTeleportRequest(Player executor, Player target, RegisteredServer targetServer) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("TP");
        out.writeUTF(executor.getUniqueId().toString());
        out.writeUTF(target.getUniqueId().toString());
        targetServer.sendPluginMessage(VeloAdminPlugin.CHANNEL, out.toByteArray());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("veloadmin.tpgui");
    }
}
