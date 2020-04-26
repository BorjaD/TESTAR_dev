/***************************************************************************************************
*
* Copyright (c) 2019 Universitat Politecnica de Valencia - www.upv.es
* Copyright (c) 2018, 2019 Open Universiteit - www.ou.nl
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice,
* this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright
* notice, this list of conditions and the following disclaimer in the
* documentation and/or other materials provided with the distribution.
* 3. Neither the name of the copyright holder nor the names of its
* contributors may be used to endorse or promote products derived from
* this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*******************************************************************************************************/
import nl.ou.testar.temporal.behavior.TemporalController;
import nl.ou.testar.temporal.behavior.TemporalControllerFactory;
import org.fruit.alayer.Action;
import org.fruit.alayer.State;
import org.fruit.monkey.ConfigTags;
import org.fruit.monkey.Settings;
import org.testar.protocols.DesktopProtocol;

import java.util.Set;

/**
 * This is a small change to Desktop Generic Protocol to use the learned state model for
 * improved action selection algorithm.
 *
 * Please note, that this requires state model learning to be enabled in the test settings
 * (or in Setting Dialog user interface of TESTAR).
 *
 *  It only changes the selectAction() method.
 */


public class Protocol_desktop_generic_temporaloracles extends DesktopProtocol {
	protected TemporalController temporalController;

	/**
	 * Called once during the life time of TESTAR
	 * This method can be used to perform initial setup work
	 * @param   settings  the current TESTAR settings as specified by the user.
	 */
	@Override
	protected void initialize(Settings settings){
				super.initialize(settings);
		// comment the following block if output is to be conserved for each test sequence
		if ( mode() == Modes.Generate || mode() == Modes.Record  ) {
			if (settings.get(ConfigTags.TemporalOffLineEnabled)) {
				temporalController = TemporalControllerFactory.getTemporalController(settings);
			}
		}
	}
	@Override
	protected void preSequencePreparations() {
		super.preSequencePreparations();
		// uncomment the following block if output is to be conserved for each test sequence
		/*		if ( mode() == Modes.Generate || mode() == Modes.Record  ) {
					if (settings.get(ConfigTags.TemporalOffLineEnabled)) {
					temporalController = TemporalControllerFactory.getTemporalController(settings);
					}
				}
		*/
	}


	/**
	 * Select one of the available actions using an action selection algorithm (for example random action selection)
	 *
	 * @param state the SUT's current state
	 * @param actions the set of derived actions
	 * @return  the selected action (non-null!)
	 */
	@Override
	protected Action selectAction(State state, Set<Action> actions){

		//Call the preSelectAction method from the AbstractProtocol so that, if necessary,
		//unwanted processes are killed and SUT is put into foreground.
		Action retAction = preSelectAction(state, actions);
		if (retAction== null) {
			//if no preSelected actions are needed, then implement your own action selection strategy
			//using the action selector of the state model:
			retAction = stateModelManager.getAbstractActionToExecute(actions);
		}
		if(retAction==null) {
			System.out.println("State model based action selection did not find an action. Using default action selection.");
			// if state model fails, use default:
			retAction = super.selectAction(state, actions);
		}
		return retAction;
	}
	@Override
	protected void postSequenceProcessing() {
		super.postSequenceProcessing();
		// uncomment the following block if modelcheck is required per test sequence
		/*		if (settings.get(ConfigTags.TemporalOffLineEnabled)) {
				temporalController.MCheck();
		} */
	}
	@Override
	protected void closeTestSession() {
		// comment the following block if modelcheck is required per test sequence
		if (settings.get(ConfigTags.TemporalOffLineEnabled)) {
			temporalController.MCheck();
		}
		super.closeTestSession();
	}

}
