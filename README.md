# 🏰 B12Clans - Sistema de Clãs para Minecraft

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21-green.svg)](https://www.minecraft.net/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![PlaceholderAPI](https://img.shields.io/badge/PlaceholderAPI-Compatible-purple.svg)](https://www.spigotmc.org/resources/placeholderapi.6245/)

> **Sistema de clãs moderno e completo para servidores Minecraft com suporte a cores hexadecimais, placeholders avançados e integração com MariaDB/MySQL.**

---

## 📋 Índice

- [✨ Características](#-características)
- [🚀 Instalação](#-instalação)
- [⚙️ Configuração](#️-configuração)
- [🎮 Comandos](#-comandos)
- [🏷️ Placeholders](#️-placeholders)
- [🎨 Sistema de Cores](#-sistema-de-cores)
- [📊 Banco de Dados](#-banco-de-dados)
- [🔧 Desenvolvimento](#-desenvolvimento)
- [📝 Changelog](#-changelog)
- [🤝 Contribuição](#-contribuição)

---

## ✨ Características

### 🏆 **Sistema de Clãs Completo**
- ✅ Criação e gerenciamento de clãs
- ✅ Sistema de convites e aceitação
- ✅ Hierarquia de cargos (Owner, Admin, Member)
- ✅ Títulos personalizados para membros
- ✅ Sistema de expulsão e saída

### 🎨 **Cores e Formatação Avançada**
- ✅ Suporte completo a **cores hexadecimais** (`&#FF0000`)
- ✅ Cores tradicionais do Minecraft (`&a`, `&b`, etc.)
- ✅ **Small caps estilizado** para tags
- ✅ Reset automático de cores (sem vazamento)
- ✅ Validação flexível de símbolos: `[ ] ( ) - _`

### 🏷️ **Placeholders Dinâmicos**
- ✅ **6 placeholders** diferentes para máxima flexibilidade
- ✅ Colchetes coloridos baseados em **role do jogador**
- ✅ Integração completa com **PlaceholderAPI**
- ✅ Cache otimizado para performance

### 🗄️ **Banco de Dados Robusto**
- ✅ **MariaDB/MySQL** com HikariCP
- ✅ Pool de conexões otimizado
- ✅ Transações seguras
- ✅ Estrutura normalizada e eficiente

---

## 🚀 Instalação

### 📋 **Pré-requisitos**
- **Minecraft**: 1.21+
- **Java**: 17+
- **MariaDB/MySQL**: 10.2+
- **PlaceholderAPI**: 2.11.5+ *(opcional)*

### 📥 **Passos de Instalação**

1. **Baixe o plugin** e coloque na pasta `plugins/`
2. **Configure o banco de dados** no `config.yml`
3. **Reinicie o servidor**
4. **Instale PlaceholderAPI** para usar placeholders
5. **Configure as mensagens** no `lang.yml`

---

## ⚙️ Configuração

### 🗄️ **Banco de Dados (config.yml)**

```yaml
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
```

### 🎨 **Cores dos Placeholders**

```yaml
settings:
  placeholder-colors:
    member:
      left-bracket: "&7["
      right-bracket: "&7]"
    leader:
      left-bracket: "&4["
      right-bracket: "&4]"
```

### 📏 **Limites de Clãs**

```yaml
settings:
  clan:
    max-name-length: 32
    min-name-length: 2
    max-tag-clean-length: 16
    max-expanded-tag-length: 1000
    allow-hex-colors: true
```

---

## 🎮 Comandos

### 👤 **Comandos de Jogador**

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/clan criar <nome> <tag>` | Criar um novo clã | `b12clans.criar` |
| `/clan info` | Ver informações do seu clã | `b12clans.use` |
| `/clan ver <tag>` | Visualizar como uma tag ficará | `b12clans.use` |
| `/clan convidar <jogador>` | Convidar jogador para o clã | `b12clans.use` |
| `/clan aceitar <tag>` | Aceitar convite de clã | `b12clans.use` |
| `/clan negar <tag>` | Negar convite de clã | `b12clans.use` |
| `/clan sair` | Sair do clã atual | `b12clans.use` |

### 👑 **Comandos de Administração**

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/clan expulsar <jogador>` | Expulsar membro do clã | `b12clans.use` |
| `/clan deletar confirm` | Deletar o clã (irreversível) | `b12clans.use` |
| `/clan promover <jogador>` | Promover membro a admin | `b12clans.promover` |
| `/clan rebaixar <jogador>` | Rebaixar admin a membro | `b12clans.rebaixar` |
| `/clan titulo <jogador> [titulo]` | Definir título personalizado | `b12clans.use` |

---

## 🏷️ Placeholders

### 📊 **Lista Completa de Placeholders**

| Placeholder | Descrição | Exemplo |
|-------------|-----------|---------|
| `%b12clans_tag%` | Tag básica do clã | `MeuClã` |
| `%b12clans_tag_label%` | Tag com colchetes coloridos | `&4[&6MeuClã&4]` |
| `%b12clans_tag_small%` | Tag em small caps | `ᴍᴇᴜᴄʟã` |
| `%b12clans_tag_small_labels%` | Small caps com colchetes | `&4[ᴍᴇᴜᴄʟã&4]` |
| `%b12clans_name%` | Nome completo do clã | `Meu Clã Épico` |
| `%b12clans_has_clan%` | Se tem clã (Sim/Não) | `Sim` |

### 🎯 **Exemplos de Uso**

#### **Chat Format:**
```
%b12clans_tag_label% %player_name%: %message%
```
**Resultado:** `&4[&6Elite&4] João123: Olá pessoal!`

#### **Tab List:**
```
%b12clans_tag_small% %player_name%
```
**Resultado:** `ᴇʟɪᴛᴇ João123`

#### **Scoreboard:**
```
Clã: %b12clans_tag_small_labels%
```
**Resultado:** `Clã: &7[ᴇʟɪᴛᴇ&7]`

---

## 🎨 Sistema de Cores

### 🌈 **Cores Suportadas**

#### **Cores Tradicionais:**
```
&0 = Preto    &8 = Cinza Escuro
&1 = Azul     &9 = Azul Claro  
&2 = Verde    &a = Verde Claro
&3 = Ciano    &b = Ciano Claro
&4 = Vermelho &c = Vermelho Claro
&5 = Roxo     &d = Rosa
&6 = Dourado  &e = Amarelo
&7 = Cinza    &f = Branco
```

#### **Cores Hexadecimais:**
```
&#FF0000 = Vermelho puro
&#00FF00 = Verde puro
&#0000FF = Azul puro
&#FFD700 = Dourado
&#800080 = Roxo
```

#### **Formatação:**
```
&l = Negrito
&o = Itálico
&n = Sublinhado
&m = Riscado
&k = Mágico
&r = Reset
```

### 🏆 **Cores por Role**

| Role | Cor dos Colchetes | Exemplo |
|------|-------------------|---------|
| **Owner** | Vermelho (`&4`) | `&4[&6MeuClã&4]` |
| **Admin** | Vermelho (`&4`) | `&4[&6MeuClã&4]` |
| **Member** | Cinza (`&7`) | `&7[&6MeuClã&7]` |

---

## 📊 Banco de Dados

### 🗃️ **Estrutura das Tabelas**

#### **b12_clans**
```sql
CREATE TABLE b12_clans (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name TEXT NOT NULL,
    tag TEXT NOT NULL,
    owner_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### **b12_clan_members**
```sql
CREATE TABLE b12_clan_members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    clan_id INT NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    player_name TEXT NOT NULL,
    role ENUM('OWNER', 'ADMIN', 'MEMBER') DEFAULT 'MEMBER',
    title VARCHAR(50) NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (clan_id) REFERENCES b12_clans(id) ON DELETE CASCADE
);
```

### ⚡ **Otimizações**

- ✅ **Índices** em colunas frequentemente consultadas
- ✅ **Pool de conexões** HikariCP para performance
- ✅ **Transações** para operações críticas
- ✅ **Cache em memória** para dados frequentes
- ✅ **Charset UTF8MB4** para emojis e caracteres especiais

---

## 🔧 Desenvolvimento

### 🛠️ **Tecnologias Utilizadas**

- **Java 17+** - Linguagem principal
- **Spigot API 1.21** - API do Minecraft
- **MariaDB Connector** - Driver de banco
- **HikariCP** - Pool de conexões
- **PlaceholderAPI** - Sistema de placeholders
- **Maven** - Gerenciamento de dependências

### 📁 **Estrutura do Projeto**

```
src/main/java/com/br/b12clans/
├── Main.java                    # Classe principal
├── commands/
│   └── ClanCommand.java         # Comandos do plugin
├── database/
│   ├── DatabaseManager.java    # Gerenciamento do banco
│   └── ClanExistenceStatus.java # Enum de status
├── managers/
│   └── ClanManager.java         # Lógica de clãs
├── models/
│   └── Clan.java               # Modelo de dados
├── placeholders/
│   └── ClanPlaceholder.java    # Integração PAPI
├── listeners/
│   └── PlayerListener.java     # Eventos de jogador
└── utils/
    ├── MessagesManager.java    # Sistema de mensagens
    └── SmallTextConverter.java # Conversão de texto
```

### 🔨 **Compilação**

```bash
# Clonar repositório
git clone https://github.com/TheEternalDark/B12Clan.git

# Compilar com Maven
mvn clean package

# JAR gerado em: target/B12Clans-1.0.0.jar
```

---

## 📝 Changelog

### 🆕 **v1.0.0** - *Lançamento Inicial*

#### ✨ **Novidades:**
- Sistema completo de clãs com hierarquia
- 6 placeholders dinâmicos diferentes
- Suporte a cores hexadecimais
- Sistema de small caps estilizado
- Integração com MariaDB/MySQL
- Pool de conexões otimizado
- Sistema de convites e gerenciamento
- Títulos personalizados para membros

#### 🎨 **Recursos de Formatação:**
- Colchetes coloridos por role
- Reset automático de cores
- Validação flexível de símbolos
- Suporte a formatação avançada

#### 🔧 **Melhorias Técnicas:**
- Cache em memória para performance
- Operações assíncronas no banco
- Validação robusta de entrada
- Sistema de mensagens configurável

---

#
</div>
