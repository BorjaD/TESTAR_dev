package org.testar.protocols;

import es.upv.staq.testar.protocols.ClickFilterLayerProtocol;
import nl.ou.testar.HtmlReporting.HtmlSequenceReport;
import nl.ou.testar.RandomActionSelector;
import org.fruit.Drag;
import org.fruit.alayer.*;
import org.fruit.alayer.actions.AnnotatingActionCompiler;
import org.fruit.alayer.actions.StdActionCompiler;
import org.fruit.alayer.exceptions.ActionBuildException;
import org.fruit.alayer.exceptions.StateBuildException;

import java.util.Set;

import static org.fruit.alayer.Tags.Blocked;
import static org.fruit.alayer.Tags.Enabled;

public class DesktopProtocol extends ClickFilterLayerProtocol {
    //Attributes for adding slide actions
    protected static double SCROLL_ARROW_SIZE = 36; // sliding arrows
    protected static double SCROLL_THICK = 16; //scroll thickness
    protected HtmlSequenceReport htmlReport;
    protected int scenarioCount = 1;
    protected State latestState;


    /**
     * This methods is called before each test sequence, allowing for example using external profiling software on the SUT
     */
    @Override
    protected void preSequencePreparations() {
        //initializing the HTML sequence report:
        htmlReport = new HtmlSequenceReport(scenarioCount, sequenceCount);
        // updating scenarioCount based on existing HTML files - sequence 1 gets the correct scenarioCount:
        scenarioCount = htmlReport.getScenarioCount();
    }

    /**
     * This method is called when the TESTAR requests the state of the SUT.
     * Here you can add additional information to the SUT's state or write your
     * own state fetching routine. The state should have attached an oracle
     * (TagName: <code>Tags.OracleVerdict</code>) which describes whether the
     * state is erroneous and if so why.
     * @return  the current state of the SUT with attached oracle.
     */
    @Override
    protected State getState(SUT system) throws StateBuildException {
        latestState = super.getState(system);
        //adding state to the HTML sequence report:
        htmlReport.addState(latestState);
        return latestState;
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
    protected Set<Action> deriveActions(SUT system, State state) throws ActionBuildException {

        //The super method returns a ONLY actions for killing unwanted processes if needed, or bringing the SUT to
        //the foreground. You should add all other actions here yourself.
        // These "special" actions are prioritized over the normal GUI actions in selectAction() / preSelectAction().
        Set<Action> actions = super.deriveActions(system,state);


        // Derive left-click actions, click and type actions, and scroll actions from
        // top level (highest Z-index) widgets of the GUI:
        actions = deriveClickTypeScrollActionsFromTopLevelWidgets(actions, system, state);

        if(actions.size()==0){
            // If the top level widgets did not have any executable widgets, try all widgets:
//            System.out.println("No actions from top level widgets, changing to all widgets.");
            // Derive left-click actions, click and type actions, and scroll actions from
            // all widgets of the GUI:
            actions = deriveClickTypeScrollActionsFromAllWidgetsOfState(actions, system, state);
        }

        //return the set of derived actions
        return actions;
    }

    /**
     * Overwriting to add HTML report writing into it
     *
     * @param state
     * @param actions
     * @return
     */
    @Override
    protected Action preSelectAction(State state, Set<Action> actions){
        // adding available actions into the HTML report:
        htmlReport.addActions(actions);
        return(super.preSelectAction(state, actions));
    }

    /**
     * Select one of the available actions (e.g. at random)
     * @param state the SUT's current state
     * @param actions the set of derived actions
     * @return  the selected action (non-null!)
     */
    @Override
    protected Action selectAction(State state, Set<Action> actions){
        //Call the preSelectAction method from the DefaultProtocol so that, if necessary,
        //unwanted processes are killed and SUT is put into foreground.
        Action retAction = preSelectAction(state, actions);
        if (retAction == null)
            //if no preSelected actions are needed, then implement your own strategy
            retAction = RandomActionSelector.selectAction(actions);
        return retAction;
    }

    /**
     * Execute the selected action.
     * @param system the SUT
     * @param state the SUT's current state
     * @param action the action to execute
     * @return whether or not the execution succeeded
     */
    @Override
    protected boolean executeAction(SUT system, State state, Action action){
        // adding the action that is going to be executed into HTML report:
        htmlReport.addSelectedAction(state.get(Tags.ScreenshotPath), action);
        return super.executeAction(system, state, action);
    }

    /**
     * This methods is called after each test sequence, allowing for example using external profiling software on the SUT
     */
    @Override
    protected void postSequenceProcessing() {
        htmlReport.addTestVerdict(getVerdict(latestState));
    }

    /**
     * Iterating through all widgets of the given state
     *
     * Adding derived actions into the given set of actions and returning the modified set of actions.
     *
     * @param actions
     * @param system
     * @param state
     * @return
     */
    protected Set<Action> deriveClickTypeScrollActionsFromAllWidgetsOfState(Set<Action> actions, SUT system, State state){
        // To derive actions (such as clicks, drag&drop, typing ...) we should first create an action compiler.
        StdActionCompiler ac = new AnnotatingActionCompiler();

        // To find all possible actions that TESTAR can click on we should iterate through all widgets of the state.
        for(Widget w : state){
            //optional: iterate through top level widgets based on Z-index:
            //for(Widget w : getTopWidgets(state)){

            if(w.get(Tags.Role, Roles.Widget).toString().equalsIgnoreCase("UIAMenu")){
                // filtering out actions on menu-containers (that would add an action in the middle of the menu)
                continue; // skip this iteration of the for-loop
            }

            // Only consider enabled and non-blocked widgets
            if(w.get(Enabled, true) && !w.get(Blocked, false)){

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


    /**
     * Adding derived actions into the given set of actions and returning the modified set of actions.
     *
     * @param actions
     * @param system
     * @param state
     * @return
     */
    protected Set<Action> deriveClickTypeScrollActionsFromTopLevelWidgets(Set<Action> actions, SUT system, State state){
        // To derive actions (such as clicks, drag&drop, typing ...) we should first create an action compiler.
        StdActionCompiler ac = new AnnotatingActionCompiler();

        // To find all possible actions that TESTAR can click on we should iterate through all widgets of the state.
        for(Widget w : getTopWidgets(state)){
            //optional: iterate through top level widgets based on Z-index:
            //for(Widget w : getTopWidgets(state)){

            if(w.get(Tags.Role, Roles.Widget).toString().equalsIgnoreCase("UIAMenu")){
                // filtering out actions on menu-containers (that would add an action in the middle of the menu)
                continue; // skip this iteration of the for-loop
            }

            // Only consider enabled and non-blocked widgets
            if(w.get(Enabled, true) && !w.get(Blocked, false)){

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