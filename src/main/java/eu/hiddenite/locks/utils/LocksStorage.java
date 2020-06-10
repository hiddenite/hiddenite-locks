package eu.hiddenite.locks.utils;

import eu.hiddenite.locks.LocksPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class LocksStorage {
    private final NamespacedKey ownerNamespaceKey;
    private final NamespacedKey lockedNamespaceKey;

    public LocksStorage(LocksPlugin plugin) {
        ownerNamespaceKey = new NamespacedKey(plugin, "owner");
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
}
