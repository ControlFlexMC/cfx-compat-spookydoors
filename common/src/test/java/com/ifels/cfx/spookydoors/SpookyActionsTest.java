package com.ifels.cfx.spookydoors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ifels.controlflex.api.BindingLayer;
import com.ifels.controlflex.api.ButtonName;
import com.ifels.controlflex.api.DefaultBinding;
import com.ifels.controlflex.api.TriggerMode;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The door bindings exist twice — as a registration default (what the dispatcher resolves) and as
 * the aiming entry in the bundled compat config (what the state page shows). These tests keep the
 * two in step and pin the layout ControlFlex documents.
 */
class SpookyActionsTest {

    private static final String COMPAT_JSON =
            "/assets/cfx_compat_spookydoors/compat/spookydoors.json";

    @Test
    void everyActionDeclaresAHoldDefaultScopedToMain() {
        for (SpookyAction action : SpookyAction.values()) {
            DefaultBinding declared = SpookyActions.holdDeclaration(action);
            assertNotNull(declared, action.name());
            assertEquals(TriggerMode.HOLD, declared.mode(), action.name());
            // A HOLD declaration that also seeded shift layers would give the door a second,
            // invisible binding there; main is the only layer the layout asks for.
            assertEquals(Set.of(BindingLayer.MAIN), declared.layers(), action.name());
        }
    }

    @Test
    void defaultsUseTheLeftStickAsSpecified() {
        assertEquals(ButtonName.LEFT_STICK_CLICK,
                SpookyActions.holdDeclaration(SpookyAction.GRAB).button());
        assertEquals(ButtonName.LEFT_STICK_UP,
                SpookyActions.holdDeclaration(SpookyAction.PUSH).button());
        assertEquals(ButtonName.LEFT_STICK_DOWN,
                SpookyActions.holdDeclaration(SpookyAction.PULL).button());
    }

    @Test
    void compatConfigMirrorsTheDeclaredDefaults() throws Exception {
        JsonObject aiming = firstAimingEntry();
        Map<String, String> expected = new LinkedHashMap<String, String>();
        for (SpookyAction action : SpookyAction.values()) {
            DefaultBinding declared = SpookyActions.holdDeclaration(action);
            expected.put(action.actionId(),
                    declared.button() + "|" + declared.mode().name());
        }
        assertEquals(expected, defaultsOf(aiming));

        Set<String> overridden = new HashSet<String>();
        for (JsonElement id : aiming.getAsJsonArray("overrideActions")) {
            overridden.add(id.getAsString());
        }
        assertEquals(expected.keySet(), overridden);
    }

    @Test
    void compatConfigAimsAtDoorsAndOnlyAtDoors() throws Exception {
        JsonObject aiming = firstAimingEntry();
        JsonArray targets = aiming.getAsJsonArray("targets");
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
            // Native actions are grouped on the Spooky Doors card, so the target mod id (not the
            // bridge id) is what the grouping must use.
            assertEquals("spookydoors", SpookyAction.TARGET_MOD_ID);
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
}
