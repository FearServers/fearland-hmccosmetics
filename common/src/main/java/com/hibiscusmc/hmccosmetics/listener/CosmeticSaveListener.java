package com.hibiscusmc.hmccosmetics.listener;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.api.events.PlayerCosmeticPostEquipEvent;
import com.hibiscusmc.hmccosmetics.api.events.PlayerCosmeticRemoveEvent;
import com.hibiscusmc.hmccosmetics.database.Database;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Saves a user one tick after equipping, removing or dyeing a cosmetic (several changes in a tick make one save),
 * so a server switch right after a change does not lose it. Users that are not registered yet (still loading)
 * or no longer registered (quitting) are skipped: the load must not be written back and quit saves by itself.
 */
public class CosmeticSaveListener implements Listener {

    private final Set<UUID> pending = new HashSet<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEquip(PlayerCosmeticPostEquipEvent event) {
        schedule(event.getUser());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemove(PlayerCosmeticRemoveEvent event) {
        schedule(event.getUser());
    }

    private void schedule(CosmeticUser user) {
        if (CosmeticUsers.getUser(user.getUniqueId()) != user || !pending.add(user.getUniqueId())) return;
        Bukkit.getScheduler().runTask(HMCCosmeticsPlugin.getInstance(), () -> {
            pending.remove(user.getUniqueId());
            if (CosmeticUsers.getUser(user.getUniqueId()) == user) Database.save(user);
        });
    }
}
