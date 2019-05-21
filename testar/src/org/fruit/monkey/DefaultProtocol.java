/***************************************************************************************************
 *
 * Copyright (c) 2013, 2014, 2015, 2016, 2017, 2018 Universitat Politecnica de Valencia - www.upv.es
 * Copyright (c) 2018 Open Universiteit - www.ou.nl
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


package org.fruit.monkey;

import static org.fruit.alayer.Tags.ActionDelay;
import static org.fruit.alayer.Tags.ActionDuration;
import static org.fruit.alayer.Tags.ActionSet;
import static org.fruit.alayer.Tags.Desc;
import static org.fruit.alayer.Tags.ExecutedAction;
import static org.fruit.alayer.Tags.IsRunning;
import static org.fruit.alayer.Tags.OracleVerdict;
import static org.fruit.alayer.Tags.SystemState;
import static org.fruit.alayer.Tags.Title;
import static org.fruit.monkey.ConfigTags.LogLevel;
import static org.fruit.monkey.ConfigTags.OutputDir;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import es.upv.staq.testar.*;
import nl.ou.testar.*;
import nl.ou.testar.StateModel.StateModelManager;
import nl.ou.testar.StateModel.StateModelManagerFactory;
import org.fruit.Assert;
import org.fruit.Drag;
import org.fruit.Pair;
import org.fruit.Util;
import org.fruit.alayer.AbsolutePosition;
import org.fruit.alayer.Action;
import org.fruit.alayer.AutomationCache;
import org.fruit.alayer.Canvas;
import org.fruit.alayer.Color;
import org.fruit.alayer.FillPattern;
import org.fruit.alayer.Finder;
import org.fruit.alayer.Pen;
import org.fruit.alayer.Point;
import org.fruit.alayer.Role;
import org.fruit.alayer.Roles;
import org.fruit.alayer.SUT;
import org.fruit.alayer.Shape;
import org.fruit.alayer.State;
import org.fruit.alayer.StateBuilder;
import org.fruit.alayer.StrokePattern;
import org.fruit.alayer.Taggable;
import org.fruit.alayer.TaggableBase;
import org.fruit.alayer.Tags;
import org.fruit.alayer.Verdict;
import org.fruit.alayer.Visualizer;
import org.fruit.alayer.Widget;
import org.fruit.alayer.actions.*;
import org.fruit.alayer.devices.AWTMouse;
import org.fruit.alayer.devices.KBKeys;
import org.fruit.alayer.devices.Mouse;
import org.fruit.alayer.devices.MouseButtons;
import org.fruit.alayer.exceptions.ActionBuildException;
import org.fruit.alayer.exceptions.ActionFailedException;
import org.fruit.alayer.exceptions.NoSuchTagException;
import org.fruit.alayer.exceptions.StateBuildException;
import org.fruit.alayer.exceptions.SystemStartException;
import org.fruit.alayer.exceptions.WidgetNotFoundException;
import org.fruit.alayer.visualizers.ShapeVisualizer;
import org.fruit.alayer.windows.WinApiException;

import es.upv.staq.testar.managers.DataManager;
import es.upv.staq.testar.serialisation.LogSerialiser;
import es.upv.staq.testar.serialisation.ScreenshotSerialiser;
import es.upv.staq.testar.serialisation.TestSerialiser;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.slf4j.LoggerFactory;

public class DefaultProtocol extends RuntimeControlsProtocol {

	public static boolean faultySequence;
	private State stateForClickFilterLayerProtocol;

	public State getStateForClickFilterLayerProtocol() {
		return stateForClickFilterLayerProtocol;
	}

	public void setStateForClickFilterLayerProtocol(State stateForClickFilterLayerProtocol) {
		this.stateForClickFilterLayerProtocol = stateForClickFilterLayerProtocol;
	}

	private String generatedSequence;

	private File currentSeq;

	protected Mouse mouse = AWTMouse.build();
	protected State lastState = null;
	protected int nonReactingActionNumber;

	protected ProcessListener processListener = new ProcessListener();
	private boolean enabledProcessListener = false;
	public static Verdict processVerdict = Verdict.OK;

	protected String lastPrintParentsOf = "null-id";
	protected int actionCount;

	protected final int actionCount() {
		return actionCount;
	}

	protected int sequenceCount;

	protected final int sequenceCount() {
		return sequenceCount;
	}

	protected int firstSequenceActionNumber;
	protected int lastSequenceActionNumber;
	double startTime;

	protected final double timeElapsed() {
		return Util.time() - startTime;
	}

	protected List<ProcessInfo> contextRunningProcesses = null;
	protected static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	protected static final org.slf4j.Logger DEBUGLOG = LoggerFactory.getLogger(AbstractProtocol.class);
	protected double passSeverity = Verdict.SEVERITY_OK;
	protected static int generatedSequenceNumber = -1;

	public final static int generatedSequenceCount() {
		return generatedSequenceNumber;
	}

	protected static Action lastExecutedAction = null;

	public final static Action lastExecutedAction() {
		return lastExecutedAction;
	}

	protected long lastStamp = -1;
	protected ProtocolUtil protocolUtil = new ProtocolUtil();
	protected EventHandler eventHandler;
	protected Canvas cv;
	protected Pattern clickFilterPattern = null;
	protected Map<String, Matcher> clickFilterMatchers = new WeakHashMap<String, Matcher>();
	protected Pattern suspiciousTitlesPattern = null;
	protected Map<String, Matcher> suspiciousTitlesMatchers = new WeakHashMap<String, Matcher>();
	private StateBuilder builder;
	protected String forceKillProcess = null;
	protected boolean forceToForeground = false,
			forceNextActionESC = false;
	protected int testFailTimes = 0;
	protected boolean nonSuitableAction = false;

	protected StateModelManager stateModelManager;
	private String startOfSutDateString; //value set when SUT started, used for calculating the duration of test

	protected final static Pen RedPen = Pen.newPen().setColor(Color.Red).
			setFillPattern(FillPattern.None).setStrokePattern(StrokePattern.Solid).build(),
			BluePen = Pen.newPen().setColor(Color.Blue).
			setFillPattern(FillPattern.None).setStrokePattern(StrokePattern.Solid).build();


	/**
	 * This is the abstract flow of TESTAR (generate mode):
	 *
	 * - Initialize TESTAR settings
	 * - InitTestSession (before starting the first sequence)
	 * - OUTER LOOP:
	 * 		PreSequencePreparations (before each sequence, for example starting WebDriver, JaCoCo or another process for sequence)
	 * 		StartSUT
	 * 		BeginSequence (starting "script" on the GUI of the SUT, for example login)
	 * 		INNER LOOP
	 * 			GetState
	 * 			GetVerdict
	 * 			StopCriteria (moreActions/moreSequences/time?)
	 * 			DeriveActions
	 * 			SelectAction
	 * 			ExecuteAction
	 * 		FinishSequence (closing "script" on the GUI of the SUT, for example logout)
	 * 		StopSUT
	 * 		PostSequenceProcessing (after each sequence)
	 * - CloseTestSession (after finishing the last sequence)
	 *
	 */

	/**
	 * Starting the TESTAR loops
	 *
	 * @param settings
	 */
	public final void run(final Settings settings) {

		//Associate start settings of the first TESTAR dialog
		this.settings = settings;

		SUT system = null;

		//initialize TESTAR with the given settings:
		initialize(settings);

		try {

			if (mode() == Modes.View && isValidFile()) {
				new SequenceViewer(settings);
			} else if (mode() == Modes.Replay && isValidFile()) {
				runReplayLoop();
			} else if (mode() == Modes.Spy) {
				runSpyLoop();
			} else if(mode() == Modes.Record) {
				runRecordLoop(system);
			} else if (mode() == Modes.Generate) {
				runGenerateOuterLoop(system);
			}

		}catch(WinApiException we) {
			System.out.println("Exception: Check if current SUTs path: "+settings.get(ConfigTags.SUTConnectorValue)
			+" is a correct definition");
			this.mode = Modes.Quit;
		}catch(SystemStartException SystemStartException) {
			SystemStartException.printStackTrace();
			DEBUGLOG.error("Exception: ",SystemStartException);
			this.mode = Modes.Quit;
		} catch (Exception e) {
			e.printStackTrace();
			DEBUGLOG.error("Exception: ",e);
			this.mode = Modes.Quit;
		}

		//Closing TESTAR EventHandler
		closeTestarTestSession();
	}

	/**
	 * Initialize TESTAR with the given settings:
	 *
	 * @param settings
	 */
	protected void initialize(Settings settings) {

		startTime = Util.time();
		this.settings = settings;
		mode = settings.get(ConfigTags.Mode);

		//EventHandler is implemented in RuntimeControlsProtocol (super class):
		eventHandler = initializeEventHandler();

		//builder = new UIAStateBuilder(settings.get(ConfigTags.TimeToFreeze));
		builder = NativeLinker.getNativeStateBuilder(
				settings.get(ConfigTags.TimeToFreeze),
				settings.get(ConfigTags.AccessBridgeEnabled),
				settings.get(ConfigTags.SUTProcesses)
				);

		// new state model manager
		if ( mode() == Modes.Generate || mode() == Modes.Record || mode() == Modes.Replay )
			stateModelManager = StateModelManagerFactory.getStateModelManager(settings);

		try {
			if (!settings.get(ConfigTags.UnattendedTests)) {
				LogSerialiser.log("Registering keyboard and mouse hooks\n", LogSerialiser.LogLevel.Debug);
				Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
				logger.setLevel(Level.OFF);
				logger.setUseParentHandlers(false);

				if (GlobalScreen.isNativeHookRegistered()) {
					GlobalScreen.unregisterNativeHook();
				}
				GlobalScreen.registerNativeHook();
				GlobalScreen.addNativeKeyListener(eventHandler);
				GlobalScreen.addNativeMouseListener(eventHandler);
				GlobalScreen.addNativeMouseMotionListener(eventHandler);
				LogSerialiser.log("Successfully registered keyboard and mouse hooks!\n", LogSerialiser.LogLevel.Debug);
			}

			LogSerialiser.log("'" + mode() + "' mode active.\n", LogSerialiser.LogLevel.Info);


		} catch (NativeHookException e) {
			LogSerialiser.log("Unable to install keyboard and mouse hooks!\n", LogSerialiser.LogLevel.Critical);
			throw new RuntimeException("Unable to install keyboard and mouse hooks!", e);
		}
	}

	/**
	 * Check if the selected file to Replay or View contains a valid fragment object
	 */

	public boolean isValidFile(){
		try {

			File seqFile = new File(settings.get(ConfigTags.PathToReplaySequence));

			FileInputStream fis = new FileInputStream(seqFile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			GZIPInputStream gis = new GZIPInputStream(bis);
			ObjectInputStream ois = new ObjectInputStream(gis);

			ois.readObject();
			ois.close();

		} catch (ClassNotFoundException | IOException e) {

			System.out.println("ERROR: File is not a readable, please select a correct file");
			DEBUGLOG.error("Exception: ",e);

			return false;	
		}

		return true;
	}

	/**
	 * This method is called from runGenerate() to initialize TESTAR for Generate-mode
	 */
	private void initGenerateMode() {
		//TODO check why LogSerializer is closed and started again in the beginning of Generate-mode
		sequenceCount = 1;
		lastStamp = System.currentTimeMillis();
		escAttempts = 0;
	}

	/**
	 * This method creates the name for generated sequence that can be replayed
	 * and starts the LogSerialiser for outputting the test sequence
	 *
	 * @return name of the generated sequence file
	 */
	private String getAndStoreGeneratedSequence() {
		//TODO refactor replayable sequences with something better (model perhaps?)
		String generatedSequence = Util.generateUniqueFile(settings.get(ConfigTags.OutputDir) + File.separator + "sequences", "sequence").getName();
		generatedSequenceNumber = new Integer(generatedSequence.replace("sequence", ""));

		try {
			LogSerialiser.start(new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(
					settings.get(OutputDir) + File.separator + "logs" + File.separator + generatedSequence + ".log"), true))),
					settings.get(LogLevel));
		} catch (NoSuchTagException e3) {
			// TODO Auto-generated catch block
			DEBUGLOG.error("Exception: ",e3);
			e3.printStackTrace();
		} catch (FileNotFoundException e3) {
			// TODO Auto-generated catch block
			DEBUGLOG.error("Exception: ",e3);
			e3.printStackTrace();
		}
		ScreenshotSerialiser.start(settings.get(ConfigTags.OutputDir), generatedSequence);
		return generatedSequence;
	}

	/**
	 * This method creates a temporary file for saving the test sequence (that can be replayed)
	 * The name of the temporary file is changed in the end of the test sequence (not in this function)
	 *
	 * @return temporary file for saving the test sequence
	 */
	private File getAndStoreSequenceFile() {
		LogSerialiser.log("Creating new sequence file...\n", LogSerialiser.LogLevel.Debug);
		final File currentSeq = new File(settings.get(ConfigTags.TempDir) + File.separator + "tmpsequence");

		try {
			Util.delete(currentSeq);
		} catch (IOException e2) {
			LogSerialiser.log("I/O exception deleting <" + currentSeq + ">\n", LogSerialiser.LogLevel.Critical);
			DEBUGLOG.error("Exception: ",e2);
		}
		try {
			TestSerialiser.start(new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(currentSeq, true))));
			LogSerialiser.log("Created new sequence file!\n", LogSerialiser.LogLevel.Debug);
		} catch (IOException e) {
			LogSerialiser.log("I/O exception creating new sequence file\n", LogSerialiser.LogLevel.Critical);
			DEBUGLOG.error("Exception: ",e);
		}
		return currentSeq;
	}


	/**
	 * This method calls the startSystem() if the system is not yet running
	 *
	 * @param system
	 * @return SUT system
	 */
	private SUT startSutIfNotRunning(SUT system) {
		//If system==null, we have started TESTAR from the Generate mode and system has not been started yet (if started in SPY-mode or Record-mode, the system is running already)
		if (system == null || !system.isRunning()) {
			system = startSystem();
			//Reset LogSerialiser
			LogSerialiser.finish();
			LogSerialiser.exit();
			startOfSutDateString = Util.dateString(DATE_FORMAT);
			LogSerialiser.log(startOfSutDateString + " Starting SUT ...\n", LogSerialiser.LogLevel.Info);
			LogSerialiser.log("SUT is running!\n", LogSerialiser.LogLevel.Debug);
			LogSerialiser.log("Building canvas...\n", LogSerialiser.LogLevel.Debug);

			//Activate process Listeners if enabled in the test.settings
			if(enabledProcessListener)
				processListener.startListeners(system, settings);
		}

		return system;
	}

	private Taggable fragment; // Fragment is used for saving a replayable sequence:
	private long tStart;

	/**
	 * This method is initializing TESTAR for the start of test sequence
	 *
	 * @param system
	 */
	private void startTestSequence(SUT system) {
		//for measuring the time of one sequence:
		tStart = System.currentTimeMillis();
		DEBUGLOG.info("Starting test sequence {}", sequenceCount());

		actionCount = 1;
		this.testFailTimes = 0;
		lastSequenceActionNumber = settings().get(ConfigTags.SequenceLength) + actionCount - 1;
		firstSequenceActionNumber = actionCount;
		passSeverity = Verdict.SEVERITY_OK;
		processVerdict = Verdict.OK;
		this.cv = buildCanvas();
	}

	/**
	 * Closing test sequence of TESTAR in normal operation
	 */
	private void endTestSequence(){
		LogSerialiser.log("Releasing canvas...\n", LogSerialiser.LogLevel.Debug);
		cv.release();
		ScreenshotSerialiser.exit();
		TestSerialiser.exit();
		//        String stopDateString = Util.dateString(DATE_FORMAT);
		//        String durationDateString = Util.diffDateString(DATE_FORMAT, startOfSutDateString, stopDateString);
		//        LogSerialiser.log("TESTAR stopped execution at " + stopDateString + "\n", LogSerialiser.LogLevel.Critical);
		//        LogSerialiser.log("Test duration was " + durationDateString + "\n", LogSerialiser.LogLevel.Critical);
		LogSerialiser.flush();
		LogSerialiser.finish();
		LogSerialiser.exit();
		DEBUGLOG.info("Test sequence {} finished in {} ms", sequenceCount(), System.currentTimeMillis() - tStart);
	}

	/**
	 * Shutting down TESTAR in case of exception during Generate-mode outer loop (test sequence generation)
	 *
	 * @param system
	 * @param e
	 */
	private void emergencyTerminateTestSequence(SUT system, Exception e){
		SystemProcessHandling.killTestLaunchedProcesses(this.contextRunningProcesses);
		ScreenshotSerialiser.finish();
		TestSerialiser.finish();
		ScreenshotSerialiser.exit();
		LogSerialiser.log("Exception <" + e.getMessage() + "> has been caught\n", LogSerialiser.LogLevel.Critical); // screenshots must be serialised
		int i = 1;
		StringBuffer trace = new StringBuffer();
		for (StackTraceElement t : e.getStackTrace())
			trace.append("\n\t[" + i++ + "] " + t.toString());
		System.out.println("Exception <" + e.getMessage() + "> has been caught; Stack trace:" + trace.toString());
		stopSystem(system);
		TestSerialiser.exit();
		LogSerialiser.flush();
		LogSerialiser.finish();
		LogSerialiser.exit();
		this.mode = Modes.Quit;
	}

	/**
	 * Method to run TESTAR on Generate mode
	 *
	 * @param system
	 */
	protected void runGenerateOuterLoop(SUT system) {
		boolean startFromGenerate = false;
		if(system==null)
			startFromGenerate = true;
		//method for defining other init actions, like setup of external environment
		initTestSession();

		//initializing TESTAR for generate mode:

		initGenerateMode();

		/*
		 ***** OUTER LOOP - STARTING A NEW SEQUENCE
		 */
		while (mode() != Modes.Quit && moreSequences()) {

			//empty method in defaultProtocol - allowing implementation in application specific protocols:
			preSequencePreparations();

			//starting system if it's not running yet (TESTAR could be started in SPY-mode or Record-mode):
			system = startSutIfNotRunning(system);

			if(startFromGenerate) {
				//Generating the sequence file that can be replayed:
				generatedSequence = getAndStoreGeneratedSequence();
				currentSeq = getAndStoreSequenceFile();
			}

			//initializing TESTAR for a new sequence:
			startTestSequence(system);

			try {
				// getState() called before beginSequence:
				LogSerialiser.log("Obtaining system state before beginSequence...\n", LogSerialiser.LogLevel.Debug);
				State state = getState(system);

				// beginSequence() - a script to interact with GUI, for example login screen
				LogSerialiser.log("Starting sequence " + sequenceCount + " (output as: " + generatedSequence + ")\n\n", LogSerialiser.LogLevel.Info);
				beginSequence(system, state);

				//initializing fragment for recording replayable test sequence:
				initFragmentForReplayableSequence(state);

				// notify the statemodelmanager
				stateModelManager.notifyTestSequencedStarted();

				/*
				 ***** starting the INNER LOOP:
				 */
				Verdict stateVerdict = runGenerateInnerLoop(system, state);

				//Saving the state into replayable test sequence:
				saveStateIntoFragmentForReplayableSequence(state);

				//calling finishSequence() to allow scripting GUI interactions to close the SUT:
				finishSequence();

				// notify the state model manager of the sequence end
				stateModelManager.notifyTestSequenceStopped();

				writeAndCloseFragmentForReplayableSequence();

				if (faultySequence)
					LogSerialiser.log("Sequence contained faults!\n", LogSerialiser.LogLevel.Critical);
				
				Verdict finalVerdict = stateVerdict.join(processVerdict);

				processVerdict = Verdict.OK;

				//Copy sequence file into proper directory:
				classifyAndCopySequenceIntoAppropriateDirectory(finalVerdict,generatedSequence,currentSeq);

				//calling postSequenceProcessing() to allow resetting test environment after test sequence, etc
				postSequenceProcessing();

				//Ending test sequence of TESTAR:
				endTestSequence();

				LogSerialiser.log("End of test sequence - shutting down the SUT...\n", LogSerialiser.LogLevel.Info);
				stopSystem(system);
				LogSerialiser.log("... SUT has been shut down!\n", LogSerialiser.LogLevel.Debug);

				sequenceCount++;

			} catch (Exception e) {
				System.out.println("Thread: name=" + Thread.currentThread().getName() + ",id=" + Thread.currentThread().getId() + ", TESTAR throws exception");
				e.printStackTrace();
				DEBUGLOG.error("Exception: ",e);
				emergencyTerminateTestSequence(system, e);
			}
		}
		// notify the statemodelmanager that the testing has finished
		stateModelManager.notifyTestingEnded();
		//allowing close-up in the end of test session:
		closeTestSession();
		//Closing TESTAR internal test session:
		closeTestarTestSession();
		mode = Modes.Quit;
	}

	private void classifyAndCopySequenceIntoAppropriateDirectory(Verdict finalVerdict,String generatedSequence,File currentSeq){
		if (!settings().get(ConfigTags.OnlySaveFaultySequences) ||
				finalVerdict.severity() >= settings().get(ConfigTags.FaultThreshold)) {
			LogSerialiser.log("Copying generated sequence (\"" + generatedSequence + "\") to output directory...\n", LogSerialiser.LogLevel.Info);
			try {
				Util.copyToDirectory(currentSeq.getAbsolutePath(),
						settings.get(ConfigTags.OutputDir) + File.separator + "sequences",
						generatedSequence,
						true);
				LogSerialiser.log("Copied generated sequence to output directory!\n", LogSerialiser.LogLevel.Debug);
			} catch (NoSuchTagException e) {
				LogSerialiser.log("No such tag exception copying test sequence\n", LogSerialiser.LogLevel.Critical);
				DEBUGLOG.error("Exception: ",e);
			} catch (IOException e) {
				LogSerialiser.log("I/O exception copying test sequence\n", LogSerialiser.LogLevel.Critical);
				DEBUGLOG.error("Exception: ",e);
			}
			FileHandling.copyClassifiedSequence(generatedSequence, currentSeq, finalVerdict, settings.get(ConfigTags.OutputDir));
		}
	}

	/**
	 * This is the inner loop of TESTAR Generate-mode:
	 * * 			GetState
	 * * 			GetVerdict
	 * * 			StopCriteria (moreActions/moreSequences/time?)
	 * * 			DeriveActions
	 * * 			SelectAction
	 * * 			ExecuteAction
	 *
	 * @param system
	 */
	private Verdict runGenerateInnerLoop(SUT system, State state) {
		/*
		 ***** INNER LOOP:
		 */
		while (mode() != Modes.Quit && moreActions(state = getState(system))) {

			if (mode() == Modes.Record) {
				runRecordLoop(system);
			}

			// getState() including getVerdict() that is saved into the state:
			LogSerialiser.log("Obtained system state in inner loop of TESTAR...\n", LogSerialiser.LogLevel.Debug);
			cv.begin(); Util.clear(cv);
			//Not visualizing the widget info under cursor while in Generate-mode:
			//SutVisualization.visualizeState(false, markParentWidget, mouse, protocolUtil, lastPrintParentsOf, delay, cv);

			//Deriving actions from the state:
			Set<Action> actions = deriveActions(system, state);
			CodingManager.buildIDs(state, actions);
			// notify to state model the current state
			stateModelManager.notifyNewStateReached(state, actions);


			if(actions.isEmpty()){
				if (mode() != Modes.Spy && escAttempts >= MAX_ESC_ATTEMPTS){
					LogSerialiser.log("No available actions to execute! Tried ESC <" + MAX_ESC_ATTEMPTS + "> times. Stopping sequence generation!\n", LogSerialiser.LogLevel.Critical);
				}
				//----------------------------------
				// THERE MUST ALMOST BE ONE ACTION!
				//----------------------------------
				// if we did not find any actions, then we just hit escape, maybe that works ;-)
				Action escAction = new AnnotatingActionCompiler().hitKey(KBKeys.VK_ESCAPE);
				CodingManager.buildIDs(state, escAction);
				actions.add(escAction);
				escAttempts++;
			} else
				escAttempts = 0;
			//Showing the green dots if visualization is on:
			if(visualizationOn) visualizeActions(cv, state, actions);

			//Selecting one of the available actions:
			Action action = selectAction(state, actions);
			//Showing the red dot if visualization is on:
			if(visualizationOn) SutVisualization.visualizeSelectedAction(settings, cv, state, action);

			//before action execution, pass it to the state model manager
			stateModelManager.notifyActionExecution(action);

			//Executing the selected action:
			executeAction(system, state, action);
			lastExecutedAction = action;
			actionCount++;

			//Saving the actions and the executed action into replayable test sequence:
			saveActionIntoFragmentForReplayableSequence(action, state, actions);

			// Resetting the visualization:
			Util.clear(cv);
			cv.end();
		}

		// notify to state model the last state
		Set<Action> actions = deriveActions(system, state);
		CodingManager.buildIDs(state, actions);
		stateModelManager.notifyNewStateReached(state, actions);
		
		return getVerdict(state);
	}

	/**
	 * This method initializes the fragment for replayable sequence
	 *
	 * @param state
	 */
	private void initFragmentForReplayableSequence(State state){
		// Fragment is used for saving a replayable sequence:
		fragment = new TaggableBase();
		fragment.set(SystemState, state);
		fragment.set(OracleVerdict, getVerdict(state));
	}

	/**
	 * Saving the action into the fragment for replayable sequence
	 *
	 * @param action
	 */
	private void saveActionIntoFragmentForReplayableSequence(Action action, State state, Set<Action> actions) {
		fragment.set(OracleVerdict, getVerdict(state).join(processVerdict));
		fragment.set(ExecutedAction,action);
		fragment.set(ActionSet, actions);
		fragment.set(ActionDuration, settings().get(ConfigTags.ActionDuration));
		fragment.set(ActionDelay, settings().get(ConfigTags.TimeToWaitAfterAction));
		LogSerialiser.log("Writing fragment to sequence file...\n",LogSerialiser.LogLevel.Debug);
		TestSerialiser.write(fragment);
		//resetting the fragment:
		fragment =new TaggableBase();
		fragment.set(SystemState, state);
	}


	/**
	 * Saving the action into the fragment for replayable sequence
	 *
	 * @param state
	 */
	private void saveStateIntoFragmentForReplayableSequence(State state) {
		fragment.set(OracleVerdict, getVerdict(state).join(processVerdict));
		fragment.set(ActionDuration, settings().get(ConfigTags.ActionDuration));
		fragment.set(ActionDelay, settings().get(ConfigTags.TimeToWaitAfterAction));
		LogSerialiser.log("Writing fragment to sequence file...\n",LogSerialiser.LogLevel.Debug);
		TestSerialiser.write(fragment);
		//resetting the fragment:
		fragment =new TaggableBase();
		fragment.set(SystemState, state);
	}

	/**
	 * Writing the fragment into file and closing the test serialiser
	 */
	private void writeAndCloseFragmentForReplayableSequence() {
		//closing ScreenshotSerialiser:
		ScreenshotSerialiser.finish();
		LogSerialiser.log("Writing fragment to sequence file...\n", LogSerialiser.LogLevel.Debug);
		TestSerialiser.write(fragment);

		//Wait since TestSerialiser write all fragments on sequence File
		while(!TestSerialiser.isSavingQueueEmpty() && !ScreenshotSerialiser.isSavingQueueEmpty()) {
			//System.out.println("Saving sequences...");
			synchronized (this) {
				try {
					this.wait(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		TestSerialiser.finish();
		LogSerialiser.log("Wrote fragment to sequence file!\n", LogSerialiser.LogLevel.Debug);
		LogSerialiser.log("Sequence " + sequenceCount + " finished.\n", LogSerialiser.LogLevel.Info);
	}

	/**
	 * Saving the action information into the logs
	 * 
	 * @param state
	 * @param action
	 * @param actionMode
	 */
	private void saveActionInfoInLogs(State state, Action action, String actionMode) {

		//Obtain action information
		String[] actionRepresentation = Action.getActionRepresentation(state,action,"\t");

		//Output/logs folder
		LogSerialiser.log(String.format(actionMode+" [%d]: %s\n%s",
				actionCount,
				"\n @Action ConcreteID = " + action.get(Tags.ConcreteID,"ConcreteID not available") +
				" AbstractID = " + action.get(Tags.AbstractID,"AbstractID not available") +"\n"+
				" ConcreteID CUSTOM = " +  action.get(Tags.ConcreteIDCustom,"ConcreteID CUSTOM not available")+
				" AbstractID CUSTOM = " +  action.get(Tags.AbstractIDCustom,"AbstractID CUSTOM not available")+"\n"+

				" @State ConcreteID = " + state.get(Tags.ConcreteID,"ConcreteID not available") +
				" AbstractID = " + state.get(Tags.Abstract_R_ID,"Abstract_R_ID not available") +"\n"+
				" ConcreteID CUSTOM = "+ state.get(Tags.ConcreteIDCustom,"ConcreteID CUSTOM not available")+
				" AbstractID CUSTOM = "+state.get(Tags.AbstractIDCustom,"AbstractID CUSTOM not available")+"\n",
				actionRepresentation[0]) + "\n",
				LogSerialiser.LogLevel.Info);

		//bin folder 
		DEBUGLOG.info(actionMode+" number {} Widget {} finished in {} ms",
				actionCount,actionRepresentation[1],System.currentTimeMillis()-tStart);


	}

	/**
	 * Method to run TESTAR on Spy Mode.
	 */
	protected void runSpyLoop() {

		//Create or detect the SUT & build canvas representation
		SUT system = startSystem();
		this.cv = buildCanvas();

		while(mode() == Modes.Spy && system.isRunning()) {

			//Instead of use getState(SUT system) method, build our own State into Spy Mode
			//This will prevent issues with other protocols and reports
			Assert.notNull(system);
			State state = builder.apply(system);
			CodingManager.buildIDs(state);
			calculateZIndices(state);
			setStateForClickFilterLayerProtocol(state);
			
			cv.begin(); Util.clear(cv);

			//in Spy-mode, always visualize the widget info under the mouse cursor:
			SutVisualization.visualizeState(visualizationOn, markParentWidget, mouse, protocolUtil, lastPrintParentsOf, cv,state);

			Set<Action> actions = deriveActions(system,state);
			CodingManager.buildIDs(state, actions);

			//in Spy-mode, always visualize the green dots:
			visualizeActions(cv, state, actions);
			cv.end();

			//TODO: Prepare Spy Mode refresh time on settings file
			//Wait 500ms to show the actions
			synchronized (this) {
				try {
					this.wait(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		//If user closes the SUT while in Spy-mode, TESTAR will close (or go back to SettingsDialog):
		if(!system.isRunning()){
			this.mode = Modes.Quit;
		}

		Util.clear(cv);
		cv.end();

		//Stop and close the SUT before return to the detectModeLoop
		stopSystem(system);

	}

	/**
	 * Method to run TESTAR on Record User Actions Mode.
	 * @param system
	 */
	protected void runRecordLoop(SUT system) {
		boolean startedRecordMode = false;

		//If system it's null means that we have started TESTAR from the Record User Actions Mode
		//We need to invoke the SUT & the canvas representation
		if(system == null) {
			
			preSequencePreparations();
			
			system = startSystem();
			startedRecordMode = true;
			this.cv = buildCanvas();
			actionCount = 1;

			//Reset LogSerialiser
			LogSerialiser.finish();
			LogSerialiser.exit();

			//Generating the sequence file that can be replayed:
			generatedSequence = getAndStoreGeneratedSequence();
			currentSeq = getAndStoreSequenceFile();

			//Activate process Listeners if enabled in the test.settings
			if(enabledProcessListener)
				processListener.startListeners(system, settings);

			//initializing fragment for recording replayable test sequence:
			initFragmentForReplayableSequence(getState(system));

			// notify the statemodelmanager
			stateModelManager.notifyTestSequencedStarted();
		}
		//else, SUT & canvas exists (startSystem() & buildCanvas() created from Generate mode)


		/**
		 * Start Record User Action Loop
		 */
		while(mode() == Modes.Record && system.isRunning()) {
			State state = getState(system);
			cv.begin(); Util.clear(cv);

			Set<Action> actions = deriveActions(system,state);
			CodingManager.buildIDs(state, actions);

			//notify the state model manager of the new state
			stateModelManager.notifyNewStateReached(state, actions);

			if(actions.isEmpty()){
				if (escAttempts >= MAX_ESC_ATTEMPTS){
					LogSerialiser.log("No available actions to execute! Tried ESC <" + MAX_ESC_ATTEMPTS + "> times. Stopping sequence generation!\n", LogSerialiser.LogLevel.Critical);
				}
				//----------------------------------
				// THERE MUST ALMOST BE ONE ACTION!
				//----------------------------------
				// if we did not find any actions, then we just hit escape, maybe that works ;-)
				Action escAction = new AnnotatingActionCompiler().hitKey(KBKeys.VK_ESCAPE);
				CodingManager.buildIDs(state, escAction);
				actions.add(escAction);
				escAttempts++;
			} else
				escAttempts = 0;

			ActionStatus actionStatus = new ActionStatus();

			//Start Wait User Action Loop to obtain the Action did by the User
			waitUserActionLoop(cv, system, state, actionStatus);

			//Save the user action information into the logs
			if (actionStatus.isUserEventAction()) {

				CodingManager.buildIDs(state, actionStatus.getAction());

				//notify the state model manager of the executed action
				stateModelManager.notifyActionExecution(actionStatus.getAction());

				saveActionInfoInLogs(state, actionStatus.getAction(), "RecordedAction");
				lastExecutedAction = actionStatus.getAction();
				actionCount++;
			}

			/**
			 * When we close TESTAR with Shift+down arrow, last actions is detected like null
			 */
			if(actionStatus.getAction()!=null) {
				//System.out.println("DEBUG: User action is not null");
				saveActionIntoFragmentForReplayableSequence(actionStatus.getAction(), state, actions);
			}else {
				//System.out.println("DEBUG: User action ----- null");
			}


			Util.clear(cv);
			cv.end();
		}

		//If user change to Generate mode & we start TESTAR on Record mode, invoke Generate mode with the created SUT
		if(mode() == Modes.Generate && startedRecordMode){
			Util.clear(cv);
			cv.end();

			runGenerateOuterLoop(system);
		}

		//If user closes the SUT while in Record-mode, TESTAR will close (or go back to SettingsDialog):
		if(!system.isRunning()){
			this.mode = Modes.Quit;
		}

		if(startedRecordMode && mode() == Modes.Quit){
			// notify the statemodelmanager
			stateModelManager.notifyTestSequenceStopped();

			// notify the state model manager of the sequence end
			stateModelManager.notifyTestingEnded();

			//Closing fragment for recording replayable test sequence:
			writeAndCloseFragmentForReplayableSequence();

			//Copy sequence file into proper directory:
			classifyAndCopySequenceIntoAppropriateDirectory(Verdict.OK,generatedSequence,currentSeq);
			
			postSequenceProcessing();

			//If we want to Quit the current execution we stop the system
			stopSystem(system);
		}
	}

	protected void runReplayLoop(){
		actionCount = 1;
		boolean success = true;
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		GZIPInputStream gis = null;
		ObjectInputStream ois = null;
		
		preSequencePreparations();
		
		SUT system = startSystem();
		try{
			File seqFile = new File(settings.get(ConfigTags.PathToReplaySequence));
			//ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(seqFile)));
			fis = new FileInputStream(seqFile);
			bis = new BufferedInputStream(fis);
			gis = new GZIPInputStream(bis);
			ois = new ObjectInputStream(gis);

			Canvas cv = buildCanvas();
			State state = getState(system);

			String replayMessage;

			double rrt = settings.get(ConfigTags.ReplayRetryTime);

			while(success && mode() != Modes.Quit){
				Taggable fragment;
				try{
					fragment = (Taggable) ois.readObject();
				} catch(IOException ioe){
					success = true;
					break;
				}

				success = false;
				int tries = 0;
				double start = Util.time();

				while(!success && (Util.time() - start < rrt)){
					tries++;
					cv.begin(); Util.clear(cv);
					//In Replay-mode, we DO NOT show the widget info under cursor:
					//SutVisualization.visualizeState(visualizationOn, markParentWidget, mouse, protocolUtil, lastPrintParentsOf, cv,state);
					cv.end();

					if(mode() == Modes.Quit) break;
					Action action = fragment.get(ExecutedAction, new NOP());
					// In Replay-mode, we only show the red dot if visualizationOn is true:
					if(visualizationOn) SutVisualization.visualizeSelectedAction(settings, cv, state, action);
					if(mode() == Modes.Quit) break;

					double actionDuration = settings.get(ConfigTags.UseRecordedActionDurationAndWaitTimeDuringReplay) ? fragment.get(Tags.ActionDuration, 0.0) : settings.get(ConfigTags.ActionDuration);
					double actionDelay = settings.get(ConfigTags.UseRecordedActionDurationAndWaitTimeDuringReplay) ? fragment.get(Tags.ActionDelay, 0.0) : settings.get(ConfigTags.TimeToWaitAfterAction);

					try{
						if(tries < 2){
							replayMessage = String.format("Trying to execute (%d): %s... [time window = " + rrt + "]", actionCount, action.get(Desc, action.toString()));
							//System.out.println(replayMessage);
							LogSerialiser.log(replayMessage, LogSerialiser.LogLevel.Info);
						}else{
							if(tries % 50 == 0)
								LogSerialiser.log(".\n", LogSerialiser.LogLevel.Info);
							else
								LogSerialiser.log(".", LogSerialiser.LogLevel.Info);
						}

						action.run(system, state, actionDuration);
						success = true;
						actionCount++;
						LogSerialiser.log("Success!\n", LogSerialiser.LogLevel.Info);
					} catch(ActionFailedException afe){}

					Util.pause(actionDelay);

					if(mode() == Modes.Quit) break;
					state = getState(system);
				}

			}

			cv.release();
			//ois.close();
			stopSystem(system);
			if (system != null && system.isRunning())
				system.stop();


		} catch(IOException ioe){
			DEBUGLOG.error("Exception: ",ioe);
			throw new RuntimeException("Cannot read file.", ioe);
		} catch (ClassNotFoundException cnfe) {
			DEBUGLOG.error("Exception: ",cnfe);
			throw new RuntimeException("Cannot read file.", cnfe);
		} finally {
			if (ois != null){
				try { ois.close(); } catch (IOException e) { e.printStackTrace(); }
			}
			if (gis != null){
				try { gis.close(); } catch (IOException e) { e.printStackTrace(); }
			}
			if (bis != null){
				try { bis.close(); } catch (IOException e) { e.printStackTrace(); }
			}
			if (fis != null){
				try { fis.close(); } catch (IOException e) { e.printStackTrace(); }
			}
			if (cv != null)
				cv.release();
			if (system != null)
				system.stop();
		}

		if(success){
			String msg = "Sequence successfully replayed!\n";
			System.out.println(msg);
			LogSerialiser.log(msg, LogSerialiser.LogLevel.Info);

		} else{
			String msg = "Failed to replay sequence.\n";
			System.out.println(msg);
			LogSerialiser.log(msg, LogSerialiser.LogLevel.Critical);
		}
		
		LogSerialiser.finish();
		postSequenceProcessing();

		// Going back to TESTAR settings dialog if it was used to start replay:
		mode = Modes.Quit;
	}

	/**
	 * This method is called before the first test sequence, allowing for example setting up the test environment
	 */
	@Override
	protected void initTestSession() {

	}

	/**
	 * This methods is called before each test sequence, allowing for example using external profiling software on the SUT
	 */
	@Override
	protected void preSequencePreparations() {

	}

	protected Canvas buildCanvas() {
		//return GDIScreenCanvas.fromPrimaryMonitor(Pen.DefaultPen);
		return NativeLinker.getNativeCanvas(Pen.PEN_DEFAULT);
	}

	@Override
	protected void beginSequence(SUT system, State state){
		faultySequence = false;
		nonReactingActionNumber = 0;
	}

	@Override
	protected void finishSequence(){
		SystemProcessHandling.killTestLaunchedProcesses(this.contextRunningProcesses);
	}

	// refactored
	protected SUT startSystem() throws SystemStartException{
		return startSystem(null);
	}

	/**
	 *
	 * @param mustContain Format is &lt;SUTConnector:string&gt; (e.g. SUT_PROCESS_NAME:proc_name or SUT_WINDOW_TITLE:window_title)
	 * @return
	 * @throws SystemStartException
	 */
	protected SUT startSystem(String mustContain) throws SystemStartException{
		return startSystem(mustContain, true, Math.round(settings().get(ConfigTags.StartupTime).doubleValue() * 1000.0));
	}


	protected SUT startSystem(String mustContain, boolean tryToKillIfRunning, long maxEngageTime) throws SystemStartException{
		this.contextRunningProcesses = SystemProcessHandling.getRunningProcesses("START");
		try{// refactored from "protected SUT startSystem() throws SystemStartException"
			for(String d : settings().get(ConfigTags.Delete))
				Util.delete(d);
			for(Pair<String, String> fromTo : settings().get(ConfigTags.CopyFromTo))
				Util.copyToDirectory(fromTo.left(), fromTo.right());
		}catch(IOException ioe){
			throw new SystemStartException(ioe);
		} // end refactoring
		String sutConnector = settings().get(ConfigTags.SUTConnector);
		if (mustContain != null && mustContain.startsWith(Settings.SUT_CONNECTOR_WINDOW_TITLE))
			return getSUTByWindowTitle(mustContain.substring(Settings.SUT_CONNECTOR_WINDOW_TITLE.length()+1));
		else if (mustContain != null && mustContain.startsWith(Settings.SUT_CONNECTOR_PROCESS_NAME))
			return getSUTByProcessName(mustContain.substring(Settings.SUT_CONNECTOR_PROCESS_NAME.length()+1));
		else if (sutConnector.equals(Settings.SUT_CONNECTOR_WINDOW_TITLE))
			return getSUTByWindowTitle(settings().get(ConfigTags.SUTConnectorValue));
		else if (sutConnector.startsWith(Settings.SUT_CONNECTOR_PROCESS_NAME))
			return getSUTByProcessName(settings().get(ConfigTags.SUTConnectorValue));
		else{ // Settings.SUT_CONNECTOR_CMDLINE
			Assert.hasText(settings().get(ConfigTags.SUTConnectorValue));

			//Read the settings to know if user wants to start the process listener
			if(settings.get(ConfigTags.ProcessListenerEnabled)) {
				enabledProcessListener = processListener.enableProcessListeners(settings);
			}

			SUT sut = NativeLinker.getNativeSUT(settings().get(ConfigTags.SUTConnectorValue), enabledProcessListener);

			//Print info to the user to know that TESTAR is NOT READY for its use :-(
			String printSutInfo = "Waiting for the SUT to be accessible ...";
			double startupTime = settings().get(ConfigTags.StartupTime)*1000;
			int timeFlash = (int)startupTime;

			//Refresh the flash information, to avoid that SUT hide the information
			int countTimeFlash = 0;
			while(countTimeFlash<timeFlash) {
				FlashFeedback.flash(printSutInfo, 2000);
				countTimeFlash += 2000;
			}

			final long now = System.currentTimeMillis(),
					ENGAGE_TIME = tryToKillIfRunning ? Math.round(maxEngageTime / 2.0) : maxEngageTime; // half time is expected for the implementation
					State state;
					do{
						if (sut.isRunning()){
							//Print info to the user to know that TESTAR is READY for its use :-)
							printSutInfo = "SUT is READY";
							FlashFeedback.flash(printSutInfo,2000);
							System.out.println("SUT is running after <" + (System.currentTimeMillis() - now) + "> ms ... waiting UI to be accessible");
							state = builder.apply(sut);
							if (state != null && state.childCount() > 0){
								long extraTime = tryToKillIfRunning ? 0 : ENGAGE_TIME;
								System.out.println("SUT accessible after <" + (extraTime + (System.currentTimeMillis() - now)) + "> ms");
								return sut;
							}
						}else {
							//Print info to the user to know that TESTAR is NOT READY for its use :-(
							printSutInfo = "Waiting for the SUT to be accessible ...";
							FlashFeedback.flash(printSutInfo, 500);
						}
						Util.pauseMs(500);				
					} while (mode() != Modes.Quit && System.currentTimeMillis() - now < ENGAGE_TIME);
					if (sut.isRunning())
						sut.stop();

					if(settings.get(ConfigTags.SUTConnectorValue).contains("java -jar"))
						throw new WinApiException("JAVA SUT PATH EXCEPTION");

					// issue starting the SUT
					if (tryToKillIfRunning){
						System.out.println("Unable to start the SUT after <" + ENGAGE_TIME + "> ms");
						return tryKillAndStartSystem(mustContain, sut, ENGAGE_TIME);
					} else
						throw new SystemStartException("SUT not running after <" + Math.round(ENGAGE_TIME * 2.0) + "> ms!");							
		}
	}

	private SUT tryKillAndStartSystem(String mustContain, SUT sut, long pendingEngageTime) throws SystemStartException{
		// kill running SUT processes
		System.out.println("Trying to kill potential running SUT: <" + sut.get(Tags.Desc) + ">");
		if (SystemProcessHandling.killRunningProcesses(sut, Math.round(pendingEngageTime / 2.0))){ // All killed?
			// retry start system
			System.out.println("Retry SUT start: <" + sut.get(Tags.Desc) + ">");
			return startSystem(mustContain, false, pendingEngageTime); // no more try to kill
		} else // unable to kill SUT
			throw new SystemStartException("Unable to kill SUT <" + sut.get(Tags.Desc) + "> while trying to rerun it after <" + pendingEngageTime + "> ms!");
	}

	private SUT getSUTByProcessName(String processName) throws SystemStartException{
		Assert.hasText(processName);
		List<SUT> suts = null;
		long now = System.currentTimeMillis();
		final double MAX_ENGAGE_TIME = Math.round(settings().get(ConfigTags.StartupTime) * 1000.0);
		do{
			Util.pauseMs(100);
			suts = NativeLinker.getNativeProcesses();
			if (suts != null){
				String desc;
				for (SUT theSUT : suts){
					desc = theSUT.get(Tags.Desc, null);
					if (desc != null && desc.contains(processName)){
						System.out.println("SUT with Process Name -" + processName + "- DETECTED!");
						return theSUT;
					}
				}
			}
		} while (System.currentTimeMillis() - now < MAX_ENGAGE_TIME);
		throw new SystemStartException("SUT Process Name not found!: -" + processName + "-");	
	}

	private SUT getSUTByWindowTitle(String windowTitle) throws SystemStartException{
		Assert.hasText(windowTitle);
		List<SUT> suts = null;
		State state; Role role; String title;
		long now = System.currentTimeMillis();
		final double MAX_ENGAGE_TIME = Math.round(settings().get(ConfigTags.StartupTime).doubleValue() * 1000.0);
		do{
			Util.pauseMs(100);
			suts = NativeLinker.getNativeProcesses();
			if (suts != null){
				for (SUT theSUT : suts){
					state = getStateByWindowTitle(theSUT);
					if (state.get(Tags.Foreground)){
						for (Widget w : state){
							role = w.get(Tags.Role, null);
							if (role != null && Role.isOneOf(role, NativeLinker.getNativeRole_Window())){
								title = w.get(Tags.Title, null);
								if (title != null && title.contains(windowTitle)){
									System.out.println("SUT with Window Title -" + windowTitle + "- DETECTED!");
									return theSUT;
								}
							}
						}
					}
				}
			}
		} while (System.currentTimeMillis() - now < MAX_ENGAGE_TIME);
		throw new SystemStartException("SUT Window Title not found!: -" + windowTitle + "-");			
	}

	protected State getStateByWindowTitle(SUT system) throws StateBuildException{
		Assert.notNull(system);
		State state = builder.apply(system);

		CodingManager.buildIDs(state);

		Shape viewPort = state.get(Tags.Shape, null);
		if(viewPort != null){
			//AWTCanvas scrShot = AWTCanvas.fromScreenshot(Rect.from(viewPort.x(), viewPort.y(), viewPort.width(), viewPort.height()), AWTCanvas.StorageFormat.PNG, 1);
			state.set(Tags.ScreenshotPath, protocolUtil.getStateshot(state));
		}

		calculateZIndices(state);

		return state;
	}

	/**
	 * This method gets the state of the SUT
	 * It also call getVerdict() and saves it into the state
	 *
	 * @param system
	 * @return
	 * @throws StateBuildException
	 */
	@Override
	protected State getState(SUT system) throws StateBuildException{
		Assert.notNull(system);
		State state = builder.apply(system);

		CodingManager.buildIDs(state);

		Shape viewPort = state.get(Tags.Shape, null);
		if(viewPort != null){
			//AWTCanvas scrShot = AWTCanvas.fromScreenshot(Rect.from(viewPort.x(), viewPort.y(), viewPort.width(), viewPort.height()), AWTCanvas.StorageFormat.PNG, 1);
			state.set(Tags.ScreenshotPath, protocolUtil.getStateshot(state));
		}

		calculateZIndices(state);
		Verdict verdict = getVerdict(state);
		state.set(Tags.OracleVerdict, verdict);
		if (mode() != Modes.Spy && verdict.severity() >= settings().get(ConfigTags.FaultThreshold)){
			faultySequence = true;
			LogSerialiser.log("Detected fault: " + verdict + "\n", LogSerialiser.LogLevel.Critical);
			// this was added to kill the SUT if it is frozen:
			if(verdict.severity()==Verdict.SEVERITY_NOT_RESPONDING){
				//if the SUT is frozen, we should kill it!
				LogSerialiser.log("SUT frozen, trying to kill it!\n", LogSerialiser.LogLevel.Critical);
				SystemProcessHandling.killRunningProcesses(system, 100);
			}
		} else if (verdict.severity() != Verdict.SEVERITY_OK && verdict.severity() > passSeverity){
			passSeverity = verdict.severity();
			LogSerialiser.log("Detected warning: " + verdict + "\n", LogSerialiser.LogLevel.Critical);
		}
		setStateForClickFilterLayerProtocol(state);
		return state;
	}

	@Override
	protected Verdict getVerdict(State state){
		Assert.notNull(state);
		//-------------------
		// ORACLES FOR FREE
		//-------------------		

		// if the SUT is not running, we assume it crashed
		if(!state.get(IsRunning, false))
			return new Verdict(Verdict.SEVERITY_NOT_RUNNING, "System is offline! I assume it crashed!");

		// if the SUT does not respond within a given amount of time, we assume it crashed
		if(state.get(Tags.NotResponding, false)){
			return new Verdict(Verdict.SEVERITY_NOT_RESPONDING, "System is unresponsive! I assume something is wrong!");
		}
		//------------------------
		// ORACLES ALMOST FOR FREE
		//------------------------

		if (this.suspiciousTitlesPattern == null)
			this.suspiciousTitlesPattern = Pattern.compile(settings().get(ConfigTags.SuspiciousTitles), Pattern.UNICODE_CHARACTER_CLASS);
		//System.out.println(this.suspiciousTitlesMatchers.size() + " suspiciousTitles matchers");
		Matcher m;
		// search all widgets for suspicious titles
		for(Widget w : state){
			String title = w.get(Title, "");
			if (title != null && !title.isEmpty()){
				m = this.suspiciousTitlesMatchers.get(title);
				if (m == null){
					m = this.suspiciousTitlesPattern.matcher(title);
					this.suspiciousTitlesMatchers.put(title, m);
				}
				if (m.matches()){
					Visualizer visualizer = Util.NullVisualizer;
					// visualize the problematic widget, by marking it with a red box
					if(w.get(Tags.Shape, null) != null)
						visualizer = new ShapeVisualizer(RedPen, w.get(Tags.Shape), "Suspicious Title", 0.5, 0.5);
					return new Verdict(Verdict.SEVERITY_SUSPICIOUS_TITLE, "Discovered suspicious widget title: '" + title + "'.", visualizer);
				}
			}
		}

		if (this.nonSuitableAction){
			this.nonSuitableAction = false;
			return new Verdict(Verdict.SEVERITY_WARNING, "Non suitable action for state");
		}

		// if everything was OK ...
		return Verdict.OK;
	}

	/**
	 * This methods prepares for deriving actions, but does not really derive them yet. This is left for lower
	 * level protocols. Here the parameters are set in case unwanted processes need to be killed or the SUT needs to be brought back
	 * to foreground. The latter is then done by selectActions in the AbstractProtocol.
	 * @param system
	 * @param state
	 * @return
	 * @throws ActionBuildException
	 */
	protected Set<Action> deriveActions(SUT system, State state) throws ActionBuildException{
		Assert.notNull(state);
		Set<Action> actions = new HashSet<Action>();	

		// create an action compiler, which helps us create actions, such as clicks, drag + drop, typing...
		StdActionCompiler ac = new AnnotatingActionCompiler();

		// If there is an unwanted process running, we need to kill it.
		// This is an unwanted process that is defined in the filter.
		// So we set this.forceKillProcess = process.right();
		// And then select action will take care of making the next action to select the killing of the process.
		String processRE = settings().get(ConfigTags.ProcessesToKillDuringTest);
		if (processRE != null && !processRE.isEmpty()){
			state.set(Tags.RunningProcesses, system.getRunningProcesses());
			for(Pair<Long, String> process : state.get(Tags.RunningProcesses, Collections.<Pair<Long,String>>emptyList())){
				if(process.left().longValue() != system.get(Tags.PID).longValue() &&
						process.right() != null && process.right().matches(processRE)){ // pid x name
					//actions.add(ac.killProcessByName(process.right(), 2));
					this.forceKillProcess = process.right();
					System.out.println("will kill unwanted process: " + process.left().longValue() + " (SYSTEM <" + system.get(Tags.PID).longValue() + ">)");
					return actions;
				}
			}
		}

		// If the system is in the background, we need to force it into the foreground!
		// We set this.forceToForeground to true and selectAction will make sure that the next action we will select
		// is putting the SUT back into the foreground.
		if(!state.get(Tags.Foreground, true) && system.get(Tags.SystemActivator, null) != null){
			//actions.add(ac.activateSystem());
			this.forceToForeground = true;
			return actions;
		}

		//Note this list is always empty in this deriveActions.
		return actions;
	}

	//TODO is this process handling Windows specific? move to SystemProcessHandling
	/**
	 * If unwanted processes need to be killed, the action returns an action to do that. If the SUT needs
	 * to be put in the foreground, then the action that is returned is putting it in the foreground.
	 * Otherwise it returns null.
	 * @param state
	 * @param actions
	 * @return null if no preSelected actions are needed.
	 */
	protected Action preSelectAction(State state, Set<Action> actions){
		//Assert.isTrue(actions != null && !actions.isEmpty());

		//If deriveActions indicated that there are processes that need to be killed
		//because they are in the process filters
		//Then here we will select the action to do that killing

		if (this.forceKillProcess != null){
			System.out.println("DEBUG: preActionSelection, forceKillProcess="+forceKillProcess);
			LogSerialiser.log("Forcing kill-process <" + this.forceKillProcess + "> action\n", LogSerialiser.LogLevel.Info);
			Action a = KillProcess.byName(this.forceKillProcess, 0);
			a.set(Tags.Desc, "Kill Process with name '" + this.forceKillProcess + "'");
			CodingManager.buildIDs(state, a);
			this.forceKillProcess = null;
			return a;
		}
		//If deriveActions indicated that the SUT should be put back in the foreground
		//Then here we will select the action to do that

		else if (this.forceToForeground){
			LogSerialiser.log("Forcing SUT activation (bring to foreground) action\n", LogSerialiser.LogLevel.Info);
			Action a = new ActivateSystem();
			a.set(Tags.Desc, "Bring the system to the foreground.");
			CodingManager.buildIDs(state, a);
			this.forceToForeground = false;
			return a;
		}

		//TODO: This seems not to be used yet...
		// It is set in a method actionExecuted that is not being called anywhere (yet?)
		else if (this.forceNextActionESC){
			System.out.println("DEBUG: Forcing ESC action in preActionSelection");
			LogSerialiser.log("Forcing ESC action\n", LogSerialiser.LogLevel.Info);
			Action a = new AnnotatingActionCompiler().hitKey(KBKeys.VK_ESCAPE);
			CodingManager.buildIDs(state, a);
			this.forceNextActionESC = false;
			return a;
		} else
			return null;
	}

	final static double MAX_ACTION_WAIT_FRAME = 1.0; // (seconds)
	//TODO move the CPU metric to another helper class that is not default "TrashBinCode" or "SUTprofiler"
	//TODO check how well the CPU usage based waiting works
	protected boolean executeAction(SUT system, State state, Action action){
		double waitTime = settings.get(ConfigTags.TimeToWaitAfterAction);
		try{
			double halfWait = waitTime == 0 ? 0.01 : waitTime / 2.0; // seconds
			Util.pause(halfWait); // help for a better match of the state' actions visualization
			action.run(system, state, settings.get(ConfigTags.ActionDuration));
			int waitCycles = (int) (MAX_ACTION_WAIT_FRAME / halfWait);
			long actionCPU;
			do {
				long CPU1[] = NativeLinker.getCPUsage(system);
				Util.pause(halfWait);
				long CPU2[] = NativeLinker.getCPUsage(system);
				actionCPU = ( CPU2[0] + CPU2[1] - CPU1[0] - CPU1[1] );
				waitCycles--;
			} while (actionCPU > 0 && waitCycles > 0);

			protocolUtil.getActionshot(state,action);

			//Save the executed action information into the logs
			saveActionInfoInLogs(state, action, "ExecutedAction");

			return true;
		}catch(ActionFailedException afe){
			return false;
		}
	}

	/**
	 * This method is here, so that ClickFilterLayerProtocol can override it, and the behaviour is updated
	 *
	 * @param canvas
	 * @param state
	 * @param actions
	 */
	protected void visualizeActions(Canvas canvas, State state, Set<Action> actions){
		SutVisualization.visualizeActions(canvas, state, actions);
	}

	/**
	 * Returns the next action that will be selected. If unwanted processes need to be killed, the action kills them. If the SUT needs
	 * to be put in the foreground, then the action is putting it in the foreground. Otherwise the action is selected according to
	 * action selection mechanism selected.
	 * @param state
	 * @param actions
	 * @return
	 */
	protected Action selectAction(State state, Set<Action> actions){
		Assert.isTrue(actions != null && !actions.isEmpty());

		Action a = preSelectAction(state, actions);
		if (a != null){
			return a;
		} else
			return RandomActionSelector.selectAction(actions);
	}

	protected String getRandomText(Widget w){
		return DataManager.getRandomData();
	}

	/**
	 * Calculate the max and the min ZIndex of all the widgets in a state
	 * @param state
	 */
	protected void calculateZIndices(State state) {
		double minZIndex = Double.MAX_VALUE,
				maxZIndex = Double.MIN_VALUE,
				zindex;
		for (Widget w : state){
			zindex = w.get(Tags.ZIndex).doubleValue();
			if (zindex < minZIndex)
				minZIndex = zindex;
			if (zindex > maxZIndex)
				maxZIndex = zindex;
		}
		state.set(Tags.MinZIndex, minZIndex);
		state.set(Tags.MaxZIndex, maxZIndex);
	}

	/**
	 * Return a list of widgets that have the maximal Zindex
	 * @param state
	 * @return
	 */
	protected List<Widget> getTopWidgets(State state){
		List<Widget> topWidgets = new ArrayList<>();
		double maxZIndex = state.get(Tags.MaxZIndex);
		for (Widget w : state)
			if (w.get(Tags.ZIndex) == maxZIndex)
				topWidgets.add(w);
		return topWidgets;
	}

	/**
	 * Check whether widget w should be filtered based on
	 * its title (matching the regular expression of the Dialog --> clickFilterPattern)
	 * that is cannot be hit
	 * @param w
	 * @return
	 */
	protected boolean isUnfiltered(Widget w){
		//Check whether the widget can be hit
		// If not, it should be filtered
		if(!Util.hitTest(w, 0.5, 0.5))
			return false;

		//Check whether the widget has an empty title or no title
		//If it has, it is unfiltered
		//Because it cannot match the regular expression of the Action Filter.
		String title = w.get(Title, "");
		if (title == null || title.isEmpty())
			return true;

		//If no clickFilterPattern exists, then create it
		//Get the clickFilterPattern from the regular expression provided by the tester in the Dialog
		if (this.clickFilterPattern == null)
			this.clickFilterPattern = Pattern.compile(settings().get(ConfigTags.ClickFilter), Pattern.UNICODE_CHARACTER_CLASS);

		//Check whether the title matches any of the clickFilterPatterns
		Matcher m = this.clickFilterMatchers.get(title);
		if (m == null){
			m = this.clickFilterPattern.matcher(title);
			this.clickFilterMatchers.put(title, m);
		}
		return !m.matches();
	}

	/**
	 * Check whether a widget is clickable
	 * @param w
	 * @return
	 */
	protected boolean isClickable(Widget w){
		Role role = w.get(Tags.Role, Roles.Widget);
		if(Role.isOneOf(role, NativeLinker.getNativeClickableRoles()))
			return true;
		return false;
	}

	/**
	 * Check whether a widget is typeable
	 * @param w
	 * @return
	 */
	protected boolean isTypeable(Widget w){
		return NativeLinker.isNativeTypeable(w);
	}

	/**
	 * STOP criteria for selecting more actions for a sequence
	 * @param state
	 * @return
	 */
	protected boolean moreActions(State state) {
		return (!settings().get(ConfigTags.StopGenerationOnFault) || !faultySequence) && 
				state.get(Tags.IsRunning, false) && !state.get(Tags.NotResponding, false) &&
				//actionCount() < settings().get(ConfigTags.SequenceLength) &&
				actionCount() <= lastSequenceActionNumber &&
				timeElapsed() < settings().get(ConfigTags.MaxTime);
	}

	/**
	 * STOP criteria deciding whether more sequences are required in a test run
	 * @return
	 */
	protected boolean moreSequences() {
		//        System.out.println("DEBUG: moreSequences(), sequenceCount="+sequenceCount()+", config sequences="+settings().get(ConfigTags.Sequences)
		//                +", timeElapsed="+timeElapsed()+", maxTime="+settings().get(ConfigTags.MaxTime));
		return sequenceCount() <= settings().get(ConfigTags.Sequences) &&
				timeElapsed() < settings().get(ConfigTags.MaxTime);
	}

	@Override
	public void mouseMoved(double x, double y) {} //for iEventListener

	@Override
	protected void stopSystem(SUT system) {

		if (system != null){
			AutomationCache ac = system.getNativeAutomationCache();
			if (ac != null)
				ac.releaseCachedAutomationElements();
		}
		if(system !=null){
			system.stop();
		}
	}

	@Override
	protected void postSequenceProcessing() {

	}

	/**
	 * method for closing the internal TESTAR test session
	 */
	private void closeTestarTestSession(){
		//cleaning the variables started in initialize()
		try {
			if (!settings.get(ConfigTags.UnattendedTests)) {
				if (GlobalScreen.isNativeHookRegistered()) {
					LogSerialiser.log("Unregistering keyboard and mouse hooks\n", LogSerialiser.LogLevel.Debug);
					GlobalScreen.removeNativeMouseMotionListener(eventHandler);
					GlobalScreen.removeNativeMouseListener(eventHandler);
					GlobalScreen.removeNativeKeyListener(eventHandler);
					GlobalScreen.unregisterNativeHook();
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void closeTestSession() {
	}

	//TODO move to ManualRecording helper class??
	//	/**
	//	 * Records user action (for example for Generate-Manual)
	//	 *
	//	 * @param state
	//	 * @return
	//	 */
	protected Action mapUserEvent(State state){
		Assert.notNull(userEvent);
		if (userEvent[0] instanceof MouseButtons){ // mouse events
			double x = ((Double)userEvent[1]).doubleValue();
			double y = ((Double)userEvent[2]).doubleValue();
			Widget w = null;
			try {
				w = Util.widgetFromPoint(state, x, y);
				x = 0.5; y = 0.5;
				if (userEvent[0] == MouseButtons.BUTTON1) // left click
					return (new AnnotatingActionCompiler()).leftClickAt(w,x,y);
				else if (userEvent[0] == MouseButtons.BUTTON3) // right click
					return (new AnnotatingActionCompiler()).rightClickAt(w,x,y);
			} catch (WidgetNotFoundException we){
				System.out.println("Mapping user event ... widget not found @(" + x + "," + y + ")");
				return null;
			}
		} else if (userEvent[0] instanceof KBKeys) // key events
			return (new AnnotatingActionCompiler()).hitKey((KBKeys)userEvent[0]);
		else if (userEvent[0] instanceof String){ // type events
			if (lastExecutedAction == null)
				return null;
			List<Finder> targets = lastExecutedAction.get(Tags.Targets,null);
			if (targets == null || targets.size() != 1)
				return null;
			try {
				Widget w = targets.get(0).apply(state);
				return (new AnnotatingActionCompiler()).clickTypeInto(w,(String)userEvent[0], true);
			} catch (WidgetNotFoundException we){
				return null;
			}
		}
		return null;
	}

	//TODO move to ManualRecording helper class??
	//	/**
	//	 * Waits for an user UI action.
	//	 * Requirement: Mode must be GenerateManual.
	//	 */
	protected void waitUserActionLoop(Canvas cv, SUT system, State state, ActionStatus actionStatus){
		while (mode() == Modes.Record && !actionStatus.isUserEventAction()){
			if (userEvent != null){
				actionStatus.setAction(mapUserEvent(state));
				actionStatus.setUserEventAction((actionStatus.getAction() != null));
				userEvent = null;
			}
			synchronized(this){
				try {
					this.wait(100);
				} catch (InterruptedException e) {}
			}
			state = getState(system);
			cv.begin(); Util.clear(cv);

			//In Record-mode, we activate the visualization with Shift+ArrowUP:
			if(visualizationOn) SutVisualization.visualizeState(false, markParentWidget, mouse, protocolUtil, lastPrintParentsOf, cv,state);

			Set<Action> actions = deriveActions(system,state);
			CodingManager.buildIDs(state, actions);

			//In Record-mode, we activate the visualization with Shift+ArrowUP:
			if(visualizationOn) visualizeActions(cv, state, actions);

			cv.end();
		}
	}

	protected int escAttempts = 0;
	protected static final int MAX_ESC_ATTEMPTS = 99;

	protected boolean isNOP(Action action){
		String as = action.toString();
		if (as != null && as.equals(NOP.NOP_ID))
			return true;
		else
			return false;
	}

	protected boolean isESC(Action action){
		Role r = action.get(Tags.Role, null);
		if (r != null && r.isA(ActionRoles.HitKey)){
			String desc = action.get(Tags.Desc, null);
			if (desc != null && desc.contains("VK_ESCAPE"))
				return true;
		}
		return false;
	}

	/**
	 * Adds sliding actions (like scroll, drag and drop) to the given Set of Actions
	 * @param actions
	 * @param ac
	 * @param scrollArrowSize
	 * @param scrollThick
	 * @param w
	 */
	protected void addSlidingActions(Set<Action> actions, StdActionCompiler ac, double scrollArrowSize, double scrollThick, Widget w, State state){
		Drag[] drags = null;
		//If there are scroll (drags/drops) actions possible
		if((drags = w.scrollDrags(scrollArrowSize,scrollThick)) != null){
			//For each possible drag, create an action and add it to the derived actions
			for (Drag drag : drags){
				//Create a slide action with the Action Compiler, and add it to the set of derived actions
				actions.add(ac.slideFromTo(
						new AbsolutePosition(Point.from(drag.getFromX(),drag.getFromY())),
						new AbsolutePosition(Point.from(drag.getToX(),drag.getToY()))
						));

			}
		}
	}

}
