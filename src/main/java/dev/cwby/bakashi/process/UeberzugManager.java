package dev.cwby.bakashi.process;

import dev.cwby.bakashi.Main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the lifecycle and operations of a running Ueberzug instance.
 *
 * <p>Ueberzug is used to overlay images in terminal applications, and this class provides methods
 * to start the Ueberzug process, communicate with it, and terminate it when needed.
 *
 * <p>**Features:** - Starts the Ueberzug process and tracks its PID. - Retrieves the Ueberzug
 * socket path for communication. - Sends exit commands to cleanly terminate the Ueberzug process. -
 * Ensures the PID and socket are properly initialized before performing operations.
 *
 * <p>**Usage Notes:** - This class is specific to environments where Ueberzug is installed and
 * accessible via the command line. - The process relies on a PID file stored in the temporary
 * directory defined by {@link Main.TEMP}. - Any failure in process execution or communication is
 * reported via exceptions.
 */
public class UeberzugManager {

  private static final String CMD = "ueberzug";
  private final Path pidPath = Paths.get(Main.TEMP, ".bakashicli");
  private String pid;

  /**
   * Starts the Ueberzug process and initializes its PID.
   *
   * <p>This method executes the Ueberzug process with the required arguments and reads the PID from
   * the file specified by {@code pidPath}.
   *
   * @throws IllegalStateException If the process fails to start or exits with a non-zero code.
   */
  public void spawn() {
    final int exitCode =
        executeProcess(
            CMD,
            "layer",
            "--no-stdin",
            "--silent",
            "--use-escape-codes",
            "--pid-file",
            pidPath.toAbsolutePath().toString());

    if (exitCode == 0) {
      this.pid = readPidFromFile();
    } else {
      throw new IllegalStateException("Ueberzug exited with code: " + exitCode);
    }
  }

  /**
   * Retrieves the socket path for the running Ueberzug instance.
   *
   * <p>The socket path is constructed using the PID of the running Ueberzug process.
   *
   * @return The socket path for communication with Ueberzug.
   * @throws IllegalStateException If the Ueberzug process has not been initialized.
   */
  public String getSocket() {
    ensurePidInitialized();
    return "/tmp/ueberzugpp-" + pid + ".socket";
  }

  /**
   * Sends an exit signal to the Ueberzug process.
   *
   * <p>This method uses the Ueberzug command to send a clean exit signal via the process socket.
   *
   * @return The exit code of the Ueberzug process.
   * @throws IllegalStateException If the Ueberzug process is not initialized or the command fails.
   */
  public int exit() {
    ensurePidInitialized();
    final int exitCode = executeProcess(CMD, "cmd", "-s", getSocket(), "-a", "exit");

    if (exitCode != 0) {
      throw new IllegalStateException("Ueberzug (send exit process) exited with code: " + exitCode);
    }

    return exitCode;
  }

  /**
   * Reads the PID of the Ueberzug process from the file specified during process initialization.
   *
   * @return The PID of the running Ueberzug process as a {@link String}.
   * @throws IllegalStateException If the PID file cannot be read or does not exist.
   */
  private String readPidFromFile() {
    try {
      return Files.readString(pidPath);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read PID from file: " + pidPath, e);
    }
  }

  /**
   * Ensures that the Ueberzug process is initialized before performing operations.
   *
   * @throws IllegalStateException If the PID has not been initialized.
   */
  private void ensurePidInitialized() {
    if (pid == null) {
      throw new IllegalStateException("Ueberzug process is not initialized.");
    }
  }

  /**
   * Executes a command as an external process and waits for it to complete.
   *
   * @param command The command and its arguments to be executed.
   * @return The exit code of the process, where 0 indicates success.
   * @throws IllegalStateException If the process execution is interrupted or fails.
   */
  private int executeProcess(final String... command) {
    try {
      return new ProcessBuilder(command).redirectErrorStream(true).start().waitFor();
    } catch (IOException | InterruptedException e) {
      System.out.println("Failed to execute process: " + e.getMessage());
      Thread.currentThread().interrupt();
    }
    return 1;
  }

  /**
   * Checks if Ueberzug is installed and available on the system.
   *
   * <p>This method runs the `ueberzug --version` command to verify the presence of Ueberzug. If the
   * command executes successfully and returns an exit code of 0, Ueberzug is considered present. If
   * an exception occurs or the command fails, the method returns {@code false}.
   *
   * @return {@code true} if Ueberzug is present on the system; {@code false} otherwise.
   */
  public static boolean checkUeberzugPresence() {
    try {
      final int code = new ProcessBuilder(CMD, "--version").start().waitFor();
      return code == 0;
    } catch (InterruptedException | IOException e) {
      return false;
    }
  }
}
