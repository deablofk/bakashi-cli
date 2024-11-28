package dev.cwby.bakashi.data;

/**
 * A record class representing data related to an anime episode.
 *
 * <p>This class holds the basic details of an episode, including: - The name of the episode. - The
 * URL of the episode page. - The URL of the thumbnail image for the episode.
 *
 * <p>This class is used to encapsulate and transfer episode-related information in a structured
 * way.
 *
 * @param episodeName The name of the episode.
 * @param episodeUrl The URL to the page for the episode.
 * @param thumbnailUrl The URL of the thumbnail image for the episode.
 */
public record EpisodeData(String episodeName, String episodeUrl, String thumbnailUrl) {}
