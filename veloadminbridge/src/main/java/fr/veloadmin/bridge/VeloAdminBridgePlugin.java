package fr.veloadmin.bridge;

import org.bukkit.plugin.java.JavaPlugin;

public class VeloAdminBridgePlugin extends JavaPlugin {

    public static final String CHANNEL = "veloadmin:main";

    private final java.util.Set<java.util.UUID> vanished = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, new BridgeMessageListener(this));
        getServer().getPluginManager().registerEvents(new VanishJoinListener(this), this);

        // Overrides vanilla /list to filter out vanished players (declared in plugin.yml).
        var listCommand = getCommand("list");
        if (listCommand != null) {
            listCommand.setExecutor(new ListCommand(this));
        }

        // Pushes real OP status to the proxy: on join, and periodically to catch /op /deop changes.
        getServer().getPluginManager().registerEvents(new OpSyncListener(this), this);
        getServer().getScheduler().runTaskTimer(this, this::syncAllOpStatus, 100L, 200L);

        getLogger().info("VeloAdminBridge activé sur ce serveur.");
    }

    public void syncAllOpStatus() {
        for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
            OpSyncListener.sendOpStatus(this, player);
        }
    }

    public java.util.Set<java.util.UUID> getVanished() {
        return vanished;
    }
}
