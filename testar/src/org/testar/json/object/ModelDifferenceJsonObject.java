package org.testar.json.object;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.fruit.Pair;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ModelDifferenceJsonObject {
	
	List<String> stateModelOne;
	List<String> stateModelTwo;
	Set<String> disappearedAbstractStates;
	Set<String> newAbstractStates;
	HashMap<String, Set<Pair<String,String>>> disappearedActions;
	HashMap<String, Set<Pair<String,String>>> newActions;
	
	@JsonCreator
	public ModelDifferenceJsonObject(List<String> stateModelOne, List<String> stateModelTwo,
			Set<String> disappearedAbstractStates, Set<String> newAbstractStates,
			HashMap<String, Set<Pair<String,String>>> disappearedActions, HashMap<String, Set<Pair<String,String>>> newActions) {
		this.stateModelOne = stateModelOne;
		this.stateModelTwo = stateModelTwo;
		this.disappearedAbstractStates = disappearedAbstractStates;
		this.newAbstractStates = newAbstractStates;
		this.disappearedActions = disappearedActions;
		this.newActions = newActions;
	}

}
