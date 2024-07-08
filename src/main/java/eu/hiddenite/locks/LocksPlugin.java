package eu.hiddenite.locks;

import eu.hiddenite.locks.commands.LockCommand;
import eu.hiddenite.locks.commands.UnlockCommand;
import eu.hiddenite.locks.listeners.LocksListeners;
import eu.hiddenite.locks.utils.LocksStorage;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocksPlugin extends JavaPlugin {
    private final LocksStorage storage = new LocksStorage(this);

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginCommand lockCommand = getCommand("lock");
        if (lockCommand != null) {
            lockCommand.setExecutor(new LockCommand(this));
        }

        PluginCommand unlockCommand = getCommand("unlock");
        if (unlockCommand != null) {
            unlockCommand.setExecutor(new UnlockCommand(this));
        }

        getServer().getPluginManager().registerEvents(new LocksListeners(this, storage), this);
    }

    public void sendMessage(Player player, String configPath, Object... parameters) {
        String message = getConfig().getString("messages." + configPath);
        if (message != null) {
            for (int i = 0; i < parameters.length - 1; i += 2) {
                message = message.replace(parameters[i].toString(), parameters[i + 1].toString());
            }
            player.sendMessage(MiniMessage.miniMessage().deserialize(message));
        }
    }

    public OfflinePlayer findExistingPlayer(String name) {
        OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
        for (OfflinePlayer player : allPlayers) {
            if (player.getName() != null && player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    public boolean isLockable(Block block) {
        return block.getState() instanceof Container && (getConfig().getStringList("inventory-lockable").contains(block.getType().toString()) || block.getType() == Material.CHEST);
    }

    public String getSupportedConfigPath(String oldPath, String newPath) {
        if (getConfig().getString("messages." + newPath) != null) return newPath;
        else return oldPath;
    }

    public void lockContainer(Player player, Block block) {
        boolean alreadyLocked = storage.isContainerLocked(block);
        UUID owner = storage.getContainerOwner(block);

        if (!player.getUniqueId().equals(owner)) {
            sendMessage(player, "error-not-owner");
            return;
        }
        if (alreadyLocked) {
            sendMessage(player, "error-already-locked");
            return;
        }

        String blockName;
        if (block.getType() == Material.CHEST) {
            Chest chest = (Chest)block.getState();
            Chest[] chestSides = getChestSides(chest);
            for (Chest side : chestSides) {
                UUID sideOwner = storage.getContainerOwner(side.getBlock());
                if (!player.getUniqueId().equals(sideOwner)) {
                    sendMessage(player, "error-not-owner-side");
                    return;
                }
            }
            for (Chest side : chestSides) {
                storage.lockContainer(side.getBlock());
            }
            blockName = chestSides.length == 2 ? "double-chest" : "chest";
        } else {
            storage.lockContainer(block);
            blockName = block.getType().toString();
        }

        getLogger().info(String.format(
                "Player %s locked the %s %s:[%d, %d, %d].",
                player.getName(),
                blockName,
                player.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ()));

        sendMessage(player, "lock-success");
    }

    public void unlockContainer(Player player, Block block) {
        if (invalidOwnerPermissions(player, block)) {
            return;
        }

        String blockName;
        if (block.getType() == Material.CHEST) {
            Chest chest = (Chest)block.getState();
            Chest[] chestSides = getChestSides(chest);
            for (Chest side : chestSides) {
                storage.unlockContainer(side.getBlock());
            }
            blockName = chestSides.length == 2 ? "double-chest" : "chest";
        } else {
            storage.unlockContainer(block);
            blockName = block.getType().toString();
        }

        getLogger().info(String.format(
                "Player %s unlocked the %s %s:[%d, %d, %d].",
                player.getName(),
                blockName,
                player.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ()));

        sendMessage(player, "unlock-success");
    }

    public void addPlayerToLock(Player owner, OfflinePlayer target, Block block) {
        if (invalidOwnerPermissions(owner, block)) {
            return;
        }

        if (owner.getUniqueId().equals(target.getUniqueId())) {
            sendMessage(owner, "error-already-has-access", "{NAME}", target.getName());
            return;
        }

        List<UUID> allowedUsers = storage.getContainerUsers(block);
        if (allowedUsers.contains(target.getUniqueId())) {
            sendMessage(owner, "error-already-has-access", "{NAME}", target.getName());
            return;
        }

        allowedUsers.add(target.getUniqueId());

        if (block.getType() == Material.CHEST) {
            Chest chest = (Chest)block.getState();
            Chest[] chestSides = getChestSides(chest);
            for (Chest side : chestSides) {
                storage.setContainerUsers(side.getBlock(), allowedUsers);
            }
        } else storage.setContainerUsers(block, allowedUsers);

        sendMessage(owner, "lock-add-success", "{NAME}", target.getName());
    }

    public void removePlayerFromLock(Player owner, OfflinePlayer target, Block block) {
        if (invalidOwnerPermissions(owner, block)) {
            return;
        }

        if (owner.getUniqueId().equals(target.getUniqueId())) {
            return;
        }

        List<UUID> allowedUsers = storage.getContainerUsers(block);
        if (!allowedUsers.remove(target.getUniqueId())) {
            sendMessage(owner, "error-already-has-no-access", "{NAME}", target.getName());
            return;
        }

        if (block.getType() == Material.CHEST) {
            Chest chest = (Chest)block.getState();
            Chest[] chestSides = getChestSides(chest);
            for (Chest side : chestSides) {
                storage.setContainerUsers(side.getBlock(), allowedUsers);
            }
        } else storage.setContainerUsers(block, allowedUsers);

        sendMessage(owner, "lock-remove-success", "{NAME}", target.getName());
    }

    public void listPlayersAllowedToAccess(Player player, Block block) {
        if (invalidOwnerPermissions(player, block)) {
            return;
        }

        List<UUID> allowedUsers = storage.getContainerUsers(block);
        List<String> allowedUsersNames = new ArrayList<>();

        UUID ownerId = storage.getContainerOwner(block);
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
        allowedUsersNames.add(owner.getName());

        allowedUsers.forEach(user -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(user);
            allowedUsersNames.add(offlinePlayer.getName());
        });

        sendMessage(player, "lock-list-success", "{PLAYERS}", String.join(", ", allowedUsersNames));
    }

    private boolean invalidOwnerPermissions(Player owner, Block block) {
        boolean alreadyLocked = storage.isContainerLocked(block);
        UUID ownerId = storage.getContainerOwner(block);

        if (!owner.getUniqueId().equals(ownerId) && !owner.hasPermission("hiddenite.locks.bypass")) {
            sendMessage(owner, "error-not-owner");
            return true;
        }
        if (!alreadyLocked) {
            sendMessage(owner, "error-not-locked");
            return true;
        }
        return false;
    }

    private Chest[] getChestSides(Chest chest) {
        boolean isDoubleChest = chest.getInventory() instanceof DoubleChestInventory;
        if (isDoubleChest) {
            DoubleChest doubleChest = (DoubleChest)chest.getInventory().getHolder();
            if (doubleChest == null) {
                getLogger().warning("Chest has a double inventory but no holder?");
                return new Chest[] { chest };
            }
            Chest leftSide = (Chest)doubleChest.getLeftSide();
            Chest rightSide = (Chest)doubleChest.getRightSide();
            if (leftSide == null || rightSide == null) {
                getLogger().warning("DoubleChest is missing a side?");
                return new Chest[] { chest };
            }
            return new Chest[] { leftSide, rightSide };
        } else {
            return new Chest[] { chest };
        }
    }
}
