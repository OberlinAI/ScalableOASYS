package pomcp.beans;

import burlap.mdp.core.action.Action;
import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.State;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * This class represents the intermediate node in the MCTS tree after choosing an action to perform given the sampled
 * configuration. The node has just visitation count and Q-Value to represent the node for further selection.
 * @author Maulik
 */
public class ActionNode implements Serializable,ObjectInstance {

    /**
     * Selected action for this node.
     */
    Action action;
    /**
     * Total visit to this node.
     */
    int nodeVisit;
    /**
     * Q-Value of this node.
     */
    double nodeQValue;

    /**
     * Class name and variable keys for the Node state class.
     */
    public static final String CLASS_NAME = "ACTION_NODE";
    private static final String ACTION = "ACTION";
    private static final String VISITS = "VISITS";
    private static final String VALUE = "VALUE";
    private static final List<Object> VARIABLE_KEYS = Arrays.<Object>asList(VISITS, VALUE,ACTION);


    /**
     * This is an empty constructor.
     */
    public ActionNode() {
        this.action = null;
        this.nodeVisit = 0;
        this.nodeQValue = 0;
    }

    /**
     * Constructor with the available values.
     * @param action is the action being taken.
     * @param nodeVisit is the updated node visit.
     * @param nodeQValue is the updated Q-Value of the node.
     */
    public ActionNode(Action action, int nodeVisit, double nodeQValue) {
        this.action = action;
        this.nodeVisit = nodeVisit;
        this.nodeQValue = nodeQValue;
    }

    /**
     * Copy the Action Node object.
     * @param actionNode is the object to copy.
     */
    public ActionNode(ActionNode actionNode) {
        this.action = actionNode.getAction();
        this.nodeVisit = actionNode.getNodeVisit();
        this.nodeQValue = actionNode.getNodeQValue();
    }


    //Getter and Setter methods.
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public int getNodeVisit() {
        return nodeVisit;
    }

    public void setNodeVisit(int nodeVisit) {
        this.nodeVisit = nodeVisit;
    }

    /**
     * This method increases the current visitation count by 1.
     */
    public void incrementVisit() {
        this.nodeVisit +=  1;
    }

    public double getNodeQValue() {
        return nodeQValue;
    }

    public void setNodeQValue(double nodeQValue) {
        this.nodeQValue = nodeQValue;
    }

    /**
     * This method increases the Q-value of the node using the equation on the line 16 of algorithm.
     * Q = R-Q/Count for the current p -> Config -> Action node.
     * @param reward
     */
    public void incrementQValue(double reward) {
        if(this.nodeVisit == 0){
            this.nodeQValue = reward;
        }else{
            this.nodeQValue += (reward- this.nodeQValue)/this.nodeVisit;
        }//end if-else.
    }

    @Override
    public String className() {
        return ActionNode.CLASS_NAME;
    }

    @Override
    public String name() {
        return this.hashCode()+"";
    }

    @Override
    public ObjectInstance copyWithName(String objectName) {
        return null;
    }

    @Override
    public List<Object> variableKeys() {
        return ActionNode.VARIABLE_KEYS;
    }

    @Override
    public Object get(Object variableKey) {
        String key = (String) variableKey;

        if(ActionNode.ACTION.equals(key)){
            return this.getAction();
        }else if(ActionNode.VALUE.equals(key)){
            return this.getNodeQValue();
        }else if(ActionNode.VISITS.equals(key)){
            return this.getNodeVisit();
        }else{
            return  null;
        }//end if-else.
    }

    @Override
    public State copy() {
        return new ActionNode(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActionNode)) return false;
        ActionNode that = (ActionNode) o;
        return getNodeVisit() == that.getNodeVisit() &&
                Double.compare(that.getNodeQValue(), getNodeQValue()) == 0 &&
                Objects.equals(getAction(), that.getAction());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getAction(), getNodeVisit(), getNodeQValue());
    }


    @Override
    public String toString() {
        return "ActionNode{" +
                "action=" + action +
                ":nodeVisit=" + nodeVisit +
                ":nodeQValue=" + nodeQValue +
                '}';
    }
}
