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
        getLogger().info("VeloAdminBridge activé sur ce serveur.");
    }

    public java.util.Set<java.util.UUID> getVanished() {
        return vanished;
    }
}
