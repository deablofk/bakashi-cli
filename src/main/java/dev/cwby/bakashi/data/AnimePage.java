package dev.cwby.bakashi.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnimePage(
    String id,
    String title,
    String slug,
    String synopsis,
    @JsonProperty("total_eps") int totalEpisodes,
    @JsonProperty("generic_path") String genericPath,
    String thumbnail) {}
