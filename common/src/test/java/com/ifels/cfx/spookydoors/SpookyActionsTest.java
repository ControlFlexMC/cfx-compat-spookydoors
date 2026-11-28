package com.ifels.cfx.spookydoors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ifels.controlflex.api.ActionLayer;
import com.ifels.controlflex.api.ActionRegistration;
import com.ifels.controlflex.api.TriggerMode;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The door actions ship with no bindings at all — not a registration default, not a compat
 * config entry. The player binds them in a layer whose left stick is otherwise free; that layer
 * choice is the whole conflict-avoidance story, so a default sneaking back in would silently put
 * the door on the movement stick again.
 */
class SpookyActionsTest {

    private static final String COMPAT_JSON =
            "/assets/cfx_compat_spookydoors/compat/spookydoors.json";

    /** The three state dimensions the core understands; this config must not use any of them. */
    private static final String[] STATE_SECTIONS = {"driving", "holding", "aiming"};

    @Test
    void actionsAreRegisteredWithoutADefaultBinding() {
        for (SpookyAction action : SpookyAction.values()) {
            ActionRegistration registration = SpookyActions.registration(action);
            assertNull(registration.defaultBinding(), action.name());
        }
    }

    @Test
    void actionsAreScopedToSpookyDoorsAndInGameOnly() {
        for (SpookyAction action : SpookyAction.values()) {
            ActionRegistration registration = SpookyActions.registration(action);
            assertEquals(SpookyAction.TARGET_MOD_ID, registration.groupModId(), action.name());
            assertEquals(EnumSet.of(ActionLayer.IN_GAME), registration.layers(), action.name());
            assertTrue(registration.targetClasses().isEmpty(), action.name());
        }
    }

    @Test
    void compatConfigShipsNoBindings() throws Exception {
        JsonObject version = firstVersionEntry();

        for (String section : STATE_SECTIONS) {
            assertFalse(version.has(section),
                    "a '" + section + "' entry would seed bindings the player is meant to choose");
        }
        // KeyMapping rules would be just as bad: this mod's actions are not KeyMappings, so any
        // rule here would be dead weight that only looks like it does something.
        for (String section : new String[]{"inGameKeys", "screenKeys", "overlayKeys"}) {
            assertTrue(version.getAsJsonObject(section).isEmpty(), section);
        }
        assertNoDefaults(version, "mod_versions[0]");
    }

    @Test
    void compatConfigIsOwnedBySpookyDoorsAndGatedOnTheBridge() throws Exception {
        JsonObject root = readConfig();
        assertEquals("spookydoors", root.get("mod_id").getAsString());
        JsonObject dependency = root.getAsJsonArray("dependencies").get(0).getAsJsonObject();
        assertEquals("cfx_compat_spookydoors", dependency.get("mod_id").getAsString());
    }

    @Test
    void actionIdsLiveInTheBridgeNamespace() {
        for (SpookyAction action : SpookyAction.values()) {
            assertTrue(action.actionId().startsWith(SpookyAction.MOD_ID + ":"),
                    action.actionId());
        }
    }

    @Test
    void holdStyleModesStepEveryTickAndOneShotsStepOnce() {
        // The step size differs by mode (5% vs 15%), so this classification decides how far a
        // press moves the door. Latching modes belong with the held ones: the action stays active
        // until it is turned off, and the door has to keep moving for that whole time.
        for (TriggerMode mode : new TriggerMode[]{
                TriggerMode.HOLD, TriggerMode.DELAY_HOLD, TriggerMode.LONG_HOLD,
                TriggerMode.DOUBLE_HOLD, TriggerMode.TOGGLE, TriggerMode.DOUBLE_TOGGLE}) {
            assertTrue(SpookyActions.isHoldLike(mode), mode.name());
        }
        for (TriggerMode mode : new TriggerMode[]{
                TriggerMode.PRESS, TriggerMode.RELEASE, TriggerMode.TAP, TriggerMode.DELAY_PRESS,
                TriggerMode.LONG_PRESS, TriggerMode.DOUBLE_PRESS, TriggerMode.DOUBLE_TAP}) {
            assertFalse(SpookyActions.isHoldLike(mode), mode.name());
        }
    }

    // ===== helpers =====

    private static void assertNoDefaults(JsonElement element, String path) {
        if (element == null) {
            return;
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                assertFalse("defaults".equals(entry.getKey()),
                        "bindings under " + path + "/defaults");
                assertNoDefaults(entry.getValue(), path + "/" + entry.getKey());
            }
        } else if (element.isJsonArray()) {
            int i = 0;
            for (JsonElement child : element.getAsJsonArray()) {
                assertNoDefaults(child, path + "[" + i++ + "]");
            }
        }
    }

    private static JsonObject readConfig() throws Exception {
        try (InputStream in = SpookyActionsTest.class.getResourceAsStream(COMPAT_JSON)) {
            assertNotNull(in, "bundled compat config missing: " + COMPAT_JSON);
            return JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static JsonObject firstVersionEntry() throws Exception {
        JsonObject entry = readConfig().getAsJsonArray("mod_versions")
                .get(0).getAsJsonObject();
        assertEquals("", entry.get("version").getAsString(),
                "the single entry should match every Spooky Doors version");
        return entry;
    }
}
