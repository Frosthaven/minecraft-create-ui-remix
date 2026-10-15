package dev.frosthaven.havencreateuiux;

import dev.frosthaven.havencreateuiux.hud.HudLog;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Haven pack only: a one-time bridge for instances whose Haven launcher (Prism's pre-launch step, haven-launcher.jar in
 * the game folder) predates its self-update (launcher 1.2). The pack delivers the newest launcher to
 * haven/haven-launcher.jar; while the game runs the launcher has finished, so its jar is not locked (Windows) and is
 * replaced here when the two differ. Launchers from 1.2 also run the delivered copy themselves, so this only matters
 * for older ones. Outside the Haven pack neither file exists.
 */
final class LauncherSwap {
    private LauncherSwap() {
    }

    static void run() {
        try {
            Path game = FMLPaths.GAMEDIR.get();
            Path delivered = game.resolve("haven/haven-launcher.jar");
            Path installed = game.resolve("haven-launcher.jar");
            if (!Files.isRegularFile(delivered) || !Files.isRegularFile(installed)
                    || Arrays.equals(Files.readAllBytes(delivered), Files.readAllBytes(installed))) {
                return;
            }
            Path part = game.resolve("haven-launcher.jar.part");
            Files.copy(delivered, part, StandardCopyOption.REPLACE_EXISTING);
            Files.move(part, installed, StandardCopyOption.REPLACE_EXISTING);
            HudLog.once("launcher-swap", "Haven launcher updated from haven/haven-launcher.jar (used from the next launch)");
        } catch (Exception e) {
            HudLog.error("launcher-swap", "Haven launcher update skipped", e);
        }
    }
}
