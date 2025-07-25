// ARQUIVO: src/main/java/com/br/b12clans/chat/DiscordManager.java
package com.br.b12clans.chat;

import com.br.b12clans.Main;
import com.br.b12clans.models.Clan;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiscordManager extends ListenerAdapter {

    private final Main plugin;
    private JDA jda;
    private Guild guild;
    private TextChannel verificationChannel;
    private TextChannel clanChannel;

    private final Map<UUID, String> pendingVerifications;
    private final Map<UUID, String> verifiedPlayers; // Cache em memória
    private final Map<Integer, String> clanThreads; // clanId -> threadId
    private final ScheduledExecutorService scheduler;

    public DiscordManager(Main plugin) {
        this.plugin = plugin;
        this.pendingVerifications = new ConcurrentHashMap<>();
        this.verifiedPlayers = new ConcurrentHashMap<>();
        this.clanThreads = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);

        if (plugin.getConfig().getBoolean("discord.enabled", false)) {
            initializeBot();
        }
    }

    private void initializeBot() {
        try {
            String token = plugin.getConfig().getString("discord.token");
            if (token == null || token.isEmpty() || token.equals("SEU_TOKEN_AQUI")) {
                plugin.getLogger().warning("Token do Discord não configurado!");
                return;
            }

            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(this)
                    .build();

            jda.awaitReady();

            String guildId = plugin.getConfig().getString("discord.guild-id");
            guild = jda.getGuildById(guildId);

            if (guild == null) {
                plugin.getLogger().severe("Servidor Discord não encontrado! ID: " + guildId);
                return;
            }

            String verificationChannelId = plugin.getConfig().getString("discord.verification-channel-id");
            verificationChannel = guild.getTextChannelById(verificationChannelId);

            String clanChannelId = plugin.getConfig().getString("discord.clan-channel-id");
            clanChannel = guild.getTextChannelById(clanChannelId);

            registerSlashCommands();
            loadVerificationData();
            loadClanThreadData();

            plugin.getLogger().info("Bot Discord conectado com sucesso!");

        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao inicializar bot Discord: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void loadClanThreadData() {
        plugin.getDatabaseManager().loadAllClanThreadsAsync().thenAccept(threads -> {
            clanThreads.clear();
            clanThreads.putAll(threads);
            plugin.getLogger().info("Carregados " + threads.size() + " mapeamentos de tópicos de clãs do Discord.");
        });
    }

    private void registerSlashCommands() {
        if (guild == null) return;
        try {
            guild.upsertCommand("validar", "Validar código de verificação do Minecraft")
                    .addOption(OptionType.STRING, "codigo", "Código de 6 dígitos recebido no jogo", true)
                    .queue(
                            success -> plugin.getLogger().info("Slash command /validar registrado com sucesso!"),
                            error -> plugin.getLogger().severe("Erro ao registrar slash command: " + error.getMessage())
                    );
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao registrar slash commands: " + e.getMessage());
        }
    }

    public String generateVerificationCode(UUID playerUuid) {
        if (plugin.getDatabaseManager().isPlayerLinkedToDiscord(playerUuid)) {
            return null;
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        pendingVerifications.put(playerUuid, code);

        scheduler.schedule(() -> pendingVerifications.remove(playerUuid), 1, TimeUnit.HOURS);

        return code;
    }

    public boolean verifyPlayer(String discordUserId, String code) {
        for (Map.Entry<UUID, String> entry : pendingVerifications.entrySet()) {
            if (entry.getValue().equals(code)) {
                UUID playerUuid = entry.getKey();
                pendingVerifications.remove(playerUuid);

                boolean saved = plugin.getDatabaseManager().saveDiscordLink(playerUuid, discordUserId);
                if (saved) {
                    verifiedPlayers.put(playerUuid, discordUserId);
                    plugin.getLogger().info("Jogador " + playerUuid + " vinculou sua conta Discord: " + discordUserId);
                    addPlayerToClanThread(playerUuid);
                    return true;
                } else {
                    plugin.getLogger().severe("Erro ao salvar vinculação Discord no banco de dados!");
                    return false;
                }
            }
        }
        return false;
    }

    private void loadVerificationData() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<UUID, String> links = plugin.getDatabaseManager().loadAllDiscordLinks();
            verifiedPlayers.clear();
            verifiedPlayers.putAll(links);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("Vinculações Discord carregadas: " + verifiedPlayers.size() + " contas vinculadas.");
            });
        });
    }

    private void addPlayerToClanThread(UUID playerUuid) {
        Clan clan = plugin.getClanManager().getPlayerClan(playerUuid);
        if (clan == null || guild == null) return;

        String threadId = clanThreads.get(clan.getId());
        ThreadChannel thread;

        if (threadId == null) {
            thread = createClanThread(clan);
            if (thread == null) return;
            clanThreads.put(clan.getId(), thread.getId());
        } else {
            thread = guild.getThreadChannelById(threadId);
            if (thread == null) {
                thread = createClanThread(clan);
                if (thread == null) return;
                clanThreads.put(clan.getId(), thread.getId());
            }
        }

        String discordUserId = verifiedPlayers.get(playerUuid);
        if (discordUserId != null) {
            String mention = "<@" + discordUserId + ">";
            String formattedName = clan.getName().replace("_", " ");
            String messageText = clan.getOwnerUuid().equals(playerUuid) ?
                    "👑 " + mention + " - Líder do clã **" + formattedName + "**" :
                    "⚔️ " + mention + " - Membro do clã **" + formattedName + "**";

            thread.sendMessage(messageText).queue(message -> {
                scheduler.schedule(() -> message.delete().queue(), 3, TimeUnit.SECONDS);
            });
        }
    }

    public void onClanCreated(Clan clan) {
        if (jda == null || guild == null) return;
        UUID ownerUuid = clan.getOwnerUuid();
        if (verifiedPlayers.containsKey(ownerUuid)) {
            addPlayerToClanThread(ownerUuid);
        }
    }

    // Método que agora se chama onMemberJoined para clareza
    public void onMemberJoined(Clan clan, Player player) {
        if (jda == null || guild == null) return;
        UUID playerUuid = player.getUniqueId();
        if (verifiedPlayers.containsKey(playerUuid)) {
            addPlayerToClanThread(playerUuid);
        }
    }

    // ##### MÉTODO ADICIONADO #####
    public void onMemberLeft(Clan clan, OfflinePlayer player) {
        if (jda == null || guild == null) return;
        String discordUserId = verifiedPlayers.get(player.getUniqueId());
        if (discordUserId == null) return; // Jogador não tem conta vinculada
        removePlayerFromClanThread(player.getUniqueId(), clan);
    }

    // ##### MÉTODO ADICIONADO #####
    public void onClanDisbanded(Clan clan) {
        if (jda == null || guild == null) return;
        archiveClanThread(clan);
    }

    private ThreadChannel createClanThread(Clan clan) {
        if (clanChannel == null) return null;
        try {
            String formattedName = clan.getName().replace("_", " ");
            String threadName = "🏰 " + formattedName;
            ThreadChannel thread = clanChannel.createThreadChannel(threadName, true).complete();

            EmbedBuilder embedBuilder = new EmbedBuilder();
            String embedTitle = plugin.getConfig().getString("discord.embed.title", "🏰 Chat do Clã: %clan_name%").replace("%clan_name%", formattedName);
            String embedDescription = plugin.getConfig().getString("discord.embed.description", "Bem-vindos ao chat privado do clã!");
            String embedFooter = plugin.getConfig().getString("discord.embed.footer", "B12Clans • Sistema de Clãs");
            String colorName = plugin.getConfig().getString("discord.embed.color", "RED");

            embedBuilder.setTitle(embedTitle);
            embedBuilder.setDescription(embedDescription);
            embedBuilder.setColor(getColorFromName(colorName));
            embedBuilder.setTimestamp(Instant.now());
            embedBuilder.setFooter(embedFooter, null);
            thread.sendMessageEmbeds(embedBuilder.build()).queue();
            plugin.getDatabaseManager().setClanDiscordThreadIdAsync(clan.getId(), thread.getId());
            clanThreads.put(clan.getId(), thread.getId());

            UUID ownerUuid = clan.getOwnerUuid();
            String ownerDiscordId = verifiedPlayers.get(ownerUuid);
            if (ownerDiscordId != null) {
                String mention = "<@" + ownerDiscordId + ">";
                thread.sendMessage("👑 " + mention + " - Líder do clã **" + formattedName + "**").queue(message -> {
                    scheduler.schedule(() -> message.delete().queue(), 3, TimeUnit.SECONDS);
                });
            }
            return thread;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao criar tópico do clã: " + e.getMessage());
            return null;
        }
    }

    public void sendClanMessage(Clan clan, String playerName, String message) {
        if (jda == null || guild == null) return;
        String threadId = clanThreads.get(clan.getId());
        if (threadId == null) return;
        ThreadChannel thread = guild.getThreadChannelById(threadId);
        if (thread == null) return;
        String formattedMessage = String.format("**%s**: %s", playerName, message);
        thread.sendMessage(formattedMessage).queue();
    }

    public void removePlayerFromClanThread(UUID playerUuid, Clan clan) {
        String discordUserId = verifiedPlayers.get(playerUuid);
        if (discordUserId == null) return;
        String threadId = clanThreads.get(clan.getId());
        if (threadId == null) return;
        ThreadChannel thread = guild.getThreadChannelById(threadId);
        if (thread == null) return;

        Member member = guild.getMemberById(discordUserId);
        if (member != null) {
            thread.removeThreadMember(member).queue(
                    success -> {
                        String logMsg = "Membro " + member.getEffectiveName() + " removido do tópico do clã " + clan.getName();
                        plugin.getLogger().info(logMsg);
                        thread.sendMessage("👋 " + member.getEffectiveName() + " saiu do clã.").queue(msg -> scheduler.schedule(() -> msg.delete().queue(), 5, TimeUnit.SECONDS));
                    },
                    error -> {
                        String logMsg = "Erro ao remover membro do tópico: " + error.getMessage();
                        plugin.getLogger().warning(logMsg);
                        thread.sendMessage("👋 " + member.getEffectiveName() + " saiu do clã.").queue(msg -> scheduler.schedule(() -> msg.delete().queue(), 5, TimeUnit.SECONDS));
                    }
            );
        }
    }

    public void archiveClanThread(Clan clan) {
        String threadId = clanThreads.get(clan.getId());
        if (threadId == null) return;
        ThreadChannel thread = guild.getThreadChannelById(threadId);
        if (thread == null) return;

        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("🏰 Clã Deletado");
            embedBuilder.setDescription("O clã **" + clan.getName().replace("_", " ") + "** foi deletado.");
            embedBuilder.setColor(Color.ORANGE);
            embedBuilder.setTimestamp(Instant.now());
            embedBuilder.setFooter("B12Clans • Sistema de Clãs", null);

            thread.sendMessageEmbeds(embedBuilder.build()).queue(message -> {
                scheduler.schedule(() -> {
                    thread.getManager().setLocked(true).queue(
                            lockSuccess -> thread.getManager().setArchived(true).queue(
                                    archiveSuccess -> clanThreads.remove(clan.getId()),
                                    archiveError -> clanThreads.remove(clan.getId())
                            ),
                            lockError -> thread.getManager().setArchived(true).queue(
                                    archiveSuccess -> clanThreads.remove(clan.getId()),
                                    archiveError -> clanThreads.remove(clan.getId())
                            )
                    );
                }, 3, TimeUnit.SECONDS);
            });
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao arquivar tópico do clã: " + e.getMessage());
            clanThreads.remove(clan.getId());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getChannel() instanceof ThreadChannel) {
            ThreadChannel thread = (ThreadChannel) event.getChannel();
            for (Map.Entry<Integer, String> entry : clanThreads.entrySet()) {
                if (entry.getValue().equals(thread.getId())) {
                    String playerName = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
                    sendDiscordMessageToGame(entry.getKey(), playerName, event.getMessage().getContentRaw());
                    break;
                }
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("validar")) return;

        // 1. AVISA O DISCORD IMEDIATAMENTE QUE ESTAMOS TRABALHANDO NISSO
        event.deferReply(true).queue();

        // 2. EXECUTA A LÓGICA LENTA (COM BANCO DE DADOS) EM SEGUNDO PLANO
        plugin.getThreadPool().submit(() -> {
            try {
                String code = event.getOption("codigo").getAsString();
                boolean success = verifyPlayer(event.getUser().getId(), code);

                // 3. USA O "HOOK" PARA ENVIAR A RESPOSTA FINAL QUANDO TERMINAR
                String responseMessage = success ? "✅ Conta verificada com sucesso!" : "❌ Código inválido ou expirado!";
                event.getHook().sendMessage(responseMessage).queue();

            } catch (Exception e) {
                // Em caso de qualquer erro, avisa o usuário
                event.getHook().sendMessage("Ocorreu um erro ao processar seu código.").queue();
                plugin.getLogger().log(Level.SEVERE, "Erro ao processar comando /validar do Discord", e);
            }
        });
    }


    private void sendDiscordMessageToGame(int clanId, String discordName, String message) {
        Clan clan = plugin.getDatabaseManager().getClanById(clanId);
        if (clan == null) return;

        // 1. A mensagem formatada com cores para os jogadores continua a mesma
        String format = plugin.getConfig().getString("chat.discord-to-game-format", "&8[&9DISCORD§8] &b%discord_name%&8: &f%message%");

        StringBuilder sb = new StringBuilder(format);
        replace(sb, "%discord_name%", discordName);
        replace(sb, "%message%", message);

        String formattedMessage = plugin.getClanManager().translateColors(sb.toString());

        // 2. Lógica de log no console
        if (plugin.getConfig().getBoolean("discord.log-discord-chat-to-console", true)) {
            // NOVO: Remove os códigos de cor ANTES de enviar para o console
            String cleanMessageForConsole = ChatColor.stripColor(formattedMessage);
            plugin.getLogger().info(cleanMessageForConsole);
        }

        // 3. Envia a mensagem colorida para os jogadores no jogo
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (playerClan != null && playerClan.getId() == clanId) {
                if (!plugin.getClanChatManager().isMuted(player.getUniqueId())) {
                    player.sendMessage(formattedMessage);
                }
            }
        });
    }
    private void replace(StringBuilder sb, String placeholder, String value) {
        int index;
        while ((index = sb.indexOf(placeholder)) != -1) {
            sb.replace(index, index + placeholder.length(), value);
        }
    }

    public boolean isPlayerVerified(UUID playerUuid) {
        return verifiedPlayers.containsKey(playerUuid);
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
        scheduler.shutdown();
    }

    public boolean unlinkPlayer(UUID playerUuid) {
        boolean removedFromDB = plugin.getDatabaseManager().removeDiscordLink(playerUuid);
        if (removedFromDB) {
            String discordUserId = verifiedPlayers.remove(playerUuid);
            if (discordUserId != null) {
                plugin.getLogger().info("Jogador " + playerUuid + " desvinculou sua conta Discord: " + discordUserId);
                return true;
            }
        }
        return false;
    }

    private Color getColorFromName(String colorName) {
        switch (colorName.toUpperCase()) {
            case "RED": return Color.RED;
            case "BLUE": return Color.BLUE;
            case "GREEN": return Color.GREEN;
            case "YELLOW": return Color.YELLOW;
            case "ORANGE": return Color.ORANGE;
            case "PINK": return Color.PINK;
            case "MAGENTA": return Color.MAGENTA;
            case "CYAN": return Color.CYAN;
            case "WHITE": return Color.WHITE;
            case "BLACK": return Color.BLACK;
            case "GRAY": return Color.GRAY;
            case "LIGHT_GRAY": return Color.LIGHT_GRAY;
            case "DARK_GRAY": return Color.DARK_GRAY;
            default: return Color.RED;
        }
    }
}