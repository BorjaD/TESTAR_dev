package nl.ou.testar.StateModel.Persistence.OrientDB.Hydrator;

import nl.ou.testar.StateModel.Exception.HydrationException;

import java.util.HashMap;
import java.util.Map;

public abstract class HydratorFactory {

    public static final int HYDRATOR_ABSTRACT_STATE = 1;

    public static final int HYDRATOR_ABSTRACT_ACTION = 2;

    public static final int HYDRATOR_ABSTRACT_STATE_MODEL = 3;

    public static final int HYDRATOR_CONCRETE_STATE = 4;

    public static final int HYDRATOR_WIDGET = 5;

    public static final int HYDRATOR_WIDGET_RELATION = 6;

    public static final int HYDRATOR_ABSTRACTED_BY = 7;

    public static final int HYDRATOR_BLACKHOLE = 8;

    public static final int HYDRATOR_CONCRETE_ACTION = 9;

    // a repo for generated classes, so we don't execute the same generation code over and over if not needed
    private static Map<Integer, EntityHydrator> hydrators = new HashMap<>();

    public static EntityHydrator getHydrator(int hydratorType) throws HydrationException {
        if (hydrators.containsKey(hydratorType)) {
            return hydrators.get(hydratorType);
        }

        switch (hydratorType) {
            case HYDRATOR_ABSTRACT_STATE:
                return createAbstractStateHydrator();

            case HYDRATOR_ABSTRACT_ACTION:
                return createAbstractActionHydrator();

            case HYDRATOR_ABSTRACT_STATE_MODEL:
                return createAbstractStateModelHydrator();

            case HYDRATOR_CONCRETE_STATE:
                return createConcreteStateHydrator();

            case HYDRATOR_CONCRETE_ACTION:
                return createConcreteActionHydrator();

            case HYDRATOR_WIDGET:
                return createWidgetHydrator();

            case HYDRATOR_WIDGET_RELATION:
                return createWidgetRelationHydrator();

            case HYDRATOR_ABSTRACTED_BY:
                return createIsAbstractedByHydrator();

            case HYDRATOR_BLACKHOLE:
                return createBlackHoleHydrator();

            default:
                throw new HydrationException("Invalid hydrator type provided to the hydrator factory");
        }
    }

    private static AbstractStateHydrator createAbstractStateHydrator() {
        AbstractStateHydrator abstractStateHydrator = new AbstractStateHydrator();
        hydrators.put(HYDRATOR_ABSTRACT_STATE, abstractStateHydrator);
        return abstractStateHydrator;
    }

    private static AbstractActionHydrator createAbstractActionHydrator() {
        AbstractActionHydrator abstractActionHydrator = new AbstractActionHydrator();
        hydrators.put(HYDRATOR_ABSTRACT_ACTION, abstractActionHydrator);
        return abstractActionHydrator;
    }

    private static AbstractStateModelHydrator createAbstractStateModelHydrator() {
        AbstractStateModelHydrator abstractStateModelHydrator  = new AbstractStateModelHydrator();
        hydrators.put(HYDRATOR_ABSTRACT_STATE_MODEL, abstractStateModelHydrator);
        return abstractStateModelHydrator;
    }

    private static ConcreteStateHydrator createConcreteStateHydrator() {
        ConcreteStateHydrator concreteStateHydrator = new ConcreteStateHydrator();
        hydrators.put(HYDRATOR_CONCRETE_STATE, concreteStateHydrator);
        return concreteStateHydrator;
    }

    private static WidgetHydrator createWidgetHydrator() {
        WidgetHydrator widgetHydrator = new WidgetHydrator();
        hydrators.put(HYDRATOR_WIDGET, widgetHydrator);
        return widgetHydrator;
    }

    private static WidgetRelationHydrator createWidgetRelationHydrator() {
        WidgetRelationHydrator widgetRelationHydrator = new WidgetRelationHydrator();
        hydrators.put(HYDRATOR_WIDGET_RELATION, widgetRelationHydrator);
        return widgetRelationHydrator;
    }

    private static IsAbstractedByHydrator createIsAbstractedByHydrator() {
        IsAbstractedByHydrator isAbstractedByHydrator = new IsAbstractedByHydrator();
        hydrators.put(HYDRATOR_ABSTRACTED_BY, isAbstractedByHydrator);
        return isAbstractedByHydrator;
    }

    private static BlackHoleHydrator createBlackHoleHydrator() {
        BlackHoleHydrator blackHoleHydrator = new BlackHoleHydrator();
        hydrators.put(HYDRATOR_BLACKHOLE, blackHoleHydrator);
        return blackHoleHydrator;
    }

    private static ConcreteActionHydrator createConcreteActionHydrator() {
        ConcreteActionHydrator concreteActionHydrator = new ConcreteActionHydrator();
        hydrators.put(HYDRATOR_CONCRETE_ACTION, concreteActionHydrator);
        return concreteActionHydrator;
    }
}
