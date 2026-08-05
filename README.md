<img alt="Better /help Header" src="static/header.png">

<p align="center">
  <strong>A Minecraft mod that replaces <code>/help</code> with a clearer, interactable, paginated list.<br>
  Adds descriptions for commands and arguments, plus optional metadata from other mods.</strong>
</p>

<p align="center">
  <a href="https://betterhelp.expecticament.com"><img alt="documentation" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/ghpages_vector.svg"></a>
  <a href="https://github.com/expecticament/better-help"><img alt="github" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy-minimal/available/github_vector.svg"></a>
  <a href="https://modrinth.com/mod/better-help"><img alt="modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy-minimal/available/modrinth_vector.svg"></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/better-help"><img alt="curseforge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy-minimal/available/curseforge_vector.svg"></a>
  <a href="https://ko-fi.com/expecticament"><img alt="kofi" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy-minimal/donate/kofi-singular_vector.svg"></a>
</p>

## ✨ Features

Vanilla `/help` throws a hard-to-read wall of text at you with almost no useful detail. **Better /help** replaces it with something players can actually use.

- **Clear, paginated command list**: browse commands page by page instead of scrolling through a wall of text
- **Interactive output**: hover for details and click to navigate pages
- **Descriptions for commands and arguments**: most vanilla commands are covered, and arguments can have their own descriptions too
- **Easy to integrate with**: other mods can add descriptions and metadata (like homepage links) using only their language files
- **Attribution**: when a description comes from another mod, players can see which mod the command is from
- **Suggestions**: `/help <command...>` autocompletes like typing the command itself, unlike vanilla `/help`

## ⌨️ Usage

| Command              | What it does                         |
|----------------------|--------------------------------------|
| `/help`              | Displays the paginated command list  |
| `/help <command...>` | Shows help for a command or argument |

Examples:

```text
/help
/help gamemode
/help advancement grant @s everything
```

## 💻 For mod developers

You can add descriptions and other metadata with language files (for example `assets/<mod_id>/lang/en_us.json`).
**Better /help** scans installed mods for those keys on server startup. Translations are resolved server-side.

<details>
<summary>Command or argument descriptions</summary>

When you add a command description, **Better /help** attributes that command to your mod:

```json
{
  "examplemod.commands.examplecommand.description": "Description of your command."
}
```

Replace `examplemod` with your mod's id, and `examplecommand` with your command's name.

You can omit the mod id prefix, but that is not recommended because of translation key conflicts:

```json
{
  "commands.examplecommand.description": "Description of your command."
}
```

You can also describe nested literals and arguments. Path segments are Brigadier **node names** (the same strings you pass to `Commands.literal("name")` and `Commands.argument("name", ...)`, not what the player types):

```json
{
  "examplemod.commands.examplecommand.targets.description": "Who the command applies to."
}
```

For example, an `EntityArgument` registered as `Commands.argument("targets", EntityArgument.players())` uses `targets` in the translation key, not `@s`, `@a`, or a player name.

Examples from **Better /help**:

```json
{
  "commands.advancement.grant.description": "Grants player advancements.",
  "commands.advancement.grant.targets.everything.description": "Grants all advancements to players.",
  "commands.advancement.grant.targets.only.description": "Grants one specified advancement to players."
}
```

</details>

<details>
<summary>Homepage link</summary>

When set, the mod id in `/help <command...>` output can become a clickable link:

```json
{
  "betterhelp.homepage.examplemod": "https://example.com"
}
```

Replace `examplemod` with your mod's id.

You can omit the mod id suffix, but that is not recommended because of translation key conflicts:

```json
{
  "betterhelp.homepage": "https://example.com"
}
```

</details>

> More details are in the [documentation](https://betterhelp.expecticament.com/docs/For%20Developers/Basics).

## ❓ FAQ

**If Better /help is installed on a server, do players also need to install it?**<br>
No. It is a server-side mod. Clients do not need it.

**Does it work in singleplayer?**<br>
Yes.

**Will commands from other mods still show if the developer did not integrate with Better /help?**<br>
Yes, but without their descriptions or other metadata. Usage is still shown.

**Can I include it in a modpack?**<br>
Yes. No need to ask for permission.

## 🔨 How to build

Requirements: a compatible JDK (see `java_version` in `gradle.properties`). [Eclipse Temurin](https://adoptium.net/) is recommended.

```bash
./gradlew build
```

Where to find the jars:

| Loader   | Output                 |
|----------|------------------------|
| Fabric   | `fabric/build/libs/`   |
| NeoForge | `neoforge/build/libs/` |
