package dev.cwby.bakashi.scrapper;

import java.util.HashMap;
import java.util.Map;

public class ScrapperManager {

  private static final Map<String, IScrapper> SCRAPPERS =
      new HashMap<>(Map.of("bakashi", new BakashiScrapper(), "anroll", new AnrollScrapper()));

  public static void registerScrapper(String key, IScrapper value) {
    SCRAPPERS.put(key.toLowerCase(), value);
  }

  public static IScrapper getScrapper(String key) {
    return SCRAPPERS.get(key.toLowerCase());
  }

  public static IScrapper getScrapperOrDefault(String key) {
    try {
      return getScrapper(key);
    } catch (Exception e) {
      return getScrapper("bakashi");
    }
  }
}
