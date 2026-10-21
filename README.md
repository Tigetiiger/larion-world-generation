> [!WARNING]
> **Disclaimer:** This is an unofficial fork of [Larion: Disc World](https://github.com/ViciousBadger/larion-disc-world) by Badgerson, ported to Minecraft 26.2 with AI. The port's changes were made by an AI assistant (Claude) and tested automatically, not written by hand. It is not made or endorsed by the original author. It is meant to be used with the unofficial [Larion 26.2 port](https://github.com/Tigetiiger/larion-world-generation) (Fabric, Larion 4.3.1).
>
> 26.2 changes: `pack.mcmeta` updated to the new pack format (107); the Fabric mod version now requires Minecraft 26.2 and Larion 4.3.1+ (and still Fabric API). The worldgen data is unchanged.

This is a simple add-on for my Larion World Generation mod that limits land
generation to a circle of a configurable radius. The shape of the
landmass(es) within this circle vary wildly between seeds. The temperature bands have also been altered to fit within the circle, with frozen biomes north and hot biomes south.

Only two files from Larion have been changed to make this possible - continents
and the temperature sinewave, which has been made slightly smaller and capped so
it doesn't repeat.

This mod/datapack depends on Larion to work and must be loaded **after**
Larion. If you use the Fabric mod versions, this should happen automagically,
but if you use the datapack versions, you may have to look into Paxi or similar
tools to get them to order correctly.

You may have a hard time finding woodland mansions - if your world has none,
you'll have to create one yourself with cheats!

If you want to make the generated area larger or smaller it's easy to do so, just edit the file `data/laron/worldgen/density_function/overworld/circle.json` file and change the two really small fractional numbers to something else, or even make them differ from each other for an ellipsis world.  

The numbers correspond to the disk radius (one per world axis) in blocks to the power of -1. For
example if you want a radius of 5000, you can write 0.0002, because `5000^-1 = 0.0002`.

You can adjust the other small number in `temperature_delta.json` to fit with the new size. This one relates to the "length" in blocks of the temperature curve like this: `number = length^-1 * 3.14`. Try adjusting it and using a mod like World Preview to see if it fits.
