package dev.cwby.bakashi.scrapper;

import dev.cwby.bakashi.data.AnimePage;
import dev.cwby.bakashi.data.EpisodeData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/** Simple interface for creating and managing multiples website anime scrappers. */
public interface IScrapper {

  /**
   * Fetches the last episodes listed on the index of the referer page.
   *
   * @return A list of {@link EpisodeData} objects representing the most recent episodes.
   * @throws IOException If there is an issue fetching or parsing the webpage.
   */
  List<EpisodeData> getLastEpisodes() throws IOException;

  /**
   * Search for an anime page using a String search.
   *
   * @return A page containing a list of {@link EpisodeData} objects representing the episodes.
   * @throws IOException If there is an issue while fetching or parsing the response
   */
  List<AnimePage> findAnimePage(String search) throws IOException;

  /**
   * Fetches all episodes of the AnimePage.
   *
   * @return A list of {@link EpisodeData} objects representing the episodes for the specified anime
   *     page.
   * @throws IOException If there is an issue while fetching or parsing the response
   */
  List<EpisodeData> fetchEpisodesFromPage(AnimePage animePage) throws IOException;

  /**
   * Extracts the video URL from a given episode page, this is specific for every referer page.
   *
   * @param episodeLink The URL of the episode page.
   * @return The direct video content URL.
   * @throws IOException If there is an issue fetching or parsing the page or script data.
   */
  String extractVideoUrl(final String episodeLink) throws IOException;

  /**
   * The root endpoint, or the referer website page
   *
   * @return the referer page e.g "https://bakashi.tv/"
   */
  String referer();

  /**
   * Fetches an HTML document from a given URL using Jsoup.
   *
   * @param url The URL to fetch.
   * @return A {@link Document} object representing the fetched HTML.
   * @throws IOException If there is an issue fetching the URL.
   */
  default Document fetchDocument(final String url) throws IOException {
    return Jsoup.parse(URI.create(url).toURL(), 60000); // 60-second timeout
  }
}
