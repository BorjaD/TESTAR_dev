package nl.ou.testar.SimpleGuiStateGraph.strategy.actions;

import nl.ou.testar.SimpleGuiStateGraph.strategy.StrategyGuiState;
import nl.ou.testar.SimpleGuiStateGraph.strategy.actionTypes.StrategyNode;
import nl.ou.testar.SimpleGuiStateGraph.strategy.actionTypes.StrategyNodeAction;
import nl.ou.testar.SimpleGuiStateGraph.strategy.actionTypes.StrategyNodeActionType;
import org.fruit.alayer.Action;

import java.util.List;
import java.util.Optional;

public class SnRandomActionOfTypeOtherThan extends StrategyNodeAction {
    private StrategyNodeActionType child;

    public SnRandomActionOfTypeOtherThan(final List<StrategyNode> children) {
        super(children);
        this.child = (StrategyNodeActionType) children.get(0);

    }

    @Override
    public Optional<Action> getAction(final StrategyGuiState state) {
        return this.child.getActionType(state).map(state::getRandomActionOfTypeOtherThan);
    }

}