/**
 *  {@link domains.wildfire.FireObservation}  is the class to describe the fireObservation in the premise.
 *  The FireObservation is the description of the fire and its intensity difference from the previous state.
 *  This class is being used in implementing the DiscreteObservationFunction.
 * @author : Maulik Shah.
 */
package domains.wildfire;

import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.MutableState;
import burlap.mdp.core.state.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FireObservation implements ObjectInstance,MutableState {
    /**
     * The fire Difference is the difference in the intensity of the fire from the last stage to current.
     */
    private int fireDifference;


    /**
     * Class name and variable keys for the state class.
     */
    public static final String OBSERVATION_CLASS = "FIRE_OBS";
    private static final String FIRE_DIFF = "FIRE_DIFF";
    private static final List<Object> VARIABLE_KEYS = Arrays.<Object>asList(FIRE_DIFF);

    /**
     * This is the default constructor of the fire, which initiates every member to -1.
     */
    public FireObservation() {
        this.fireDifference = -1;
    }

    /**
     * This primary constructor initiates the fire Observation with required parameters.
     * @param fireDifference is the difference in the intensity of the fire from the previous stage.
     */
    public FireObservation(int fireDifference) {
        this.fireDifference = fireDifference;
    }


    /**
     * This constructor creates a new FireObservation object from the passed FireObservation object.
     * @param fireObservation is the fire object to be copied into the new one.
     */
    public FireObservation(FireObservation fireObservation) {
        this.fireDifference = fireObservation.getFireDifference();
    }


    //Getter and Setter methods.

    public int getFireDifference() {
        return fireDifference;
    }

    public void setFireDifference(int fireDifference) {
        this.fireDifference = fireDifference;
    }

    /**
     * This method will generate FireObservation class as String.
     * output: e.g. FireObservation{fire=Fire{fireLocation=Location{x=1,y=1},intensity=2},fireDifference=1}.
     * It also uses Fire.toString() method intrinsically.
     * @return the FireObservation string.
     */
    @Override
    public String toString() {
        return "Fire Observation: fireDifference=" + fireDifference ;
    }

    /**
     * This method overrides the standard equals method to compare an object with current one.
     * @param o is the object to be compared.
     * @return true or false based on observation.
     */
    @Override
    public boolean equals(Object o) {
        //Check if the object is the same object of the calling method.
        if (this == o) return true;
        //Check if the object is an instance of FireObservation or not.
        if (!(o instanceof FireObservation)) return false;
        //Convert the object to FireObservation and compare all the members.
        FireObservation fireOb = (FireObservation) o;
        return fireDifference == fireOb.fireDifference;
    }


    @Override
    public String className() {
        return FireObservation.OBSERVATION_CLASS;
    }

    @Override
    public String name() {
        return FireObservation.OBSERVATION_CLASS;
    }

    @Override
    public ObjectInstance copyWithName(String objectName) {
        return null;
    }

    @Override
    public MutableState set(Object variableKey, Object value) {
        String key = (String)variableKey;
        if(FireObservation.OBSERVATION_CLASS.equals(key)){
            int fireDiff = ((ArrayList<Integer>)value).get(0);
            this.setFireDifference(fireDiff);
        }
        return this;
    }

    @Override
    public List<Object> variableKeys() {
        return FireObservation.VARIABLE_KEYS;
    }

    @Override
    public Object get(Object variableKey) {
        String key = (String)variableKey;
        if(FireObservation.OBSERVATION_CLASS.equals(key)){
            return (Integer)this.getFireDifference();
        }else{
            return null;
        }
    }

    @Override
    public State copy() {
        return new FireObservation(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fireDifference);
    }


}
