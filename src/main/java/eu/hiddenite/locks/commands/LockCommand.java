package eu.hiddenite.locks.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.hiddenite.locks.LocksPlugin;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

public class LockCommand implements CommandExecutor, TabCompleter {
    private final LocksPlugin plugin;

    public LockCommand(LocksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@Nonnull final CommandSender sender,
                             @Nonnull final Command command,
                             @Nonnull final String alias,
                             @Nonnull final String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player)sender;

        if (args.length != 0 && args.length != 2) {
            plugin.sendMessage(player, "lock-usage");
            return true;
        }

        Block block = player.getTargetBlockExact(6);

        if (block == null || block.getType() != Material.CHEST) {
            plugin.sendMessage(player, "error-look-at-chest");
            return true;
        }

        if (args.length == 0) {
            plugin.lockChest(player, block);
        } else {
            String operation = args[0];
            if (!operation.equals("+") && !operation.equals("-")) {
                plugin.sendMessage(player, "lock-usage");
                return true;
            }

            OfflinePlayer targetPlayer = plugin.findExistingPlayer(args[1]);
            if (targetPlayer == null) {
                plugin.sendMessage(player, "error-player-does-not-exist", "{NAME}", args[1]);
                return true;
            }

            if (operation.equals("+")) {
                plugin.addPlayerToLock(player, targetPlayer, block);
            } else {
                plugin.removePlayerFromLock(player, targetPlayer, block);
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@Nonnull final CommandSender sender,
                                      @Nonnull final Command command,
                                      @Nonnull final String alias,
                                      @Nonnull final String[] args) {
        if (args.length == 1) {
            return Arrays.asList("+", "-");
        }
        if (args.length == 2) {
            return null; // Player name
        }
        return Collections.emptyList();
    }
}
