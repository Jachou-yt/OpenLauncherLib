[version]: https://img.shields.io/maven-central/v/fr.flowarg/openlauncherlib.svg?label=Download
[download]: https://search.maven.org/search?q=g:%22fr.flowarg%22%20AND%20a:%22openlauncherlib%22

[ ![version][] ][download]

# OpenLauncherLib

Add OpenLauncherLib in your dependencies :

```groovy
repositories {
    mavenCentral()
}
```

```groovy
dependencies {
    implementation 'fr.flowarg:openlauncherlib:VERSION'
}
```

## External Java Launching

You can launch a Java program using the external system. It launches directly the java program, to run a simple runnable jar with its librairies.

```java
ClasspathConstructor constructor = new ClasspathConstructor();
constructor.add(new File("mymainjar.jar"));
constructor.add(Explorer.dir("libs").files());

ExternalLaunchProfile profile = new ExternalLaunchProfile("fr.theshark34.MyClass", classpath.make());
ExternalLauncher launcher = new ExternalLauncher(profile);

Process p = launcher.launch(); // throws LaunchException
```

This will launch a java process with in classpath: mymainjar.jar and all the files of the libs folder.
You can also add program/vm parameters, and a LaunchingEvent to customize the ProcessBuilder just before launching.

## Tools

There are a lot of tools that can be used before or after the program launching.

### The Single Saver

The Saver uses Java Properties to save or load some data. It is useful if your launch has authentication by exemple to save the user name, or things like this.

```java
Saver saver = new Saver(new File("myfile.properties"));
saver.set("username", "jack");
String age = saver.get("age");
```

The data is automatically saved when you do set()

### The Splash Screen

The Splash Screen can be used to display a simple splash that you can personalize.

```java
SplashScreen splash = new SplashScreen("MySplashTitle", mySplashImage);
splash.add(new JProgressBar());
splash.displayFor(5000L);
```

### The Ram Selector

The Ram Selector can be used to display a Ram Choosing Frame easily

```java
RamSelector selector = new RamSelector(new File("ram.txt"));
selector.display();

// Then when your launching
String[] ramArguments = selector.getRamArguments();
```

## Minecraft

There is a support for Minecraft launching, you can use these tools to create a Minecraft Launcher:

### Minecraft Launcher

You can use the Minecraft Launcher to create external launch profile for Minecraft

```java
GameInfos infos = new GameInfos("MyMinecraft", new GameVersion("1.7.2", GameType.V1_7_2_LOWER), new GameTweak[] {GameTweak.FORGE});
AuthInfos authInfos = new AuthInfos("PlayerUsername", "token", "uuid");

ExternalLaunchProfile profile = MinecraftLauncher.createExternalProfile(infos, GameFolder.BASIC, authInfos);
ExternalLauncher launcher = new ExternalLauncher(profile);

launcher.launch();
```
