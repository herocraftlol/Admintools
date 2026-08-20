package fr.veloadmin.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabListEntry;

/**
 * Applies the "invisible on the network" side effects that Velocity itself
 * can control: removing/adding the player from every other player's tab
 * list, network-wide (not just their current server).
 */
public final class VanishTabListUtil {

    private VanishTabListUtil() {}

    public static void hideFromEveryone(ProxyServer server, Player vanished) {
        for (Player viewer : server.getAllPlayers()) {
            if (viewer.equals(vanished)) continue;
            if (viewer.hasPermission("veloadmin.vanish.see")) continue;
            viewer.getTabList().removeEntry(vanished.getUniqueId());
        }
    }

    public static void showToEveryone(ProxyServer server, Player unvanished) {
        for (Player viewer : server.getAllPlayers()) {
            if (viewer.equals(unvanished)) continue;
            addEntry(viewer, unvanished);
        }
    }

    /** Called when a new player joins the proxy, to hide already-vanished players from them. */
    public static void applyCurrentVanishState(ProxyServer server, VanishManager vanishManager, Player newViewer) {
        if (newViewer.hasPermission("veloadmin.vanish.see")) return;
        for (var uuid : vanishManager.getVanished()) {
            newViewer.getTabList().removeEntry(uuid);
        }
    }

    private static void addEntry(Player viewer, Player target) {
        if (viewer.getTabList().getEntry(target.getUniqueId()).isPresent()) return;
        TabListEntry entry = TabListEntry.builder()
                .tabList(viewer.getTabList())
                .profile(target.getGameProfile())
                .displayName(null)
                .latency((int) viewer.getPing())
                .gameMode(0)
                .listed(true)
                .build();
        viewer.getTabList().addEntry(entry);
    }
}
