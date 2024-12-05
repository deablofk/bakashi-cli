package dev.cwby.bakashi.process;

import dev.cwby.bakashi.Main;
import dev.cwby.bakashi.ThumbnailUtils;
import dev.cwby.bakashi.data.AnimePage;
import dev.cwby.bakashi.data.EpisodeData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class FzfManager {
  // TODO: requires  refactor and proper documentation

  private Process process;
  private final UeberzugManager ueberzugManager;
  private final boolean isUeberzugPresent;
  private List<EpisodeData> episodeDataList = new ArrayList<>();
  private List<AnimePage> animePageList = new ArrayList<>();

  public FzfManager(final UeberzugManager ueberzugManager) {
    this.isUeberzugPresent = UeberzugManager.checkUeberzugPresence();
    this.ueberzugManager = ueberzugManager;
  }

  public Process spawn() {
    if (isUeberzugPresent) {
      ueberzugManager.spawn();
      final String fullcmd =
          "(fzf --reverse --preview=\"ueberzug cmd -s "
              + ueberzugManager.getSocket()
              + " -i bakashicli -a add -x \\$FZF_PREVIEW_LEFT -y \\$FZF_PREVIEW_TOP --max-width \"\\$FZF_PREVIEW_COLUMNS\" --max-height \"\\$FZF_PREVIEW_LINES\" -f "
              + Main.THUMBNAIL_FOLDER
              + "{}"
              + ThumbnailUtils.THUMB_EXTENSION
              + "\")";
      this.process = startShProcess(fullcmd);
    } else {
      this.process = startShProcess("(fzf --reverse)");
    }

    return this.process;
  }

  public void writeEpisodes(final List<EpisodeData> episodeDataList) {
    this.episodeDataList = episodeDataList;
    final PrintWriter writer = new PrintWriter(process.getOutputStream());
    for (final EpisodeData episodeData : episodeDataList) {
      if (!ThumbnailUtils.thumbnailExists(episodeData.episodeName())) {
        try {
          if (episodeData.thumbnailUrl() != null && episodeData.episodeName() != null) {
            ThumbnailUtils.fetchThumbnail(episodeData.thumbnailUrl(), episodeData.episodeName());
          }
        } catch (IOException e) {
        }
      }
      writer.println(episodeData.episodeName());
    }
    writer.flush();
    writer.close();
  }

  public void writeAnimePages(final List<AnimePage> pageList) {
    this.animePageList = pageList;
    final PrintWriter writer = new PrintWriter(process.getOutputStream());
    for (final AnimePage page : animePageList) {
      if (!ThumbnailUtils.thumbnailExists(page.title())) {
        try {
          ThumbnailUtils.fetchThumbnail(page.thumbnail(), page.title());
        } catch (IOException e) {
        }
      }
      writer.println(page.title());
    }
    writer.flush();
    writer.close();
  }

  public String getResult() {
    try {
      final int exitCode = process.waitFor();
      if (exitCode == 0) {
        return new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
      }
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public EpisodeData getEpisodeDataFromResult(final String result) {
    for (final EpisodeData episodeData : episodeDataList) {
      if (episodeData.episodeName().equals(result)) {
        return episodeData;
      }
    }
    return null;
  }

  public AnimePage waitForAnimeSelect() {
    String result = getResult();
    for (final AnimePage page : animePageList) {
      if (page.title().equals(result)) {
        return page;
      }
    }
    return null;
  }

  public EpisodeData waitForEpisodeSelect() {
    return getEpisodeDataFromResult(getResult());
  }

  public void exit() {
    if (isUeberzugPresent) {
      ueberzugManager.exit();
    }
    try {
      process.destroyForcibly().waitFor();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static Process startShProcess(final String fullCommand) {
    try {
      return new ProcessBuilder("sh", "-c", fullCommand).redirectErrorStream(true).start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
