package eu.hiddenite.locks.listeners;

import eu.hiddenite.locks.LocksPlugin;
import eu.hiddenite.locks.utils.LocksStorage;
import io.papermc.paper.event.entity.ItemTransportingEntityValidateTargetEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;

import java.util.ArrayList;
import java.util.Collection;
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCreatureSpawnMonitor(final CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BUILD_COPPERGOLEM) {
            return;
        }

        Collection<Player> builders = event.getLocation().getNearbyPlayers(3);
        double minDistance = 10.0;
        Player bestPlayer = null;
        for (Player player : builders) {
            double distance = player.getLocation().distance(event.getLocation());
            if (distance < minDistance) {
                minDistance = distance;
                bestPlayer = player;
            }
        }

        if (bestPlayer != null) {
            plugin.getLogger().info("Player " + bestPlayer.getName() + " probably summoned a copper golem at " + event.getLocation());
            storage.setEntityOwner(event.getEntity(), bestPlayer.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!LocksPlugin.isChestMaterial(block.getType())) {
            return;
        }

        boolean mightCreateDoubleChest = isBlockSingleLockedChest(block.getRelative(BlockFace.NORTH)) ||
                isBlockSingleLockedChest(block.getRelative(BlockFace.SOUTH)) ||
                isBlockSingleLockedChest(block.getRelative(BlockFace.WEST)) ||
                isBlockSingleLockedChest(block.getRelative(BlockFace.EAST));

        if (mightCreateDoubleChest) {
            event.setCancelled(true);
            plugin.sendMessage(player, "chest-nearby-locked");
        }
    }

    private boolean isBlockSingleLockedChest(Block block) {
        if (!LocksPlugin.isChestMaterial(block.getType())) {
            return false;
        }
        Chest chest = (Chest)block.getState();
        if (chest.getInventory() instanceof DoubleChestInventory) {
            return false;
        }
        return storage.isContainerLocked(block);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!plugin.isLockable(block)) {
            return;
        }

        if (!storage.isContainerLocked(block)) {
            return;
        }

        UUID owner = storage.getContainerOwner(block);
        if (!event.getPlayer().getUniqueId().equals(owner) && !player.hasPermission("hiddenite.locks.bypass")) {
            event.setCancelled(true);
            sendContainerLockedMessage(player, block, owner);
        } else {
            Container container = (Container)block.getState();
            if (!(container.getInventory() instanceof DoubleChestInventory)) {
                plugin.sendMessage(player, "unlock-success");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null || !plugin.isLockable(event.getClickedBlock())) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (!storage.isContainerLocked(block)) {
            return;
        }

        UUID owner = storage.getContainerOwner(block);
        if (!event.getPlayer().getUniqueId().equals(owner)) {
            List<UUID> users = storage.getContainerUsers(block);
            if (!users.contains(player.getUniqueId())) {
                sendContainerLockedMessage(player, block, owner);
                if (!player.hasPermission("hiddenite.locks.bypass")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemTransportingEntityValidateTarget(final ItemTransportingEntityValidateTargetEvent event) {
        Block block = event.getBlock();
        if (!plugin.isLockable(block)) {
            return;
        }
        if (!storage.isContainerLocked(block)) {
            return;
        }
        UUID entityOwner = storage.getEntityOwner(event.getEntity());
        if (entityOwner == null) {
            event.setAllowed(false);
            return;
        }

        UUID containerOwner = storage.getContainerOwner(block);
        List<UUID> containerUsers = storage.getContainerUsers(block);
        if (!entityOwner.equals(containerOwner) && !containerUsers.contains(entityOwner)) {
            event.setAllowed(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(final InventoryMoveItemEvent event) {
        if (event.getDestination().getType() != InventoryType.HOPPER) {
            return;
        }

        Container container = null;
        if (event.getSource().getHolder() instanceof Container) {
            container = (Container)event.getSource().getHolder();
        } else if (event.getSource().getHolder() instanceof DoubleChest) {
            DoubleChest doubleChest = (DoubleChest)event.getSource().getHolder();
            container = (Container)doubleChest.getLeftSide();
        }

        if (container != null && storage.isContainerLocked(container)) {
            event.setCancelled(true);
            if (event.getDestination().getHolder() instanceof Hopper) {
                Hopper hopper = (Hopper)event.getDestination().getHolder();
                UUID chestOwner = storage.getContainerOwner(container);
                UUID hopperOwner = storage.getContainerOwner(hopper);
                if (chestOwner != null && chestOwner.equals(hopperOwner)) {
                    event.setCancelled(false);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        preventLockedContainerExplosion(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        preventLockedContainerExplosion(event.blockList());
    }

    private void preventLockedContainerExplosion(List<Block> affectedBlocks)  {
        List<Block> safeBlocks = new ArrayList<>();
        for (Block block : affectedBlocks) {
            if (plugin.isLockable(block) && storage.isContainerLocked(block)) {
                safeBlocks.add(block);
            }
        }
        for (Block block : safeBlocks) {
            affectedBlocks.remove(block);
        }
    }

    private void sendContainerLockedMessage(Player player, Block block, UUID ownerId) {
        if (ownerId == null) {
            plugin.getLogger().warning("Chest locked without owner: " + block.getLocation());
            return;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
        if (owner.getName() == null) {
            plugin.getLogger().warning("Owner without name: " + owner.getUniqueId());
            return;
        }
        plugin.sendMessage(player, "container-locked", "{NAME}", owner.getName());
    }
}
