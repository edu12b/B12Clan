package com.br.b12clans.database;

import com.br.b12clans.Main;
import com.br.b12clans.models.Clan;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final Main plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            setupHikariCP();
            try (Connection connection = getConnection()) {
                if (!connection.isValid(5)) {
                    throw new SQLException("Conexão com o banco de dados inválida.");
                }
                plugin.getLogger().info("Conectado ao " + detectMariaDBInfo() + " com sucesso!");
            }
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao inicializar o DatabaseManager: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void setupHikariCP() {
        FileConfiguration config = plugin.getConfig();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mariadb://" + config.getString("database.host", "localhost") + ":" + config.getInt("database.port", 3306) + "/" + config.getString("database.database", "minecraft") + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true");
        hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
        hikariConfig.setUsername(config.getString("database.username", "root"));
        hikariConfig.setPassword(config.getString("database.password", ""));
        hikariConfig.setMaximumPoolSize(config.getInt("database.pool.maximum-pool-size", 10));
        hikariConfig.setMinimumIdle(config.getInt("database.pool.minimum-idle", 2));
        hikariConfig.setConnectionTimeout(config.getLong("database.pool.connection-timeout", 10000));
        hikariConfig.setIdleTimeout(config.getLong("database.pool.idle-timeout", 300000));
        hikariConfig.setMaxLifetime(config.getLong("database.pool.max-lifetime", 900000));
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    private void createTables() throws SQLException {
        String createClansTable = "CREATE TABLE IF NOT EXISTS b12_clans (id INT AUTO_INCREMENT PRIMARY KEY, name TEXT NOT NULL, tag TEXT NOT NULL, owner_uuid VARCHAR(36) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, INDEX idx_owner (owner_uuid), INDEX idx_name (name(32)), INDEX idx_tag (tag(32))) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
        String createMembersTable = "CREATE TABLE IF NOT EXISTS b12_clan_members (id INT AUTO_INCREMENT PRIMARY KEY, clan_id INT NOT NULL, player_uuid VARCHAR(36) NOT NULL, player_name TEXT NOT NULL, role ENUM('OWNER', 'ADMIN', 'MEMBER') DEFAULT 'MEMBER', joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (clan_id) REFERENCES b12_clans(id) ON DELETE CASCADE, UNIQUE KEY unique_member (clan_id, player_uuid), INDEX idx_player (player_uuid), INDEX idx_clan (clan_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            stmt.execute(createClansTable);
            plugin.getLogger().info("Tabela b12_clans verificada/criada com sucesso.");
            stmt.execute(createMembersTable);
            plugin.getLogger().info("Tabela b12_clan_members verificada/criada com sucesso.");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private String detectMariaDBInfo() {
        try (Connection connection = getConnection()) {
            return connection.getMetaData().getDatabaseProductName() + " " + connection.getMetaData().getDatabaseProductVersion();
        } catch (SQLException e) {
            return "MariaDB (versão não detectada)";
        }
    }

    public boolean createClan(String name, String tag, UUID ownerUuid, String ownerName) {
        String insertClan = "INSERT INTO b12_clans (name, tag, owner_uuid) VALUES (?, ?, ?)";
        String insertMember = "INSERT INTO b12_clan_members (clan_id, player_uuid, player_name, role) VALUES (?, ?, ?, 'OWNER')";
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            int clanId;
            try (PreparedStatement stmt = connection.prepareStatement(insertClan, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.setString(2, tag);
                stmt.setString(3, ownerUuid.toString());
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        clanId = rs.getInt(1);
                    } else {
                        throw new SQLException("Falha ao obter ID do clã criado");
                    }
                }
            }
            try (PreparedStatement stmt = connection.prepareStatement(insertMember)) {
                stmt.setInt(1, clanId);
                stmt.setString(2, ownerUuid.toString());
                stmt.setString(3, ownerName);
                stmt.executeUpdate();
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao criar clã em transação", e);
            return false;
        }
    }

    public Clan getClanByPlayer(UUID playerUuid) {
        String query = "SELECT c.id, c.name, c.tag, c.owner_uuid, c.created_at FROM b12_clans c INNER JOIN b12_clan_members m ON c.id = m.clan_id WHERE m.player_uuid = ?";
        try (Connection connection = getConnection(); PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Clan(rs.getInt("id"), rs.getString("name"), rs.getString("tag"), UUID.fromString(rs.getString("owner_uuid")), rs.getTimestamp("created_at"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar clã do jogador", e);
        }
        return null;
    }

    public ClanExistenceStatus clanExists(String name, String tag) {
        String query = "SELECT 1 FROM b12_clans WHERE name = ? OR tag = ? LIMIT 1";
        try (Connection connection = getConnection(); PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, tag);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? ClanExistenceStatus.EXISTS : ClanExistenceStatus.DOES_NOT_EXIST;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao verificar existência do clã: " + e.getMessage());
            return ClanExistenceStatus.DATABASE_ERROR;
        }
    }

    public void updatePlayerName(UUID playerUuid, String newName) {
        String sql = "UPDATE b12_clan_members SET player_name = ? WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setString(2, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar o nome do jogador " + newName, e);
        }
    }

    public boolean addClanMember(int clanId, UUID playerUuid, String playerName) {
        String sql = "INSERT INTO b12_clan_members (clan_id, player_uuid, player_name, role) VALUES (?, ?, ?, 'MEMBER')";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.setString(2, playerUuid.toString());
            ps.setString(3, playerName);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() != 1062) { // 1062 = "Duplicate entry"
                plugin.getLogger().log(Level.SEVERE, "Erro ao adicionar membro ao clã ID " + clanId, e);
            }
            return false;
        }
    }

    public Clan getClanById(int clanId) {
        String query = "SELECT * FROM b12_clans WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Clan(rs.getInt("id"), rs.getString("name"), rs.getString("tag"), UUID.fromString(rs.getString("owner_uuid")), rs.getTimestamp("created_at"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar clã pelo ID " + clanId, e);
        }
        return null;
    }
    public boolean removeClanMember(int clanId, UUID playerUuid) {
        String sql = "DELETE FROM b12_clan_members WHERE clan_id = ? AND player_uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, clanId);
            ps.setString(2, playerUuid.toString());

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao remover membro " + playerUuid + " do clã ID " + clanId, e);
            return false;
        }
    }

    public boolean deleteClan(int clanId) {
        String sql = "DELETE FROM b12_clans WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao deletar o clã ID " + clanId, e);
            return false;
        }
    }
}