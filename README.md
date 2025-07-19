# 🏰 B12Clans - Sistema de Clãs para Minecraft

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spigot](https://img.shields.io/badge/Spigot-1.21+-yellow.svg)](https://www.spigotmc.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/badge/Discord-Integration-7289da.svg)](https://discord.com/)

Um sistema completo de clãs para servidores Minecraft com integração Discord, chat privado, sistema de ranks e muito mais!

## 📋 Índice

- [✨ Funcionalidades](#-funcionalidades)
- [📦 Requisitos](#-requisitos)
- [🚀 Instalação](#-instalação)
- [⚙️ Configuração](#️-configuração)
- [🎮 Comandos](#-comandos)
- [🔗 Integração Discord](#-integração-discord)
- [📊 Placeholders](#-placeholders)
- [🎨 Personalização](#-personalização)
- [🤝 Contribuição](#-contribuição)
- [📄 Licença](#-licença)

## ✨ Funcionalidades

### 🏰 Sistema de Clãs
- ✅ Criação e gerenciamento de clãs
- ✅ Sistema de convites e aceitação
- ✅ Ranks: Owner, Admin, Member
- ✅ Títulos personalizados para membros
- ✅ Tags coloridas com suporte a cores hexadecimais
- ✅ Validação inteligente de nomes e tags

### 💬 Sistema de Chat
- ✅ Chat privado do clã (`/.`)
- ✅ Chat de aliados (`/ally`)
- ✅ Sistema de mute individual
- ✅ Sincronização com Discord
- ✅ Formatação personalizável

### 🔗 Integração Discord
- ✅ Tópicos privados automáticos para cada clã
- ✅ Verificação de contas via slash command `/validar`
- ✅ Sincronização bidirecional de mensagens
- ✅ Embeds personalizáveis
- ✅ Sistema de menções automáticas

### 🎯 Placeholders (PlaceholderAPI)
- ✅ Tags com e sem formatação
- ✅ Colchetes coloridos por rank
- ✅ Versão small caps das tags
- ✅ Status de clã e informações

### 🛠️ Tecnologia
- ✅ MariaDB/MySQL com HikariCP
- ✅ Arquitetura assíncrona
- ✅ Cache inteligente
- ✅ Sistema de migração automática

## 📦 Requisitos

### Obrigatórios
- **Java 17+**
- **Spigot/Paper 1.21+**
- **MariaDB 10.2+** ou **MySQL 8.0+**

### Opcionais
- **PlaceholderAPI** (para placeholders)
- **Bot Discord** (para integração Discord)

## 🚀 Instalação

### 1. Download e Instalação
\`\`\`bash
# 1. Baixe o plugin
wget https://github.com/seu-usuario/B12Clans/releases/latest/download/B12Clans.jar

# 2. Coloque na pasta plugins do servidor
mv B12Clans.jar /caminho/para/servidor/plugins/

# 3. Reinicie o servidor
\`\`\`

### 2. Configuração do Banco de Dados
\`\`\`sql
-- Crie um banco de dados
CREATE DATABASE minecraft CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Crie um usuário (opcional)
CREATE USER 'minecraft'@'localhost' IDENTIFIED BY 'senha_segura';
GRANT ALL PRIVILEGES ON minecraft.* TO 'minecraft'@'localhost';
FLUSH PRIVILEGES;
\`\`\`

### 3. Configuração Inicial
Edite o arquivo `plugins/B12Clans/config.yml`:

\`\`\`yaml
database:
  host: "localhost"
  port: 3306
  database: "minecraft"
  username: "minecraft"
  password: "senha_segura"
\`\`\`

## ⚙️ Configuração

### 🗄️ Banco de Dados
\`\`\`yaml
database:
  host: "localhost"
  port: 3306
  database: "minecraft"
  username: "root"
  password: ""
  pool:
    maximum-pool-size: 10
    minimum-idle: 2
    connection-timeout: 10000
    idle-timeout: 300000
    max-lifetime: 900000
\`\`\`

### 🎨 Cores dos Placeholders
\`\`\`yaml
settings:
  placeholder-colors:
    member:
      left-bracket: "&7["
      right-bracket: "&7]"
    leader:
      left-bracket: "&4["
      right-bracket: "&4]"
\`\`\`

### 💬 Formato do Chat
\`\`\`yaml
chat:
  clan-format: "&8[&6CLÃN&8] &7%player%&8: &f%message%"
  ally-format: "&8[&aALIADO&8] &7%player%&8: &f%message%"
  discord-to-game-format: "&8[&9DISCORD&8] &b%discord_name%&8: &f%message%"
\`\`\`

## 🎮 Comandos

### 🏰 Comandos de Clã
| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/clan criar <nome> <tag>` | Criar um novo clã | `b12clans.criar` |
| `/clan info` | Ver informações do clã | `b12clans.use` |
| `/clan ver <tag>` | Visualizar como uma tag ficará | `b12clans.use` |
| `/clan convidar <jogador>` | Convidar um jogador | `b12clans.use` |
| `/clan aceitar <tag>` | Aceitar convite de clã | `b12clans.use` |
| `/clan negar <tag>` | Negar convite de clã | `b12clans.use` |
| `/clan sair` | Sair do clã atual | `b12clans.use` |
| `/clan expulsar <jogador>` | Expulsar um membro | `b12clans.use` |
| `/clan deletar confirm` | Deletar o clã | `b12clans.use` |
| `/clan promover <jogador>` | Promover membro a admin | `b12clans.promover` |
| `/clan rebaixar <jogador>` | Rebaixar admin a membro | `b12clans.rebaixar` |
| `/clan titulo <jogador> [titulo]` | Definir título personalizado | `b12clans.use` |

### 💬 Comandos de Chat
| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/. <mensagem>` | Enviar mensagem para o clã | `b12clans.chat.clan` |
| `/. join` | Entrar no canal do clã | `b12clans.chat.clan` |
| `/. leave` | Sair do canal do clã | `b12clans.chat.clan` |
| `/. mute` | Silenciar/ativar chat do clã | `b12clans.chat.clan` |
| `/ally <mensagem>` | Enviar mensagem para aliados | `b12clans.chat.ally` |
| `/ally join` | Entrar no canal dos aliados | `b12clans.chat.ally` |
| `/ally leave` | Sair do canal dos aliados | `b12clans.chat.ally` |

### 🔗 Comandos Discord
| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/vincular` | Gerar código de verificação | `b12clans.discord` |
| `/desvincular` | Desvincular conta do Discord | `b12clans.discord` |
| `/discord vincular` | Alternativa do vincular | `b12clans.discord` |
| `/discord desvincular` | Alternativa do desvincular | `b12clans.discord` |
| `/discord status` | Ver status da verificação | `b12clans.discord` |

## 🔗 Integração Discord

### 🤖 Configuração do Bot

1. **Crie um Bot no Discord Developer Portal**
   - Acesse https://discord.com/developers/applications
   - Crie uma nova aplicação
   - Vá em "Bot" e copie o token

2. **Configure as Permissões**
   \`\`\`
   ✅ Ler Mensagens
   ✅ Enviar Mensagens
   ✅ Gerenciar Tópicos
   ✅ Criar Tópicos Privados
   ✅ Mencionar Usuários
   ✅ Incorporar Links
   ✅ Usar Comandos de Barra
   \`\`\`

3. **Adicione ao Servidor**
   - Use o OAuth2 URL Generator
   - Selecione "bot" e "applications.commands"
   - Adicione as permissões necessárias

### ⚙️ Configuração no Plugin
\`\`\`yaml
discord:
  enabled: true
  token: "SEU_TOKEN_AQUI"
  guild-id: "ID_DO_SEU_SERVIDOR"
  clan-channel-id: "ID_DO_CANAL_DOS_CLAS"
  embed:
    color: "RED"
    title: "🏰 Chat do Clã: %clan_name%"
    description: |
      Bem-vindos ao chat privado do clã!
      
      📋 **Informações:**
      • Este é um tópico privado apenas para membros do clã
      • Mensagens aqui são sincronizadas com o jogo
      
      🎮 **Comandos no jogo:**
      • `/. <mensagem>` - Enviar mensagem para o clã
\`\`\`

### 🔄 Como Funciona

1. **Jogador usa `/vincular` no jogo**
2. **Recebe código de 6 dígitos**
3. **No Discord, usa `/validar <código>`**
4. **Bot cria tópico PRIVADO para o clã**
5. **Mensagens são sincronizadas automaticamente**

## 📊 Placeholders

### 📝 Lista Completa (PlaceholderAPI)
| Placeholder | Descrição | Exemplo |
|-------------|-----------|---------|
| `%b12clans_tag%` | Tag do clã sem formatação | `[MC]` |
| `%b12clans_tag_label%` | Tag com colchetes coloridos | `§4[§6MC§4]` |
| `%b12clans_tag_small%` | Tag em small caps | `[ᴍᴄ]` |
| `%b12clans_tag_small_labels%` | Tag small caps com colchetes | `§4[§6ᴍᴄ§4]` |
| `%b12clans_name%` | Nome do clã | `MeuClan` |
| `%b12clans_has_clan%` | Se tem clã (Sim/Não) | `Sim` |

### 🎨 Cores por Rank
- **👑 Leader/Admin**: Colchetes vermelhos (`&4`)
- **⚔️ Member**: Colchetes cinzas (`&7`)
- **🔧 Configurável**: Personalize no `config.yml`

## 🎨 Personalização

### 🏷️ Tags Coloridas
\`\`\`yaml
# Suporte completo a cores
/clan criar MeuClan &6[&lMC&6]           # Cores básicas
/clan criar MeuClan &#FF0000[&#00FF00MC&#FF0000]  # Cores hexadecimais
\`\`\`

### 💬 Formatos de Chat
\`\`\`yaml
chat:
  clan-format: "&8[&6CLÃN&8] &7%player%&8: &f%message%"
  # Variáveis disponíveis:
  # %player% - Nome do jogador
  # %clan_tag% - Tag do clã
  # %clan_name% - Nome do clã
  # %message% - Mensagem
\`\`\`

### 🎯 Embeds Discord
\`\`\`yaml
discord:
  embed:
    color: "RED"  # RED, BLUE, GREEN, YELLOW, etc.
    title: "🏰 Chat do Clã: %clan_name%"
    description: "Sua descrição personalizada aqui"
    footer: "Seu Servidor • Sistema de Clãs"
\`\`\`

## 🛠️ Desenvolvimento

### 📁 Estrutura do Projeto
\`\`\`
src/main/java/com/br/b12clans/
