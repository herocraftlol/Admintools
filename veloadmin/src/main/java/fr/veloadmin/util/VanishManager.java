package fr.veloadmin.util;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks who is currently vanished, proxy-side. Actual in-game invisibility
 * is applied by the VeloAdminBridge plugin on the backend server, notified
 * via the plugin messaging channel.
 */
public class VanishManager {

    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    public boolean toggle(UUID uuid) {
        if (vanished.contains(uuid)) {
            vanished.remove(uuid);
            return false;
        } else {
            vanished.add(uuid);
            return true;
        }
    }

    public Set<UUID> getVanished() {
        return Collections.unmodifiableSet(vanished);
    }
}
