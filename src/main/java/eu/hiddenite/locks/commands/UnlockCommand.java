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

import javax.annotation.Nonnull;

public class UnlockCommand implements CommandExecutor, TabCompleter {
    private final LocksPlugin plugin;

    public UnlockCommand(LocksPlugin plugin) {
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
        Block block = player.getTargetBlockExact(5);

        if (block == null || block.getType() != Material.CHEST) {
            plugin.sendMessage(player, "error-look-at-chest");
            return true;
        }

        plugin.unlockChest(player, block);
        return true;
    }

    @Override
    public List<String> onTabComplete(@Nonnull final CommandSender sender,
                                      @Nonnull final Command command,
                                      @Nonnull final String alias,
                                      @Nonnull final String[] args) {
        return Collections.emptyList();
    }
}
