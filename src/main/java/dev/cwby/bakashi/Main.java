package dev.cwby.bakashi;

import dev.cwby.bakashi.data.AnimePage;
import dev.cwby.bakashi.data.EpisodeData;
import dev.cwby.bakashi.process.FzfManager;
import dev.cwby.bakashi.process.UeberzugManager;
import dev.cwby.bakashi.scrapper.IScrapper;
import dev.cwby.bakashi.scrapper.ScrapperManager;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Main {
  // TODO: remember to put fillers tags (https://www.animefillerlist.com/shows/naruto-shippuden)

  public static final String TEMP = "/tmp/bakashicli/";
  public static final String THUMBNAIL_FOLDER = TEMP + "thumbnails/";

  private static void makeTempDir() {
    var thumbDir = new File(THUMBNAIL_FOLDER);
    if (!thumbDir.exists()) {
      thumbDir.mkdirs();
    }
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    makeTempDir();

    FzfManager fzfManager = new FzfManager(new UeberzugManager());
    IScrapper scrapper = ScrapperManager.getScrapper("anroll");

    List<EpisodeData> episodesToPlay = new ArrayList<>();

    for (int i = 0; i < args.length; i++) {
      final String arg = args[i];
      switch (arg.toLowerCase()) {
        case "-o":
          if (i < (args.length - 1)) {
            scrapper = ScrapperManager.getScrapperOrDefault(args[i + 1]);
          } else {
            System.out.println("Expected a value for -o, e.g 'anroll' (is the only available)");
          }
          break;
        case "-l":
          fzfManager.spawn();
          fzfManager.writeEpisodes(scrapper.getLastEpisodes());
          episodesToPlay.add(fzfManager.waitForEpisodeSelect());
          break;
        case "-s":
          if (i < (args.length - 1)) {
            String search = URLEncoder.encode(args[i + 1], StandardCharsets.UTF_8);
            List<AnimePage> animesPages = scrapper.findAnimePage(search);
            fzfManager.spawn();
            fzfManager.writeAnimePages(animesPages);
            AnimePage page = fzfManager.waitForAnimeSelect();
            if (page != null) {
              fzfManager.exit();
              fzfManager.spawn();
              List<EpisodeData> episodes = scrapper.fetchEpisodesFromPage(page);
              episodes.add(new EpisodeData("CMD: Play All", null, null));
              fzfManager.writeEpisodes(episodes);
              String result = fzfManager.getResult();
              if (result.equals("CMD: Play All")) {
                episodesToPlay.addAll(episodes);
              } else {
                episodesToPlay.add(fzfManager.getEpisodeDataFromResult(result));
              }
            }
          } else {
            System.out.println("Expected a value for -s, e.g 'naruto'");
          }
          break;
        case "-h":
          displayHelp();
          break;
      }
    }

    try {
      fzfManager.exit();
    } catch (Exception e) {
    }

    if (!episodesToPlay.isEmpty()) {
      for (EpisodeData episode : episodesToPlay) {
        if (episode != null) {
          String videoUrl = scrapper.extractVideoUrl(episode.episodeUrl());
          spawnMpv(scrapper.referer(), videoUrl, episode.episodeName());
        }
      }
    } else {
      displayHelp();
    }
  }

  // TODO: maybe extract this to another class
  private static void spawnMpv(String referer, String url, String title)
      throws IOException, InterruptedException {
    new ProcessBuilder(
            "bash",
            "-c",
            "mpv -fs --force-media-title='"
                + title
                + "' --http-header-fields='referer: "
                + referer
                + "' '"
                + url
                + "'")
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
        .waitFor();
  }

  private static void displayHelp() {
    var builder = new StringBuilder();
    builder.append("Bakashi-CLI 1.0\n\n");
    builder.append("OPTIONS:\n");
    builder.append("\t-s").append("\tanime search\n");
    builder.append("\t-l").append("\tdisplay the latests episodes available\n");
    builder.append("\t-o").append("\tset the origin for search: anroll\n");
    System.out.println(builder);
  }
}
