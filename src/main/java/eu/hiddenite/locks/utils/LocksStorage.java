package eu.hiddenite.locks.utils;

import eu.hiddenite.locks.LocksPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class LocksStorage {
    private final NamespacedKey ownerNamespaceKey;
    private final NamespacedKey usersNamespaceKey;
    private final NamespacedKey lockedNamespaceKey;

    public LocksStorage(LocksPlugin plugin) {
        ownerNamespaceKey = new NamespacedKey(plugin, "owner");
        usersNamespaceKey = new NamespacedKey(plugin, "users");
        lockedNamespaceKey = new NamespacedKey(plugin, "locked");
    }

    public boolean isContainerLocked(Block block) {
        if (block == null || !(block.getState() instanceof Container)) {
            return false;
        }
        return isContainerLocked((Container)block.getState());
    }

    public boolean isContainerLocked(Container container) {
        if (container == null) {
            return false;
        }
        Byte lockedData = container.getPersistentDataContainer().get(lockedNamespaceKey, PersistentDataType.BYTE);
        if (lockedData == null) {
            return false;
        }
        return lockedData == 1;
    }

    public UUID getContainerOwner(Block block) {
        if (block == null || !(block.getState() instanceof Container)) {
            return null;
        }
        return getContainerOwner((Container)block.getState());
    }

    public UUID getContainerOwner(Container container) {
        if (container == null) {
            return null;
        }
        String ownerData = container.getPersistentDataContainer().get(ownerNamespaceKey, PersistentDataType.STRING);
        if (ownerData == null) {
            return null;
        }
        return UUID.fromString(ownerData);
    }

    public void setContainerOwner(Block block, UUID owner) {
        Container container = (Container)block.getState();
        container.getPersistentDataContainer().set(ownerNamespaceKey, PersistentDataType.STRING, owner.toString());
        container.update();
    }

    public void lockContainer(Block block) {
        Container container = (Container)block.getState();
        container.getPersistentDataContainer().set(lockedNamespaceKey, PersistentDataType.BYTE, (byte)1);
        container.update();
    }

    public void unlockContainer(Block block) {
        Container container = (Container)block.getState();
        container.getPersistentDataContainer().set(lockedNamespaceKey, PersistentDataType.BYTE, (byte)0);
        container.update();
    }

    public List<UUID> getContainerUsers(Block block) {
        if (block == null || !(block.getState() instanceof Container)) {
            return null;
        }
        return getContainerUsers((Container)block.getState());
    }

    public List<UUID> getContainerUsers(Container container) {
        if (container == null) {
            return new ArrayList<>();
        }
        String usersData = container.getPersistentDataContainer().get(usersNamespaceKey, PersistentDataType.STRING);
        if (usersData == null || usersData.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(usersData.split(";")).map(UUID::fromString).collect(Collectors.toList());
    }

    public void setContainerUsers(Block block, List<UUID> allowedUsers) {
        setContainerUsers((Container)block.getState(), allowedUsers);
    }

    public void setContainerUsers(Container container, List<UUID> allowedUsers) {
        String usersData = allowedUsers.stream().map(UUID::toString).collect(Collectors.joining(";"));
        container.getPersistentDataContainer().set(usersNamespaceKey, PersistentDataType.STRING, usersData);
        container.update();
    }
}
