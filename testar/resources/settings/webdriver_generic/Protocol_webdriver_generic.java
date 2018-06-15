/*
 * *
 * COPYRIGHT (2017):                                                                     *
 * Universitat Politecnica de Valencia                                                   *
 * Camino de Vera, s/n                                                                   *
 * 46022 Valencia, Spain                                                                 *
 * www.upv.es                                                                            *
 * *
 * D I S C L A I M E R:                                                                  *
 * This software has been developed by the Universitat Politecnica de Valencia (UPV)     *
 * in the context of the TESTAR Proof of Concept project:                                *
 * "UPV, Programa de Prueba de Concepto 2014, SP20141402"                  *
 * This software is distributed FREE of charge under the TESTAR license, as an open      *
 * source project under the BSD3 licence (http://opensource.org/licenses/BSD-3-Clause)   *                                                                                        *
 * *
 *
 */

/*
 *  @author (base) Sebastian Bauersfeld
 *  @author Govert Buijs
 */

import es.upv.staq.testar.NativeLinker;
import es.upv.staq.testar.protocols.ClickFilterLayerProtocol;
import org.fruit.Pair;
import org.fruit.alayer.*;
import org.fruit.alayer.actions.AnnotatingActionCompiler;
import org.fruit.alayer.actions.StdActionCompiler;
import org.fruit.alayer.exceptions.ActionBuildException;
import org.fruit.alayer.exceptions.StateBuildException;
import org.fruit.alayer.exceptions.SystemStartException;
import org.fruit.alayer.webdriver.CanvasDimensions;
import org.fruit.alayer.webdriver.WdDriver;
import org.fruit.alayer.webdriver.WdElement;
import org.fruit.alayer.webdriver.WdWidget;
import org.fruit.alayer.webdriver.enums.WdRoles;
import org.fruit.monkey.ConfigTags;
import org.fruit.monkey.Settings;

import java.io.File;
import java.util.*;

import static org.fruit.alayer.Tags.Blocked;
import static org.fruit.alayer.Tags.Enabled;
import static org.fruit.alayer.webdriver.Constants.scrollArrowSize;
import static org.fruit.alayer.webdriver.Constants.scrollThick;


public class Protocol_webdriver_generic extends ClickFilterLayerProtocol {
  // Classes that are deemed clickable by the web framework
  private static List<String> clickableClasses = Arrays.asList(
      "list__item__title");

  // Go back once we encounter certain files
  private static List<String> deniedExtensions = Arrays.asList(
      "pdf");

  // If set to NULL, only the sut connector domain will be used
  private static List<String> domainsAllowed = Arrays.asList(
      "www.ou.nl");

  /**
   * Called once during the life time of TESTAR
   * This method can be used to perform initial setup work
   *
   * @param settings the current TESTAR settings as specified by the user.
   */
  protected void initialize(Settings settings) {
    super.initialize(settings);
    ensureDomainsAllowed();
  }

  /**
   * This method is invoked each time TESTAR starts to generate a new sequence
   */
  protected void beginSequence(SUT system, State state) {
    super.beginSequence(system, state);
  }

  /**
   * This method is called when TESTAR starts the System Under Test (SUT). The method should
   * take care of
   *   1) starting the SUT (you can use TESTAR's settings obtainable from <code>settings()</code> to find
   *      out what executable to run)
   *   2) bringing the system into a specific start state which is identical on each start (e.g. one has to delete or restore
   *      the SUT's configuratio files etc.)
   *   3) waiting until the system is fully loaded and ready to be tested (with large systems, you might have to wait several
   *      seconds until they have finished loading)
   *
   * @return a started SUT, ready to be tested.
   */
  protected SUT startSystem() throws SystemStartException {
    SUT sut = super.startSystem();

    // See remarks in WdMouse
    mouse = sut.get(Tags.StandardMouse);

    return sut;
  }

  /**
   * This method is called when TESTAR requests the state of the SUT.
   * Here you can add additional information to the SUT's state or write your
   * own state fetching routine. The state should have attached an oracle
   * (TagName: <code>Tags.OracleVerdict</code>) which describes whether the
   * state is erroneous and if so why.
   *
   * @return the current state of the SUT with attached oracle.
   */
  protected State getState(SUT system) throws StateBuildException {
    State state = super.getState(system);

    return state;
  }

  /**
   * This is a helper method used by the default implementation of <code>buildState()</code>
   * It examines the SUT's current state and returns an oracle verdict.
   *
   * @return oracle verdict, which determines whether the state is erroneous and why.
   */
  protected Verdict getVerdict(State state) {

    Verdict verdict = super.getVerdict(state); // by urueda
    // system crashes, non-responsiveness and suspicious titles automatically detected!

    //-----------------------------------------------------------------------------
    // MORE SOPHISTICATED ORACLES CAN BE PROGRAMMED HERE (the sky is the limit ;-)
    //-----------------------------------------------------------------------------

    // ... YOU MAY WANT TO CHECK YOUR CUSTOM ORACLES HERE ...

    return verdict;
  }

  /**
   * This method is used by TESTAR to determine the set of currently available actions.
   * You can use the SUT's current state, analyze the widgets and their properties to create
   * a set of sensible actions, such as: "Click every Button which is enabled" etc.
   * The return value is supposed to be non-null. If the returned set is empty, TESTAR
   * will stop generation of the current action and continue with the next one.
   *
   * @param system the SUT
   * @param state  the SUT's current state
   * @return a set of actions
   */
  protected Set<Action> deriveActions(SUT system, State state) throws ActionBuildException {
    Set<Action> actions = super.deriveActions(system, state); // by urueda
    // unwanted processes, force SUT to foreground, ... actions automatically derived!

    //----------------------
    // BUILD CUSTOM ACTIONS
    //----------------------

    // Don't add actions if prolog is activated
    if (settings().get(ConfigTags.PrologActivated)) {
      return actions;
    }

    // Check if forced actions are needed to stay within allowed domains
    Set<Action> forcedActions = detectForcedActions();
    if (forcedActions != null && forcedActions.size() > 0) {
      return forcedActions;
    }
    // create an action compiler, which helps us create actions
    // such as clicks, drag&drop, typing ...
    StdActionCompiler ac = new AnnotatingActionCompiler();

    // iterate through all widgets
    for (Widget widget : state) {
      // only consider enabled, non-blocked widgets and non-tabu widgets
      if (!widget.get(Enabled, true) || widget.get(Blocked, false) || blackListed(widget)) {
        continue;
      }

      // left clicks, but ignore links outside domain
      if (isAtBrowserCanvas(widget) && (whiteListed(widget) || isClickable(widget))) {
        if (!isLinkDenied(widget)) {
          actions.add(ac.leftClickAt(widget));
        }
      }

      // type into text boxes
      if (isAtBrowserCanvas(widget) && (whiteListed(widget) || isTypeable(widget))) {
        actions.add(ac.clickTypeInto(widget, this.getRandomText(widget)));
      }

      // slides
      addSlidingActions(actions, ac, scrollArrowSize, scrollThick, widget);
    }

    return actions;
  }

  /*
   * Check the state if we need to force an action
   */
  private Set<Action> detectForcedActions() {
    String currentURL = WdDriver.getCurrentUrl();

    Set<Action> actions = new HashSet<>();

    // Don't get caught in a PDFs etc. and non-whitelisted domains
    if (isUrlDenied(currentURL) || isExtensionDenied(currentURL)) {
      // If opened in new tab, close it, else go back
      if (WdDriver.getWindowHandles().size() > 1) {
        actions.add(NativeLinker.getWdCloseTabAction());
      }
      else {
        actions.add(NativeLinker.getWdBackAction());
      }
    }

    return actions;
  }

  /*
   * Check if the current address has a denied extension (PDF etc.)
   */
  private boolean isExtensionDenied(String currentURL) {
    // If the current page doesn't have an extension, always allow
    if (!currentURL.contains(".")) {
      return false;
    }

    // Deny if the extension is in the list
    String ext = currentURL.substring(currentURL.lastIndexOf(".") + 1);
    ext = ext.replace("/", "");
    return deniedExtensions.contains(ext);
  }

  /*
   * Check if the URL is denied
   */
  private boolean isUrlDenied(String currentUrl) {
    if (currentUrl.startsWith("mailto:")) {
      return true;
    }

    // Always allow local file
    if (currentUrl.startsWith("file:///")) {
      return false;
    }

    // Only allow pre-approved domains
    String domain = getDomain(currentUrl);
    return !domainsAllowed.contains(domain);
  }

  /*
   * Check if the widget has a denied URL as hyperlink
   */
  private boolean isLinkDenied(Widget widget) {
    String linkUrl = widget.get(Tags.ValuePattern, "");

    // Not a link or local file
    if (linkUrl == null || linkUrl.startsWith("file:///")) {
      return false;
    }

    // Mail link, deny
    if (linkUrl.startsWith("mailto:")) {
      return true;
    }

    // Not a web link, allow
    if (!(linkUrl.startsWith("https://") || linkUrl.startsWith("https://"))) {
      return false;
    }

    // Only allow pre-approved domains
    String domain = getDomain(linkUrl);
    return !domainsAllowed.contains(domain);
  }

  /*
   * Get the domain from a full URL
   */
  private String getDomain(String url) {
    if (url == null) {
      return null;
    }

    // When serving from file, 'domain' is filesystem
    if (url.startsWith("file://")) {
      return "file://";
    }

    url = url.replace("https://", "").replace("http://", "").replace("file://", "");
    return (url.split("/")[0]).split("\\?")[0];
  }

  /*
   * If domainsAllowed not set, allow the domain from the SUT Connector
   */
  private void ensureDomainsAllowed() {
    // Already defined
    if (domainsAllowed != null && domainsAllowed.size() > 0) {
      return;
    }

    String[] parts = settings().get(ConfigTags.SUTConnectorValue).split(" ");
    String url = parts[parts.length - 1].replace("\"", "");
    domainsAllowed = Arrays.asList(getDomain(url));
  }

  //////////////////////////////////////////////////////////////////////////////
  // TODO
  private static int counter = 0;

  private void showWidgetOrg(Widget w) {
    String role = w.get(Tags.Role, Roles.Widget).name();
    String title = w.get(Tags.Title, "_") + " :: " + w.get(Tags.Text, "_");
    title = title.replace("\n", " ");
    title = title.substring(0, Math.min(100, title.length()));
    boolean yes = w.get(Enabled, true) && !w.get(Blocked, false) && !blackListed(w);
    yes &= (whiteListed(w) || (isClickable(w) || isTypeable(w)));

    Shape shape = w.get(Tags.Shape);
    int x = (int) shape.x();
    int y = (int) shape.y();
    int width = (int) shape.width();
    int height = (int) shape.height();
    WdElement element = ((WdWidget) w).element;

    System.out.format("%-5s %-5s | %-5s | %-5s %-5s | %-5s %-5s | %-50s | %-30s %s\n",
        isClickable(w), isTypeable(w), yes, x, y, width, height, element.display, role, title);
  }

  private void showWidget(Widget w) {
    String role = w.get(Tags.Role, Roles.Widget).name();
    if (!role.equals("WdA")) {
      return;
    }

    String title = w.get(Tags.Title, "_") + " :: " + w.get(Tags.Text, "_");
    title = title.replace("\n", " ");
    title = title.substring(0, Math.min(100, title.length()));
    boolean yes = w.get(Enabled, true) && !w.get(Blocked, false) && !blackListed(w);
    yes &= (whiteListed(w) || (isClickable(w) || isTypeable(w)));

    Shape shape = w.get(Tags.Shape);
    int x = (int) shape.x();
    int y = (int) shape.y();
    int width = (int) shape.width();
    int height = (int) shape.height();
    int clickX = x + width / 2;
    int clickY = y + height / 2;
    WdElement element = ((WdWidget) w).element;

    System.out.format("%-5s %-5s | %-5d %-5d | %d | %s\n",
        w.get(Blocked, false), isAtBrowserCanvas(w),
        clickX, clickY, CanvasDimensions.getCanvasHeight(), title);
  }

  /*
   * We need to check if the screen position is within the canvas
   */
  private boolean isAtBrowserCanvas(Pair<Double, Double> pos) {
    return pos.left() >= 0 && pos.left() <= CanvasDimensions.getCanvasWidth() &&
           pos.right() >= 0 && pos.right() <= CanvasDimensions.getInnerWidth();
  }
  //////////////////////////////////////////////////////////////////////////////

  /*
   * We need to check if click position is within the canvas
   */
  private boolean isAtBrowserCanvas(Widget widget) {
    Shape shape = widget.get(Tags.Shape, null);
    if (shape == null) {
      return false;
    }

    double x = shape.x() + shape.width() / 2;
    double y = shape.y() + shape.height() / 2;
    return x > 0 && x < CanvasDimensions.getCanvasWidth() &&
        y > 0 && y < CanvasDimensions.getInnerWidth();
  }

  protected boolean isClickable(Widget w) {
    Role role = w.get(Tags.Role, Roles.Widget);
    if (Role.isOneOf(role, NativeLinker.getNativeClickableRoles())) {
      // Input type are special...
      if (!role.equals(WdRoles.WdINPUT)) {
        String type = ((WdWidget) w).element.type;
        if (WdRoles.clickableInputTypes().contains(type)) {
          return true;
        }
      }
      return true;
    }

    WdElement element = ((WdWidget) w).element;
    if (element.isClickable) {
      return true;
    }

    Set<String> clickSet = new HashSet<>(clickableClasses);
    clickSet.retainAll(element.cssClasses);
    return clickSet.size() > 0;
  }

  protected boolean isTypeable(Widget w) {
    Role role = w.get(Tags.Role, Roles.Widget);
    if (Role.isOneOf(role, NativeLinker.getNativeTypeableRoles())) {
      // Input type are special...
      if (role.equals(WdRoles.WdINPUT)) {
        String type = ((WdWidget) w).element.type;
        return WdRoles.typeableInputTypes().contains(type);
      }
      return true;
    }

    return false;
  }

  /**
   * Select one of the possible actions (e.g. at random)
   *
   * @param state   the SUT's current state
   * @param actions the set of available actions as computed by <code>buildActionsSet()</code>
   * @return the selected action (non-null!)
   */
  protected Action selectAction(State state, Set<Action> actions) {

    return super.selectAction(state, actions);
  }

  /**
   * Execute the selected action.
   *
   * @param system the SUT
   * @param state  the SUT's current state
   * @param action the action to execute
   * @return whether or not the execution succeeded
   */
  protected boolean executeAction(SUT system, State state, Action action) {
    return super.executeAction(system, state, action);
  }

  /**
   * TESTAR uses this method to determine when to stop the generation of actions for the
   * current sequence. You could stop the sequence's generation after a given amount of executed
   * actions or after a specific time etc.
   *
   * @return if <code>true</code> continue generation, else stop
   */
  protected boolean moreActions(State state) {
    return super.moreActions(state);
  }


  /**
   * This method is invoked each time after TESTAR finished the generation of a sequence.
   */
  protected void finishSequence(File recordedSequence) {
    super.finishSequence(recordedSequence);
  }


  /**
   * TESTAR uses this method to determine when to stop the entire test.
   * You could stop the test after a given amount of generated sequences or
   * after a specific time etc.
   *
   * @return if <code>true</code> continue test, else stop
   */
  protected boolean moreSequences() {
    return super.moreSequences();
  }
}