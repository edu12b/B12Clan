package com.br.b12clans.database;

import com.br.b12clans.B12Clans;
import com.br.b12clans.models.Clan;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final B12Clans plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(B12Clans plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            setupHikariCP();

            // Testar conexão
            try (Connection connection = getConnection()) {
                if (connection.isValid(5)) {
                    String dbInfo = detectMariaDBInfo();
                    plugin.getLogger().info("Conectado ao " + dbInfo + " com sucesso!");
                } else {
                    throw new SQLException("Conexão inválida");
                }
            }

            createTables();
            updateExistingTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao conectar com o MariaDB: " + e.getMessage());
            plugin.getLogger().severe("Verifique suas configurações de MariaDB no config.yml");
            plugin.getLogger().severe("Certifique-se que o MariaDB está instalado e rodando");
            e.printStackTrace();
            return false;
        }
    }

    private void setupHikariCP() {
        FileConfiguration config = plugin.getConfig();

        HikariConfig hikariConfig = new HikariConfig();

        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.database", "minecraft");
        String username = config.getString("database.username", "root");
        String password = config.getString("database.password", "");

        // URL específica para MariaDB
        hikariConfig.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true");
        hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");

        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        // Configurações de performance
        hikariConfig.setMaximumPoolSize(config.getInt("database.pool.maximum-pool-size", 10));
        hikariConfig.setMinimumIdle(config.getInt("database.pool.minimum-idle", 2));
        hikariConfig.setConnectionTimeout(config.getLong("database.pool.connection-timeout", 10000));
        hikariConfig.setIdleTimeout(config.getLong("database.pool.idle-timeout", 300000));
        hikariConfig.setMaxLifetime(config.getLong("database.pool.max-lifetime", 900000));

        // Configurações otimizadas para MariaDB
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("useCompression", "true");
        hikariConfig.addDataSourceProperty("autoReconnect", "true");
        hikariConfig.addDataSourceProperty("failOverReadOnly", "false");
        hikariConfig.addDataSourceProperty("maxReconnects", "3");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    private void createTables() throws SQLException {
        String createClansTable = """
            CREATE TABLE IF NOT EXISTS b12_clans (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name TEXT NOT NULL,
                tag TEXT NOT NULL,
                owner_uuid VARCHAR(36) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_owner (owner_uuid),
                INDEX idx_name (name(32)),
                INDEX idx_tag (tag(32))
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """;

        String createMembersTable = """
            CREATE TABLE IF NOT EXISTS b12_clan_members (
                id INT AUTO_INCREMENT PRIMARY KEY,
                clan_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name TEXT NOT NULL,
                role ENUM('OWNER', 'ADMIN', 'MEMBER') DEFAULT 'MEMBER',
                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (clan_id) REFERENCES b12_clans(id) ON DELETE CASCADE,
                UNIQUE KEY unique_member (clan_id, player_uuid),
                INDEX idx_player (player_uuid),
                INDEX idx_clan (clan_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """;

        try (Connection connection = getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(createClansTable)) {
                stmt.executeUpdate();
                plugin.getLogger().info("Tabela b12_clans verificada/criada com sucesso");
            }
            try (PreparedStatement stmt = connection.prepareStatement(createMembersTable)) {
                stmt.executeUpdate();
                plugin.getLogger().info("Tabela b12_clan_members verificada/criada com sucesso");
            }
        }
    }

    private void updateExistingTables() throws SQLException {
        try (Connection connection = getConnection()) {
            // Verificar se precisa atualizar a estrutura das tabelas existentes
            String checkClansStructure = """
                SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH 
                FROM INFORMATION_SCHEMA.COLUMNS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'b12_clans' 
                AND COLUMN_NAME IN ('name', 'tag')
                """;

            boolean needsUpdate = false;
            try (PreparedStatement stmt = connection.prepareStatement(checkClansStructure);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("DATA_TYPE");

                    if (!dataType.equalsIgnoreCase("text")) {
                        needsUpdate = true;
                        plugin.getLogger().info("Coluna " + columnName + " precisa ser atualizada de " + dataType + " para TEXT");
                    }
                }
            }

            if (needsUpdate) {
                plugin.getLogger().info("Atualizando estrutura das tabelas para suportar tags maiores...");

                // Atualizar colunas para TEXT
                String[] updateQueries = {
                        "ALTER TABLE b12_clans MODIFY COLUMN name TEXT NOT NULL",
                        "ALTER TABLE b12_clans MODIFY COLUMN tag TEXT NOT NULL",
                        "ALTER TABLE b12_clan_members MODIFY COLUMN player_name TEXT NOT NULL"
                };

                for (String query : updateQueries) {
                    try (PreparedStatement stmt = connection.prepareStatement(query)) {
                        stmt.executeUpdate();
                        plugin.getLogger().info("Executado: " + query);
                    } catch (SQLException e) {
                        plugin.getLogger().warning("Erro ao executar: " + query + " - " + e.getMessage());
                    }
                }

                plugin.getLogger().info("Estrutura das tabelas atualizada com sucesso!");
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public CompletableFuture<Boolean> createClan(String name, String tag, UUID ownerUuid, String ownerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                connection.setAutoCommit(false);

                // Inserir clã
                String insertClan = "INSERT INTO b12_clans (name, tag, owner_uuid) VALUES (?, ?, ?)";
                int clanId;
                try (PreparedStatement stmt = connection.prepareStatement(insertClan, PreparedStatement.RETURN_GENERATED_KEYS)) {
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

                // Adicionar owner como membro
                String insertMember = "INSERT INTO b12_clan_members (clan_id, player_uuid, player_name, role) VALUES (?, ?, ?, 'OWNER')";
                try (PreparedStatement stmt = connection.prepareStatement(insertMember)) {
                    stmt.setInt(1, clanId);
                    stmt.setString(2, ownerUuid.toString());
                    stmt.setString(3, ownerName);
                    stmt.executeUpdate();
                }

                connection.commit();
                plugin.getLogger().info("Clã '" + name + "' criado com sucesso por " + ownerName + " no MariaDB");
                return true;

            } catch (SQLException e) {
                plugin.getLogger().severe("Erro ao criar clã: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Clan> getClanByPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String query = """
                SELECT c.id, c.name, c.tag, c.owner_uuid, c.created_at
                FROM b12_clans c
                INNER JOIN b12_clan_members m ON c.id = m.clan_id
                WHERE m.player_uuid = ?
                """;

            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new Clan(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("tag"),
                                UUID.fromString(rs.getString("owner_uuid")),
                                rs.getTimestamp("created_at")
                        );
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Erro ao buscar clã do jogador: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }

    public CompletableFuture<Boolean> clanExists(String name, String tag) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT 1 FROM b12_clans WHERE name = ? OR tag = ? LIMIT 1";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, name);
                stmt.setString(2, tag);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Erro ao verificar existência do clã: " + e.getMessage());
                e.printStackTrace();
                return true; // Retorna true para evitar criação em caso de erro
            }
        });
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Conexão com MariaDB fechada.");
        }
    }

    private String detectMariaDBInfo() {
        try (Connection connection = getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            String version = connection.getMetaData().getDatabaseProductVersion();
            String driverName = connection.getMetaData().getDriverName();
            String driverVersion = connection.getMetaData().getDriverVersion();

            return String.format("%s %s (Driver: %s %s)",
                    productName, version, driverName, driverVersion);
        } catch (SQLException e) {
            return "MariaDB (versão não detectada)";
        }
    }
}
