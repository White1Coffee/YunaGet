package nl.yunaget.client.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransferConfirmationTest {
    @Test void localPredictionAndUnrelatedUpdatesAreNotAcknowledgements() {
        assertFalse(TransferConfirmation.confirmed(64,0,0,64,10,10,10));
        assertFalse(TransferConfirmation.confirmed(64,0,0,64,10,11,10));
        assertFalse(TransferConfirmation.confirmed(64,0,0,64,10,10,11));
    }
    @Test void requiresCorrespondingSourceAndDestinationChanges() {
        assertFalse(TransferConfirmation.confirmed(64,64,0,64,10,11,12));
        assertFalse(TransferConfirmation.confirmed(64,0,0,0,10,11,12));
        assertFalse(TransferConfirmation.confirmed(64,0,0,63,10,11,12));
        assertTrue(TransferConfirmation.confirmed(64,0,0,64,10,11,12));
        assertTrue(TransferConfirmation.confirmed(64,32,64,96,10,12,11));
    }
}
