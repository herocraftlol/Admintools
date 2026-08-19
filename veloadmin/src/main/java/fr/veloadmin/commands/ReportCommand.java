package fr.veloadmin.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.veloadmin.storage.Database;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReportCommand implements SimpleCommand {

    private final Database database;

    public ReportCommand(Database database) {
        this.database = database;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player reporter)) {
            source.sendMessage(Component.text("Seuls les joueurs peuvent utiliser cette commande.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            reporter.sendMessage(Component.text("Usage: /report <joueur> <raison>", NamedTextColor.RED));
            return;
        }

        String target = args[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        if (target.equalsIgnoreCase(reporter.getUsername())) {
            reporter.sendMessage(Component.text("Tu ne peux pas te reporter toi-même.", NamedTextColor.RED));
            return;
        }

        database.addReport(reporter.getUsername(), target, reason);
        reporter.sendMessage(Component.text("Ton report contre " + target + " a été envoyé aux admins. Merci !", NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("veloadmin.report") || invocation.source() instanceof Player;
    }
}
