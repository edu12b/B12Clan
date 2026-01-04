# 🏰 B12Clans - Sistema de Clãs para Minecraft

Sistema completo de clãs para servidores Minecraft, com chat privado, ranks, placeholders e integração total com Discord.

---

## ✨ Funcionalidades
- Sistema completo de clãs
- Chat de clã e aliados
- Ranks internos (Owner, Admin, Member)
- Tags coloridas (cores normais e hex)
- Placeholders via PlaceholderAPI
- Integração bidirecional com Discord
- Banco de dados MySQL/MariaDB
- Arquitetura assíncrona com cache

---

## 📦 Requisitos
- Java 17+
- Spigot/Paper 1.21+
- MySQL 8.0+ ou MariaDB 10.2+
- (Opcional) PlaceholderAPI
- (Opcional) Bot Discord

---

## 🚀 Instalação
1. Coloque `B12Clans.jar` na pasta `/plugins`
2. Reinicie o servidor
3. Configure `plugins/B12Clans/config.yml`

---

## 🎮 Comandos

### 🏰 Clã
| Comando | Descrição |
|-------|---------|
| `/clan criar <nome> <tag>` | Criar clã |
| `/clan info` | Informações do clã |
| `/clan ver <tag>` | Visualizar tag |
| `/clan convidar <jogador>` | Convidar jogador |
| `/clan aceitar <tag>` | Aceitar convite |
| `/clan negar <tag>` | Negar convite |
| `/clan sair` | Sair do clã |
| `/clan expulsar <jogador>` | Expulsar membro |
| `/clan deletar confirm` | Deletar clã |
| `/clan promover <jogador>` | Promover a admin |
| `/clan rebaixar <jogador>` | Rebaixar para membro |
| `/clan titulo <jogador> [titulo]` | Definir título |

### 💬 Chat
| Comando | Função |
|-------|-------|
| `/. <msg>` | Chat do clã |
| `/. join` | Entrar no chat |
| `/. leave` | Sair do chat |
| `/. mute` | Mutar chat |
| `/ally <msg>` | Chat de aliados |
| `/ally join` | Entrar |
| `/ally leave` | Sair |

### 🔗 Discord
| Comando | Função |
|-------|-------|
| `/vincular` | Gerar código |
| `/desvincular` | Desvincular |
| `/discord status` | Status da conta |

---

## 📊 Placeholders (PlaceholderAPI)

| Placeholder | Descrição |
|------------|----------|
| `%b12clans_tag%` | Tag sem cor |
| `%b12clans_tag_label%` | Tag colorida |
| `%b12clans_tag_small%` | Tag small caps |
| `%b12clans_tag_small_labels%` | Small caps + colchetes |
| `%b12clans_name%` | Nome do clã |
| `%b12clans_has_clan%` | Possui clã (Sim/Não) |

### 🎨 Cores por Rank
- Leader/Admin: `&4`
- Member: `&7`
- Totalmente configurável no `config.yml`

---

## 🔗 Discord – Funcionamento
1. Jogador usa `/vincular`
2. Recebe código no jogo
3. Usa `/validar <código>` no Discord
4. Tópico privado do clã é criado
5. Mensagens sincronizadas

---

## 📄 Licença
MIT License
