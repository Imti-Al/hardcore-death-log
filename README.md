I created this mod to keep a history/logs of deaths on my multiplayer hardcore world. It records the first player death in each world, including who dies, when it happened and the death message. Later deaths in that world do not get recorded.

The server saves the history in `config/hardcore-deaths.json`, so it carries over when starting a new world on the same server. Each world keeps its own small state file to remember whether its first death has already been recorded.

Install mod on both server and client, alongside Fabric API. Downloads are labelled with their Minecraft version. Updating the mod JAR doesn't reset the saved history.
