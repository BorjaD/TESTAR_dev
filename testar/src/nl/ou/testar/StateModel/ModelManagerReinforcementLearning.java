package nl.ou.testar.StateModel;

import nl.ou.testar.ReinforcementLearning.GuiStateGraphForQlearning;
import nl.ou.testar.ReinforcementLearning.RLTags;
import nl.ou.testar.ReinforcementLearning.QFunctions.QFunction;
import nl.ou.testar.ReinforcementLearning.QFunctions.QLearningQFunction;
import nl.ou.testar.ReinforcementLearning.RewardFunctions.QLearningRewardFunction;
import nl.ou.testar.ReinforcementLearning.RewardFunctions.RewardFunction;
import nl.ou.testar.StateModel.ActionSelection.ActionSelector;
import nl.ou.testar.StateModel.Exception.ActionNotFoundException;
import nl.ou.testar.StateModel.Persistence.PersistenceManager;
import nl.ou.testar.StateModel.Sequence.SequenceManager;
import org.fruit.alayer.Action;
import org.fruit.alayer.State;
import org.fruit.alayer.Tag;
import org.fruit.alayer.Tags;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Set;

public class ModelManagerReinforcementLearning extends ModelManager implements StateModelManager  {
    // TODO should this be a singleton?
    private final static GuiStateGraphForQlearning graph = new GuiStateGraphForQlearning();

    /**
     * Constructor
     *
     * @param abstractStateModel
     * @param actionSelector
     * @param persistenceManager
     * @param concreteStateTags
     * @param sequenceManager
     */
    public ModelManagerReinforcementLearning(AbstractStateModel abstractStateModel, ActionSelector actionSelector, PersistenceManager persistenceManager, Set<Tag<?>> concreteStateTags, SequenceManager sequenceManager, boolean storeWidgets) {
        super(abstractStateModel, actionSelector, persistenceManager, concreteStateTags, sequenceManager, storeWidgets);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyNewStateReached(State newState, Set<Action> actions) {

        super.notifyNewStateReached(newState, actions);
        
    	HashMap<String, Action> surfaceActions = new HashMap<>();
    	actions.forEach(a -> surfaceActions.put(a.get(Tags.AbstractIDCustom), a));
    	
    	if (currentAbstractState == null) {
    		return;
    	}
    	
    	for (AbstractAction modelAbstractAction : currentAbstractState.getActions()) {
    		
			if(modelAbstractAction.getAttributes().get(RLTags.HalfValue, null) != null 
					&& surfaceActions.containsKey(modelAbstractAction.getActionId())) {
				
				Action ia = surfaceActions.get(modelAbstractAction.getActionId());
				ia.set(RLTags.HalfValue, modelAbstractAction.getAttributes().get(RLTags.HalfValue));
				
			}
			
		}
    	
        for(Action a : actions)
        	getAbstractActionQValue(a);
    }
    
    @Override
    public void notifyActionExecution(Action action) {
    	super.notifyActionExecution(action);
    	updateQValue();
    }

    /**
     * Update the Q-value
     * @param incomingState
     */
    private void updateQValue() {
        // Set RL Tag in the AbstractAction
        if(actionUnderExecution.getAttributes().get(RLTags.HalfValue, null) == null) {
        	actionUnderExecution.addAttribute(RLTags.HalfValue, 1.0);
        }
        else {
        	double halfq = (actionUnderExecution.getAttributes().get(RLTags.HalfValue)) / 2.0;
        	actionUnderExecution.addAttribute(RLTags.HalfValue, halfq);
        }
    }
    
    public double getAbstractActionQValue(Action action) {

    	if (this.currentAbstractState == null) {
    		// This should not happen
    		System.out.println("Abstract State is null");
    		return 0.0;
    	}
    	
    	AbstractAction absAction = null;
    	try {
    		absAction = currentAbstractState.getAction(action.get(Tags.AbstractIDCustom,""));
    	} catch (ActionNotFoundException e) {
    		// This should not happen
    		// Action does not exist in the current Abstract State
    		// Max Q-Value ?
    		System.out.println(String.format("Action: %s doesnt exists yet, returning X value", action.get(Tags.AbstractIDCustom,"")));
    		return 1.0;
    	}

    	if(absAction.getAttributes().get(RLTags.HalfValue, null) == null) {
    		// Not Q-Value associated, Max Q-Value ?
    		System.out.println(String.format("Action %s has not a Q-Value associated, returning X value", action.get(Tags.AbstractIDCustom,"")));
    		return 1.0;
    	}
    	
    	else {
        	double qValue = absAction.getAttributes().get(RLTags.HalfValue);
    		System.out.println(String.format("Action %s has a Q-Value of %s", action.get(Tags.AbstractIDCustom,""), qValue));
    		return qValue;
    	}
    }

}
