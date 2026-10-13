package dev.frosthaven.createdarkinventory.hud;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Web map viewers following this player (the Haven server's Haven Link mod): it tells the player in chat when one
 * starts or stops ("[Web] Name is following you on the web map" / "... stopped following you on the web map").
 * The count resets when leaving a server. Followers already watching at login appear once Haven Link sends the
 * current list on join.
 */
public final class Followers {
    private static final Pattern LINE = Pattern.compile("^\\[Web] (.+?) (is following|stopped following) you on the web map$");
    private static final Set<String> NAMES = new LinkedHashSet<>();

    private Followers() {}

    public static int count() {
        return NAMES.size();
    }

    public static void onChat(ClientChatReceivedEvent event) {
        Matcher m = LINE.matcher(event.getMessage().getString());
        if (!m.matches()) return;
        if (m.group(2).startsWith("is")) NAMES.add(m.group(1));
        else NAMES.remove(m.group(1));
    }

    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        NAMES.clear();
    }
}
