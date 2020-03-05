package org.testar.json.object;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ModelDifferenceJsonObject {
	
	List<String> stateModelOne;
	List<String> stateModelTwo;
	Set<String> disappearedAbstractStates;
	Set<String> newAbstractStates;
	HashMap<String, Set<String>> disappearedActionsDesc;
	HashMap<String, Set<String>> newActionsDesc;
	
	@JsonCreator
	public ModelDifferenceJsonObject(List<String> stateModelOne, List<String> stateModelTwo,
			Set<String> disappearedAbstractStates, Set<String> newAbstractStates,
			HashMap<String, Set<String>> disappearedActionsDesc, HashMap<String, Set<String>> newActionsDesc) {
		this.stateModelOne = stateModelOne;
		this.stateModelTwo = stateModelTwo;
		this.disappearedAbstractStates = disappearedAbstractStates;
		this.newAbstractStates = newAbstractStates;
		this.disappearedActionsDesc = disappearedActionsDesc;
		this.newActionsDesc = newActionsDesc;
	}

}
