# lavabili-plugin
<img src="https://github.com/user-attachments/assets/1bfb4369-6438-4e5e-9e6e-72b94cb69a37" alt="Alt Text" style="width:33%; height:auto;">

## What?
A lavalink plugin written to add Bilibili as an additional audio playing source.

## Why?
This plugin is rebuilt for latest Lavalink (v4) based on [(Allvaa/lp-sources, 2022)](https://github.com/Allvaa/lp-sources).

Differences:
+ Removed unknown & unresolvable build dependencies in [(Allvaa/lp-sources, 2022)](https://github.com/Allvaa/lp-sources).
+ `artworkUrl` extraction from **Bilibili** videos.

## How?
In your `application.yml`, add the following dependency under `lavalink/plugins` section.
```
...
lavalink:
  plugins:
    - dependency: "com.github.hoyiliang:lavabili-plugin:1e2de78246"
      snapshot: false
      repository: "https://jitpack.io"
...
```

Then, under `lavalink/server/sources` section, add `bilibili: true`.
```
...
lavalink:
  server:
    sources:
      bilibili: true
...
```

**Note: The decision to use `MIT License` is derived from [(Allvaa/lp-sources, 2022)](https://github.com/Allvaa/lp-sources).**

# lavalink-plugin-template

**Note: This project has been converted to use Kotlin 1.8.22.**

This is a template for creating a plugin for [Lavalink](https://github.com/lavalink-devs/Lavalink). It is written in
java, but you can also use kotlin (version `1.8.22`) if you want.

## How to use this template

1. Clone this repository
2. Rename the package `com.example.plugin` to your package name
3. Rename the class `ExamplePlugin` to your plugin name
4. Rename the file `ExamplePlugin.java` to your plugin name
5. fill in the `lavalinkPlugin` in [build.gradle.kts](build.gradle.kts)
6. Write your plugin

## How to test your plugin

1. Place a `application.yml` file in the root directory (see [here](https://lavalink.dev/configuration/index.html#example-applicationyml) for an example)
2. Run `./gradlew runLavalink` (for windows: `./gradlew.bat runLavalink`) in the root directory
3. The plugin will be loaded
4. You can now test your plugin
5. If you change something in the plugin, you can just run `./gradlew runLavalink` again

## How to build your plugin

1. Run `./gradlew build` (for windows: `./gradlew.bat build`) in the root directory
2. The jar file will be in `build/libs/`

## How to publish your plugin

This template uses [jitpack](https://jitpack.io/) to publish the plugin. You just need to push your changes to github
and jitpack will build the plugin for you.

## How to use your plugin

Go to [jitpack](https://jitpack.io/) and paste the link to your repository. There you can find the version you want to use.

```yml
lavalink:
  plugins:
    - dependency: com.github.lavalink:lavalink-plugin-template:{VERSION} # replace {VERSION} with the version you want to use from jitpack
      repository: https://jitpack.io
```

## How to get help

If you need help, you can join the [Lavalink Discord Server](https://discord.gg/jttmwHTAad) and ask in
the `#plugin-dev` channel.
