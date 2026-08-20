package fr.veloadmin.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import fr.veloadmin.storage.Database;
import fr.veloadmin.util.OpCache;
import fr.veloadmin.util.PermissionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Chat-based "GUI" for report management (Velocity has no inventory access).
 * /reports                -> list latest unverified reports
 * /reports all            -> list including verified ones
 * /reports verify <id>    -> toggle verified state on a report
 */
public final class ReportsAdminCommand {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm")
            .withZone(ZoneId.systemDefault());

    private ReportsAdminCommand() {}

    public static BrigadierCommand create(Database database, OpCache opCache) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("reports")
                .requires(source -> PermissionUtil.has(source, "veloadmin.admin.reports", opCache))
                .executes(ctx -> {
                    list(ctx.getSource(), database, false);
                    return 1;
                })
                .then(LiteralArgumentBuilder.<CommandSource>literal("all")
                        .executes(ctx -> {
                            list(ctx.getSource(), database, true);
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("verify")
                        .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("id", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int id = IntegerArgumentType.getInteger(ctx, "id");
                                    boolean ok = database.toggleReportVerified(id);
                                    CommandSource source = ctx.getSource();
                                    if (ok) {
                                        source.sendMessage(Component.text("Report #" + id + " mis à jour.", NamedTextColor.GREEN));
                                    } else {
                                        source.sendMessage(Component.text("Report #" + id + " introuvable.", NamedTextColor.RED));
                                    }
                                    return 1;
                                })
                        )
                )
                .build();

        return new BrigadierCommand(node);
    }

    private static void list(CommandSource source, Database database, boolean showAll) {
        List<Database.ReportEntry> reports = database.getReports(!showAll);

        source.sendMessage(Component.text("──── Reports " + (showAll ? "(tous)" : "(non vérifiés)") + " ────", NamedTextColor.GOLD, TextDecoration.BOLD));

        if (reports.isEmpty()) {
            source.sendMessage(Component.text("Aucun report à afficher.", NamedTextColor.GRAY));
            return;
        }

        for (Database.ReportEntry r : reports) {
            Component status = r.verified()
                    ? Component.text("[✔ Vérifié]", NamedTextColor.GREEN)
                    : Component.text("[✘ Non vérifié]", NamedTextColor.RED);

            Component toggle = Component.text(" [Basculer]", NamedTextColor.YELLOW, TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.runCommand("/reports verify " + r.id()))
                    .hoverEvent(HoverEvent.showText(Component.text("Cliquer pour marquer comme "
                            + (r.verified() ? "non vérifié" : "vérifié"))));

            Component line = Component.text("#" + r.id() + " ", NamedTextColor.GRAY)
                    .append(Component.text(r.reporter(), NamedTextColor.AQUA))
                    .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(r.reported(), NamedTextColor.AQUA))
                    .append(Component.text(" : " + r.reason(), NamedTextColor.WHITE))
                    .append(Component.text("  " + FORMAT.format(Instant.ofEpochMilli(r.timestamp())), NamedTextColor.DARK_GRAY))
                    .append(Component.text("  "))
                    .append(status)
                    .append(toggle);

            source.sendMessage(line);
        }
    }
}
