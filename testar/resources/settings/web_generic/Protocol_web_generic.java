/***************************************************************************************************
 *
 * Copyright (c) 2013, 2014, 2015, 2016, 2017, 2018, 2019 Universitat Politecnica de Valencia - www.upv.es
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

import java.util.Set;

import org.fruit.Util;
import org.fruit.alayer.*;
import org.fruit.alayer.actions.AnnotatingActionCompiler;
import org.fruit.alayer.actions.CompoundAction;
import org.fruit.alayer.actions.KeyDown;
import org.fruit.alayer.actions.KeyUp;
import org.fruit.alayer.actions.StdActionCompiler;
import org.fruit.alayer.actions.Type;
import org.fruit.alayer.devices.AWTKeyboard;
import org.fruit.alayer.devices.KBKeys;
import org.fruit.alayer.devices.Keyboard;
import org.fruit.alayer.exceptions.ActionBuildException;
import org.fruit.alayer.exceptions.StateBuildException;
import org.fruit.alayer.exceptions.SystemStartException;
import org.fruit.alayer.windows.UIATags;
import org.fruit.alayer.windows.WinProcess;

import es.upv.staq.testar.CodingManager;
import es.upv.staq.testar.NativeLinker;
import es.upv.staq.testar.serialisation.LogSerialiser;
import nl.ou.testar.RandomActionSelector;

import org.fruit.monkey.ConfigTags;
import org.fruit.monkey.Settings;
import org.testar.protocols.DesktopProtocol;

/**
 * This protocol is using the default Windows accessibility API (Windows UI Automation API) to test Web applications.
 */
public class Protocol_web_generic extends DesktopProtocol {

	// This protocol expects Mozilla Firefox or Microsoft Internet Explorer on Windows10

	static Role webText; // browser dependent
	static double browser_toolbar_filter;

	/** 
	 * Called once during the life time of TESTAR
	 * This method can be used to perform initial setup work
	 * @param   settings   the current TESTAR settings as specified by the user.
	 */
	@Override
	protected void initialize(Settings settings){
		super.initialize(settings);
		initBrowser();
	}

	// check browser
	private void initBrowser(){
		webText = NativeLinker.getNativeRole("UIAEdit"); // just init with some value
		String sutPath = settings().get(ConfigTags.SUTConnectorValue);
		if (sutPath.contains("iexplore.exe"))
			webText = NativeLinker.getNativeRole("UIAEdit");
		else if (sutPath.contains("firefox"))
			webText = NativeLinker.getNativeRole("UIAText");
	}

	@Override
	protected SUT startSystem() throws SystemStartException{

		SUT sut = super.startSystem();

		return sut;

	}

	@Override
	protected void beginSequence(SUT system, State state) {

		// Bring IExplorer to the foreground
		if(!state.get(Tags.Foreground, true) && system.get(Tags.SystemActivator, null) != null){
			WinProcess.toForeground(system.get(Tags.PID), 0.5, 100);
			state = super.getState(system);
		}

		//Execute IExplorer on maximized windows (-k flags hide the url information)
		for(Widget w : state) {
			if(w.get(Tags.Title,"").contains("Maximise")) {
				Role role = w.get(Tags.Role, Roles.Widget);
				if(Role.isOneOf(role, new Role[]{NativeLinker.getNativeRole("UIAButton")})) {
					StdActionCompiler ac = new AnnotatingActionCompiler();
					executeAction(system, state, ac.leftClickAt(w));

					Util.pause(2);

					state = getState(system);
				}
			}
		}

		for(Widget w : state) {
			if(w.get(Tags.Title, "").equals("Usuario")){
				StdActionCompiler ac = new AnnotatingActionCompiler();
				//Action a = ac.clickTypeInto(w, "username", true);
				Action a = ac.clickTypeInto(w, settings.get(ConfigTags.LoginUsername, "NoUsername"), true);
				executeAction(system,state, a);
				Util.pause(1);
			}
		}

		for(Widget w : state) {
			if(w.get(Tags.Title, "").equals("Password")){
				StdActionCompiler ac = new AnnotatingActionCompiler();
				//Action a = ac.clickTypeInto(w, "password", true);
				Action a = ac.clickTypeInto(w, settings.get(ConfigTags.LoginPassword, "NoPassword"), true);
				executeAction(system,state, a);
				Util.pause(1);
			}
		}

		//login is performed by ENTER 
		new CompoundAction.Builder()
		.add(new KeyDown(KBKeys.VK_ENTER),0.5)
		.build()
		.run(system, null, 0.1);

		Util.pause(2);

		//Don't save any previous executed actions by TESTAR �Bug?
		userEvent = null;

		state = getState(system);

	}

	/**
	 * This method is called when TESTAR requests the state of the SUT.
	 * Here you can add additional information to the SUT's state or write your
	 * own state fetching routine. The state should have attached an oracle 
	 * (TagName: <code>Tags.OracleVerdict</code>) which describes whether the 
	 * state is erroneous and if so why.
	 * @return  the current state of the SUT with attached oracle.
	 */
	@Override
	protected State getState(SUT system) throws StateBuildException{

		State state = super.getState(system);

		if(!state.get(Tags.Foreground, true) && system.get(Tags.SystemActivator, null) != null){
			WinProcess.toForeground(system.get(Tags.PID), 0.5, 5);
			state = super.getState(system);
		}

		for(Widget w : state){
			Role role = w.get(Tags.Role, Roles.Widget);
			if(Role.isOneOf(role, new Role[]{NativeLinker.getNativeRole("UIAToolBar")})) {
				browser_toolbar_filter = w.get(Tags.Shape,null).y() + w.get(Tags.Shape,null).height();
			}

			//Disable specific widgets (protocol is prioritized over test.settings)
			if(w.get(Tags.Title,"").contains("Help")) {
				w.set(Tags.Enabled,false);
			}
			if(w.get(Tags.Title,"").contains("LOGOUT")) {
				w.set(Tags.Enabled,false);
			}
			if(w.get(Tags.Title,"").contains("Logout")) {
				w.set(Tags.Enabled,false);
			}
			if(w.get(Tags.Title,"").contains("No es seguro")) {
				w.set(Tags.Enabled,false);
			}
			if(w.get(Tags.Title,"").contains("Export to Pdf")) {
				w.set(Tags.Enabled,false);
			}
			if(w.get(Tags.Title,"").contains("POSIDONIA")) {
				w.set(Tags.Enabled,false);
			}

		}

		return state;

	}

	@Override
	protected Set<Action> deriveActions(SUT system, State state) throws ActionBuildException {

		Set<Action> actions = super.deriveActions(system,state);

		// To derive actions (such as clicks, drag&drop, typing ...) we should first create an action compiler.
		StdActionCompiler ac = new AnnotatingActionCompiler();

		// To find all possible actions that TESTAR can click on we should iterate through all widgets of the state.
		for(Widget w : (settings.get(ConfigTags.TopWidgetsState, false) ? getTopWidgets(state) : state)){

			if(w.get(Tags.Role, Roles.Widget).toString().equalsIgnoreCase("UIAMenu")){
				// filtering out actions on menu-containers (that would add an action in the middle of the menu)
				continue; // skip this iteration of the for-loop
			}

			// Only consider enabled and non-blocked widgets
			if(w.get(Tags.Enabled, true) && !w.get(Tags.Blocked, false)){

				// Do not build actions for widgets on the blacklist
				// The blackListed widgets are those that have been filtered during the SPY mode with the
				//CAPS_LOCK + SHIFT + Click clickfilter functionality.
				if (!blackListed(w)){

					//For widgets that are:
					// - clickable
					// and
					// - unFiltered by any of the regular expressions in the Filter-tab, or
					// - whitelisted using the clickfilter functionality in SPY mode (CAPS_LOCK + SHIFT + CNTR + Click)
					// We want to create actions that consist of left clicking on them
					if(isClickable(w) && (isUnfiltered(w) || whiteListed(w))) {
						//Create a left click action with the Action Compiler, and add it to the set of derived actions
						actions.add(ac.leftClickAt(w));
					}

					//For widgets that are:
					// - typeable
					// and
					// - unFiltered by any of the regular expressions in the Filter-tab, or
					// - whitelisted using the clickfilter functionality in SPY mode (CAPS_LOCK + SHIFT + CNTR + Click)
					// We want to create actions that consist of typing into them
					if(isTypeable(w) && (isUnfiltered(w) || whiteListed(w))) {
						//Create a type action with the Action Compiler, and add it to the set of derived actions
						actions.add(ac.clickTypeInto(w, this.getRandomText(w), true));
					}
					//Add sliding actions (like scroll, drag and drop) to the derived actions
					//method defined below.
					addSlidingActions(actions,ac,SCROLL_ARROW_SIZE,SCROLL_THICK,w, state);
				}
			}
		}
		return actions;
	}


	@Override
	protected boolean isClickable(Widget w){
		if (isAtBrowserCanvas(w))
			return super.isClickable(w);
		else
			return false;		
	} 

	@Override
	protected boolean isTypeable(Widget w){
		if (!isAtBrowserCanvas(w))
			return false;	

		Role role = w.get(Tags.Role, null);
		if (role != null && Role.isOneOf(role, webText))
			return isUnfiltered(w);

		return false;
	}

	private boolean isAtBrowserCanvas(Widget w){
		Shape shape = w.get(Tags.Shape,null);
		if (shape != null && shape.y() > browser_toolbar_filter)
			return true;
		else
			return false;		
	}

	@Override
	protected Action selectAction(State state, Set<Action> actions){

		for(Widget w : state){
			//Check current browser tab, to close possible undesired tabs
			if(settings.get(ConfigTags.Mode, Modes.Spy).equals(Modes.Generate) && 
					w.get(Tags.Title,"").contains("Address and search")) {

				if(!w.get(Tags.ValuePattern,"").contains("prodevelop")
						|| !w.get(UIATags.UIAValueValue,"").contains("prodevelop")
						|| w.get(Tags.ValuePattern,"").contains("exportarPdf")
						|| w.get(UIATags.UIAValueValue,"").contains("exportarPdf")) {

					Action closeTab = new CompoundAction.Builder()  
							.add(new KeyDown(KBKeys.VK_CONTROL), 0.5)
							.add(new KeyDown(KBKeys.VK_W), 0.5)
							.add(new KeyUp(KBKeys.VK_CONTROL), 0.5)
							.add(new KeyUp(KBKeys.VK_W),0.5).build();
					closeTab.set(Tags.Desc, "Close Non Desired Tab: Control + W");
					CodingManager.buildEnvironmentActionIDs(state, closeTab);
					return closeTab;
				}
			}
		}

		return super.selectAction(state, actions);
	}

	@Override
	protected boolean executeAction(SUT system, State state, Action action){

		for(Widget w: state) {
			if(w.get(Tags.Title,"").contains("Address and search")) {
				LogSerialiser.log(String.format("URL: %s \n", w.get(UIATags.UIAValueValue, "No URL detected")));
				htmlReport.addInformation(String.format("URL: %s", w.get(UIATags.UIAValueValue, "No URL detected")));
			}
		}
		
		
		boolean executed = super.executeAction(system, state, action);

		Keyboard kb = AWTKeyboard.build();
		kb.release(KBKeys.VK_CONTROL);
		kb.release(KBKeys.VK_W);

		return executed;
	}

}
