# Trinkets
A data-driven accessory mod for Minecraft using Fabric.

![Image of the Trinkets UI](https://i.imgur.com/CgWhc5a.png)

## About
Trinkets adds a slot group and slot system to Minecraft. Slot groups are collections of slots for a certain body part or more vague area. By default there are 6 slot groups (head, chest, legs, feet, offhand, hand) that can have slots added to them, but more groups can be added if desired. Trinkets' UI is intuitive to use, accessible, and attempts to do away with clutter. Its system means that you'll never have a slot that's not used for anything, as mods request the slots they want.

## Developers
To add Trinkets to your project you need to add jitpack and nerdhubmc to your repositories in your build.gradle
```
repositories {
	maven {
		name = "TerraformersMC"
		url = "https://maven.terraformersmc.com/"
	}
	maven {
		name = "Ladysnake Libs"
		url = "https://ladysnake.jfrog.io/artifactory/mods"
	}
}
```
And then to add Trinkets you add it as a dependency in your build.gradle
```
dependencies {
	modImplementation "dev.emi:trinkets:${trinkets_version}"
}
```

For basic tutorials and comprehensive documentation, visit this repository's [wiki](https://github.com/emilyploszaj/trinkets/wiki/Home).
