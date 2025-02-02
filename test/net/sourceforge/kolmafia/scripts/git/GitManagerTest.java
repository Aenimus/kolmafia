package net.sourceforge.kolmafia.scripts.git;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.CliCaller;
import internal.helpers.Player;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(
    value = {OS.WINDOWS},
    disabledReason = "deleting script can fail")
public class GitManagerTest {

  /**
   * Run tests using the public API accessible through the CLI command "git" and the ASH commands to
   * avoid tying tests to current internal API
   */
  @Nested
  public class BasicTests {
    private static final String id = "midgleyc-mafia-script-install-test-test-basic";

    @BeforeAll
    public static void cloneRepo() {
      String output =
          CliCaller.callCli(
              "git",
              "checkout https://github.com/midgleyc/mafia-script-install-test.git test-basic");
      assertThat(output, containsString("Cloned project " + id));
      assertTrue(Files.exists(Paths.get("git", id)));
    }

    @AfterAll
    public static void removeRepo() {
      removeGitIfExists(id);
      assertFalse(Files.exists(Paths.get("git", id)));
      assertFalse(Files.exists(Paths.get("scripts", "1.ash")));
    }

    @Test
    public void shouldCopyPermissibleFolders() {
      assertTrue(Files.exists(Paths.get("scripts", "1.ash")));
      assertTrue(Files.exists(Paths.get("relay", "1.ash")));
      assertTrue(Files.exists(Paths.get("data", "1.txt")));
    }

    @Test
    public void shouldNotCopyUnpermissibleFiles() {
      assertFalse(Files.exists(Paths.get("uncopied.js")));
      assertFalse(Files.exists(Paths.get("unpermissible", "1.txt")));
    }

    @Test
    public void shouldUpdate() {
      // there is nothing it can do, but check this doesn't error
      CliCaller.callCli("git", "update");
      CliCaller.callCli("git", "update " + id);
      assertEquals(MafiaState.CONTINUE, StaticEntity.getContinuationState());
    }

    @Test
    public void shouldList() {
      String output = CliCaller.callCli("git", "list");
      assertThat(output, containsString(id));
    }

    @Test
    public void shouldSync() throws IOException {
      // delete a script file
      Path scriptFile = Path.of("scripts", "1.ash");
      Files.delete(scriptFile);
      // sync
      CliCaller.callCli("git", "sync");
      // file should exist
      assertTrue(Files.exists(scriptFile));
    }

    @Test
    public void shouldListAsh() {
      String output = CliCaller.callCli("ash", "git_list()");
      assertThat(output, containsString("0 => " + id));
    }

    @Test
    public void shouldCheckUpdated() {
      String output = CliCaller.callCli("ash", "git_at_head(\"" + id + "\")");
      assertThat(output, containsString("Returned: true"));
      assertEquals(MafiaState.CONTINUE, StaticEntity.getContinuationState());
    }

    @Test
    public void shouldCheckExists() {
      String output = CliCaller.callCli("ash", "git_exists(\"" + id + "\")");
      assertThat(output, containsString("Returned: true"));
      output = CliCaller.callCli("ash", "git_exists(\"absent-script\")");
      assertThat(output, containsString("Returned: false"));
    }

    @Test
    public void shouldCheckInfo() {
      String output = CliCaller.callCli("ash", "git_info(\"" + id + "\").url");
      assertThat(
          output,
          containsString("Returned: https://github.com/midgleyc/mafia-script-install-test.git"));
    }
  }

  @Nested
  public class DependencyTests {
    private static final String id = "midgleyc-mafia-script-install-test-test-deps";

    @BeforeAll
    public static void cloneRepo() {
      String output =
          CliCaller.callCli(
              "git",
              "checkout https://github.com/midgleyc/mafia-script-install-test.git test-deps");
      assertThat(output, containsString("Installing dependencies"));
      assertThat(output, containsString("Cloned project " + id));
    }

    @AfterAll
    public static void removeRepo() {
      removeGitIfExists(id);
      removeGitIfExists(id + "-git");
      removeSvnIfExists("midgleyc-mafia-script-install-test-branches-test-deps-svn");
    }

    @Test
    public void installedDependencies() {
      assertTrue(Files.exists(Paths.get("scripts", "1-git.ash")));
      assertTrue(Files.exists(Paths.get("scripts", "1-svn.ash")));
    }

    @Test
    public void syncReinstallsDependencies() {
      String dep = id + "-git";
      // delete script files
      CliCaller.callCli("git", "delete " + dep);

      // sync
      String output = CliCaller.callCli("git", "sync");
      assertThat(output, containsString("Installing dependencies"));

      // files should return
      output = CliCaller.callCli("git", "list");
      assertThat(output, containsString(dep));
    }

    @Test
    public void preferenceFalseIgnoresDependencies() {
      String dep = id + "-git";
      // delete script files
      CliCaller.callCli("git", "delete " + dep);

      // sync
      var cleanups = Player.withProperty("gitInstallDependencies", false);
      try (cleanups) {
        String output = CliCaller.callCli("git", "sync");
        assertThat(output, not(containsString("Installing dependencies")));
      }

      // files should return
      String output = CliCaller.callCli("git", "list");
      assertThat(output, not(containsString(dep)));
    }
  }

  @Nested
  public class DependencySvnTests {

    @BeforeAll
    public static void cloneRepo() {
      String output =
          CliCaller.callCli(
              "svn",
              "checkout https://github.com/midgleyc/mafia-script-install-test/branches/test-deps");
      assertThat(output, containsString("Installing dependencies"));
      assertThat(output, containsString("Successfully checked out working copy"));
    }

    @AfterAll
    public static void removeRepo() {
      removeSvnIfExists("midgleyc-mafia-script-install-test-branches-test-deps");
      removeGitIfExists("midgleyc-mafia-script-install-test-test-deps-git");
      removeSvnIfExists("midgleyc-mafia-script-install-test-branches-test-deps-svn");
    }

    @Test
    public void installedDependencies() {
      assertTrue(Files.exists(Paths.get("scripts", "1-git.ash")));
      assertTrue(Files.exists(Paths.get("scripts", "1-svn.ash")));
    }

    @Test
    public void installedDependenciesWithRightVersionControl() {
      assertTrue(
          Files.isDirectory(Paths.get("git", "midgleyc-mafia-script-install-test-test-deps-git")));
      assertTrue(
          Files.isDirectory(
              Paths.get("svn", "midgleyc-mafia-script-install-test-branches-test-deps-svn")));
    }
  }

  @Nested
  public class ManifestTests {

    private static final String id = "midgleyc-mafia-script-install-test-test-manifest";

    @BeforeAll
    public static void cloneRepo() {
      String output =
          CliCaller.callCli(
              "git",
              "checkout https://github.com/midgleyc/mafia-script-install-test.git test-manifest");
      assertThat(output, containsString("Installing dependencies"));
      assertThat(output, containsString("Cloned project " + id));
    }

    @AfterAll
    public static void removeRepo() {
      removeGitIfExists(id);
      removeGitIfExists("midgleyc-mafia-script-install-test-test-deps-git");
    }

    @Test
    public void installedFilesAndDepenciesRelativeToManifest() {
      assertTrue(Files.exists(Paths.get("scripts", "1-manifest.ash")));
      assertTrue(Files.exists(Paths.get("scripts", "1-git.ash")));
    }

    @Test
    public void didNotInstallFilesRelativeToRoot() {
      assertFalse(Files.exists(Paths.get("scripts", "1-root.ash")));
    }
  }

  private static void removeGitIfExists(String remove) {
    if (Files.exists(Paths.get("git", remove))) {
      String output = CliCaller.callCli("git", "delete " + remove);
      assertThat(output, containsString("Project " + remove + " removed"));
    }
  }

  private static void removeSvnIfExists(String remove) {
    if (Files.exists(Paths.get("svn", remove))) {
      String output = CliCaller.callCli("svn", "delete " + remove);
      assertThat(output, containsString("Project uninstalled." + remove));
    }
  }
}
