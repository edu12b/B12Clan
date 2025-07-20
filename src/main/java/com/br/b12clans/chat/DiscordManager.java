package com.br.b12clans.chat;

import com.br.b12clans.Main;
import com.br.b12clans.models.Clan;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.EmbedBuilder;

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
            if (token == null || token.isEmpty()) {
                plugin.getLogger().warning("Token do Discord não configurado!");
                return;
            }

            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
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

            // Registrar slash command /validar na guild
            registerSlashCommands();

            // Carregar vinculações do banco de dados
            loadVerificationData();

            plugin.getLogger().info("Bot Discord conectado com sucesso!");

        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao inicializar bot Discord: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerSlashCommands() {
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
        // Verificar se já está verificado no banco de dados
        if (plugin.getDatabaseManager().isPlayerLinkedToDiscord(playerUuid)) {
            return null; // Retorna null se já estiver verificado
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        pendingVerifications.put(playerUuid, code);

        // Remove o código após 1 hora
        scheduler.schedule(() -> {
            pendingVerifications.remove(playerUuid);
        }, 1, TimeUnit.HOURS);

        return code;
    }

    public boolean verifyPlayer(String discordUserId, String code) {
        for (Map.Entry<UUID, String> entry : pendingVerifications.entrySet()) {
            if (entry.getValue().equals(code)) {
                UUID playerUuid = entry.getKey();
                pendingVerifications.remove(playerUuid);

                // Salvar no banco de dados
                boolean saved = plugin.getDatabaseManager().saveDiscordLink(playerUuid, discordUserId);
                if (saved) {
                    // Adicionar ao cache
                    verifiedPlayers.put(playerUuid, discordUserId);

                    plugin.getLogger().info("Jogador " + playerUuid + " vinculou sua conta Discord: " + discordUserId);

                    // Adicionar jogador ao tópico do clã se estiver em um
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

    /**
     * Carrega todas as vinculações Discord do banco de dados para o cache
     */
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
        if (clan == null) return;

        String threadId = clanThreads.get(clan.getId());
        ThreadChannel thread;

        if (threadId == null) {
            // Criar novo tópico para o clã
            thread = createClanThread(clan);
            if (thread == null) return;
            clanThreads.put(clan.getId(), thread.getId());
        } else {
            thread = guild.getThreadChannelById(threadId);
            if (thread == null) {
                // Tópico foi deletado, criar novo
                thread = createClanThread(clan);
                if (thread == null) return;
                clanThreads.put(clan.getId(), thread.getId());
            }
        }

        String discordUserId = verifiedPlayers.get(playerUuid);
        if (discordUserId != null) {
            // Usar formato correto de menção <@ID>
            String mention = "<@" + discordUserId + ">";

            // Verificar se é o líder (owner) do clã
            String formattedName = clan.getName().replace("_", " ");

            if (clan.getOwnerUuid().equals(playerUuid)) {
                thread.sendMessage("👑 " + mention + " - Líder do clã **" + formattedName + "**").queue(message -> {
                    // Deletar a mensagem após 3 segundos
                    scheduler.schedule(() -> {
                        message.delete().queue();
                    }, 3, TimeUnit.SECONDS);
                });
            } else {
                // Mencionar membro para adicioná-lo ao tópico privado
                thread.sendMessage("⚔️ " + mention + " - Membro do clã **" + formattedName + "**").queue(message -> {
                    // Deletar a mensagem após 3 segundos
                    scheduler.schedule(() -> {
                        message.delete().queue();
                    }, 3, TimeUnit.SECONDS);
                });
            }
        }
    }

    public void onClanCreated(Clan clan) {
        if (jda == null || guild == null) return;

        // Verificar se o líder está verificado e adicioná-lo ao tópico
        UUID ownerUuid = clan.getOwnerUuid();
        if (verifiedPlayers.containsKey(ownerUuid)) {
            addPlayerToClanThread(ownerUuid);
        }
    }

    public void onPlayerJoinedClan(UUID playerUuid) {
        if (jda == null || guild == null) return;

        // Verificar se o jogador está verificado e adicioná-lo ao tópico
        if (verifiedPlayers.containsKey(playerUuid)) {
            addPlayerToClanThread(playerUuid);
        }
    }

    private ThreadChannel createClanThread(Clan clan) {
        if (clanChannel == null) return null;

        try {
            String formattedName = clan.getName().replace("_", " ");
            String threadName = "🏰 " + formattedName;

            // Criar tópico PRIVADO
            ThreadChannel thread = clanChannel.createThreadChannel(threadName, true).complete(); // true = privado

            // Criar embed inicial para manter o tópico ativo
            EmbedBuilder embedBuilder = new EmbedBuilder();
            String embedTitle = plugin.getConfig().getString("discord.embed.title", "🏰 Chat do Clã: %clan_name%")
                    .replace("%clan_name%", formattedName);
            String embedDescription = plugin.getConfig().getString("discord.embed.description",
                    "Bem-vindos ao chat privado do clã!\n\n" +
                            "📋 **Informações:**\n" +
                            "• Este é um tópico privado apenas para membros do clã\n" +
                            "• Mensagens aqui são sincronizadas com o jogo\n" +
                            "• Use este espaço para coordenar atividades do clã\n\n" +
                            "🎮 **Comandos no jogo:**\n" +
                            "• `/. <mensagem>` - Enviar mensagem para o clã\n" +
                            "• `/. join` - Entrar no canal do clã\n" +
                            "• `/. leave` - Sair do canal do clã");
            String embedFooter = plugin.getConfig().getString("discord.embed.footer", "B12Clans • Sistema de Clãs");
            String colorName = plugin.getConfig().getString("discord.embed.color", "RED");

            embedBuilder.setTitle(embedTitle);
            embedBuilder.setDescription(embedDescription);
            embedBuilder.setColor(getColorFromName(colorName));
            embedBuilder.setTimestamp(Instant.now());
            embedBuilder.setFooter(embedFooter, null);

            // Enviar embed inicial
            thread.sendMessageEmbeds(embedBuilder.build()).queue();

            // Mencionar o líder do clã usando o formato correto
            UUID ownerUuid = clan.getOwnerUuid();
            String ownerDiscordId = verifiedPlayers.get(ownerUuid);

            if (ownerDiscordId != null) {
                String mention = "<@" + ownerDiscordId + ">";
                thread.sendMessage("👑 " + mention + " - Líder do clã **" + formattedName + "**").queue(message -> {
                    // Deletar a mensagem após 3 segundos
                    scheduler.schedule(() -> {
                        message.delete().queue();
                    }, 3, TimeUnit.SECONDS);
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
            try {
                // Tentar remover o membro do tópico privado
                thread.removeThreadMember(member).queue(
                        success -> {
                            plugin.getLogger().info("Membro " + member.getEffectiveName() + " removido do tópico do clã " + clan.getName());

                            // Enviar mensagem informativa que será deletada
                            thread.sendMessage("👋 " + member.getEffectiveName() + " saiu do clã.").queue(message -> {
                                scheduler.schedule(() -> {
                                    message.delete().queue();
                                }, 5, TimeUnit.SECONDS);
                            });
                        },
                        error -> {
                            plugin.getLogger().warning("Erro ao remover membro do tópico: " + error.getMessage());

                            // Se não conseguir remover, apenas enviar mensagem
                            thread.sendMessage("👋 " + member.getEffectiveName() + " saiu do clã.").queue(message -> {
                                scheduler.schedule(() -> {
                                    message.delete().queue();
                                }, 5, TimeUnit.SECONDS);
                            });
                        }
                );
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao processar remoção do membro: " + e.getMessage());
            }
        }

        // IMPORTANTE: NÃO remover da lista verifiedPlayers nem do banco
        // A conta permanece vinculada permanentemente
    }

    public void archiveClanThread(Clan clan) {
        String threadId = clanThreads.get(clan.getId());
        if (threadId == null) return;

        ThreadChannel thread = guild.getThreadChannelById(threadId);
        if (thread == null) return;

        try {
            // Enviar mensagem final antes de arquivar
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("🏰 Clã Deletado");
            embedBuilder.setDescription("O clã **" + clan.getName().replace("_", " ") + "** foi deletado pelo líder.\n\n" +
                    "📋 **Informações:**\n" +
                    "• Este tópico será arquivado automaticamente\n" +
                    "• O histórico de mensagens será preservado\n" +
                    "• Se um novo clã for criado, um novo tópico será gerado");
            embedBuilder.setColor(Color.ORANGE);
            embedBuilder.setTimestamp(Instant.now());
            embedBuilder.setFooter("B12Clans • Sistema de Clãs", null);

            thread.sendMessageEmbeds(embedBuilder.build()).queue(message -> {
                // Aguardar 3 segundos e então trancar e arquivar o tópico
                scheduler.schedule(() -> {
                    // Primeiro trancar o tópico
                    thread.getManager().setLocked(true).queue(
                            lockSuccess -> {
                                plugin.getLogger().info("Tópico do clã " + clan.getName() + " trancado com sucesso");

                                // Depois arquivar o tópico
                                thread.getManager().setArchived(true).queue(
                                        archiveSuccess -> {
                                            plugin.getLogger().info("Tópico do clã " + clan.getName() + " arquivado com sucesso");
                                            // Remover da lista de tópicos ativos
                                            clanThreads.remove(clan.getId());
                                        },
                                        archiveError -> {
                                            plugin.getLogger().warning("Erro ao arquivar tópico do clã " + clan.getName() + ": " + archiveError.getMessage());
                                            // Mesmo com erro, remover da lista para permitir criação de novo tópico
                                            clanThreads.remove(clan.getId());
                                        }
                                );
                            },
                            lockError -> {
                                plugin.getLogger().warning("Erro ao trancar tópico do clã " + clan.getName() + ": " + lockError.getMessage());

                                // Mesmo com erro no lock, tentar arquivar
                                thread.getManager().setArchived(true).queue(
                                        archiveSuccess -> {
                                            plugin.getLogger().info("Tópico do clã " + clan.getName() + " arquivado com sucesso (sem trancar)");
                                            clanThreads.remove(clan.getId());
                                        },
                                        archiveError -> {
                                            plugin.getLogger().warning("Erro ao arquivar tópico do clã " + clan.getName() + ": " + archiveError.getMessage());
                                            clanThreads.remove(clan.getId());
                                        }
                                );
                            }
                    );
                }, 3, TimeUnit.SECONDS);
            });

        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao arquivar tópico do clã: " + e.getMessage());
            // Em caso de erro, remover da lista para permitir criação de novo tópico
            clanThreads.remove(clan.getId());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // Verificar se é mensagem em tópico de clã
        if (event.getChannel() instanceof ThreadChannel) {
            ThreadChannel thread = (ThreadChannel) event.getChannel();

            // Encontrar qual clã corresponde a este tópico
            for (Map.Entry<Integer, String> entry : clanThreads.entrySet()) {
                if (entry.getValue().equals(thread.getId())) {
                    // Enviar mensagem para o jogo
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

        String code = event.getOption("codigo").getAsString();

        if (verifyPlayer(event.getUser().getId(), code)) {
            event.reply("✅ Conta verificada com sucesso! Você foi adicionado ao chat do seu clã.").setEphemeral(true).queue();
        } else {
            event.reply("❌ Código inválido ou expirado!").setEphemeral(true).queue();
        }
    }

    private void sendDiscordMessageToGame(int clanId, String discordName, String message) {
        Clan clan = plugin.getDatabaseManager().getClanById(clanId);
        if (clan == null) return;

        String formattedMessage = plugin.getClanManager().translateColors(
                plugin.getConfig().getString("chat.discord-to-game-format", "&8[&9DISCORD&8] &b%discord_name%&8: &f%message%")
                        .replace("%discord_name%", discordName)
                        .replace("%message%", message)
        );

        // Enviar para todos os membros online do clã
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (playerClan != null && playerClan.getId() == clanId) {
                if (!plugin.getClanChatManager().isMuted(player.getUniqueId())) {
                    player.sendMessage(formattedMessage);
                }
            }
        });
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
        // Remover do banco de dados
        boolean removedFromDB = plugin.getDatabaseManager().removeDiscordLink(playerUuid);

        if (removedFromDB) {
            // Remover do cache
            String discordUserId = verifiedPlayers.remove(playerUuid);
            if (discordUserId != null) {
                plugin.getLogger().info("Jogador " + playerUuid + " desvinculou manualmente sua conta Discord: " + discordUserId);
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
