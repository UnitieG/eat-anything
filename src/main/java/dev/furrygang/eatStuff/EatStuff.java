package dev.furrygang.eatStuff;

import dev.furrygang.eatStuff.Scoreboard.Scoreboard;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class EatStuff extends JavaPlugin {

    private Eat eatInstance;
    private Scoreboard scoreboardInstance;

    public static EatStuff getInstance() {
        return getPlugin(EatStuff.class);
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("EatStuff plugin has been enabled!");
        try {
            eatInstance = new Eat();
            scoreboardInstance = new Scoreboard();
            getServer().getPluginManager().registerEvents(eatInstance, this);
            getServer().getPluginManager().registerEvents(scoreboardInstance, this);
        } catch (SQLException e) {
            getLogger().severe("Failed to register events: " + e.getMessage());
            throw new RuntimeException(e);
        }

        PluginCommand command = getCommand("leaderboard");
        command.setExecutor(new LeaderBoardCMD());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (eatInstance != null) {
            HandlerList.unregisterAll(eatInstance);
        }
        if (scoreboardInstance != null) {
            HandlerList.unregisterAll(scoreboardInstance);
        }
        getLogger().info("EatStuff plugin has been disabled!");
    }
}