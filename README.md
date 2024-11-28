# Bakashi CLI

Bakashi CLI is a Java-based application designed to scrape episode information and stream videos from the Bakashi.tv
website. It integrates tools like fzf for fuzzy searching, mpv for video playback, and optionally Ueberzug for enhanced
terminal image previews.

This guide will walk you through building and running the project, as well as understanding its dependencies.

---

### Features

Scrape and display the latest episodes from Bakashi.tv.
Stream videos directly via mpv.
Optional thumbnail previews using Ueberzug for an enhanced terminal experience.
Fuzzy search support with fzf.

---

### Dependencies

Java 21 - The project requires Java 21 or later for compilation and runtime.

[fzf](https://github.com/junegunn/fzf) - A command-line fuzzy finder used for selecting episodes interactively.

[mpv](https://github.com/mpv-player/mpv) - A media player used to stream video content directly.

[yt-dlp](https://github.com/yt-dlp/yt-dlp) - A tool for fetching and processing video URLs.

[Ueberzug](https://github.com/jstkdng/ueberzugpp) (Optional) - A tool for rendering images in terminals. This enhances
the experience by displaying episode
thumbnails.

---

### Building the Project

This project uses Gradle as its build system. Follow the steps below to build either a runnable JAR or a native image:

#### 1. Building a Runnable JAR

The shadowJar task bundles all dependencies into a single runnable JAR file.

```bash
./gradlew shadowJar
```

After the build is complete, the resulting JAR file will be located in: `build/libs/bakashi-cli.jar`

---

#### 2. Building a Native Image

For improved performance and portability, you can build a native image using GraalVM.

##### Requirements:

GraalVM installed and configured.
The native-image tool available in your environment. Ensure it is added to the system's PATH.
Build the native image using:

```bash
./gradlew nativeCompile
```

The compiled binary will be available in: `build/native/nativeCompile/bakashi-cli`

## Usage

Run the application using either the JAR or the native binary. The CLI provides interactive options to list, search, and
stream episodes.

```bash
# for running jar file
java -jar build/libs/bakashi-cli-all.jar
```

or

```bash
# for native compile
./build/native/nativeCompile/bakashi-cli
```
