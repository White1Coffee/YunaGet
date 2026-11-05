package nl.yunaget.client.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ChestPresetTest {
    @Test void applyingPresetsDoesNotShareMutableListsOrWorldPositions() {
        var preset=ChestPreset.defaults().get("Diamonds");var a=new StorageRule();var b=new StorageRule();a.x=42;
        preset.apply(a);preset.apply(b);a.filters.clear();
        assertFalse(b.filters.isEmpty());assertFalse(preset.filters.isEmpty());assertEquals(42,a.x);assertEquals(0,b.x);
    }
    @Test void miscellaneousExplicitlyEnablesFallbackAndOtherPresetResetsIt() {
        var r=new StorageRule();ChestPreset.defaults().get("Miscellaneous").apply(r);assertTrue(r.fallback);
        ChestPreset.defaults().get("Wood").apply(r);assertFalse(r.fallback);assertFalse(r.filters.isEmpty());
    }
    @Test void customPresetCopiesItsSourceFilters() {
        var filters=new ArrayList<>(List.of("minecraft:diamond"));var preset=new ChestPreset(filters,10,false);
        filters.clear();assertEquals(1,preset.filters.size());
    }
}
