package dev.furrygang.eatStuff;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Eat implements Listener {
    private final Map<UUID, Integer> eatenBlocks = new HashMap<>();
    private final Map<UUID, Integer> eatenEntities = new HashMap<>();
    private final Set<Material> blacklistedBlocks;
    private final String dbUrl = "jdbc:sqlite:plugins/EatStuff/database.db";

    public Eat() {
        // Load blacklisted blocks from config
        File configFile = new File("plugins/EatStuff/config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        blacklistedBlocks = config.getStringList("blacklisted-blocks").stream()
                .map(Material::getMaterial)
                .collect(Collectors.toSet());
        createDatabase();
        loadEatenData();
    }

    private void createDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                String createTableSQL = "CREATE TABLE IF NOT EXISTS leaderboard (" +
                        "uuid TEXT PRIMARY KEY," +
                        "player_name TEXT," +
                        "eaten_blocks INTEGER," +
                        "eaten_entities INTEGER)";
                stmt.execute(createTableSQL);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadEatenData() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String querySQL = "SELECT * FROM leaderboard";
            try (Statement stmt = conn.createStatement()) {
                ResultSet resultSet = stmt.executeQuery(querySQL);
                while (resultSet.next()) {
                    UUID playerUUID = UUID.fromString(resultSet.getString("uuid"));
                    int eatenBlocksCount = resultSet.getInt("eaten_blocks");
                    int eatenEntitiesCount = resultSet.getInt("eaten_entities");
                    eatenBlocks.put(playerUUID, eatenBlocksCount);
                    eatenEntities.put(playerUUID, eatenEntitiesCount);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        UUID playerUUID = player.getUniqueId();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null && block.getType() != Material.AIR) {
            if (blacklistedBlocks.contains(block.getType())) {
                player.sendActionBar(ChatColor.RED + "You cannot eat this block!");
            } else {
                Location location = block.getLocation();
                player.sendActionBar(ChatColor.GREEN + "You have eaten " + ChatColor.YELLOW + block.getType().name().toLowerCase() + ChatColor.GREEN + "!");
                BlockData blockData = block.getBlockData();
                location.getWorld().spawnParticle(Particle.BLOCK, location, 10, 0.5, 0.5, 0.5, 0.1, blockData);
                location.getWorld().playSound(location, Sound.ENTITY_PLAYER_BURP, 1, 1);

                block.setType(Material.AIR);
                eatenBlocks.put(playerUUID, eatenBlocks.getOrDefault(playerUUID, 0) + 1);
                saveEatenData(player);

                // Cancel the event to prevent further processing
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        UUID playerUUID = player.getUniqueId();

        if (entity instanceof LivingEntity) {
            Location location = entity.getLocation();
            LivingEntity livingEntity = (LivingEntity) entity;

            if (livingEntity instanceof Player) {
                Player eatenPlayer = (Player) livingEntity;
                location.getWorld().playSound(location, Sound.ENTITY_PLAYER_BURP, 1, 1);
                eatenPlayer.damage(3, player);
                eatenPlayer.getWorld().spawnParticle(Particle.BLOCK, eatenPlayer.getLocation(), 50, 0.5, 0.5, 0.5, 0.01, Material.REDSTONE_BLOCK.createBlockData());
            } else {
                player.sendActionBar(ChatColor.GREEN + "You have eaten a " + ChatColor.YELLOW + livingEntity.getName() + ChatColor.GREEN + "!");
                location.getWorld().spawnParticle(Particle.BLOCK, livingEntity.getLocation(), 50, 0.5, 0.5, 0.5, 0.01, Material.REDSTONE_BLOCK.createBlockData());
                location.getWorld().playSound(location, Sound.ENTITY_PLAYER_BURP, 1, 1);
                livingEntity.damage(3);
            }

            eatenEntities.put(playerUUID, eatenEntities.getOrDefault(playerUUID, 0) + 1);
            saveEatenData(player);
        }
    }

    public void saveEatenData(Player player) {
        UUID playerUUID = player.getUniqueId();
        String playerName = player.getName();
        int eatenBlockCount = eatenBlocks.getOrDefault(playerUUID, 0);
        int eatenEntityCount = eatenEntities.getOrDefault(playerUUID, 0);

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String insertOrUpdateSQL = "INSERT INTO leaderboard (uuid, player_name, eaten_blocks, eaten_entities) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET " +
                    "player_name = excluded.player_name, " +
                    "eaten_blocks = excluded.eaten_blocks, " +
                    "eaten_entities = excluded.eaten_entities";
            try (PreparedStatement pstmt = conn.prepareStatement(insertOrUpdateSQL)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, playerName);
                pstmt.setInt(3, eatenBlockCount);
                pstmt.setInt(4, eatenEntityCount);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}