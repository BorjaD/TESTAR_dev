package nl.ou.testar.a11y.wcag2;

import org.fruit.alayer.Widget;

/**
 * The result of evaluating a success criterion
 * @author Davy Kager
 *
 */
public class EvaluationResult {
	
	public enum Type {
		/**
		 * No problem found
		 */
		OK,
		
		/**
		 * An error
		 * This is a definite problem that can be detected automatically.
		 */
		ERROR,
		
		/**
		 * A warning
		 * This is a potential problem that needs expert evaluation.
		 */
		WARNING,
		
		/**
		 * A general problem
		 * This is a potential problem that is not specific to one piece of code
		 * and that needs expert evaluation.
		 */
		GENERAL;
	}
	
	private final SuccessCriterion criterion;
	private final Type type;
	private final Widget widget;
	
	/**
	 * Constructs a new evaluation result that does not apply to a single widget
	 * @param criterion The success criterion.
	 * @param type The problem type.
	 */
	EvaluationResult(SuccessCriterion criterion, Type type) {
		this(criterion, type, null);
	}

	/**
	 * Constructs a new evaluation result that applies to a single widget
	 * @param criterion The success criterion.
	 * @param type The problem type.
	 * @param widget The widget that the success criterion applies to.
	 */
	EvaluationResult(SuccessCriterion criterion, Type type, Widget widget) {
		this.criterion = criterion;
		this.type = type;
		this.widget = widget;
	}
	
	/**
	 * Gets the success criterion associated with this evaluation result
	 * @return The success criterion.
	 */
	public SuccessCriterion getSuccessCriterion() {
		return criterion;
	}
	
	/**
	 * Gets the problem type of this evaluation result
	 * @return The problem type.
	 */
	public Type getType() {
		return type;
	}
	
	/**
	 * Gets the widget that the success criterion associated with this evaluation result applies to
	 * @return The widget.
	 */
	public Widget getWidget() {
		return widget;
	}

}
