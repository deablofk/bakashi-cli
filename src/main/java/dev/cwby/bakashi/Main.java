package dev.cwby.bakashi;

import dev.cwby.bakashi.data.EpisodeData;
import dev.cwby.bakashi.process.FzfManager;
import dev.cwby.bakashi.process.UeberzugManager;
import dev.cwby.bakashi.scrapper.BakashiScrapper;

import java.io.*;

public class Main {
  // TODO: documentation

  public static final String TEMP = "/tmp/bakashicli/";
  public static final String THUMBNAIL_FOLDER = TEMP + "thumbnails/";

  public static void makeTempDir() {
    var file = new File(TEMP);
    if (!file.exists()) {
      file.mkdirs();
    }

    var thumbDir = new File(THUMBNAIL_FOLDER);
    if (!thumbDir.exists()) {
      thumbDir.mkdirs();
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    // TODO: destroy (if present) ueberzug if the process is interrupted
    makeTempDir();

    // refactor this shit
    boolean isUeberzugPresent = UeberzugManager.checkUeberzugPresence();

    var ueberzugManager = new UeberzugManager();
    var fzfManager = new FzfManager();

    if (isUeberzugPresent) {
      ueberzugManager.spawn();
      fzfManager.spawnWithUeberzug(ueberzugManager.getSocket());
    } else {
      fzfManager.spawn();
    }

    var episodeDataList = BakashiScrapper.getLastEpisodes();
    fzfManager.writeEpisodes(episodeDataList);

    String selected = fzfManager.waitForSelect();
    for (EpisodeData episodeData : episodeDataList) {
      if (episodeData.episodeName().equals(selected)) {
        if (isUeberzugPresent) {
          ueberzugManager.exit();
        }
        fzfManager.exit();
        spawnMpv(BakashiScrapper.extractVideoUrl(episodeData.episodeUrl())).waitFor();
      }
    }
  }

  // TODO: maybe extract this to another class
  private static Process spawnMpv(String url) {
    try {
      String fullCommand =
          "yt-dlp --add-header 'Referer: https://bakashi.tv' -o - '" + url + "' | mpv -";
      return new ProcessBuilder("sh", "-c", fullCommand).redirectErrorStream(true).start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
