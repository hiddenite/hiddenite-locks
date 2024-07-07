package eu.hiddenite.locks.commands;

import java.util.Collections;
import java.util.List;

import eu.hiddenite.locks.LocksPlugin;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnlockCommand implements CommandExecutor, TabCompleter {
    private final LocksPlugin plugin;

    public UnlockCommand(LocksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender,
                             final @NotNull Command command,
                             final @NotNull String alias,
                             final @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player)sender;
        Block block = player.getTargetBlockExact(5);

        if (block == null || !plugin.isLockable(block)) {
            String configPath = plugin.getSupportedConfigPath("error-look-at-chest",  "error-look-at-container");
            plugin.sendMessage(player, configPath);
            return true;
        }

        plugin.unlockContainer(player, block);
        return true;
    }

    @Override
    public List<String> onTabComplete(final @NotNull CommandSender sender,
                                      final @NotNull Command command,
                                      final @NotNull String alias,
                                      final String[] args) {
        return Collections.emptyList();
    }
}
