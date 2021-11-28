package eu.hiddenite.locks.commands;

import java.util.Collections;
import java.util.List;

import eu.hiddenite.locks.LocksPlugin;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class UnlockCommand implements CommandExecutor, TabCompleter {
    private final LocksPlugin plugin;

    public UnlockCommand(LocksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender,
                             final Command command,
                             final String alias,
                             final String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player)sender;
        Block block = player.getTargetBlockExact(5);

        if (block == null || (!plugin.isLockable(block) && block.getType() != Material.CHEST)) {
        	String configPath = plugin.getSupportedConfigPath("error-look-at-chest",  "error-look-at-container");
            plugin.sendMessage(player, configPath);
            return true;
        }

        plugin.unlockContainer(player, block);
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender,
                                      final Command command,
                                      final String alias,
                                      final String[] args) {
        return Collections.emptyList();
    }
}
