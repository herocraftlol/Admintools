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
import fr.veloadmin.storage.Database;
import fr.veloadmin.util.VanishManager;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "veloadmin",
        name = "VeloAdmin",
        version = "1.1.0",
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

        // Plugin messaging channel used to talk to the backend "bridge" plugin
        server.getChannelRegistrar().register(CHANNEL);

        // Commands
        var commandManager = server.getCommandManager();
        commandManager.register("report", new ReportCommand(database));
        commandManager.register(
                commandManager.metaBuilder("reports").aliases("reportsadmin").build(),
                new ReportsAdminCommand(database));
        commandManager.register("tpgui", new TpGuiCommand(server, vanishManager));
        commandManager.register("tpto", new TpToCommand(server, this));
        commandManager.register("vanish", new VanishCommand(server, vanishManager, this));
        commandManager.register("tempban", new TempBanCommand(database, server));

        // Listeners
        server.getEventManager().register(this, new ConnectionListener(database, vanishManager, this));

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
}
