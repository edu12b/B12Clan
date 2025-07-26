package com.br.b12clans.database;

import com.br.b12clans.Main;
import com.br.b12clans.models.Clan;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private final Main plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    // ========================================
    // 1. INICIALIZAÇÃO E CONEXÃO
    // ========================================

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
            updateExistingTables();
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

    // ========================================
    // 2. CRIAÇÃO E ATUALIZAÇÃO DE TABELAS
    // ========================================

    private void createTables() throws SQLException {
        String createClansTable = "CREATE TABLE IF NOT EXISTS b12_clans (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "tag TEXT NOT NULL, " +
                "owner_uuid VARCHAR(36) NOT NULL, " +
                "description TEXT DEFAULT NULL, " +
                "bank_balance DECIMAL(15,2) DEFAULT 0.00, " +
                "home_world VARCHAR(50) DEFAULT NULL, " +
                "home_x DOUBLE DEFAULT NULL, " +
                "home_y DOUBLE DEFAULT NULL, " +
                "home_z DOUBLE DEFAULT NULL, " +
                "home_yaw FLOAT DEFAULT NULL, " +
                "home_pitch FLOAT DEFAULT NULL, " +
                "fee_amount DECIMAL(10,2) DEFAULT 0.00, " +
                "banner_data TEXT DEFAULT NULL, " +
                "discord_thread_id VARCHAR(20) NULL DEFAULT NULL, " +
                "friendly_fire_disabled BOOLEAN DEFAULT FALSE, " +
                "alliance_requests_disabled BOOLEAN DEFAULT FALSE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX idx_owner (owner_uuid), " +
                "INDEX idx_name (name(32)), " +
                "INDEX idx_tag (tag(32))" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

        String createMembersTable = "CREATE TABLE IF NOT EXISTS b12_clan_members (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "clan_id INT NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "player_name TEXT NOT NULL, " +
                "role ENUM('OWNER', 'VICE_LEADER', 'ADMIN', 'MEMBER') DEFAULT 'MEMBER', " +
                "title VARCHAR(50) NULL DEFAULT NULL, " +
                "kills INT DEFAULT 0, " +
                "deaths INT DEFAULT 0, " +
                "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (clan_id) REFERENCES b12_clans(id) ON DELETE CASCADE, " +
                "UNIQUE KEY unique_member (clan_id, player_uuid), " +
                "INDEX idx_player (player_uuid), " +
                "INDEX idx_clan (clan_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

        String createPlayerSettingsTable = "CREATE TABLE IF NOT EXISTS b12_player_settings (" +
                "player_uuid VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "invites_disabled BOOLEAN DEFAULT FALSE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

        String createDiscordLinksTable = "CREATE TABLE IF NOT EXISTS b12_discord_links (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "player_uuid VARCHAR(36) NOT NULL UNIQUE, " +
                "discord_user_id VARCHAR(20) NOT NULL UNIQUE, " +
                "linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX idx_player (player_uuid), " +
                "INDEX idx_discord (discord_user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

        String createAlliesTable = "CREATE TABLE IF NOT EXISTS b12_clan_allies (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "clan_id INT NOT NULL, " +
                "ally_clan_id INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (clan_id) REFERENCES b12_clans(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (ally_clan_id) REFERENCES b12_clans(id) ON DELETE CASCADE, " +
                "UNIQUE KEY unique_alliance (clan_id, ally_clan_id), " +
                "INDEX idx_clan (clan_id), " +
                "INDEX idx_ally (ally_clan_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

        String createRivalsTable = "CREATE TABLE IF NOT EXISTS b12_clan_rivals (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "clan_id INT NOT NULL, " +
                "rival_clan_id INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (clan_id) REFERENCES b12_clans(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (rival_clan_id) REFERENCES b12_clans(id) ON DELETE CASCADE, " +
                "UNIQUE KEY unique_rivalry (clan_id, rival_clan_id), " +
                "INDEX idx_clan (clan_id), " +
                "INDEX idx_rival (rival_clan_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            stmt.execute(createClansTable);
            stmt.execute(createMembersTable);
            stmt.execute(createDiscordLinksTable);
            stmt.execute(createAlliesTable);
            stmt.execute(createRivalsTable);
            stmt.execute(createPlayerSettingsTable);
            plugin.getLogger().info("Tabelas do B12Clans verificadas/criadas com sucesso.");
        }
    }

    private void updateExistingTables() {
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet rs;

            rs = meta.getColumns(null, null, "b12_clan_members", "title");
            if (!rs.next()) {
                stmt.executeUpdate("ALTER TABLE b12_clan_members ADD COLUMN title VARCHAR(50) NULL DEFAULT NULL AFTER role");
            }
            rs.close();

            rs = meta.getColumns(null, null, "b12_clans", "friendly_fire_disabled");
            if (!rs.next()) {
                stmt.executeUpdate("ALTER TABLE b12_clans ADD COLUMN friendly_fire_disabled BOOLEAN DEFAULT FALSE AFTER discord_thread_id");
            }
            rs.close();
            rs = meta.getColumns(null, null, "b12_clans", "alliance_requests_disabled");
            if (!rs.next()) {
                plugin.getLogger().info("Coluna 'alliance_requests_disabled' não encontrada. Adicionando...");
                stmt.executeUpdate("ALTER TABLE b12_clans ADD COLUMN alliance_requests_disabled BOOLEAN DEFAULT FALSE AFTER friendly_fire_disabled");
                plugin.getLogger().info("Coluna 'alliance_requests_disabled' adicionada com sucesso.");
            }
            rs.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao tentar atualizar estruturas das tabelas.", e);
        }
    }

    // ========================================
    // 3. MÉTODOS GERAIS DE CLÃ
    // ========================================

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
    public CompletableFuture<Void> setAllianceRequestsDisabledAsync(int clanId, boolean disabled) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE b12_clans SET alliance_requests_disabled = ? WHERE id = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, disabled);
                ps.setInt(2, clanId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar status de pedidos de aliança para o clã " + clanId, e);
            }
        }, plugin.getThreadPool());
    }

    public CompletableFuture<Boolean> areAllianceRequestsDisabledAsync(int clanId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT alliance_requests_disabled FROM b12_clans WHERE id = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, clanId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("alliance_requests_disabled");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao buscar status de pedidos de aliança para o clã " + clanId, e);
            }
            return false; // Por padrão, pedidos são ativados
        }, plugin.getThreadPool());
    }

    public CompletableFuture<Void> setPlayerInvitesDisabledAsync(UUID playerUuid, boolean disabled) {
        return CompletableFuture.runAsync(() -> {
            // Este comando insere ou atualiza o status do jogador de forma eficiente.
            String sql = "INSERT INTO b12_player_settings (player_uuid, invites_disabled) VALUES (?, ?) ON DUPLICATE KEY UPDATE invites_disabled = VALUES(invites_disabled)";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setBoolean(2, disabled);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar status de convite para " + playerUuid, e);
            }
        }, plugin.getThreadPool());
    }

    public CompletableFuture<Boolean> isPlayerInvitesDisabledAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT invites_disabled FROM b12_player_settings WHERE player_uuid = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("invites_disabled");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao buscar status de convite para " + playerUuid, e);
            }
            return false; // Por padrão, convites são ativados
        }, plugin.getThreadPool());
    }

    public boolean deleteClan(int clanId) {
        String sql = "DELETE FROM b12_clans WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao deletar o clã ID " + clanId, e);
            return false;
        }
    }

    public Clan getClanByPlayer(UUID playerUuid) {
        String query = "SELECT c.* FROM b12_clans c JOIN b12_clan_members m ON c.id = m.clan_id WHERE m.player_uuid = ?";
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

    public Clan getClanById(int clanId) {
        String query = "SELECT * FROM b12_clans WHERE id = ?";
        try (Connection connection = getConnection(); PreparedStatement stmt = connection.prepareStatement(query)) {
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

    public Clan getClanByTag(String tag) {
        String query = "SELECT * FROM b12_clans WHERE tag = ?";
        try (Connection connection = getConnection(); PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, tag);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Clan(rs.getInt("id"), rs.getString("name"), rs.getString("tag"), UUID.fromString(rs.getString("owner_uuid")), rs.getTimestamp("created_at"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar clã pela tag " + tag, e);
        }
        return null;
    }

    public CompletableFuture<Clan> getClanByTagAsync(String tag) {
        return CompletableFuture.supplyAsync(() -> getClanByTag(tag), plugin.getThreadPool());
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

    // ========================================
    // 4. MÉTODOS DE PROPRIEDADES DO CLÃ (TAG, HOME, FF, ETC)
    // ========================================

    public boolean updateClanTag(int clanId, String newTag) {
        String sql = "UPDATE b12_clans SET tag = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newTag);
            ps.setInt(2, clanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar tag do clã " + clanId, e);
            return false;
        }
    }

    public CompletableFuture<Boolean> updateClanTagAsync(int clanId, String newTag) {
        return CompletableFuture.supplyAsync(() -> updateClanTag(clanId, newTag), plugin.getThreadPool());
    }

    public String getClanDescription(int clanId) {
        String sql = "SELECT description FROM b12_clans WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("description");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar descrição do clã " + clanId, e);
        }
        return null;
    }

    public boolean updateClanDescription(int clanId, String description) {
        String sql = "UPDATE b12_clans SET description = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, description);
            ps.setInt(2, clanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar descrição do clã " + clanId, e);
            return false;
        }
    }

    public boolean setClanHome(int clanId, String world, double x, double y, double z, float yaw, float pitch) {
        String sql = "UPDATE b12_clans SET home_world = ?, home_x = ?, home_y = ?, home_z = ?, home_yaw = ?, home_pitch = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setDouble(2, x);
            ps.setDouble(3, y);
            ps.setDouble(4, z);
            ps.setFloat(5, yaw);
            ps.setFloat(6, pitch);
            ps.setInt(7, clanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao definir home do clã " + clanId, e);
            return false;
        }
    }

    public boolean clearClanHome(int clanId) {
        String sql = "UPDATE b12_clans SET home_world = NULL, home_x = NULL, home_y = NULL, home_z = NULL, home_yaw = NULL, home_pitch = NULL WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao limpar home do clã " + clanId, e);
            return false;
        }
    }

    public CompletableFuture<Boolean> clearClanHomeAsync(int clanId) {
        return CompletableFuture.supplyAsync(() -> clearClanHome(clanId), plugin.getThreadPool());
    }

    public Object[] getClanHome(int clanId) {
        String sql = "SELECT home_world, home_x, home_y, home_z, home_yaw, home_pitch FROM b12_clans WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String world = rs.getString("home_world");
                    if (world != null) {
                        return new Object[]{world, rs.getDouble("home_x"), rs.getDouble("home_y"), rs.getDouble("home_z"), rs.getFloat("home_yaw"), rs.getFloat("home_pitch")};
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar home do clã " + clanId, e);
        }
        return null;
    }

    public CompletableFuture<Object[]> getClanHomeAsync(int clanId) {
        return CompletableFuture.supplyAsync(() -> getClanHome(clanId), plugin.getThreadPool());
    }

    public CompletableFuture<Boolean> setClanHomeAsync(int clanId, Location loc) {
        return CompletableFuture.supplyAsync(() -> {
            if (loc == null) {
                return clearClanHome(clanId);
            } else {
                return setClanHome(clanId, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            }
        }, plugin.getThreadPool());
    }

    public boolean setClanFee(int clanId, double amount) {
        String sql = "UPDATE b12_clans SET fee_amount = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, clanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao definir taxa do clã " + clanId, e);
            return false;
        }
    }

    public CompletableFuture<Boolean> setClanFeeAsync(int clanId, double amount) {
        return CompletableFuture.supplyAsync(() -> setClanFee(clanId, amount), plugin.getThreadPool());
    }

    public CompletableFuture<Boolean> updateClanBannerAsync(int clanId, String bannerData) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE b12_clans SET banner_data = ? WHERE id = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                if (bannerData == null) {
                    ps.setNull(1, Types.VARCHAR);
                } else {
                    ps.setString(1, bannerData);
                }
                ps.setInt(2, clanId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar o banner do clã " + clanId, e);
                return false;
            }
        }, plugin.getThreadPool());
    }

    public boolean updateFriendlyFire(int clanId, boolean disabled) {
        String sql = "UPDATE b12_clans SET friendly_fire_disabled = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, disabled);
            ps.setInt(2, clanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar status de fogo amigo para o clã " + clanId, e);
            return false;
        }
    }

    public CompletableFuture<Boolean> updateFriendlyFireAsync(int clanId, boolean disabled) {
        return CompletableFuture.supplyAsync(() -> updateFriendlyFire(clanId, disabled), plugin.getThreadPool());
    }

    public boolean isFriendlyFireDisabled(int clanId) {
        String sql = "SELECT friendly_fire_disabled FROM b12_clans WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("friendly_fire_disabled");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar status de fogo amigo para o clã " + clanId, e);
        }
        return false;
    }

    public CompletableFuture<Boolean> isFriendlyFireDisabledAsync(int clanId) {
        return CompletableFuture.supplyAsync(() -> isFriendlyFireDisabled(clanId), plugin.getThreadPool());
    }

    // ========================================
    // 5. MÉTODOS DE MEMBROS (ADD, REMOVE, ROLE, KDR)
    // ========================================

    public boolean addClanMember(int clanId, UUID playerUuid, String playerName) {
        String sql = "INSERT INTO b12_clan_members (clan_id, player_uuid, player_name, role) VALUES (?, ?, ?, 'MEMBER')";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.setString(2, playerUuid.toString());
            ps.setString(3, playerName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() != 1062) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao adicionar membro ao clã ID " + clanId, e);
            }
            return false;
        }
    }

    public CompletableFuture<Boolean> addClanMemberAsync(int clanId, UUID playerUuid, String playerName) {
        return CompletableFuture.supplyAsync(() -> addClanMember(clanId, playerUuid, playerName), plugin.getThreadPool());
    }

    public boolean removeClanMember(int clanId, UUID playerUuid) {
        String sql = "DELETE FROM b12_clan_members WHERE clan_id = ? AND player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.setString(2, playerUuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao remover membro " + playerUuid + " do clã ID " + clanId, e);
            return false;
        }
    }

    public CompletableFuture<Boolean> removeClanMemberAsync(int clanId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> removeClanMember(clanId, playerUuid), plugin.getThreadPool());
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

    public String getMemberRole(int clanId, UUID playerUuid) {
        String sql = "SELECT role FROM b12_clan_members WHERE clan_id = ? AND player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.setString(2, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao obter cargo do membro " + playerUuid, e);
        }
        return null;
    }

    public CompletableFuture<String> getMemberRoleAsync(int clanId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> getMemberRole(clanId, playerUuid), plugin.getThreadPool());
    }

    public boolean updateMemberRole(int clanId, UUID playerUuid, String newRole) {
        String sql = "UPDATE b12_clan_members SET role = ? WHERE clan_id = ? AND player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setInt(2, clanId);
            ps.setString(3, playerUuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar cargo do membro " + playerUuid, e);
            return false;
        }
    }

    public CompletableFuture<Boolean> updateMemberRoleAsync(int clanId, UUID playerUuid, String newRole) {
        return CompletableFuture.supplyAsync(() -> updateMemberRole(clanId, playerUuid, newRole), plugin.getThreadPool());
    }

    public boolean updateMemberTitle(int clanId, UUID playerUuid, String title) {
        String finalTitle = (title == null || title.trim().isEmpty()) ? null : title;
        String sql = "UPDATE b12_clan_members SET title = ? WHERE clan_id = ? AND player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (finalTitle == null) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, finalTitle);
            }
            ps.setInt(2, clanId);
            ps.setString(3, playerUuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar o título do membro " + playerUuid, e);
            return false;
        }
    }

    public boolean updatePlayerKills(UUID playerUuid, int kills) {
        String sql = "UPDATE b12_clan_members SET kills = kills + ? WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kills);
            ps.setString(2, playerUuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar kills do jogador " + playerUuid, e);
            return false;
        }
    }

    public boolean updatePlayerDeaths(UUID playerUuid, int deaths) {
        String sql = "UPDATE b12_clan_members SET deaths = deaths + ? WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deaths);
            ps.setString(2, playerUuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar deaths do jogador " + playerUuid, e);
            return false;
        }
    }

    public int[] getPlayerKDR(UUID playerUuid) {
        String sql = "SELECT kills, deaths FROM b12_clan_members WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("kills"), rs.getInt("deaths")};
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar KDR do jogador " + playerUuid, e);
        }
        return new int[]{0, 0};
    }

    public CompletableFuture<int[]> getPlayerKDRAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> getPlayerKDR(playerUuid), plugin.getThreadPool());
    }

    public Object[] getMemberDataForCache(UUID playerUuid) {
        String sql = "SELECT kills, deaths, role FROM b12_clan_members WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{rs.getInt("kills"), rs.getInt("deaths"), rs.getString("role")};
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar dados de cache para o jogador " + playerUuid, e);
        }
        return new Object[]{0, 0, null};
    }

    // ========================================
    // 6. MÉTODOS DE RELACIONAMENTOS (ALIANÇAS/RIVAIS)
    // ========================================

    public boolean addAlly(int clanId, int allyClanId) {
        String sql = "INSERT IGNORE INTO b12_clan_allies (clan_id, ally_clan_id) VALUES (?, ?), (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.setInt(2, allyClanId);
            ps.setInt(3, allyClanId);
            ps.setInt(4, clanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao adicionar aliança entre " + clanId + " e " + allyClanId, e);
            return false;
        }
    }

    public boolean hasAllies(int clanId) {
        String sql = "SELECT 1 FROM b12_clan_allies WHERE clan_id = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Retorna true se encontrar pelo menos uma linha
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao verificar se o clã " + clanId + " tem aliados", e);
        }
        return false;
    }

    public CompletableFuture<Boolean> hasAlliesAsync(int clanId) {
        return CompletableFuture.supplyAsync(() -> hasAllies(clanId), plugin.getThreadPool());
    }

    public CompletableFuture<Boolean> addAllyAsync(int clanId, int allyClanId) {
        return CompletableFuture.supplyAsync(() -> addAlly(clanId, allyClanId), plugin.getThreadPool());
    }

    public boolean removeAlly(int clanId, int allyClanId) {
        String sql = "DELETE FROM b12_clan_allies WHERE (clan_id = ? AND ally_clan_id = ?) OR (clan_id = ? AND ally_clan_id = ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.setInt(2, allyClanId);
            ps.setInt(3, allyClanId);
            ps.setInt(4, clanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao remover aliança entre " + clanId + " e " + allyClanId, e);
            return false;
        }
    }

    public CompletableFuture<Boolean> removeAllyAsync(int clanId, int allyClanId) {
        return CompletableFuture.supplyAsync(() -> removeAlly(clanId, allyClanId), plugin.getThreadPool());
    }

    public boolean areAllies(int clanId1, int clanId2) {
        String sql = "SELECT 1 FROM b12_clan_allies WHERE clan_id = ? AND ally_clan_id = ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId1);
            ps.setInt(2, clanId2);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao verificar aliança entre " + clanId1 + " e " + clanId2, e);
        }
        return false;
    }

    public CompletableFuture<Boolean> areAlliesAsync(int clanId1, int clanId2) {
        return CompletableFuture.supplyAsync(() -> areAllies(clanId1, clanId2), plugin.getThreadPool());
    }

    public List<Integer> getAllAllyIds(int clanId) {
        List<Integer> allyIds = new ArrayList<>();
        String sql = "SELECT ally_clan_id FROM b12_clan_allies WHERE clan_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    allyIds.add(rs.getInt("ally_clan_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar aliados do clã " + clanId, e);
        }
        return allyIds;
    }

    public CompletableFuture<List<Integer>> getAllAllyIdsAsync(int clanId) {
        return CompletableFuture.supplyAsync(() -> getAllAllyIds(clanId), plugin.getThreadPool());
    }

    public boolean addRival(int clanId, int rivalClanId) {
        String sql = "INSERT IGNORE INTO b12_clan_rivals (clan_id, rival_clan_id) VALUES (?, ?), (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.setInt(2, rivalClanId);
            ps.setInt(3, rivalClanId);
            ps.setInt(4, clanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao adicionar rivalidade entre " + clanId + " e " + rivalClanId, e);
            return false;
        }
    }

    public CompletableFuture<Boolean> addRivalAsync(int clanId, int rivalClanId) {
        return CompletableFuture.supplyAsync(() -> addRival(clanId, rivalClanId), plugin.getThreadPool());
    }

    public boolean removeRival(int clanId, int rivalClanId) {
        String sql = "DELETE FROM b12_clan_rivals WHERE (clan_id = ? AND rival_clan_id = ?) OR (clan_id = ? AND rival_clan_id = ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.setInt(2, rivalClanId);
            ps.setInt(3, rivalClanId);
            ps.setInt(4, clanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao remover rivalidade entre " + clanId + " e " + rivalClanId, e);
            return false;
        }
    }

    public CompletableFuture<Boolean> removeRivalAsync(int clanId, int rivalClanId) {
        return CompletableFuture.supplyAsync(() -> removeRival(clanId, rivalClanId), plugin.getThreadPool());
    }

    public List<Integer> getAllRivalIds(int clanId) {
        List<Integer> rivalIds = new ArrayList<>();
        String sql = "SELECT rival_clan_id FROM b12_clan_rivals WHERE clan_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rivalIds.add(rs.getInt("rival_clan_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar rivais do clã " + clanId, e);
        }
        return rivalIds;
    }

    public CompletableFuture<List<Integer>> getAllRivalIdsAsync(int clanId) {
        return CompletableFuture.supplyAsync(() -> getAllRivalIds(clanId), plugin.getThreadPool());
    }

    // ========================================
    // 7. MÉTODOS DE BANCO DO CLÃ
    // ========================================

    public double getClanBankBalance(int clanId) {
        String sql = "SELECT bank_balance FROM b12_clans WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("bank_balance");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar saldo bancário do clã " + clanId, e);
        }
        return 0.0;
    }

    public CompletableFuture<Double> getClanBankBalanceAsync(int clanId) {
        return CompletableFuture.supplyAsync(() -> getClanBankBalance(clanId), plugin.getThreadPool());
    }

    public boolean depositToClanBank(int clanId, double amount) {
        String sql = "UPDATE b12_clans SET bank_balance = bank_balance + ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, clanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao depositar no banco do clã " + clanId, e);
            return false;
        }
    }

    public CompletableFuture<Boolean> depositToClanBankAsync(int clanId, double amount) {
        return CompletableFuture.supplyAsync(() -> depositToClanBank(clanId, amount), plugin.getThreadPool());
    }

    public boolean withdrawFromClanBank(int clanId, double amount) {
        String sql = "UPDATE b12_clans SET bank_balance = bank_balance - ? WHERE id = ? AND bank_balance >= ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, clanId);
            ps.setDouble(3, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao retirar do banco do clã " + clanId, e);
            return false;
        }
    }

    public CompletableFuture<Boolean> withdrawFromClanBankAsync(int clanId, double amount) {
        return CompletableFuture.supplyAsync(() -> withdrawFromClanBank(clanId, amount), plugin.getThreadPool());
    }

    // ========================================
    // 8. MÉTODOS DE VINCULAÇÃO DISCORD
    // ========================================

    public boolean saveDiscordLink(UUID playerUuid, String discordUserId) {
        String sql = "INSERT INTO b12_discord_links (player_uuid, discord_user_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE discord_user_id = VALUES(discord_user_id), linked_at = CURRENT_TIMESTAMP";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, discordUserId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao salvar vinculação Discord para " + playerUuid, e);
            return false;
        }
    }

    public boolean removeDiscordLink(UUID playerUuid) {
        String sql = "DELETE FROM b12_discord_links WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao remover vinculação Discord para " + playerUuid, e);
            return false;
        }
    }

    public Map<Integer, String> loadAllClanThreads() {
        Map<Integer, String> clanThreads = new HashMap<>();
        String sql = "SELECT id, discord_thread_id FROM b12_clans WHERE discord_thread_id IS NOT NULL";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clanThreads.put(rs.getInt("id"), rs.getString("discord_thread_id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao carregar os tópicos do Discord do banco de dados", e);
        }
        return clanThreads;
    }

    public CompletableFuture<Map<Integer, String>> loadAllClanThreadsAsync() {
        return CompletableFuture.supplyAsync(this::loadAllClanThreads, plugin.getThreadPool());
    }

    public boolean isPlayerLinkedToDiscord(UUID playerUuid) {
        return getDiscordUserId(playerUuid) != null;
    }

    public String getDiscordUserId(UUID playerUuid) {
        String sql = "SELECT discord_user_id FROM b12_discord_links WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("discord_user_id");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar ID Discord para " + playerUuid, e);
        }
        return null;
    }

    public Map<UUID, String> loadAllDiscordLinks() {
        Map<UUID, String> links = new HashMap<>();
        String sql = "SELECT player_uuid, discord_user_id FROM b12_discord_links";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                String discordUserId = rs.getString("discord_user_id");
                links.put(playerUuid, discordUserId);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao carregar vinculações Discord", e);
        }
        return links;
    }

    public CompletableFuture<Void> setClanDiscordThreadIdAsync(int clanId, String threadId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE b12_clans SET discord_thread_id = ? WHERE id = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, threadId);
                ps.setInt(2, clanId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao salvar o ID do tópico do Discord para o clã " + clanId, e);
            }
        }, plugin.getThreadPool());
    }
}