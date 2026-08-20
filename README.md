# 🛡️ VeloAdmin

**VeloAdmin** est une suite d'outils d'administration pour les réseaux Minecraft propulsés par **Velocity** : téléportation entre serveurs, vanish complet, système de signalements et bannissements temporaires — le tout piloté depuis le proxy et appliqué réellement sur chaque serveur Paper du réseau.

La suite se compose de **deux plugins complémentaires** :

| Plugin | Où l'installer | Rôle |
|---|---|---|
| **VeloAdmin** | `plugins/` du proxy **Velocity** | Commandes, base de données SQLite (reports & bans), menus cliquables dans le chat, logique réseau |
| **VeloAdminBridge** | `plugins/` de **chaque** serveur **Paper 1.21.x** | Exécute la vraie téléportation et le vrai vanish en jeu, synchronise le statut OP |

> ⚠️ **Pourquoi deux plugins ?** Velocity est un proxy : il gère le réseau mais n'a pas accès aux inventaires, aux positions ni au rendu des joueurs. Ces actions ne peuvent exister que côté serveur. Sans le bridge, `/tpgui` connecte bien au bon serveur mais ne téléporte pas sur le joueur, et `/vanish` reste un simple état logique sans invisibilité réelle.

## ✨ Fonctionnalités

- 🧭 **`/tpgui`** — menu cliquable dans le chat : choisis un serveur, puis un joueur, et téléporte-toi sur lui **n'importe où sur le réseau**
- 👻 **`/vanish`** — invisibilité complète :
  - invisible physiquement en jeu (effet de potion + `hidePlayer`)
  - retiré de la **tab-list de tout le réseau** (pas seulement de son serveur)
  - masqué du `/list` (commande réécrite par le bridge)
  - aucun message de connexion/déconnexion visible en changeant de serveur
  - retiré du **compteur et de l'échantillon de joueurs** affichés sur le ping du proxy dans la liste multijoueur
  - les joueurs avec `veloadmin.vanish.see` (OP par défaut) continuent de tout voir
- 🚨 **`/report <joueur> <raison>`** — signalement accessible à tous les joueurs
- 📋 **`/reports`** — liste des signalements avec bouton cliquable `[Basculer]` pour marquer vérifié/non vérifié
- 🔨 **`/tempban <joueur> <durée> <serveur|ALL> <raison>`** — bannissement temporaire sur un serveur précis ou sur **tout le réseau**
- ⌨️ **Tab-completion complète** — toutes les commandes utilisent l'API **Brigadier** : arguments typés, suggestions de joueurs (les joueurs en vanish sont exclus sauf pour les admins), de serveurs et d'`ALL`
- 🔑 **Accès automatique pour les OP** — aucun plugin de permissions requis : le bridge envoie le vrai statut OP de chaque joueur au proxy à la connexion et toutes les 10 secondes (les `/op`/`/deop` en jeu sont détectés). LuckPerms & co restent pris en compte en plus
- 💾 **Stockage SQLite autonome** — aucune configuration, le fichier `veloadmin.db` se crée tout seul au premier démarrage

## 🆕 Nouveautés de la version 1.3.0

- **Commandes réécrites avec Brigadier** : fini le texte rouge dans le client, arguments typés et suggestions intelligentes (joueurs, serveurs, `ALL`) directement dans la barre de commande
- **Vanish réseau complet** : le joueur vanish disparaît désormais de la tab-list de **tous les serveurs**, du `/list`, et même du **ping du proxy** dans la liste des serveurs multijoueur
- **Synchronisation automatique des OP** : les opérateurs obtiennent toutes les permissions admin sans rien configurer, avec détection en direct des `/op` et `/deop`
- **Respect du vanish dans les suggestions** : les joueurs invisibles n'apparaissent plus dans la tab-completion pour les non-admins
- **Bridge enrichi** : nouvelle commande `/list` filtrée côté Paper et synchronisation OP en continu

## 📦 Installation

1. Télécharge les deux `.jar` depuis la [dernière release](../../releases/latest)
2. Place **`VeloAdmin.jar`** dans `plugins/` du proxy Velocity
3. Place **`VeloAdminBridge.jar`** dans `plugins/` de **chaque** serveur Paper 1.21.x
4. Redémarre le proxy et tous les serveurs

## 🎮 Commandes

| Commande | Permission | Description |
|---|---|---|
| `/tpgui` | `veloadmin.tpgui` | Menu cliquable de téléportation inter-serveurs |
| `/vanish` | `veloadmin.vanish` | Active/désactive ton invisibilité |
| `/report <joueur> <raison>` | `veloadmin.report` (tout le monde par défaut) | Signale un joueur |
| `/reports` ou `/reports all` | `veloadmin.admin.reports` | Liste les signalements (non vérifiés ou tous) |
| `/tempban <joueur> <durée> <serveur\|ALL> <raison>` | `veloadmin.admin.ban` | Bannit temporairement, serveur précis ou réseau entier |

**Format de durée** : combinaison de `w` (semaines), `d` (jours), `h` (heures), `m` (minutes), `s` (secondes) — ex : `1d12h`, `45m`, `3h30m`.

Exemples :
```
/tempban Steve 1d ALL Insultes répétées
/tempban Steve 2h survie AFK farming interdit
/reports
/tpgui
```

## 🔐 Permissions

| Permission | Défaut | Description |
|---|---|---|
| `veloadmin.tpgui` | OP | Accès au menu de téléportation |
| `veloadmin.vanish` | OP | Accès au vanish |
| `veloadmin.vanish.see` | OP | Voir les joueurs en vanish (jeu + menus) |
| `veloadmin.report` | tous | Pouvoir signaler un joueur |
| `veloadmin.admin.reports` | OP | Gérer les signalements |
| `veloadmin.admin.ban` | OP | Utiliser `/tempban` |

## 🛠️ Compiler depuis les sources

Prérequis : **JDK 17+** et **Maven**.

```bash
git clone https://github.com/herocraftlol/Admintools.git
cd Admintools
mvn clean package
```

Tu récupères :
- `veloadmin/target/VeloAdmin.jar` → proxy Velocity
- `veloadminbridge/target/VeloAdminBridge.jar` → chaque serveur Paper

## ⚠️ Limites connues

- Le menu `/tpgui` reste un menu **dans le chat** (cliquable), pas une GUI en coffre — c'est une contrainte du proxy
- Les logs **console** du serveur peuvent encore afficher la connexion d'un joueur vanish (log serveur, pas message de jeu)
- Un plugin tiers lisant `Bukkit.getOnlinePlayers()` directement verra toujours les joueurs vanish (seul `/list` est réécrit)
- La résolution UUID des joueurs **hors ligne** utilise le mode offline classique ; en réseau `online-mode`, adapter si besoin

---

Développé par **herocraftlol** — licence libre, contributions bienvenues 🎉
