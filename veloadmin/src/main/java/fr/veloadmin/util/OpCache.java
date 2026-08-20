package fr.veloadmin.util;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks whether an online player is a server operator, as reported by the
 * VeloAdminBridge plugin running on the backend server they're connected to.
 * Velocity itself has no concept of "op" (that's a Bukkit/Paper thing), so
 * this is how we let real OPs get default access to admin commands.
 */
public class OpCache {

    private final ConcurrentHashMap<UUID, Boolean> opStatus = new ConcurrentHashMap<>();

    public void setOp(UUID uuid, boolean isOp) {
        if (isOp) {
            opStatus.put(uuid, true);
        } else {
            opStatus.remove(uuid);
        }
    }

    public boolean isOp(UUID uuid) {
        return opStatus.getOrDefault(uuid, false);
    }

    public void remove(UUID uuid) {
        opStatus.remove(uuid);
    }
}
