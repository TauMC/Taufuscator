# Taufuscator

A simple utility to make running other people's mods in dev easier on Minecraft Forge. (Currently only works with 1.18.2-1.20.1).

## Usage

Download the latest version from https://nightly.link/TauMC/Taufuscator/workflows/build-snapshot/main/Taufuscator.zip
and place it in the `mods` folder of your development workspace (usually `run/mods`).

Create a `mods.deobf` folder beside that one and place valid, obfuscated mod JAR files in that folder. The folder
structure should look like:
```
mods
└── taufuscator.jar
mods.deobf
└── create-1.20.1.jar
```

Lastly, launch the game as normal and mods in `mods.deobf` should load and work normally.