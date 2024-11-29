package dev.cwby.bakashi.scrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cwby.bakashi.data.AnimePage;
import dev.cwby.bakashi.data.EpisodeData;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class AnrollScrapper implements IScrapper {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  @Override
  public String referer() {
    return "https://anroll.net";
  }

  @Override
  public List<EpisodeData> fetchEpisodesFromPage(AnimePage animePage) throws IOException {
    String episodesApi =
        "https://apiv3-prd.anroll.net/animes/" + animePage.id() + "/episodes?page=1&order=desc";
    List<EpisodeData> episodes = new ArrayList<>();
    final var request = HttpRequest.newBuilder().uri(URI.create(episodesApi)).build();
    try {
      final String responseBody = CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
      String json = MAPPER.readTree(responseBody).get("data").toString();
      List<JsonNode> nodes = MAPPER.readValue(json, new TypeReference<>() {});
      for (JsonNode episode : nodes) {
        String episodeNum = episode.get("n_episodio").asText();
        String episodeLink = referer() + "/e/" + episode.get("generate_id").asText();
        String episodeName = animePage.title() + " " + episodeNum;
        // TODO: maybe URLEncoder
        String episodeThumbnail =
            "https://www.anroll.net/_next/image?url=https%3A%2F%2Fstatic.anroll.net%2Fimages%2Fanimes%2Fscreens%2F"
                + animePage.slug()
                + "%2F"
                + episodeNum
                + ".jpg&w=256&q=75";
        episodes.add(new EpisodeData(episodeName, episodeLink, episodeThumbnail));
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    return episodes;
  }

  @Override
  public List<EpisodeData> getLastEpisodes() throws IOException {
    final Document document = fetchDocument(referer());
    final Elements articles = extractArticlesFromDashboard(document);
    return articles.stream().map(this::extractEpisodeDataFromArticle).toList();
  }

  @Override
  public List<AnimePage> findAnimePage(final String search) throws IOException {
    List<AnimePage> pages = new ArrayList<>();
    final var request =
        HttpRequest.newBuilder()
            .uri(URI.create("https://api-search.anroll.net/data?q=" + search))
            .build();
    try {
      final String responseBody = CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
      List<JsonNode> pageNode =
          MAPPER.readValue(
              MAPPER.readTree(responseBody).get("data").toString(), new TypeReference<>() {});
      for (JsonNode node : pageNode) {
        String slug = node.get("slug").asText();
        String thumbnail = "https://static.anroll.net/images/animes/capas/" + slug + ".jpg";

        int totalEps = 0;
        if (node.has("total_eps")) {
          totalEps = node.get("total_eps").asInt();
        }

        pages.add(
            new AnimePage(
                node.get("id").asText(),
                node.get("title").asText(),
                slug,
                node.get("synopsis").asText(),
                totalEps,
                node.get("generic_path").asText(),
                thumbnail));
      }
    } catch (InterruptedException e) {
      throw new IOException(e.getMessage());
    }
    return pages;
  }

  @Override
  public String extractVideoUrl(final String episodeLink) throws IOException {
    final Document document = fetchDocument(episodeLink);
    final String scriptTag = document.select("#__NEXT_DATA__").getFirst().data();
    final JsonNode dataNode = MAPPER.readTree(scriptTag).get("props").get("pageProps").get("data");
    final String CDN_ENDPOINT = "https://cdn-zenitsu-2-gamabunta.b-cdn.net/cf/hls/animes";
    final String slugSerie = dataNode.get("anime").get("slug_serie").asText();
    final String nEpisodio = dataNode.get("n_episodio").asText();
    final String streamExtension = ".mp4/media-1/stream.m3u8";
    return CDN_ENDPOINT + "/" + slugSerie + "/" + nEpisodio + streamExtension;
  }

  private Elements extractArticlesFromDashboard(final Document document) {
    return document
        .select("#__next > main > div.sc-b2878e96-1.dburWc > ul")
        .getFirst()
        .getElementsByTag("li");
  }

  private EpisodeData extractEpisodeDataFromArticle(final Element article) {
    final Element linkElement = article.getElementsByTag("a").getFirst();

    final String episodeLink = referer() + linkElement.attr("href");
    final String episodeName = linkElement.getElementsByClass("release-item-details").text();
    final String episodeImageLink =
        referer() + linkElement.getElementsByTag("img").getFirst().attr("src");

    return new EpisodeData(episodeName, episodeLink, episodeImageLink);
  }
}
