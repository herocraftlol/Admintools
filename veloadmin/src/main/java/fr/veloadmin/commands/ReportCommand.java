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
import fr.veloadmin.util.SuggestionsUtil;
import fr.veloadmin.util.VanishManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class ReportCommand {

    private ReportCommand() {}

    public static BrigadierCommand create(Database database, ProxyServer server, VanishManager vanishManager) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("report")
                // Ouvert à tout joueur par défaut (signaler quelqu'un ne doit pas nécessiter de perm spéciale).
                .requires(source -> source instanceof Player)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("joueur", StringArgumentType.word())
                        .suggests((ctx, builder) -> SuggestionsUtil.suggestPlayers(ctx, builder, server, vanishManager))
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("raison", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    Player reporter = (Player) ctx.getSource();
                                    String target = StringArgumentType.getString(ctx, "joueur");
                                    String reason = StringArgumentType.getString(ctx, "raison");

                                    if (target.equalsIgnoreCase(reporter.getUsername())) {
                                        reporter.sendMessage(Component.text("Tu ne peux pas te reporter toi-même.", NamedTextColor.RED));
                                        return 0;
                                    }

                                    database.addReport(reporter.getUsername(), target, reason);
                                    reporter.sendMessage(Component.text("Ton report contre " + target + " a été envoyé aux admins. Merci !", NamedTextColor.GREEN));
                                    return 1;
                                })
                        )
                )
                .build();

        return new BrigadierCommand(node);
    }
}
