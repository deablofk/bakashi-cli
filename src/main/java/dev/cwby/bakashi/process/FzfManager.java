package dev.cwby.bakashi.process;

import dev.cwby.bakashi.Main;
import dev.cwby.bakashi.data.EpisodeData;
import dev.cwby.bakashi.scrapper.BakashiScrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

public class FzfManager {
  // TODO: requires  refactor and proper documentation

  private Process process = null;

  public Process spawnWithUeberzug(final String ueberzugSocket) {
    final String fullcmd =
        "(fzf --reverse --preview=\"ueberzug cmd -s "
            + ueberzugSocket
            + " -i bakashicli -a add -x \\$FZF_PREVIEW_LEFT -y \\$FZF_PREVIEW_TOP --max-width 200 --max-height 200 -f "
            + Main.THUMBNAIL_FOLDER
            + "{}"
            + BakashiScrapper.THUMB_EXTENSION
            + "\")";
    this.process = startShProcess(fullcmd);
    return this.process;
  }

  public void spawn() {
    this.process = startShProcess("(fzf --reverse)");
  }

  public void writeEpisodes(final List<EpisodeData> episodeDataList) {
    final PrintWriter writer = new PrintWriter(process.getOutputStream());
    for (final EpisodeData episodeData : episodeDataList) {
      if (!BakashiScrapper.thumbnailExists(episodeData.episodeName())) {
        try {
          BakashiScrapper.fetchThumbnail(episodeData.thumbnailUrl(), episodeData.episodeName());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      writer.println(episodeData.episodeName());
    }
    writer.flush();
    writer.close();
  }

  public String waitForSelect() {
    try {
      final int exitCode = process.waitFor();
      if (exitCode == 0) {
        return new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
      }
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
    return "";
  }

  public void exit() {
    process.destroy();
  }

  private static Process startShProcess(final String fullCommand) {
    try {
      return new ProcessBuilder("sh", "-c", fullCommand).redirectErrorStream(true).start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
