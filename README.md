# 🛡️ VeloAdmin — Outils d'administration pour réseau Velocity

**VeloAdmin** est une suite d'outils d'administration pensée pour les réseaux
Minecraft propulsés par **Velocity 4.x** (proxy) avec des serveurs **Paper 1.21.x**
en backend. Elle regroupe en un seul plugin les fonctionnalités essentielles de
modération : téléportation entre serveurs, invisibilité (vanish), signalements
joueurs et bannissements temporaires — le tout centralisé au niveau du proxy.

---

## ✨ Fonctionnalités

- 🧭 **`/tpgui`** — Un menu cliquable directement dans le chat : choisissez un
  serveur, puis un joueur, et téléportez-vous instantanément vers lui, où qu'il
  soit sur le réseau.
- 👻 **`/vanish`** — Devenez invisible en un instant. Le vanish est appliqué
  **réellement en jeu** grâce au plugin bridge installé sur chaque serveur
  Paper, et synchronisé entre tous les serveurs du réseau.
- 🚩 **`/report`** — Permet à n'importe quel joueur de signaler un fautif. Les
  signalements sont stockés dans une base centralisée, consultable par
  l'équipe via `/reports` avec des boutons cliquables pour les marquer comme
  vérifiés.
- 🔨 **`/tempban`** — Bannissez temporairement un joueur, sur un serveur
  précis ou sur **l'ensemble du réseau** (`ALL`), avec des durées flexibles
  (`1d12h`, `45m`, `3h30m`…). Déconnexion immédiate et refus de reconnexion
  tant que le ban est actif.
- 💾 **Base SQLite intégrée** — Aucune base externe à configurer : tout est
  stocké dans un simple fichier créé automatiquement au premier démarrage.

---

## 📦 Pourquoi deux JAR ?

Velocity est un **proxy** : il gère les connexions réseau, mais n'a pas accès
au monde de jeu (inventaires, positions, invisibilité). VeloAdmin se compose
donc de deux plugins complémentaires :

| JAR | Où l'installer ? | Rôle |
|---|---|---|
| **VeloAdmin.jar** | `plugins/` du proxy **Velocity** | Commandes, base de données, logique réseau, menus cliquables |
| **VeloAdminBridge.jar** | `plugins/` de **chaque** serveur Paper | Exécute la vraie téléportation et le vrai vanish en jeu, à la demande du proxy |

> ⚠️ Sans le bridge sur un serveur, `/tpgui` connectera bien le joueur au bon
> serveur mais sans téléportation précise, et `/vanish` restera un simple état
> logique sans invisibilité réelle.

---

## 🚀 Installation

1. Téléchargez les deux JAR depuis la page
   [Releases](https://github.com/herocraftlol/Admintools/releases).
2. Déposez `VeloAdmin.jar` dans le dossier `plugins/` de votre proxy Velocity.
3. Déposez `VeloAdminBridge.jar` dans le dossier `plugins/` de **chaque**
   serveur Paper 1.21.x du réseau.
4. Redémarrez le proxy et les serveurs. C'est prêt ! ✅

---

## 🎮 Commandes

| Commande | Permission | Description |
|---|---|---|
| `/tpgui` | `veloadmin.tpgui` | Menu cliquable : choisissez un serveur puis un joueur pour vous téléporter vers lui, sur n'importe quel serveur du réseau |
| `/tpto <serveur> <joueur>` | `veloadmin.tpgui` | Téléportation directe, sans passer par le menu |
| `/vanish` | `veloadmin.vanish` | Active/désactive votre invisibilité (appliquée réellement par le bridge côté serveur) |
| `/report <joueur> <raison>` | `veloadmin.report` (activé pour tous par défaut) | Signale un joueur à l'équipe de modération |
| `/reports` ou `/reports all` | `veloadmin.admin.reports` | Liste les signalements (non vérifiés, ou tous) avec un bouton cliquable pour basculer vérifié/non vérifié |
| `/tempban <joueur> <durée> <serveur\|ALL> <raison>` | `veloadmin.admin.ban` | Bannit temporairement un joueur, sur un serveur précis ou sur tout le réseau |

### ⏱️ Format des durées

Combinaison libre de `w` (semaines), `d` (jours), `h` (heures), `m` (minutes),
`s` (secondes) — par exemple : `1d12h`, `45m`, `3h30m`.

### 💡 Exemples

```
/tempban Steve 1d ALL Insultes répétées
/tempban Steve 2h survie AFK farming interdit
/reports
/reports all
/tpgui
```

---

## 🔑 Permissions

| Permission | Effet |
|---|---|
| `veloadmin.tpgui` | Accès à `/tpgui` et `/tpto` |
| `veloadmin.vanish` | Accès à `/vanish` |
| `veloadmin.vanish.see` | Voir les joueurs invisibles, dans `/tpgui` **et** en jeu (à donner aux admins/modos, sur le proxy **et** sur Paper) |
| `veloadmin.report` | Accès à `/report` (activée par défaut pour tous les joueurs) |
| `veloadmin.admin.reports` | Accès à `/reports` |
| `veloadmin.admin.ban` | Accès à `/tempban` |

---

## 💾 Stockage

Une base **SQLite** (`plugins/veloadmin/veloadmin.db` côté proxy) contient
deux tables : `reports` et `bans`. Aucune dépendance externe à installer : le
fichier se crée tout seul au premier démarrage et le driver SQLite est
directement embarqué dans le JAR.

---

## 🆕 Nouveautés

### v1.2.0 — Build corrigé & artefacts publiés 📦

- 🔧 **Chaîne de compilation corrigée** : le processeur d'annotations
  Velocity 4.x génère désormais correctement le `velocity-plugin.json`
  embarqué dans le JAR — indispensable pour que le proxy reconnaisse et
  charge le plugin au démarrage.
- 🔢 **Numéro de version harmonisé** (`1.2.0`) partout : `pom.xml`
  (parent + modules), `plugin.yml` du bridge et annotation `@Plugin`.
- 📦 **JAR prêts à l'emploi** : `VeloAdmin.jar` et `VeloAdminBridge.jar`
  sont compilés et publiés directement dans la release, avec le driver
  SQLite déjà intégré — aucune compilation requise de votre côté.

### v1.1.0 — Compatibilité Velocity 4.x ⬆️

- ⬆️ Migration de l'API Velocity **3.3.0 → 4.1.0-SNAPSHOT** : le plugin est
  désormais compatible avec les proxies **Velocity 4.0.x** et plus récents.
- 🔧 Correction du build : ajout du dépôt de snapshots PaperMC et déclaration
  explicite du processeur d'annotations Velocity 4.x (séparé depuis JDK 23+).
- 🛠️ Le build nécessite désormais **JDK 25** pour compiler (le bytecode du
  plugin reste compatible Java 17).

### v1.0.0 — Première version publique 🎉

- 🧭 Menu de téléportation inter-serveurs `/tpgui` (et `/tpto` en direct)
- 👻 Vanish réseau `/vanish`, appliqué réellement en jeu via le bridge Paper
- 🚩 Système de signalements `/report` + gestion `/reports` avec boutons cliquables
- 🔨 Bannissements temporaires `/tempban`, ciblés par serveur ou globaux (`ALL`)
- 💾 Stockage SQLite embarqué, zéro configuration
- 🔗 Plugin bridge VeloAdminBridge pour la synchronisation proxy ↔ serveurs

---

## 🔧 Compiler depuis les sources

Prérequis : **JDK 25** (requis par le processeur d'annotations Velocity 4.x)
et **Maven**.

```bash
git clone https://github.com/herocraftlol/Admintools.git
cd Admintools
mvn clean package
```

Vous obtenez :

- `veloadmin/target/VeloAdmin.jar` → proxy Velocity
- `veloadminbridge/target/VeloAdminBridge.jar` → serveurs Paper

---

## 🗺️ Limites connues & pistes d'amélioration

- Le menu est en **chat cliquable** (Adventure), pas une vraie GUI en coffre :
  une vraie interface d'inventaire nécessiterait d'étendre le bridge.
- Le vanish masque les joueurs en jeu (`hidePlayer`/`showPlayer`) mais ne
  cache pas encore la tab-list du proxy ni les messages de connexion.
- Pour bannir un joueur **hors ligne**, l'UUID calculé est celui du mode
  offline ; en mode online, une résolution via l'API Mojang serait à envisager.
