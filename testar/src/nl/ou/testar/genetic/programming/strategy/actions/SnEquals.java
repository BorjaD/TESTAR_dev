package nl.ou.testar.genetic.programming.strategy.actions;

import nl.ou.testar.genetic.programming.strategy.StrategyGuiState;
import nl.ou.testar.genetic.programming.strategy.actionTypes.StrategyNode;
import nl.ou.testar.genetic.programming.strategy.actionTypes.StrategyNodeBoolean;
import nl.ou.testar.genetic.programming.strategy.actionTypes.StrategyNodeNumber;

import java.util.List;

public class SnEquals extends StrategyNodeBoolean {
    private StrategyNodeNumber child;
    private StrategyNodeNumber child1;

    public SnEquals(final List<StrategyNode> children) {
        super(children);
        this.child = (StrategyNodeNumber) children.get(0);
        this.child1 = (StrategyNodeNumber) children.get(1);
    }

    @Override
    public boolean getValue(final StrategyGuiState state) {
        return this.child.getValue(state) == this.child1.getValue(state);
    }

}