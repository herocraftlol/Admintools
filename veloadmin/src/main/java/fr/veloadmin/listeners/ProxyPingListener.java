package fr.veloadmin.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import fr.veloadmin.util.VanishManager;

import java.util.ArrayList;
import java.util.List;

/** Removes vanished players from the online count and player sample shown on the server list (MOTD). */
public class ProxyPingListener {

    private final ProxyServer server;
    private final VanishManager vanishManager;

    public ProxyPingListener(ProxyServer server, VanishManager vanishManager) {
        this.server = server;
        this.vanishManager = vanishManager;
    }

    @Subscribe
    public void onPing(ProxyPingEvent event) {
        if (vanishManager.getVanished().isEmpty()) return;

        ServerPing original = event.getPing();
        ServerPing.Players originalPlayers = original.getPlayers().orElse(null);
        if (originalPlayers == null) return;

        int hiddenCount = (int) server.getAllPlayers().stream()
                .filter(p -> vanishManager.isVanished(p.getUniqueId()))
                .count();

        List<ServerPing.SamplePlayer> filteredSample = new ArrayList<>();
        for (ServerPing.SamplePlayer sample : originalPlayers.getSample()) {
            boolean isVanished = server.getAllPlayers().stream()
                    .filter(p -> p.getUniqueId().equals(sample.getId()))
                    .anyMatch(p -> vanishManager.isVanished(p.getUniqueId()));
            if (!isVanished) filteredSample.add(sample);
        }

        ServerPing.Players newPlayers = new ServerPing.Players(
                Math.max(originalPlayers.getOnline() - hiddenCount, 0),
                originalPlayers.getMax(),
                filteredSample
        );

        event.setPing(original.asBuilder().onlinePlayers(newPlayers.getOnline())
                .maximumPlayers(newPlayers.getMax())
                .clearSamplePlayers()
                .samplePlayers(filteredSample.toArray(new ServerPing.SamplePlayer[0]))
                .build());
    }
}
