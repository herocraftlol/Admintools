package fr.veloadmin.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.veloadmin.VeloAdminPlugin;
import fr.veloadmin.util.VanishManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class VanishCommand implements SimpleCommand {

    private final ProxyServer server;
    private final VanishManager vanishManager;
    private final VeloAdminPlugin plugin;

    public VanishCommand(ProxyServer server, VanishManager vanishManager, VeloAdminPlugin plugin) {
        this.server = server;
        this.vanishManager = vanishManager;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("Seuls les joueurs peuvent utiliser cette commande.", NamedTextColor.RED));
            return;
        }

        boolean nowVanished = vanishManager.toggle(player.getUniqueId());

        // Tell the backend server the player is currently on to apply real in-game vanish.
        player.getCurrentServer().ifPresent(sc -> {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("VANISH");
            out.writeUTF(player.getUniqueId().toString());
            out.writeBoolean(nowVanished);
            sc.getServer().sendPluginMessage(VeloAdminPlugin.CHANNEL, out.toByteArray());
        });

        if (nowVanished) {
            player.sendMessage(Component.text("Tu es maintenant invisible (vanish activé).", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Tu es maintenant visible (vanish désactivé).", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("veloadmin.vanish");
    }
}
