package pomcp.beans;

import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.pomdp.observations.DiscreteObservationFunction;
import posg.model.PartialObservationFunction;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This class is for getting the {@link TreeNode} from the intermediate node after sampling FAC and choosing the best action.
 * The current node is received after the simulation of the state particle and the actions, while the observation is sampled in
 * the simulation. The node contains a reference to the hashcode of the node which is created after.
 * @author Maulik
 */
public class ObservationNode implements Serializable,ObjectInstance{
    /**
     * sampled observation.
     */
    State observation;

    /**
     * The hashcode of the child node generated after sampling the observation.
     */
    Integer childHashCode;

    /**
     * Class name and variable keys for the Node state class.
     */
    public static final String CLASS_NAME = "OBSERVATION_NODE";
    private static final String OBSERVATION = "OBSERVATION";
    private static final String CHILD_CODE = "CHILD_CODE";
    private static final List<Object> VARIABLE_KEYS = Arrays.<Object>asList(OBSERVATION,CHILD_CODE);



    /**
     * Empty Constructor.
     */
    public ObservationNode() {
        this.observation = null;
        this.childHashCode = null;
    }

    /**
     * Copy the values of the other observation node to the current.
     * @param observation is the observation received.
     * @param childHashCode is the new child created.
     */
    public ObservationNode(State observation, Integer childHashCode) {
        this.observation = observation;
        this.childHashCode = childHashCode;
    }

    /**
     * Copy the object to the current node.
     * @param observationNode is the object to copy.
     */
    public ObservationNode(ObservationNode observationNode) {
        this.observation = observationNode.getObservation();
        this.childHashCode = observationNode.getChildHashCode();
    }

    //Getter and Setter method.
    public State getObservation() {
        return observation;
    }

    public void setObservation(State observation) {
        this.observation = observation;
    }

    public int getChildHashCode() {
        return childHashCode;
    }

    public void setChildHashCode(int childHashCode) {
        this.childHashCode = childHashCode;
    }

    @Override
    public String className() {
        return ObservationNode.CLASS_NAME;
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
        return ObservationNode.VARIABLE_KEYS;
    }

    @Override
    public Object get(Object variableKey) {
        String key = (String) variableKey;

        if(ObservationNode.CHILD_CODE.equals(key)){
            return this.getChildHashCode();
        }else if(ObservationNode.OBSERVATION.equals(key)){
            return this.getObservation();
        }else{
            return  null;
        }//end if-else.
    }

    @Override
    public State copy() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObservationNode)) return false;
        ObservationNode that = (ObservationNode) o;
        return getChildHashCode() == that.getChildHashCode() &&
                Objects.equals(getObservation(), that.getObservation());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getObservation(), getChildHashCode());
    }

    @Override
    public String toString() {
        return "ObservationNode{" +
                "observation=" + observation +
                ":childHashCode=" + childHashCode +
                '}';
    }
}
