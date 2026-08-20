package fr.veloadmin.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.veloadmin.VeloAdminPlugin;
import fr.veloadmin.util.OpCache;
import fr.veloadmin.util.PermissionUtil;
import fr.veloadmin.util.VanishManager;
import fr.veloadmin.util.VanishTabListUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class VanishCommand {

    private VanishCommand() {}

    public static BrigadierCommand create(ProxyServer server, VanishManager vanishManager, OpCache opCache, VeloAdminPlugin plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("vanish")
                .requires(source -> PermissionUtil.has(source, "veloadmin.vanish", opCache))
                .executes(ctx -> {
                    CommandSource source = ctx.getSource();
                    if (!(source instanceof Player player)) {
                        source.sendMessage(Component.text("Seuls les joueurs peuvent utiliser cette commande.", NamedTextColor.RED));
                        return 0;
                    }

                    boolean nowVanished = vanishManager.toggle(player.getUniqueId());

                    // Tell the backend server the player is currently on to apply real in-game vanish
                    // (invisibility, hidePlayer, join/quit message suppression, /list filtering...).
                    player.getCurrentServer().ifPresent(sc -> {
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("VANISH");
                        out.writeUTF(player.getUniqueId().toString());
                        out.writeBoolean(nowVanished);
                        sc.getServer().sendPluginMessage(VeloAdminPlugin.CHANNEL, out.toByteArray());
                    });

                    // Network-wide tab list + server ping player count (handled in ProxyPingListener).
                    if (nowVanished) {
                        VanishTabListUtil.hideFromEveryone(server, player);
                        player.sendMessage(Component.text("Tu es maintenant invisible sur tout le réseau (vanish activé).", NamedTextColor.GREEN));
                    } else {
                        VanishTabListUtil.showToEveryone(server, player);
                        player.sendMessage(Component.text("Tu es maintenant visible (vanish désactivé).", NamedTextColor.YELLOW));
                    }
                    return 1;
                })
                .build();

        return new BrigadierCommand(node);
    }
}
