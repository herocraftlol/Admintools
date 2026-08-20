package fr.veloadmin.util;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

/**
 * Centralizes permission checks: an explicit permission node always wins,
 * but if the source is a real server operator (status forwarded by
 * VeloAdminBridge from the backend they're on), access is granted too.
 * Console always has access, as usual on Velocity.
 */
public final class PermissionUtil {

    private PermissionUtil() {}

    public static boolean has(CommandSource source, String node, OpCache opCache) {
        if (!(source instanceof Player player)) {
            // Console / RCON etc.
            return true;
        }
        if (player.hasPermission(node)) {
            return true;
        }
        return opCache.isOp(player.getUniqueId());
    }
}
