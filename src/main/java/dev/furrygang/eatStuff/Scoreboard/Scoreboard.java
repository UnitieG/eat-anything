package dev.furrygang.eatStuff.Scoreboard;

import dev.furrygang.eatStuff.EatStuff;
import fr.mrmicky.fastboard.adventure.FastBoard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Scoreboard implements Listener {
    private final Connection connection;
    private final Map<UUID, FastBoard> boards = new HashMap<>();

    public Scoreboard() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:plugins/EatStuff/database.db");

        new BukkitRunnable() {
            @Override
            public void run() {
                updateScoreboards();
            }
        }.runTaskTimer(EatStuff.getInstance(), 0, 20);
    }

    private void updateScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = boards.computeIfAbsent(player.getUniqueId(), uuid -> new FastBoard(player));
            updateBoard(board);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        FastBoard board = new FastBoard(event.getPlayer());
        boards.put(event.getPlayer().getUniqueId(), board);
        updateBoard(board);
    }

    private void updateBoard(FastBoard board) {
        Player player = board.getPlayer();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedDate = LocalDate.now().format(formatter);
        var mm = MiniMessage.miniMessage();

        Component title = mm.deserialize("<gradient:gold:yellow><b>EatStuff <reset><gray>(<yellow>1.1</yellow>)");
        Component line0 = mm.deserialize("<gray>" + formattedDate);
        Component line1 = mm.deserialize("<white>Player: <yellow>" + player.getName());
        Component line2 = mm.deserialize("<white>Blocks Consumed: <yellow>" + getBlockEatten(player));
        Component line3 = mm.deserialize("<white>Deaths: <yellow>" + player.getStatistic(org.bukkit.Statistic.DEATHS));
        Component footer = mm.deserialize("<gradient:gold:yellow><b><i>Eat or be eaten...</i>");
        Component blank = mm.deserialize("");

        board.updateTitle(title);
        board.updateLines(
                line0,
                blank,
                line1,
                line2,
                line3,
                blank,
                footer
        );
    }

    private int getBlockEatten(Player player) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT eaten_blocks FROM leaderboard WHERE uuid = ?")) {
            preparedStatement.setString(1, player.getUniqueId().toString());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("eaten_blocks");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}