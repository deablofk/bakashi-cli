package dev.cwby.bakashi.scrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cwby.bakashi.Main;
import dev.cwby.bakashi.data.AnimePage;
import dev.cwby.bakashi.data.EpisodeData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Anime site-specific scraper for fetching video links and information about episodes from the
 * Bakashi.tv website.
 *
 * <p>This utility provides methods to: - Retrieve the latest episodes from the website's index
 * page. - Extract video URLs from episode pages using embedded JWPlayer data.
 */
public class BakashiScrapper implements IScrapper {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  /**
   * Fetches the last 20 episodes listed on the Bakashi.tv index page.
   *
   * @return A list of {@link EpisodeData} objects representing the most recent episodes.
   * @throws IOException If there is an issue fetching or parsing the webpage.
   */
  @Override
  public List<EpisodeData> getLastEpisodes() throws IOException {
    final Document document = fetchDocument(referer());
    final Elements articles = extractArticlesFromDashboard(document);

    return articles.stream().map(this::extractEpisodeDataFromArticle).toList();
  }

  @Override
  public List<AnimePage> findAnimePage(String search) throws IOException {
    List<AnimePage> animePages = new ArrayList<>();
    Document document = fetchDocument("https://bakashi.tv/?s=" + search);
    Elements elements = document.getElementsByClass("result-item");
    for (Element element : elements) {
      Element titleElement = element.getElementsByClass("title").getFirst();
      String pageThumbnail = element.getElementsByTag("img").getFirst().attr("src");
      String pageLink = titleElement.getElementsByTag("a").getFirst().attr("href");
      String pageTitle = titleElement.getElementsByTag("a").getFirst().text();
      String pageSynopsis = element.getElementsByClass("contenido").text();
      animePages.add(
          new AnimePage(null, pageTitle, null, pageSynopsis, -1, pageLink, pageThumbnail));
    }
    return animePages;
  }

  @Override
  public List<EpisodeData> fetchEpisodesFromPage(AnimePage animePage) throws IOException {
    List<EpisodeData> episodes = new ArrayList<>();
    Document document = fetchDocument(animePage.genericPath());
    Elements epElements =
        document.getElementsByClass("episodios").getFirst().getElementsByTag("li");

    for (Element element : epElements) {
      Element linkElement = element.getElementsByTag("a").getFirst();
      String link = linkElement.attr("href");
      String title = linkElement.text();
      String thumbnailLink = null;
      Elements imgs = element.getElementsByTag("img");
      if (!imgs.isEmpty()) {
        thumbnailLink = imgs.getFirst().attr("src");
      }
      episodes.add(new EpisodeData(title, link, thumbnailLink));
    }

    return episodes;
  }

  /**
   * Extracts the video URL from a given episode page.
   *
   * <p>The process involves: 1. Fetching the iframe source containing the video player. 2.
   * Extracting the script data from the iframe document. 3. Parsing the script data to retrieve the
   * actual video content URL.
   *
   * @param episodeLink The URL of the episode page.
   * @return The direct video content URL.
   * @throws IOException If there is an issue fetching or parsing the page or script data.
   */
  @Override
  public String extractVideoUrl(final String episodeLink) throws IOException {
    return parseContentUrlFromScript(fetchScriptData(extractIframeSrc(episodeLink)));
  }

  @Override
  public String referer() {
    return "https://bakashi.tv";
  }

  /**
   * Extracts the section containing articles for episodes from the dashboard page.
   *
   * @param document The parsed HTML document of the dashboard page.
   * @return A collection of elements representing episode articles.
   */
  private Elements extractArticlesFromDashboard(final Document document) {
    return document
        .select("#contenedor > div.module > div > div.animation-2.items.full")
        .getFirst()
        .getElementsByTag("article");
  }

  /**
   * Extracts episode details from a specific article element.
   *
   * <p>The details include: - The link to the episode page. - The name of the episode. - The
   * thumbnail image URL for the episode.
   *
   * @param article The article element representing an episode.
   * @return An {@link EpisodeData} object containing the episode details.
   */
  private EpisodeData extractEpisodeDataFromArticle(final Element article) {
    final String episodeLink =
        article.getElementsByClass("data").getFirst().getElementsByTag("a").getFirst().attr("href");

    final String episodeName =
        article.getElementsByClass("data").getFirst().getElementsByTag("a").getFirst().text();

    final String episodeImageLink =
        article
            .getElementsByClass("poster")
            .getFirst()
            .getElementsByTag("picture")
            .getFirst()
            .getElementsByTag("img")
            .getFirst()
            .attr("src");

    return new EpisodeData(episodeName, episodeLink, episodeImageLink);
  }

  /**
   * Extracts the iframe source URL from the given episode page.
   *
   * <p>The iframe source contains the JWPlayer embed, which is used to fetch the video player data.
   *
   * @param episodeLink The URL of the episode page.
   * @return The iframe source URL.
   * @throws IOException If there is an issue fetching or parsing the page.
   */
  private String extractIframeSrc(final String episodeLink) throws IOException {
    final Document episodeDocument = fetchDocument(episodeLink);
    final Element iframeElement =
        episodeDocument.select("#source-player-1 > div > iframe").getFirst();
    return iframeElement.attr("src").split("img")[0];
  }

  /**
   * Fetches the script data containing JSON video details from the iframe document.
   *
   * <p>The JWPlayer dynamically loads the video URL, which is embedded in a script tag in the
   * iframe document.
   *
   * @param iframeSrc The iframe source URL.
   * @return The raw script data as a string.
   * @throws IOException If there is an issue fetching or parsing the iframe document.
   */
  private String fetchScriptData(final String iframeSrc) throws IOException {
    final Document iframeDocument = fetchDocument(iframeSrc);
    final Element scriptElement =
        iframeDocument.select("head").getFirst().getElementsByTag("script").last();
    return scriptElement.data().trim();
  }

  /**
   * Parses the content URL for the video from the JSON script data.
   *
   * @param scriptData The JSON script data as a string.
   * @return The video content URL.
   * @throws IOException If there is an issue parsing the JSON.
   */
  private String parseContentUrlFromScript(final String scriptData) throws IOException {
    return new ObjectMapper().readTree(scriptData).get("contentUrl").asText();
  }
}
