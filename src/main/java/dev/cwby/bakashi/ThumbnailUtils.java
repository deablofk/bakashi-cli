package dev.cwby.bakashi;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ThumbnailUtils {

  public static final String THUMB_EXTENSION = ".jpg";

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
  public static Path getThumbnailPath(final String episodeName) {
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
  public static ByteArrayOutputStream fetchThumbnailOutputStream(final String thumbnailUrl)
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
