package com.ifels.cfx.spookydoors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ifels.controlflex.api.ActionLayer;
import com.ifels.controlflex.api.ActionRegistration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The door actions are declared unbound; every binding they ever get comes from the aiming entry
 * in the bundled compat config. These tests keep it that way and pin the layout ControlFlex
 * documents.
 */
class SpookyActionsTest {

    private static final String COMPAT_JSON =
            "/assets/cfx_compat_spookydoors/compat/spookydoors.json";

    @Test
    void actionsAreRegisteredWithoutADefaultBinding() {
        for (SpookyAction action : SpookyAction.values()) {
            ActionRegistration registration = SpookyActions.registration(action);
            // A registration default would bind the left stick during normal movement; the
            // aiming entry is the only place these bindings are supposed to exist.
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
    void compatConfigBindsEveryActionToTheLeftStick() throws Exception {
        Map<String, String> expected = new LinkedHashMap<String, String>();
        expected.put("cfx_compat_spookydoors:grab", "leftStickClick|HOLD");
        expected.put("cfx_compat_spookydoors:push", "leftStickUp|HOLD");
        expected.put("cfx_compat_spookydoors:pull", "leftStickDown|HOLD");

        assertEquals(expected, defaultsOf(firstAimingEntry()));
    }

    @Test
    void compatConfigCoversExactlyTheDeclaredActions() throws Exception {
        JsonObject aiming = firstAimingEntry();
        Set<String> declared = new LinkedHashSet<String>();
        for (SpookyAction action : SpookyAction.values()) {
            declared.add(action.actionId());
        }

        // A missing overrideActions entry hides the action from the state page; a stale one
        // points at an id this mod no longer registers.
        assertEquals(declared, stringsOf(aiming.getAsJsonArray("overrideActions")));
        assertEquals(declared, defaultsOf(aiming).keySet());
    }

    @Test
    void compatConfigAimsAtDoorsAndOnlyAtDoors() throws Exception {
        JsonArray targets = firstAimingEntry().getAsJsonArray("targets");
        assertEquals(1, targets.size());
        String target = targets.get(0).getAsString();
        // block: prefix keeps it a block target; the glob covers vanilla and modded doors while
        // still excluding trapdoors (_trapdoor does not end in _door).
        assertTrue(target.startsWith("block:"), target);
        assertTrue(target.endsWith("_door"), target);
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

    // ===== helpers =====

    private static JsonObject readConfig() throws Exception {
        try (InputStream in = SpookyActionsTest.class.getResourceAsStream(COMPAT_JSON)) {
            assertNotNull(in, "bundled compat config missing: " + COMPAT_JSON);
            return JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static JsonObject firstAimingEntry() throws Exception {
        JsonObject version = readConfig().getAsJsonArray("mod_versions")
                .get(0).getAsJsonObject();
        JsonArray aiming = version.getAsJsonArray("aiming");
        assertEquals(1, aiming.size(), "expected exactly one aiming entry");
        return aiming.get(0).getAsJsonObject();
    }

    private static Map<String, String> defaultsOf(JsonObject aiming) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        JsonObject main = aiming.getAsJsonObject("defaults").getAsJsonObject("main");
        for (Map.Entry<String, JsonElement> entry : main.entrySet()) {
            out.put(entry.getKey(), entry.getValue().getAsString());
        }
        return out;
    }

    private static Set<String> stringsOf(JsonArray array) {
        Set<String> out = new LinkedHashSet<String>();
        for (JsonElement element : array) {
            out.add(element.getAsString());
        }
        return out;
    }
}
