#!/usr/bin/env sh
filename='Larion-disc-world-1.7.0+26.2'
# Just a quick script used to distribute the mod
7z a "$1/${filename}.jar" data fabric.mod.json pack.mcmeta pack.png LICENSE.md
# Zip extension = datapack (Mojang made it so jar files cannot be used as datapacks.. for some reason..)
7z a "$1/${filename}.zip" data fabric.mod.json pack.mcmeta pack.png LICENSE.md

echo "packed to $1"
