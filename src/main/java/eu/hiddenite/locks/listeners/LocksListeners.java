package eu.hiddenite.locks.listeners;

import eu.hiddenite.locks.LocksPlugin;
import eu.hiddenite.locks.utils.LocksStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocksListeners implements Listener {
    private final LocksPlugin plugin;
    private final LocksStorage storage;

    public LocksListeners(LocksPlugin plugin, LocksStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlaceMonitor(final BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!(block.getState() instanceof Container)) {
            return;
        }
        storage.setContainerOwner(block, player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (block.getType() != Material.CHEST) {
            return;
        }

        if (!storage.isContainerLocked(block)) {
            return;
        }

        UUID owner = storage.getContainerOwner(block);
        if (!event.getPlayer().getUniqueId().equals(owner)) {
            event.setCancelled(true);
            sendChestLockedMessage(player, block, owner);
        } else {
            plugin.sendMessage(player, "unlock-success");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CHEST) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (!storage.isContainerLocked(block)) {
            return;
        }

        UUID owner = storage.getContainerOwner(block);
        if (!event.getPlayer().getUniqueId().equals(owner)) {
            event.setCancelled(true);
            sendChestLockedMessage(player, block, owner);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(final InventoryMoveItemEvent event) {
        if (event.getSource().getType() != InventoryType.CHEST) {
            return;
        }
        if (event.getDestination().getType() != InventoryType.HOPPER) {
            return;
        }
        if (!(event.getSource().getHolder() instanceof Chest)) {
            return;
        }

        Chest chest = (Chest)event.getSource().getHolder();
        if (storage.isContainerLocked(chest)) {
            event.setCancelled(true);
            Hopper hopper = (Hopper)event.getDestination().getHolder();
            UUID chestOwner = storage.getContainerOwner(chest);
            UUID hopperOwner = storage.getContainerOwner(hopper);
            if (chestOwner != null && chestOwner.equals(hopperOwner)) {
                event.setCancelled(false);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        preventLockedChestExplosion(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        preventLockedChestExplosion(event.blockList());
    }

    private void preventLockedChestExplosion(List<Block> affectedBlocks)  {
        List<Block> safeBlocks = new ArrayList<>();
        for (Block block : affectedBlocks) {
            if (block.getType() == Material.CHEST && storage.isContainerLocked(block)) {
                safeBlocks.add(block);
            }
        }
        for (Block block : safeBlocks) {
            affectedBlocks.remove(block);
        }
    }

    private void sendChestLockedMessage(Player player, Block block, UUID ownerId) {
        if (ownerId == null) {
            plugin.getLogger().warning("Chest locked without owner: " + block.getLocation());
            return;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
        if (owner.getName() == null) {
            plugin.getLogger().warning("Owner without name: " + owner.getUniqueId());
            return;
        }
        plugin.sendMessage(player, "chest-locked", "{NAME}", owner.getName());
    }
}
