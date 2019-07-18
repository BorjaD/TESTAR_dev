package nl.ou.testar.StateModel.Event;

public interface StateModelEventListener {

    /**
     * This method handles the reception of a state model event.
     * @param event
     */
    void eventReceived(StateModelEvent event);

    /**
     * This method sets the event listener to active or deactive
     * @param listening
     */
    void setListening(boolean listening);

}
