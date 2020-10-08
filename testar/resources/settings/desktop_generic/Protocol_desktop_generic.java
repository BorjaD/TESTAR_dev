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
import org.fruit.alayer.exceptions.*;
import org.fruit.alayer.windows.UIARoles;
import org.fruit.monkey.Settings;
import org.testar.action.priorization.ActionTags;
import org.testar.action.priorization.ActionTags.ActionGroupType;
import org.testar.protocols.DesktopProtocol;

// ENFOQUE 4
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.imageio.ImageIO;
import org.testar.OutputStructure;
import nl.ou.testar.a11y.reporting.HTMLReporter;

/**
 * This protocol provides default TESTAR behaviour to test Windows desktop applications.
 *
 * It uses random action selection algorithm.
 */
public class Protocol_desktop_generic extends DesktopProtocol {
	
	Action lastActionExecuted;
	
	private HTMLDifference htmlDifference;			// ENFOQUE 4
	String differenceScreenshot;

	/**
	 * Called once during the life time of TESTAR
	 * This method can be used to perform initial setup work
	 * @param   settings  the current TESTAR settings as specified by the user.
	 */
	@Override
	protected void initialize(Settings settings){
		super.initialize(settings);
		htmlDifference = new HTMLDifference();		// ENFOQUE 4
	}

	/**
	 * This methods is called before each test sequence, before startSystem(),
	 * allowing for example using external profiling software on the SUT
	 *
	 * HTML sequence report will be initialized in the super.preSequencePreparations() for each sequence
	 */
	@Override
	protected void preSequencePreparations() {
		super.preSequencePreparations();
	}

	/**
	 * This method is called when TESTAR starts the System Under Test (SUT). The method should
	 * take care of
	 *   1) starting the SUT (you can use TESTAR's settings obtainable from <code>settings()</code> to find
	 *      out what executable to run)
	 *   2) waiting until the system is fully loaded and ready to be tested (with large systems, you might have to wait several
	 *      seconds until they have finished loading)
	 * @return  a started SUT, ready to be tested.
	 */
	@Override
	protected SUT startSystem() throws SystemStartException{
		return super.startSystem();
	}

	/**
	 * This method is invoked each time the TESTAR starts the SUT to generate a new sequence.
	 * This can be used for example for bypassing a login screen by filling the username and password
	 * or bringing the system into a specific start state which is identical on each start (e.g. one has to delete or restore
	 * the SUT's configuration files etc.)
	 */
	 @Override
	protected void beginSequence(SUT system, State state){
	 	super.beginSequence(system, state);
	}

	/**
	 * This method is called when the TESTAR requests the state of the SUT.
	 * Here you can add additional information to the SUT's state or write your
	 * own state fetching routine. The state should have attached an oracle
	 * (TagName: <code>Tags.OracleVerdict</code>) which describes whether the
	 * state is erroneous and if so why.
	 *
	 * super.getState(system) puts the state information also to the HTML sequence report
	 *
	 * @return  the current state of the SUT with attached oracle.
	 */
	@Override
	protected State getState(SUT system) throws StateBuildException{
		// ENFOQUE 4
		// Save previous state object
		State previousState = latestState;
				
		// Update state information
		State state = super.getState(system);
				
		if(previousState != null && previousState.get(Tags.ScreenshotPath, null) != null && state.get(Tags.ScreenshotPath, null) != null) {
					
			// Create and obtain the image-diff path
			differenceScreenshot = getDifferenceImage(
				previousState.get(Tags.ScreenshotPath), previousState.get(Tags.ConcreteIDCustom, ""),
				state.get(Tags.ScreenshotPath), state.get(Tags.ConcreteIDCustom, "")
				);
						
			// Update: output\timestamp_app_version\HTMLreports\StateDifferenceReport.html
			htmlDifference.addStateDifferenceStep(actionCount, previousState.get(Tags.ScreenshotPath), state.get(Tags.ScreenshotPath), differenceScreenshot);
		}
				
		return state;
	}

	/**
	 * The getVerdict methods implements the online state oracles that
	 * examine the SUT's current state and returns an oracle verdict.
	 * @return oracle verdict, which determines whether the state is erroneous and why.
	 */
	@Override
	protected Verdict getVerdict(State state){
		// The super methods implements the implicit online state oracles for:
		// system crashes
		// non-responsiveness
		// suspicious titles
		Verdict verdict = super.getVerdict(state);

		//--------------------------------------------------------
		// MORE SOPHISTICATED STATE ORACLES CAN BE PROGRAMMED HERE
		//--------------------------------------------------------

		return verdict;
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
		
		// ** -->
		//for(Widget w : state) {
		//	w.get(Tags.Title, "");
		//}
		
		
		// ENFOQUE 2: Decrecimiento iterativo de los QValues
		/*
		// Si una acción no tiene tag ActionGroup asociado y si pertenece a un gupo de ActionGroup, asignar un tag ActionGroup
		for(Action a : actions) {
			ActionTags.ActionGroupType actionGroup = a.get(ActionTags.ActionGroup);
			if(actionGroup == null) {
				Role actionRole = a.get(Tags.Role);
				String actionRoleStr = actionRole.toString();
				switch(actionRoleStr) {
					case "UIAWidget":
						actionGroup = ActionGroupType.UIAWidget;
					case "UIAAppBar":
						actionGroup = ActionGroupType.UIAAppBar;
					case "UIAButton":
						actionGroup = ActionGroupType.UIAButton;
					case "UIACalendar":
						actionGroup = ActionGroupType.UIACalendar;
					case "UIACheckBox":
						actionGroup = ActionGroupType.UIACheckBox;
					case "UIAComboBox":
						actionGroup = ActionGroupType.UIAComboBox;
					case "UIACustomControl":
						actionGroup = ActionGroupType.UIACustomControl;
					case "UIADataGrid":
						actionGroup = ActionGroupType.UIADataGrid;
					case "UIADataItem":
						actionGroup = ActionGroupType.UIADataItem;
					case "UIADocument":
						actionGroup = ActionGroupType.UIADocument;
					case "UIAEdit":
						actionGroup = ActionGroupType.UIAEdit;
					case "UIAGroup":
						actionGroup = ActionGroupType.UIAGroup;
					case "UIAHeader":
						actionGroup = ActionGroupType.UIAHeader;
					case "UIAHeaderItem":
						actionGroup = ActionGroupType.UIAHeaderItem;
					case "UIAHyperlink":
						actionGroup = ActionGroupType.UIAHyperlink;
					case "UIAImage":
						actionGroup = ActionGroupType.UIAImage;
					case "UIAList":
						actionGroup = ActionGroupType.UIAList;
					case "UIAListItem":
						actionGroup = ActionGroupType.UIAListItem;
					case "UIAMenuBar":
						actionGroup = ActionGroupType.UIAMenuBar;
					case "UIAMenu":
						actionGroup = ActionGroupType.UIAMenu;
					case "UIAMenuItem":
						actionGroup = ActionGroupType.UIAMenuItem;
					case "UIAPane":
						actionGroup = ActionGroupType.UIAPane;
					case "UIAProgressBar":
						actionGroup = ActionGroupType.UIAProgressBar;
					case "UIARadioButton":
						actionGroup = ActionGroupType.UIARadioButton;
					case "UIAScrollBar":
						actionGroup = ActionGroupType.UIAScrollBar;
					case "UIASemanticZoom":
						actionGroup = ActionGroupType.UIASemanticZoom;
					case "UIASeparator":
						actionGroup = ActionGroupType.UIASeparator;
					case "UIASlider":
						actionGroup = ActionGroupType.UIASlider;
					case "UIASpinner":
						actionGroup = ActionGroupType.UIASpinner;
					case "UIASplitButton":
						actionGroup = ActionGroupType.UIASplitButton;
					case "UIAStatusBar":
						actionGroup = ActionGroupType.UIAStatusBar;
					case "UIATabControl":
						actionGroup = ActionGroupType.UIATabControl;
					case "UIATabItem":
						actionGroup = ActionGroupType.UIATabItem;
					case "UIATable":
						actionGroup = ActionGroupType.UIATable;
					case "UIAText":
						actionGroup = ActionGroupType.UIAText;
					case "UIAThumb":
						actionGroup = ActionGroupType.UIAThumb;
					case "UIATitleBar":
						actionGroup = ActionGroupType.UIATitleBar;
					case "UIAToolBar":
						actionGroup = ActionGroupType.UIAToolBar;
					case "UIAToolTip":
						actionGroup = ActionGroupType.UIAToolTip;
					case "UIATree":
						actionGroup = ActionGroupType.UIATree;
					case "UIATreeItem":
						actionGroup = ActionGroupType.UIATreeItem;
					case "UIAWindow":
						actionGroup = ActionGroupType.UIAWindow;
					// ...
				}
			}
			
			// Asignación de ZIndex
			if(actionGroup != null && a.get(ActionTags.ZIndex) == null) {
				String ZIndexStr = a.get(Tags.ZIndex).toString();
				int ZIndexInt = Integer.parseInt(ZIndexStr);
				a.set(ActionTags.ZIndex, ZIndexInt);
			}
					
			// 1) Si la acción no tiene ActionGroup y tampoco un valor en su tag QLearning, asignar el valor más alto (1) a su Tag QLearning.
			// 2) Si la acción tiene un ActionGroup pero no un valor en su tag QLearning, comprobar si existen acciones secundarias con su mismo Action Group y ZIndex.
			// 3)		Si existen, comprobar si alguna de las acciones secundarias tiene un valor en su tag QLearning.
			// 4)			Si alguna acción secundaria tiene un valor, asignar a la acción inicial el valor de QLearning de la acción secundaria.
			// 5)			Si ninguna acción secundaria tiene un valor, asignar a la acción inicial el valor más alto (1) a su Tag QLearning.
			// 6)		Si no existen, asignar a la acción inicial el valor más alto (1) a su Tag QLearning.
			double actionQLearning = a.get(ActionTags.QLearning);
			if(actionGroup == null && actionQLearning == 0) {										// 1)
				a.set(ActionTags.QLearning, 1.0);
			}
			if(actionGroup != null && actionQLearning == 0) {										// 2)
				double auxQValue = 0.0;
				for (Action a2 : actions) {
					if(a2.get(ActionTags.ActionGroup).equals(a.get(ActionTags.ActionGroup)) && a2.get(ActionTags.ZIndex).equals(a.get(ActionTags.ZIndex))) {
						if (a2.get(ActionTags.QLearning) != 0) {									// 3)
							auxQValue = a2.get(ActionTags.QLearning);								// 4)
							break;
						}
					}
				}
				if(auxQValue != 0.0) {
					a.set(ActionTags.QLearning, auxQValue);
				} else {																			// 5) 6)
					a.set(ActionTags.QLearning, 1.0);
				}
			}
		}
		*/
		
		
		// ENFOQUE 3: Dar una mayor recompensa a las acciones que produzcan un mayor cambio en el programa
		/*
		// Número de widgets que siguen estando tras ejecutar la acción
		int persistentWidgetNum = 0;
		for(Widget w : state) {
			String wID = w.get(Tags.ConcreteIDCustom);
			for (Widget lw : latestState) {
				String lwID = lw.get(Tags.ConcreteIDCustom);
				if(wID == lwID) persistentWidgetNum ++;
			}
		}
		
		double persistentDecrement = persistentWidgetNum * 0.02;
		double newQL = greaterThanZero(lastActionExecuted.get(ActionTags.QLearning) - persistentDecrement);
		lastActionExecuted.set(ActionTags.QLearning, newQL);
		
		// Número de widgets en el estado previo y en el actual
		int numWidgetsBefore = latestState.childCount();
		int numWidgetsNow = state.childCount();
				
		// Si no existe widget tree anterior, no hacer nada.
		if(numWidgetsBefore > 0) {
			if(numWidgetsBefore < numWidgetsNow) {
				double newQLearningValue = lastActionExecuted.get(ActionTags.QLearning);
				newQLearningValue += (numWidgetsNow - numWidgetsBefore) / numWidgetsBefore;
				lastActionExecuted.set(ActionTags.QLearning, newQLearningValue);
			}
			if(numWidgetsBefore > numWidgetsNow) {
				double newQLearningValue = lastActionExecuted.get(ActionTags.QLearning);
				newQLearningValue = greaterThanZero(newQLearningValue - (numWidgetsNow / numWidgetsBefore));
				lastActionExecuted.set(ActionTags.QLearning, newQLearningValue);
			}
		}
		*/
		
		
		
		// ENFOQUE 4
		// [state 1] --(a)--> [state 2]
		// Comparar la imagen de los estados [state 1] y [state 2], y ver cuántos píxeles han cambiado.
		// La recompensa de (a) será mayor cuantos más píxeles cambien entre [state 1] y [state 2].
		// Usar la información de "htmlDifference"
		
		try {
			BufferedImage diffScreanshot = ImageIO.read(new File(differenceScreenshot));			
			
			double totalPixels = diffScreanshot.getWidth() * diffScreanshot.getHeight();
			double differentPixels = 0;
			int[] pixelsArray = diffScreanshot.getRGB(0, 0, diffScreanshot.getWidth(), diffScreanshot.getHeight(), null, 0, diffScreanshot.getWidth());
			for (int i = 0; i < totalPixels; i++) {
			    if (pixelsArray[i] != Color.Black.argb32()) {
			    	differentPixels ++;
			    }
			}
			double diffPxPercentage = differentPixels / totalPixels;
			System.out.println("Totales: " + totalPixels);
			System.out.println("Diferentes: " + differentPixels);
			System.out.println("Porcentaje: " + diffPxPercentage);
			
			//double newQLearning = lastActionExecuted.get(ActionTags.QLearning, 0.0) + diffPxPercentage;
			//lastActionExecuted.set(ActionTags.QLearning, newQLearning);

			System.out.println("Reward: " + lastActionExecuted.get(ActionTags.QLearning));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		// <-- **


		//return the set of derived actions
		return actions;
	}

	/**
	 * Select one of the available actions using an action selection algorithm (for example random action selection)
	 *
	 * super.selectAction(state, actions) updates information to the HTML sequence report
	 *
	 * @param state the SUT's current state
	 * @param actions the set of derived actions
	 * @return  the selected action (non-null!)
	 */
	@Override
	protected Action selectAction(State state, Set<Action> actions){
		// Elegir mayor reward
		// for(Action a : actions) {
		// 	 if(a.get(ActionTags.SimilarityValue, 0) > 1) {
		//		return a;
		//	 }
		// }
		
		
		// ENFOQUE 2: Decrecimiento iterativo de los QValues
		/*
		//Seleccionar la acción con el mayor valor en su tag QLearning.
		Action maxAction = null;
		double maxQLearning = 0.0;
		for(Action a : actions) {
			double actionQLearning = a.get(ActionTags.QLearning);
			if(actionQLearning > maxQLearning)
				maxAction = a;
		}
		
		// Actualizar los valores de QLearning de las acciones. El valor en acciones de un ActionGroup será menor
		// a medida que se ejecuten acciones de dicho ActionGroup.
		if (maxAction != null) {
			double newQL = greaterThanZero(maxAction.get(ActionTags.QLearning) - 0.05);
			maxAction.set(ActionTags.QLearning, newQL);
			return maxAction;
		}
		*/
		
		
		
		// ENFOQUE 3 y 4: Dar una mayor recompensa a las acciones que produzcan un mayor cambio en el programa
		
		//Seleccionar la acción con el mayor valor en su tag QLearning.
		Action maxAction = null;
		double maxQLearning = 0.0;
		for(Action a : actions) {
			double actionQLearning = a.get(ActionTags.QLearning, 0.0);
			if(actionQLearning > maxQLearning)
				maxAction = a;
		}
		
		if (maxAction != null) {
			lastActionExecuted = maxAction;
			return maxAction;
		}
				
		
		// <-- **
		
		return(super.selectAction(state, actions));
	}

	/**
	 * Execute the selected action.
	 *
	 * super.executeAction(system, state, action) is updating the HTML sequence report with selected action
	 *
	 * @param system the SUT
	 * @param state the SUT's current state
	 * @param action the action to execute
	 * @return whether or not the execution succeeded
	 */
	@Override
	protected boolean executeAction(SUT system, State state, Action action){
		return super.executeAction(system, state, action);
	}

	/**
	 * TESTAR uses this method to determine when to stop the generation of actions for the
	 * current sequence. You can stop deriving more actions after:
	 * - a specified amount of executed actions, which is specified through the SequenceLength setting, or
	 * - after a specific time, that is set in the MaxTime setting
	 * @return  if <code>true</code> continue generation, else stop
	 */
	@Override
	protected boolean moreActions(State state) {
		return super.moreActions(state);
	}


	/**
	 * TESTAR uses this method to determine when to stop the entire test sequence
	 * You could stop the test after:
	 * - a specified amount of sequences, which is specified through the Sequences setting, or
	 * - after a specific time, that is set in the MaxTime setting
	 * @return  if <code>true</code> continue test, else stop
	 */
	@Override
	protected boolean moreSequences() {
		return super.moreSequences();
	}

	/**
	 * Here you can put graceful shutdown sequence for your SUT
	 * @param system
	 */
	@Override
	protected void stopSystem(SUT system) {
		super.stopSystem(system);
	}

	/**
	 * This methods is called after each test sequence, allowing for example using external profiling software on the SUT
	 *
	 * super.postSequenceProcessing() is adding test verdict into the HTML sequence report
	 */
	@Override
	protected void postSequenceProcessing() {
		super.postSequenceProcessing();
	}
	
	private double greaterThanZero (double d) {
		if(d >= 0.0) return d;
		else return 0.0;
	}

	
	// ENFOQUE 4
	/**
	 * State image difference 
	 * 
	 * https://stackoverflow.com/questions/25022578/highlight-differences-between-images
	 * 
	 * @param previousStateDisk
	 * @param namePreviousState
	 * @param stateDisk
	 * @param nameState
	 * @return
	 */
	private String getDifferenceImage(String previousStateDisk, String namePreviousState, String stateDisk, String nameState) {
		try {

			// State Images paths
			String previousStatePath = new File(previousStateDisk).getCanonicalFile().toString();
			String statePath = new File(stateDisk).getCanonicalFile().toString();
			
			System.out.println("Action: " + actionCount);
			System.out.println("PreviousState: " + previousStatePath);
			System.out.println("CurrentState: " + statePath);
			
			// TESTAR launches the process to create the image and move forward without wait if exists
			// thats why we need to check and wait to obtain the image-diff
			while(!new File(previousStatePath).exists() || !new File(statePath).exists()){
				System.out.println("Waiting for Screenshot creation");
				System.out.println("Waiting... PreviousState: " + previousStatePath);
				System.out.println("Waiting... CurrentState: " + statePath);
				Util.pause(2);
			}
			
			BufferedImage img1 = ImageIO.read(new File(previousStatePath));
			BufferedImage img2 = ImageIO.read(new File(statePath));

			int width1 = img1.getWidth(); // Change - getWidth() and getHeight() for BufferedImage
			int width2 = img2.getWidth(); // take no arguments
			int height1 = img1.getHeight();
			int height2 = img2.getHeight();
			if ((width1 != width2) || (height1 != height2)) {
				System.out.println("Error: Images dimensions mismatch");
				return "";
			}

			// NEW - Create output Buffered image of type RGB
			BufferedImage outImg = new BufferedImage(width1, height1, BufferedImage.TYPE_INT_RGB);

			// Modified - Changed to int as pixels are ints
			int diff;
			int result; // Stores output pixel
			for (int i = 0; i < height1; i++) {
				for (int j = 0; j < width1; j++) {
					int rgb1 = img1.getRGB(j, i);
					int rgb2 = img2.getRGB(j, i);
					int r1 = (rgb1 >> 16) & 0xff;
					int g1 = (rgb1 >> 8) & 0xff;
					int b1 = (rgb1) & 0xff;
					int r2 = (rgb2 >> 16) & 0xff;
					int g2 = (rgb2 >> 8) & 0xff;
					int b2 = (rgb2) & 0xff;
					diff = Math.abs(r1 - r2); // Change
					diff += Math.abs(g1 - g2);
					diff += Math.abs(b1 - b2);
					diff /= 3; // Change - Ensure result is between 0 - 255
					// Make the difference image gray scale
					// The RGB components are all the same
					result = (diff << 16) | (diff << 8) | diff;
					outImg.setRGB(j, i, result); // Set result
				}
			}

			// Now save the image on disk
			// check if we have a directory for the screenshots yet
			File screenshotDir = new File(OutputStructure.htmlOutputDir + File.separator);

			// save the file to disk
			File screenshotFile = new File(screenshotDir, "diff_"+ namePreviousState + "_" + nameState + ".png");
			if (screenshotFile.exists()) {
				try {
					return screenshotFile.getCanonicalPath();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			FileOutputStream outputStream = new FileOutputStream(screenshotFile.getCanonicalPath());

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(outImg, "png", baos);
			byte[] bytes = baos.toByteArray();

			outputStream.write(bytes);
			outputStream.flush();
			outputStream.close();

			return screenshotFile.getCanonicalPath();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return "";
	}
}

/**
 * Helper Class to prepare HTML State report Difference
 */
class HTMLDifference {
	
	PrintWriter out = null;

	public HTMLDifference () { 
		String[] HEADER = new String[] {
				"<!DOCTYPE html>",
				"<html>",
				"<style>",
				".container {display: flex;}",
				".float {display:inline-block;}",
				"</style>",
				"<head>",
				"<title>TESTAR State Model difference report</title>",
				"</head>",
				"<body>"
		};
		
		String htmlReportName =  OutputStructure.htmlOutputDir + File.separator + "StateDifferenceReport.html";

		try {
			out = new PrintWriter(new File(htmlReportName).getCanonicalPath(), HTMLReporter.CHARSET);
		} catch (IOException e) { e.printStackTrace(); }

		for(String s:HEADER){
			out.println(s);
			out.flush();
		}
	}
	
	public void addStateDifferenceStep(int actionCount, String previousState, String state, String difference) {
		out.println("<h2> Specific State changes for action " + actionCount + " </h2>");
		try {
			out.println("<p><img src=\"" + new File(previousState).getCanonicalFile().toString() + "\">");
			out.println("<img src=\"" + new File(state).getCanonicalFile().toString() + "\">");
			out.println("<img src=\"" + new File(difference).getCanonicalFile().toString() + "\"></p>");
		} catch (IOException e) {
			out.println("<p> ERROR ADDING IMAGES </p>");;
		}
		out.flush();
	}
}