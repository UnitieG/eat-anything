package dev.furrygang.eatStuff;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.sql.*;
import java.util.*;

public class LeaderBoardCMD implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:plugins/EatStuff/database.db")) {
            if (connection == null) {
                sender.sendMessage(ChatColor.RED + "Error: Unable to connect to the database!");
                sender.sendMessage(ChatColor.RED + "Please report this bug on discord.");
                return true;
            }
            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM leaderboard ORDER BY eaten_blocks DESC")) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    Map<String, Integer> leaderboard = new HashMap<>();
                    while (resultSet.next()) {
                        String playerName = resultSet.getString("player_name");
                        int eatenBlocks = resultSet.getInt("eaten_blocks");
                        leaderboard.put(playerName, eatenBlocks);
                    }

                    List<Map.Entry<String, Integer>> list = new ArrayList<>(leaderboard.entrySet());
                    list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

                    StringBuilder leaderboardMessage = new StringBuilder(ChatColor.GREEN + "Leaderboard:\n");
                    for (int i = 0; i < list.size(); i++) {
                        Map.Entry<String, Integer> entry = list.get(i);
                        String playerName = entry.getKey();
                        leaderboardMessage.append(ChatColor.GOLD).append(i + 1).append(". ").append(ChatColor.YELLOW).append(playerName)
                                .append(ChatColor.GOLD).append(" - ").append(ChatColor.YELLOW).append(entry.getValue()).append(ChatColor.GOLD).append("\n");
                    }

                    sender.sendMessage(leaderboardMessage.toString());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
}