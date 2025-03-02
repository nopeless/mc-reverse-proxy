package io.github.nopeless.mcReverseProxy;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class McrpCommand implements CommandExecutor {
    private final McReverseProxy plugin;

    public McrpCommand(McReverseProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (sender != null) {
            sender.sendMessage("Reloading config...");
        }
        plugin.reload();
        if (sender != null) {
            sender.sendMessage("Config reloaded. Check console for errors.");
        }
        return true;
    }
}