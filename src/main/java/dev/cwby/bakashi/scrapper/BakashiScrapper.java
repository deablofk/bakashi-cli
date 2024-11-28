package dev.cwby.bakashi.scrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cwby.bakashi.Main;
import dev.cwby.bakashi.data.EpisodeData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Anime site-specific scraper for fetching video links and information about episodes from the
 * Bakashi.tv website.
 *
 * <p>This utility provides methods to: - Retrieve the latest episodes from the website's index
 * page. - Extract video URLs from episode pages using embedded JWPlayer data.
 */
public class BakashiScrapper {

  // Root URL of the Bakashi.tv website
  private static final String ROOT_ENDPOINT = "https://bakashi.tv";
  public static final String THUMB_EXTENSION = ".jpg";

  /**
   * Fetches the last 20 episodes listed on the Bakashi.tv index page.
   *
   * @return A list of {@link EpisodeData} objects representing the most recent episodes.
   * @throws IOException If there is an issue fetching or parsing the webpage.
   */
  public static List<EpisodeData> getLastEpisodes() throws IOException {
    final Document document = fetchDocument(ROOT_ENDPOINT);
    final Elements articles = extractArticlesFromDashboard(document);

    return articles.stream().map(BakashiScrapper::extractEpisodeDataFromArticle).toList();
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
  public static String extractVideoUrl(final String episodeLink) throws IOException {
    return parseContentUrlFromScript(fetchScriptData(extractIframeSrc(episodeLink)));
  }

  /**
   * Fetches an HTML document from a given URL using Jsoup.
   *
   * @param url The URL to fetch.
   * @return A {@link Document} object representing the fetched HTML.
   * @throws IOException If there is an issue fetching the URL.
   */
  private static Document fetchDocument(final String url) throws IOException {
    return Jsoup.parse(URI.create(url).toURL(), 60000); // 60-second timeout
  }

  /**
   * Extracts the section containing articles for episodes from the dashboard page.
   *
   * @param document The parsed HTML document of the dashboard page.
   * @return A collection of elements representing episode articles.
   */
  private static Elements extractArticlesFromDashboard(final Document document) {
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
  private static EpisodeData extractEpisodeDataFromArticle(final Element article) {
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
  private static String extractIframeSrc(final String episodeLink) throws IOException {
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
  private static String fetchScriptData(final String iframeSrc) throws IOException {
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
  private static String parseContentUrlFromScript(final String scriptData) throws IOException {
    return new ObjectMapper().readTree(scriptData).get("contentUrl").asText();
  }

  /**
   * Returns the path where the thumbnail for the specified episode name would be stored.
   *
   * <p>Note that this method only returns the file path; the thumbnail file is not actually
   * generated or downloaded by this method. The thumbnail must be fetched using {@link
   * #fetchThumbnail(String, String)}.
   *
   * @param episodeName The name of the episode for which the thumbnail file path is to be
   *     generated.
   * @return The {@link Path} representing the file location for the episode thumbnail.
   */
  private static Path getThumbnailPath(final String episodeName) {
    return Paths.get(Main.THUMBNAIL_FOLDER, episodeName + THUMB_EXTENSION);
  }

  /**
   * Checks if the thumbnail for the specified episode already exists in the temporary storage.
   *
   * <p>This method is useful for skipping redundant thumbnail downloads by verifying if the
   * thumbnail has already been fetched and stored previously.
   *
   * @param episodeName The name of the episode to check for an existing thumbnail.
   * @return {@code true} if the thumbnail file exists; {@code false} otherwise.
   */
  public static boolean thumbnailExists(final String episodeName) {
    return Files.exists(getThumbnailPath(episodeName));
  }

  /**
   * Downloads the thumbnail image from the given URL and saves it in the temporary folder for later
   * use.
   *
   * <p>This method will download the thumbnail and store it on disk at the path specified by the
   * episode name.
   *
   * @param thumbnailUrl The URL of the thumbnail image to download.
   * @param fileName The name to assign to the downloaded thumbnail file.
   * @return The {@link Path} to the saved thumbnail file.
   * @throws IOException If there is an error while downloading or saving the thumbnail.
   */
  public static Path fetchThumbnail(final String thumbnailUrl, final String fileName)
      throws IOException {
    System.out.println("downloading: " + thumbnailUrl);
    final Path thumbnailPath = getThumbnailPath(fileName);
    try (final ByteArrayOutputStream outputStream = fetchThumbnailOutputStream(thumbnailUrl);
        final FileOutputStream fos = new FileOutputStream(thumbnailPath.toFile())) {
      fos.write(outputStream.toByteArray());
      return thumbnailPath;
    }
  }

  /**
   * Downloads the thumbnail image into memory (as a {@link ByteArrayOutputStream}) from the
   * specified URL.
   *
   * <p>This method is used to download the thumbnail image and store it temporarily in memory. The
   * data can be later saved to a file using {@link #fetchThumbnail(String, String)} if needed.
   *
   * @param thumbnailUrl The URL of the thumbnail image to download.
   * @return A {@link ByteArrayOutputStream} containing the thumbnail image data.
   * @throws IOException If there is an error while downloading the thumbnail.
   */
  private static ByteArrayOutputStream fetchThumbnailOutputStream(final String thumbnailUrl)
      throws IOException {
    final URL url = URI.create(thumbnailUrl).toURL();
    try (final InputStream inputStream = url.openStream();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      final byte[] buffer = new byte[4096];
      int bytesRead;

      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }

      return outputStream;
    }
  }
}
