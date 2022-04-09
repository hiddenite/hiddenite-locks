package eu.hiddenite.locks.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.hiddenite.locks.LocksPlugin;

import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class LockCommand implements CommandExecutor, TabCompleter {
    private final LocksPlugin plugin;

    public LockCommand(LocksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             final Command command,
                             final String alias,
                             final String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player)sender;

        Block block = player.getTargetBlockExact(6);

        if (block == null || !plugin.isLockable(block)) {
            String configPath = plugin.getSupportedConfigPath("error-look-at-chest",  "error-look-at-container");
            plugin.sendMessage(player, configPath);
            return true;
        }

        if (args.length == 0) {
            plugin.lockContainer(player, block);
        } else {
            String operation = args[0];
            if (operation.equals("+") || operation.equals("-")) {
                if (args.length == 1) {
                    plugin.sendMessage(player, "lock-usage");
                    return true;
                }
                for (int i = 1; i < args.length; i++) {
                    OfflinePlayer targetPlayer = plugin.findExistingPlayer(args[i]);
                    if (targetPlayer == null) {
                        plugin.sendMessage(player, "error-player-does-not-exist", "{NAME}", args[i]);
                        continue;
                    }

                    if (operation.equals("+")) {
                        plugin.addPlayerToLock(player, targetPlayer, block);
                    } else {
                        plugin.removePlayerFromLock(player, targetPlayer, block);
                    }
                }
            } else if (operation.equals("?")) {
                plugin.listPlayersAllowedToAccess(player, block);
            } else {
                plugin.sendMessage(player, "lock-usage");
                return true;
            }

        }

        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender,
                                      final Command command,
                                      final String alias,
                                      final String[] args) {
        if (args.length == 1) {
            return Arrays.asList("+", "-", "?");
        }
        if (args.length >= 2) {
            return null; // Player name
        }
        return Collections.emptyList();
    }
}
