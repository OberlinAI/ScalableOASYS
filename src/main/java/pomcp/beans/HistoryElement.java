package pomcp.beans;


import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.State;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The History Element represents annotation on the edges of Monte Carlo tree. In case of Scalable I-POMCP, an edge would
 * be annotated with one of this category:
 * ConfigurationNodes, Action or Observation.
 * All the elements can be present in the class, or at least one of them.
 */
public class HistoryElement implements Serializable,ObjectInstance{

    /**
     * ConfigurationNode for the MCTS Tree edge.
     */
    private FACNode configurationNode;
    /**
     * Action for the MCTS Tree edge.
     */
    private ArrayList<ActionNode> actionNodes;
    /**
     * Observation for the MCTS Tree edge.
     */
    private ArrayList<ArrayList<ObservationNode>> observationNodes;


    /**
     * History Element with all the null values.
     */
    public HistoryElement() {
        this.configurationNode = null;
        this.actionNodes = new ArrayList<>();
        this.observationNodes = new ArrayList<>();
    }

    /**
     * History Element initialization with the given values.
     * @param configurationNode is the FAC for the edge from the parent.
     * @param actionNodes is the Action for the edge from the parent.
     * @param observationNodes is the Observation for the edge from the parent.
     */
    public HistoryElement(FACNode configurationNode, ArrayList<ActionNode> actionNodes, ArrayList<ArrayList<ObservationNode>> observationNodes) {
        this.configurationNode = configurationNode;
        this.actionNodes = actionNodes;
        this.observationNodes = observationNodes;
    }

    /**
     * Copy the history element to another.
     * @param historyElement is the element to copy.
     */
    public HistoryElement(HistoryElement historyElement) {
        this.configurationNode = historyElement.getConfigurationNode();
        this.actionNodes = historyElement.getActionNodes();
        this.observationNodes = historyElement.getObservationNodes();
    }


    //Getter and Setter Methods.
    public FACNode getConfigurationNode() {
        return configurationNode;
    }

    public void setConfigurationNode(FACNode configurationNode) {
        this.configurationNode = configurationNode;
    }

    public ArrayList<ActionNode> getActionNodes() {
        return actionNodes;
    }

    public void setActionNodes(ArrayList<ActionNode> actionNodes) {
        this.actionNodes = actionNodes;
    }

    public ArrayList<ArrayList<ObservationNode>> getObservationNodes() {
        return observationNodes;
    }

    public void setObservationNodes(ArrayList<ArrayList<ObservationNode>> observationNodes) {
        this.observationNodes = observationNodes;
    }

    @Override
    public String className() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public ObjectInstance copyWithName(String objectName) {
        return null;
    }

    @Override
    public List<Object> variableKeys() {
        return null;
    }

    @Override
    public Object get(Object variableKey) {
        return null;
    }

    @Override
    public State copy() {
        return null;
    }

    /**
     * Implements equals method to compare two objects.
     * @param o is the History Element object.(should be).
     * @return is true or false based on the equality.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HistoryElement)) return false;
        HistoryElement that = (HistoryElement) o;
        return this.getConfigurationNode().equals(that.getConfigurationNode()) &&
                Objects.equals(getActionNodes(), that.getActionNodes()) &&
                Objects.equals(getObservationNodes(), that.getObservationNodes());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getConfigurationNode(), getActionNodes(), getObservationNodes());
    }

    @Override
    public String toString() {
        String printStr = "HistoryElement{" +
                "\nconfigurationNode=" + configurationNode +
                "\nAction and Observation Nodes=";

        for(int actionIndex = 0 ; actionIndex < this.getActionNodes().size(); actionIndex++){
            printStr += "\nAction Node:" + actionIndex + " " + this.actionNodes.get(actionIndex).toString();
            ArrayList<ObservationNode> observationNodes = this.observationNodes.get(actionIndex);
            for(int obsIndex = 0 ; obsIndex < observationNodes.size(); obsIndex++){
                printStr += "\nObs Node:" + obsIndex + " " + observationNodes.get(obsIndex).toString();
            }//end for obsnodes.
        }//end for.

        return printStr;
    }


    /**
     * This method calculates the best action from the Q-values of the actions.
     * @return is the index of the best action.
     */
    public int bestActionIndex(){
        int bestActionIndex = 0;
        double maxValue = -10000;
        //Loop through all the nodes and find the best action node.
        for(int actionIndex = 0 ; actionIndex < this.getActionNodes().size(); actionIndex++){
            if(this.getActionNodes().get(actionIndex).getNodeQValue() >= maxValue){
                maxValue = this.getActionNodes().get(actionIndex).getNodeQValue();
                bestActionIndex = actionIndex;
            }//end if.
        }//end for.
        return  bestActionIndex;
    }//end method
}//end class.
