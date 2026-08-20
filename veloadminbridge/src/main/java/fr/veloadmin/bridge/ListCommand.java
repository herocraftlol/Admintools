package fr.veloadmin.bridge;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Replacement for vanilla /list that hides vanished players from those without the bypass permission. */
public class ListCommand implements CommandExecutor {

    private final VeloAdminBridgePlugin plugin;

    public ListCommand(VeloAdminBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean canSeeVanished = sender.hasPermission("veloadmin.vanish.see");

        List<String> visibleNames = new ArrayList<>();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            boolean isVanished = plugin.getVanished().contains(p.getUniqueId());
            if (isVanished && !canSeeVanished) continue;
            visibleNames.add(p.getName());
        }

        sender.sendMessage(ChatColor.YELLOW + "Joueurs en ligne (" + visibleNames.size() + "/"
                + plugin.getServer().getMaxPlayers() + ") : " + ChatColor.WHITE + String.join(", ", visibleNames));
        return true;
    }
}
