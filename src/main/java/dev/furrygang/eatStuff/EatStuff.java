package dev.furrygang.eatStuff;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class EatStuff extends JavaPlugin {

    private FileConfiguration config;

    public static EatStuff getInstance() {
        return getPlugin(EatStuff.class);
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("EatStuff plugin has been enabled!");
        getServer().getPluginManager().registerEvents(new Eat(), this);

        PluginCommand command = getCommand("leaderboard");
        command.setExecutor(new LeaderBoardCMD());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        HandlerList.unregisterAll((Listener) new Eat());
        getLogger().info("EatStuff plugin has been disabled!");
    }
}