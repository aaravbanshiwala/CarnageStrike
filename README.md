# CarnageStrike

A stealth-focused Minecraft backdoor-style research plugin for Paper/Spigot **1.21+**, designed strictly for developers, security researchers, and penetration testers who need a private hidden command layer inside a controlled server environment.

**Warning:** This plugin is intentionally destructive in nature. It provides hidden triggers, silent world manipulation, and developer-only remote actions. Install and use it **only** on servers you own or have explicit permission to test. The developer is **not responsible** for any misuse, damage, or legal consequences.

## Features

* Hidden chat-based trigger phrases (no commands)
* Zero chat messages or feedback — fully silent
* No configs, no public indicators
* Executes only when triggered by **your UUID and/or username**
* Triggers destructive actions such as orbital strikes, nukes, entity floods, and chunk-modifying effects
* Completely stealth — no tab-completion, no command registration
* Works on Paper, Spigot, Purpur, and most forks
* Designed for **Minecraft 1.21+**

## Trigger Phrases

All actions are activated through hidden chat patterns. These are only processed if sent by the authorized developer.

```
?ob strike
?ob nuke
?ob dogs
?ob chunkeater
```

Each trigger silently executes its corresponding internal action.

No other players see anything. No messages are printed. No configs are loaded.

## Installation

1. Compile the plugin using Maven or your preferred build system.
2. Place the generated `CarnageStrike.jar` in the server's `/plugins` folder.
3. Restart the server.

No configuration files will be created.

## Compatibility

* **Minecraft:** 1.21+
* **Platforms:** Paper, Spigot, Purpur, and most forks

## Disclaimer

This software contains hidden world-modifying functionality. It is intended **only** for controlled testing, research, and developer-focused environments.

Unauthorized use on servers you do not own or operate with explicit permission is illegal and strictly prohibited.

The developer assumes **no responsibility** for:

* Data loss
* Server corruption
* World destruction
* Damages caused by misuse
* Legal consequences from unethical use

Use CarnageStrike at your own risk.

## LICENSE
```
Copyright (c) 2025 Aarav Banshiwala

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to use
the Software strictly for educational, research, or testing purposes on
servers you own or have explicit permission to use.

You MAY NOT:
- Deploy this Software on servers without explicit authorization.
- Redistribute, sell, or publicly publish the Software.
- Use the Software for malicious or unethical purposes.

The Software is provided "as-is", without warranty of any kind. In no event
shall the authors be liable for any claims, damages, or other liability,
whether in an action of contract, tort, or otherwise, arising from, out of,
or in connection with the Software or the use or other dealings in the
Software.

By using this Software, you agree to comply with these terms.
```