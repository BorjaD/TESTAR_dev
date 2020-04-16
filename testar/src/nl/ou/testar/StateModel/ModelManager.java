package nl.ou.testar.StateModel;

import nl.ou.testar.StateModel.ActionSelection.ActionSelector;
import nl.ou.testar.StateModel.Exception.ActionNotFoundException;
import nl.ou.testar.StateModel.Exception.StateModelException;
import nl.ou.testar.StateModel.Persistence.OrientDB.Stats.ModelStats;
import nl.ou.testar.StateModel.Persistence.PersistenceManager;
import nl.ou.testar.StateModel.Sequence.SequenceError;
import nl.ou.testar.StateModel.Sequence.SequenceManager;
import nl.ou.testar.StateModel.Util.AbstractStateService;
import org.fruit.alayer.Action;
import org.fruit.alayer.State;
import org.fruit.alayer.Tag;
import org.fruit.alayer.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ModelManager implements StateModelManager {

    // the abstract state model that this class is managing
    private AbstractStateModel abstractStateModel;

    // current abstract state of the SUT
    private AbstractState currentAbstractState;

    // the action that is currently being executed, if applicable
    private AbstractAction actionUnderExecution;

    // the testar action that is being executed
    private Action rawActionUnderExecution;

    // action selector that chooses actions to execute
    private ActionSelector actionSelector;

    // persistence manager interface for persisting our model entities
    private PersistenceManager persistenceManager;

    // tags containing the attributes that were used in creating the concrete state ID
    private Set<Tag<?>> concreteStateTags;

    // current concrete state
    private ConcreteState currentConcreteState;

    // the concrete action that is being executed.
    private ConcreteAction concreteActionUnderExecution;

    // manager that is responsible for recording test sequences as they are executed
    private SequenceManager sequenceManager;

    // if there any irregularities that occur during runs, they should be appended here
    private StringJoiner errorMessages;

    // the number of actions in the model that end in more than one unique state
    // use this to monitor non-determinism in the model
    private int nrOfNonDeterministicActions;

    // should the widgets of concrete states be stored in the model?
    private boolean storeWidgets;

    private AbstractStateIdExtractor abstractStateIdExtractor;

    private ConcreteStateIdExtractor concreteStateIdExtractor;

    // logger instance
    Logger logger = LoggerFactory.getLogger(ModelManager.class);



    /**
     * Constructor
     * @param abstractStateModel
     * @param actionSelector
     */
    public ModelManager(AbstractStateModel abstractStateModel,
                        ActionSelector actionSelector,
                        PersistenceManager persistenceManager,
                        Set<Tag<?>> concreteStateTags,
                        SequenceManager sequenceManager,
                        boolean storeWidgets,
                        AbstractStateIdExtractor abstractStateIdExtractor,
                        ConcreteStateIdExtractor concreteStateIdExtractor
                        ) {
        this.abstractStateModel = abstractStateModel;
        this.actionSelector = actionSelector;
        this.persistenceManager = persistenceManager;
        this.concreteStateTags = concreteStateTags;
        this.sequenceManager = sequenceManager;
        errorMessages = new StringJoiner(", ");
        nrOfNonDeterministicActions = 0;
        this.storeWidgets = storeWidgets;
        this.abstractStateIdExtractor = abstractStateIdExtractor;
        this.concreteStateIdExtractor = concreteStateIdExtractor;
        init();
    }

    /**
     * Initialization logic needs to go here
     */
    private void init() {
        // check if the model is deterministic
//        boolean modelIsDeterministic = persistenceManager.modelIsDeterministic(abstractStateModel);
        boolean modelIsDeterministic = abstractStateModel.isDeterministic();

        System.out.println("Model is deterministic: " + modelIsDeterministic);
        if (!modelIsDeterministic) {
//            nrOfNonDeterministicActions = persistenceManager.getNrOfNondeterministicActions(abstractStateModel);
            nrOfNonDeterministicActions = abstractStateModel.getNrOfNonDeterministicActions();

        }
    }

    /**
     * This method should be called once when a new state is reached after the execution
     * of an action or successfully starting the SUT.
     * @param newState
     * @param actions
     */
    @Override
    public void notifyNewStateReached(State newState, Set<Action> actions) {
        //////////////////// NOTICE ///////////////////////
        // During development, this class has gotten bigger and bigger, with more functionality tacked on.
        // It really needs refactoring at this stage.

        // check if we are dealing with a new state or an existing one
        String abstractStateId = abstractStateIdExtractor.extractAbstractStateId(newState, rawActionUnderExecution);
        AbstractState newAbstractState;

        // fetch or create an abstract state
        if (abstractStateModel.containsState(abstractStateId)) {
            try {
                newAbstractState = abstractStateModel.getState(abstractStateId);
                // update the abstract state
                AbstractStateService.updateAbstractStateActions(newAbstractState, actions);
            }
            catch (StateModelException ex) {
                ex.printStackTrace();
                throw new RuntimeException("An error occurred while retrieving abstract state from the state model");
            }
        } else {
            newAbstractState = AbstractStateFactory.createAbstractState(abstractStateId, actions);
        }

        // add the concrete state id to the abstract state
        String concreteStateId = concreteStateIdExtractor.extractConcreteStateId(newState, rawActionUnderExecution);
        newAbstractState.addConcreteStateId(concreteStateId);
        logger.debug("Reached abstract state: '{}' and concrete state: '{}'", newAbstractState.getStateId(), newState.get(Tags.ConcreteIDCustom));

        // check if an action was executed
        if (actionUnderExecution == null) {
            // no action is being executed, so we consider this an initial state
            newAbstractState.setInitial(true);
            try {
                abstractStateModel.addState(newAbstractState);
            } catch (StateModelException e) {
                e.printStackTrace();
                throw new RuntimeException("An error occurred while adding a new abstract state to the model");
            }
        }
        else {
            // an action is being executed
            // that means we need to have a current abstract state already set
            if (currentAbstractState == null) {
                throw new RuntimeException("An action was being executed without a recorded current state");
            }

            //add a transition to the statemodel
            try {
                abstractStateModel.addTransition(currentAbstractState, newAbstractState, actionUnderExecution);
            } catch (StateModelException e) {
                e.printStackTrace();
                throw new RuntimeException("Encountered a problem adding a state transition into the statemodel");
            }
            // we reset the executed action to await the next one.
            actionUnderExecution = null;
        }

        // we now store this state to be the current abstract state
        currentAbstractState = newAbstractState;

        // and then we store the concrete state and possibly the action
        ConcreteState newConcreteState = ConcreteStateFactory.createConcreteState(newState, concreteStateTags, newAbstractState, storeWidgets, concreteStateId);
        if (concreteActionUnderExecution == null) {
            persistenceManager.persistConcreteState(newConcreteState);
        }
        else {
            ConcreteStateTransition concreteStateTransition = new ConcreteStateTransition(currentConcreteState, newConcreteState, concreteActionUnderExecution);
            persistenceManager.persistConcreteStateTransition(concreteStateTransition);
        }

        // check if non-determinism was introduced into the model
        // first the new way:
        System.out.println("Model check - deterministic: " + abstractStateModel.isDeterministic());
        if (!abstractStateModel.isDeterministic()) {
            System.out.println("Model check - nr of non-deterministic actions: " + abstractStateModel.getNrOfNonDeterministicActions());
        }

//        int currentNrOfNonDeterministicActions = persistenceManager.getNrOfNondeterministicActions(abstractStateModel);
        int currentNrOfNonDeterministicActions = abstractStateModel.getNrOfNonDeterministicActions();
        if (currentNrOfNonDeterministicActions > nrOfNonDeterministicActions) {
            System.out.println("Non-deterministic action was executed!");
            sequenceManager.notifyStateReached(newConcreteState, concreteActionUnderExecution, SequenceError.NON_DETERMINISTIC_ACTION);
            nrOfNonDeterministicActions = currentNrOfNonDeterministicActions;
        }
        else {
            sequenceManager.notifyStateReached(newConcreteState, concreteActionUnderExecution);
        }

        currentConcreteState = newConcreteState;
        concreteActionUnderExecution = null;

        // temporarily output the nr of states in the model
        System.out.println(abstractStateModel.getStates().size() + " abstract states in the model");

        // temporarily output the number of unvisited actions still left
        System.out.println(abstractStateModel.getStates().stream().map(AbstractState::getUnvisitedActions).flatMap(
                Collection::stream
        ).count() + " unvisited actions left");
        System.out.println("----------------------------");
        System.out.println();
    }

    /**
     * This method should be called when an action is about to be executed.
     * @param action
     */
    @Override
    public void notifyActionExecution(Action action) {
        rawActionUnderExecution = action;
        // the action that is executed should always be traceable to an action on the current abstract state
        // in other words, we should be able to find the action on the current abstract state
        String abstractActionId = AbstractActionIdExtractor.extract(currentAbstractState, action);
        try {
            actionUnderExecution = currentAbstractState.getAction(abstractActionId);
        }
        catch (ActionNotFoundException ex) {
            System.out.println("Action not found in state model");
            errorMessages.add("Action with id: " + action.get(Tags.AbstractIDCustom) + " was not found in the model.");
            actionUnderExecution = new AbstractAction(abstractActionId);
            currentAbstractState.addNewAction(actionUnderExecution);
        }
        String concreteActionId = ConcreteActionIdExtractor.extract(currentConcreteState, action);
        concreteActionUnderExecution = ConcreteActionFactory.createConcreteAction(action, actionUnderExecution, concreteActionId);
        actionUnderExecution.addConcreteActionId(concreteActionUnderExecution.getActionId());
        logger.debug("Current abstract state: {}", currentAbstractState.getStateId());
        logger.debug("Current concrete state: {}", currentConcreteState.getId());
        logger.debug("Abstract action under execution: {}", actionUnderExecution.getActionId());
        logger.debug("Executing concrete action with id {} and description '{}'", concreteActionUnderExecution.getActionId(), action.get(Tags.Desc));
        System.out.println("Executing action: " + action.get(Tags.Desc));
        System.out.println("----------------------------------");

        // if we have error messages, we tell the sequence manager about it now, right before we move to a new state
        if (errorMessages.length() > 0) {
            sequenceManager.notifyErrorInCurrentState(errorMessages.toString());
            errorMessages = new StringJoiner(", ");
        }
    }

    @Override
    public void notifyTestingEnded() {
        persistenceManager.shutdown();
    }

    /**
     * This method uses the abstract state model to return the abstract id of an action to execute
     * @return
     */
    @Override
    public Action getAbstractActionToExecute(Set<Action> actions) {
        if (currentAbstractState == null) {
            return null;
        }
        try {
            AbstractAction abstractAction = actionSelector.selectAction(currentAbstractState, abstractStateModel);
            if (abstractAction == null) {
                String message = "The action selector could not retrieve an action for state " + currentAbstractState.getStateId();
                System.out.println(message);
                errorMessages.add(message);
                return null;
            }
            String abstractActionId = abstractAction.getActionId();
            System.out.println("Finding action with abstractActionId : " + abstractActionId);
            for(Action action : actions) {
            	try {
                if (AbstractActionIdExtractor.extract(currentAbstractState, action).equals(abstractActionId)) {
                    return action;
                }
            	}catch (Exception e) {
            	    String message = "ERROR getAbstractActionToExecute : " + action.get(Tags.Desc, "No description");
            		System.out.println(message);
            		errorMessages.add(message);
            	}
            }
            System.out.println("Could not find action with abstractActionId : " + abstractActionId);
            errorMessages.add("The actions selector returned the action with abstractActionId: " + abstractActionId + " . However, TESTAR was " +
                    "unable to find the action in its executable actions");
        } catch (ActionNotFoundException e) {
            System.out.println("Could not find an action to execute for abstract state id : " + currentAbstractState.getStateId());
        }
        return null;
    }

    @Override
    public void notifyTestSequencedStarted() {
        sequenceManager.startNewSequence();
    }

    @Override
    public void notifyTestSequenceStopped() {
        currentAbstractState = null;
        currentConcreteState = null;
        actionUnderExecution = null;
        concreteActionUnderExecution = null;
        sequenceManager.stopSequence();
    }

    @Override
    public void notifyTestSequenceInterruptedByUser() {
        sequenceManager.notifyInterruptionByUser();
    }

    @Override
    public void notifyTestSequenceInterruptedBySystem(String message) {
        sequenceManager.notifyInterruptionBySystem(message);
    }

    @Override
    public boolean modelIsDeterministic() {
        return nrOfNonDeterministicActions == 0;
    }

    @Override
    public int getTotalStepsExecuted() {
        return persistenceManager.getTotalNrOfStepsExecuted(abstractStateModel);
    }

    @Override
    public ModelStats getModelStats() {
        return persistenceManager.getModelStats(abstractStateModel);
    }

    @Override
    public int getNrOfNonDeterministicActions() {
        return nrOfNonDeterministicActions;
    }
}
