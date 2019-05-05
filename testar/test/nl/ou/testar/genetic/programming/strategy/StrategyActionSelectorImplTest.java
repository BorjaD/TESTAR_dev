package nl.ou.testar.genetic.programming.strategy;

import nl.ou.testar.genetic.programming.strategy.actionTypes.StrategyNode;
import nl.ou.testar.genetic.programming.strategy.actions.SnClickAction;
import nl.ou.testar.genetic.programming.strategy.actions.SnEscape;
import nl.ou.testar.genetic.programming.strategy.actions.SnRandomLeastExecutedAction;
import org.fruit.alayer.Action;
import org.fruit.alayer.Role;
import org.fruit.alayer.State;
import org.fruit.alayer.Tags;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StrategyActionSelectorImplTest {

    private StrategyActionSelector strategyActionSelector;

    @Mock
    State state;

    private Set<Action> actions = new HashSet<>();

    private Action action1;

    @Mock
    private Action action2;
    private Role role1;


    @Before
    public void setup() {

    }

    @Test
    public void selectAction() {
//        when(state.get(Tags.ConcreteID)).thenReturn("concreteId");
//        when(action2.get(Tags.ConcreteID)).thenReturn("ConcreteActionId");
//        this.actions.add(action2);
//        final Action action = this.strategyActionSelector.selectAction(state, this.actions);
//        assertNotNull(action);
//        assertEquals(1, this.actions.size());
    }
}
