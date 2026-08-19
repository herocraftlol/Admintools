package fr.veloadmin.commands;

import com.velocitypowered.api.command.SimpleCommand;
import fr.veloadmin.storage.Database;
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
 * /reports            -> list latest reports (unverified first)
 * /reports all        -> list including verified ones
 * /reports verify <id>-> toggle verified state on a report
 */
public class ReportsAdminCommand implements SimpleCommand {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm")
            .withZone(ZoneId.systemDefault());

    private final Database database;

    public ReportsAdminCommand(Database database) {
        this.database = database;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length >= 2 && args[0].equalsIgnoreCase("verify")) {
            try {
                int id = Integer.parseInt(args[1]);
                boolean ok = database.toggleReportVerified(id);
                if (ok) {
                    source.sendMessage(Component.text("Report #" + id + " mis à jour.", NamedTextColor.GREEN));
                } else {
                    source.sendMessage(Component.text("Report #" + id + " introuvable.", NamedTextColor.RED));
                }
            } catch (NumberFormatException e) {
                source.sendMessage(Component.text("ID invalide.", NamedTextColor.RED));
            }
            return;
        }

        boolean showAll = args.length >= 1 && args[0].equalsIgnoreCase("all");
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

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("veloadmin.admin.reports");
    }
}
