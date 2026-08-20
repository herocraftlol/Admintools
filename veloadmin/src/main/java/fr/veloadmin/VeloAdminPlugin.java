package fr.veloadmin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import fr.veloadmin.commands.ReportCommand;
import fr.veloadmin.commands.ReportsAdminCommand;
import fr.veloadmin.commands.TempBanCommand;
import fr.veloadmin.commands.TpGuiCommand;
import fr.veloadmin.commands.TpToCommand;
import fr.veloadmin.commands.VanishCommand;
import fr.veloadmin.listeners.ConnectionListener;
import fr.veloadmin.listeners.ProxyPingListener;
import fr.veloadmin.storage.Database;
import fr.veloadmin.util.OpCache;
import fr.veloadmin.util.VanishManager;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "veloadmin",
        name = "VeloAdmin",
        version = "1.3.0",
        description = "GUI teleport, vanish, /report et /tempban multi-serveurs",
        authors = {"herocraftlol"}
)
public class VeloAdminPlugin {

    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.create("veloadmin", "main");

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private Database database;
    private VanishManager vanishManager;
    private OpCache opCache;

    @Inject
    public VeloAdminPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.database = new Database(dataDirectory, logger);
        this.database.init();

        this.vanishManager = new VanishManager();
        this.opCache = new OpCache();

        // Plugin messaging channel used to talk to the backend "bridge" plugin
        server.getChannelRegistrar().register(CHANNEL);

        // Commands (Brigadier-based: proper argument types + tab-completion, no more red highlighting)
        var commandManager = server.getCommandManager();
        commandManager.register(ReportCommand.create(database, server, vanishManager));
        commandManager.register(ReportsAdminCommand.create(database, opCache));
        commandManager.register(TpGuiCommand.create(server, vanishManager, opCache));
        commandManager.register(TpToCommand.create(server, this, vanishManager, opCache));
        commandManager.register(VanishCommand.create(server, vanishManager, opCache, this));
        commandManager.register(TempBanCommand.create(database, server, vanishManager, opCache));

        // Listeners
        server.getEventManager().register(this, new ConnectionListener(database, vanishManager, opCache, server, this));
        server.getEventManager().register(this, new ProxyPingListener(server, vanishManager));

        logger.info("VeloAdmin chargé. N'oublie pas d'installer VeloAdminBridge sur chaque serveur backend !");
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (database != null) database.close();
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Database getDatabase() {
        return database;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public OpCache getOpCache() {
        return opCache;
    }
}
