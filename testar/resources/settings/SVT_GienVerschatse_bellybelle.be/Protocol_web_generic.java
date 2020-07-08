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
import org.fruit.alayer.actions.StdActionCompiler;
import org.fruit.alayer.actions.Type;
import org.fruit.alayer.devices.AWTKeyboard;
import org.fruit.alayer.devices.KBKeys;
import org.fruit.alayer.devices.Keyboard;
import org.fruit.alayer.exceptions.ActionBuildException;
import org.fruit.alayer.exceptions.StateBuildException;
import es.upv.staq.testar.NativeLinker;
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
    protected void beginSequence(SUT system, State state) {
        super.beginSequence(system, state);

        
        Keyboard kb = AWTKeyboard.build();
        
        // go to e-mail field of contact page
        // after every actions it needs a pause, or the key actions get swallowed and testar starts typing the wrong field
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        
        /**
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        **/
        
        // not writing a valid e-mail address because I don't want to send out the contact form
        // it will hit send and trigger validation
        // code on how to do that however is present in comment
        new CompoundAction.Builder()
        .add(new Type("gien"),0.5).build()
        .run(system, null, 1);
        
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
       
        /**
        
        Util.pause(1);
        
        // write @
        kb.press(KBKeys.VK_SHIFT);
		kb.press(KBKeys.VK_2);
		kb.release(KBKeys.VK_2);
		kb.release(KBKeys.VK_SHIFT);

        Util.pause(1);
        
        new CompoundAction.Builder()
        .add(new Type("eightpointsquared.com"),0.5)
        .add(new KeyDown(KBKeys.VK_TAB),0.5).build()
        .run(system, null, 1);
        **/
        
        // pause or G wil not appear
        Util.pause(1);
        
        new CompoundAction.Builder()
        .add(new Type("Gien"),0.5).build()
        .run(system, null, 1);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        // pause or V wil not appear
        Util.pause(1);
        
        new CompoundAction.Builder()
        .add(new Type("Verschatse"),0.5).build()
        .run(system, null, 1);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        // pause or letters will be swallowed
        Util.pause(1);
        
        new CompoundAction.Builder()
        .add(new Type("Bestelling mondmasker - TESTAR testing"),0.5).build()
        .run(system, null, 1);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_TAB);
        kb.release(KBKeys.VK_TAB);
        
        Util.pause(1);
        
        kb.press(KBKeys.VK_ENTER);
        kb.release(KBKeys.VK_ENTER);
        
        Util.pause(5);
        
        // other method could be used by searching the Title field in the spy mode for each component
        // which is activated by shift + arrow_up
        // and loop over the widgets
        // this seemed a better approach however because a lot of pauses were needed
       
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

        for(Widget w : state){
            Role role = w.get(Tags.Role, Roles.Widget);
            if(Role.isOneOf(role, new Role[]{NativeLinker.getNativeRole("UIAToolBar")}))
            	browser_toolbar_filter = w.get(Tags.Shape,null).y() + w.get(Tags.Shape,null).height();
        }
		
		return state;
		
	}


	/**
	 * This method is used by TESTAR to determine the set of currently available actions.
	 * You can use the SUT's current state, analyze the widgets and their properties to create
	 * a set of sensible actions, such as: "Click every Button which is enabled" etc.
	 * The return value is supposed to be non-null. If the returned set is empty, TESTAR
	 * will stop generation of the current action and continue with the next one.
	 * @param system the SUT
	 * @param state the SUT's current state
	 * @return  a set of actions
	 */
	@Override
	protected Set<Action> deriveActions(SUT system, State state) throws ActionBuildException{

		//The super method returns a ONLY actions for killing unwanted processes if needed, or bringing the SUT to
		//the foreground. You should add all other actions here yourself.
		// These "special" actions are prioritized over the normal GUI actions in selectAction() / preSelectAction().
		Set<Action> actions = super.deriveActions(system,state);


		// Derive left-click actions, click and type actions, and scroll actions from
		// top level (highest Z-index) widgets of the GUI:
		actions = deriveClickTypeScrollActionsFromTopLevelWidgets(actions, system, state);

		if(actions.isEmpty()){
			// If the top level widgets did not have any executable widgets, try all widgets:
//			System.out.println("No actions from top level widgets, changing to all widgets.");
			// Derive left-click actions, click and type actions, and scroll actions from
			// all widgets of the GUI:
			actions = deriveClickTypeScrollActionsFromAllWidgetsOfState(actions, system, state);
		}

		//return the set of derived actions
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
		
}
