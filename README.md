![armsrace logo](https://cdn.modrinth.com/data/cached_images/94c7c54ee549e6310e8aac8e630beed3b855d217.png)

# 🔫 Arms Race (Mini-Game Mod)
**⚠️ BETA** *This mod is currently in early development. Core mechanics work, but full compatibility with other mods is still being tested. Bug reports and feedback are highly appreciated!*

Welcome to **Arms Race** — a fully customizable, server-side friendly mini-game mod for NeoForge! 

Bring the classic "Gun Game" experience to your Minecraft server. Players battle each other to upgrade their weapons. The first player to get a kill with the final weapon on the list wins the match!

This mod is an independent core. It relies on vanilla mechanics, meaning it can technically issue ANY item as a weapon (Vanilla swords, bows, etc.). 

### ✨ Current Features
*   **Customizable Arenas:** Create multiple arenas via a simple JSON config.
*   **Team Support:** Set up Free-For-All or Team Deathmatch modes.
*   **Custom Weapon & Armor Ladders:** Define your own progression list using item IDs. You can configure automatic armor equipping and extra items (like ammo or shields) for each level!
*   **Dynamic UI:** Built-in clean Scoreboard to track kills, lobby status, and warmup timers.
*   **Safe Environment:** Configurable block-breaking protection and spawn point management.

### 📜 Commands (Requires OP / Permission Level 2 for setup)
*   `/armsrace create <template_id>` - Creates a lobby based on the config template.
*   `/armsrace join` - Joins an available lobby.
*   `/armsrace leave` - Leaves the current lobby.
*   `/armsrace start` - Force starts the game (bypasses the warmup).
*   `/armsrace stop` - Stop game and delete lobby.
*   `/armsrace setteam <player> <team_id>` - Moves a player to a specific team.
*   `/armsrace reload` - Reloads the JSON config without restarting the server!

### 🛠️ Configuration Guide
When you run the mod for the first time, it will generate an advanced default configuration file located at `config/armsrace_arenas.json`. 

The config allows deep customization of weapons, team spawns, armor progression, and even specific inventory slots!
How to set up the config - completely written in the [wiki](https://github.com/aleksti21/ArmsRace/wiki/1.-Home)
