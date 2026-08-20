package fr.veloadmin.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.concurrent.CompletableFuture;

public final class SuggestionsUtil {

    private SuggestionsUtil() {}

    /** Suggests online player names, hiding vanished players from anyone without the bypass permission. */
    public static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestPlayers(
            CommandContext<CommandSource> ctx, SuggestionsBuilder builder, ProxyServer server, VanishManager vanishManager) {

        CommandSource source = ctx.getSource();
        boolean canSeeVanished = source.hasPermission("veloadmin.vanish.see");

        String remaining = builder.getRemaining().toLowerCase();
        for (Player p : server.getAllPlayers()) {
            if (vanishManager.isVanished(p.getUniqueId()) && !canSeeVanished) continue;
            if (p.getUsername().toLowerCase().startsWith(remaining)) {
                builder.suggest(p.getUsername());
            }
        }
        return builder.buildFuture();
    }

    /** Suggests known backend server names, optionally including "ALL". */
    public static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestServers(
            SuggestionsBuilder builder, ProxyServer server, boolean includeAll) {

        String remaining = builder.getRemaining().toLowerCase();
        if (includeAll && "all".startsWith(remaining)) {
            builder.suggest("ALL");
        }
        for (RegisteredServer rs : server.getAllServers()) {
            String name = rs.getServerInfo().getName();
            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }
}
