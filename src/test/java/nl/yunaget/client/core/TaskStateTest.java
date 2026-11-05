package nl.yunaget.client.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskStateTest {
    @Test void completeStorageWorkflowIsLegal() {
        var path=new TaskState[]{TaskState.IDLE,TaskState.SEARCHING,TaskState.NAVIGATING,TaskState.INTERACTING,TaskState.VERIFYING,TaskState.INTERACTING,TaskState.VERIFYING,TaskState.COMPLETE};
        for(int i=1;i<path.length;i++)assertTrue(path[i-1].canTransitionTo(path[i]),path[i-1]+" -> "+path[i]);
    }
    @Test void cannotVerifyBeforeAnInteractionOrSkipOpen() {
        assertFalse(TaskState.IDLE.canTransitionTo(TaskState.VERIFYING));
        assertFalse(TaskState.SEARCHING.canTransitionTo(TaskState.VERIFYING));
        assertFalse(TaskState.NAVIGATING.canTransitionTo(TaskState.VERIFYING));
    }
    @Test void everyPhaseCanPauseAbortOrStopAndPauseResumesThroughSearch() {
        for(var state:TaskState.values()) {
            assertTrue(state.canTransitionTo(TaskState.PAUSED));assertTrue(state.canTransitionTo(TaskState.ERROR));assertTrue(state.canTransitionTo(TaskState.IDLE));
        }
        assertTrue(TaskState.PAUSED.canTransitionTo(TaskState.SEARCHING));
        assertFalse(TaskState.PAUSED.canTransitionTo(TaskState.INTERACTING));
    }
}
