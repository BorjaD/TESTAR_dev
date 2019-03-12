package nl.ou.testar.StateModel.Sequence;

import nl.ou.testar.StateModel.ConcreteState;
import nl.ou.testar.StateModel.Event.StateModelEvent;
import nl.ou.testar.StateModel.Event.StateModelEventListener;
import nl.ou.testar.StateModel.Event.StateModelEventType;
import org.fruit.alayer.Tag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Sequence {

    private boolean active = false;

    /**
     * The unique hash that identifies the current sequence.
     */
    private String currentSequenceId;

    /**
     * The nr that identifies the sequence order in the test run.
     */
    private int currentSequenceNr;

    /**
     * The nr that identifies the last node in the sequence.
     */
    private int currentNodeNr = 0;

    /**
     * A list of nodes in this sequence
     */
    private List<SequenceNode> nodes;

    /**
     * The identifier for the abstract state model version that we are currently testing in.
     */
    private String abstractionLevelIdentifier;

    /**
     * Tags containing the attributes that were used in creating the concrete state ID.
     */
    private Set<Tag<?>> concreteStateTags;

    /**
     * The starting date and time for this sequence.
     */
    private Instant startDateTime;

    /**
     * A set of event listeners to notify of changes in the sequence.
     */
    private Set<StateModelEventListener> eventListeners;

    public Sequence(int currentSequenceNr, Set<StateModelEventListener> eventListeners, String abstractionLevelIdentifier) {
        currentSequenceId = UUID.randomUUID().toString();
        this.eventListeners = eventListeners;
        this.currentSequenceNr = currentSequenceNr;
        this.abstractionLevelIdentifier = abstractionLevelIdentifier;
        nodes = new ArrayList<>();
    }

    /**
     * Start the sequence.
     */
    public void start() {
        startDateTime = Instant.now();
        active = true;
        emitEvent(new StateModelEvent(StateModelEventType.SEQUENCE_STARTED, this));
    }

    /**
     * Notify our listeners of emitted events
     * @param event
     */
    private void emitEvent(StateModelEvent event) {
        for (StateModelEventListener eventListener: eventListeners) {
            eventListener.eventReceived(event);
        }
    }

    public String getCurrentSequenceId() {
        return currentSequenceId;
    }

    public int getCurrentSequenceNr() {
        return currentSequenceNr;
    }

    public String getAbstractionLevelIdentifier() {
        return abstractionLevelIdentifier;
    }

    public Set<Tag<?>> getConcreteStateTags() {
        return concreteStateTags;
    }

    public Instant getStartDateTime() {
        return startDateTime;
    }

    public boolean isRunning() {
        return active;
    }

    public void stop() {
        active = false;
        //todo event here?
    }

    /**
     * Add a new note to the sequence.
     * @param concreteState
     */
    public void addNode(ConcreteState concreteState) {
        SequenceNode node = new SequenceNode(currentSequenceId, ++currentNodeNr);
        nodes.add(node);
        emitEvent(new StateModelEvent(StateModelEventType.SEQUENCE_NODE_ADDED, node));
    }


}
