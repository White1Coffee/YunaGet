package nl.yunaget.client.core;

import com.google.gson.JsonParser;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ModMetadataTest {
    @Test void packagedModSupportsInstalledLoaderAndHasNoServerEntrypoint() throws Exception {
        try(var stream=getClass().getResourceAsStream("/fabric.mod.json")) {
            assertNotNull(stream);
            var json=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("client",json.get("environment").getAsString());
            assertFalse(json.getAsJsonObject("entrypoints").has("main"));
            assertTrue(VersionPredicate.parse(json.getAsJsonObject("depends").get("fabricloader").getAsString()).test(Version.parse("0.19.5")));
            assertEquals("0.3.0",json.get("version").getAsString());
        }
    }
}
