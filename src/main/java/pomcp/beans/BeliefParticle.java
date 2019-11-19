package pomcp.beans;

import burlap.mdp.core.action.Action;
import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.MutableState;
import burlap.mdp.core.state.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BeliefParticle implements MutableState,ObjectInstance {

    /**
     * State representation of the node.
     */
    private State stateParticle;
    /**
     * Mental model of the other agents.
     */
    private ArrayList<Integer> mentalModels;
    /**
     * Before generating FAC, keep a list of the sampled action of each agent.
     * This would be helpful for transition of the mental model.
     */
    private ArrayList<Action> sampledActions;



    /**
     * Class name and variable keys for the Node state class.
     */
    public static final String CLASS_NAME = "BELIEF_PARTICLE";
    private static final String STATE_PARTICLE = "STATE_PARTICLE";
    private static final String MENTAL_MODELS = "MENTAL_MODELS";
    private static final String SAMPLED_ACTIONS = "MENTAL_MODELS";
    private static final List<Object> VARIABLE_KEYS = Arrays.<Object>asList(STATE_PARTICLE,SAMPLED_ACTIONS);


    public BeliefParticle(State stateParticle, ArrayList<Integer> mentalModels) {
        this.stateParticle = stateParticle;
        this.mentalModels = mentalModels;
        this.sampledActions = new ArrayList<>(mentalModels.size());
    }

    public BeliefParticle(BeliefParticle beliefParticle) {
        this.stateParticle = beliefParticle.getStateParticle();
        this.mentalModels = beliefParticle.getMentalModels();
        this.sampledActions = beliefParticle.getSampledActions();
    }

    public State getStateParticle() {
        return stateParticle;
    }

    public void setStateParticle(State stateParticle) {
        this.stateParticle = stateParticle;
    }

    public ArrayList<Integer> getMentalModels() {
        return mentalModels;
    }

    public void setMentalModels(ArrayList<Integer> mentalModels) {
        this.mentalModels = mentalModels;
    }

    public ArrayList<Action> getSampledActions() {
        return sampledActions;
    }

    public void setSampledActions(ArrayList<Action> sampledActions) {
        this.sampledActions = sampledActions;
    }

    @Override
    public String className() {
        return BeliefParticle.CLASS_NAME;
    }

    @Override
    public String name() {
        return hashCode()+"";
    }

    @Override
    public ObjectInstance copyWithName(String objectName) {
        return null;
    }

    @Override
    public MutableState set(Object variableKey, Object value) {
        String key = (String) variableKey;
        if(BeliefParticle.MENTAL_MODELS.equals(key)){
            this.setMentalModels((ArrayList<Integer>)value);
        }else if(BeliefParticle.STATE_PARTICLE.equals(key)){
            this.setStateParticle((State)value);
        }else if(BeliefParticle.SAMPLED_ACTIONS.equals(key)){
            this.setSampledActions((ArrayList<Action>) value);
        }else{
           //Do Nothing
        }//end if-else.
        return  this;
    }

    @Override
    public List<Object> variableKeys() {
        return BeliefParticle.VARIABLE_KEYS;
    }

    @Override
    public Object get(Object variableKey) {
        String key = (String) variableKey;

        if(BeliefParticle.MENTAL_MODELS.equals(key)){
            return this.getMentalModels();
        }else if(BeliefParticle.STATE_PARTICLE.equals(key)){
            return this.getStateParticle();
        }else if(BeliefParticle.SAMPLED_ACTIONS.equals(key)){
            return this.getSampledActions();
        }else{
            return  null;
        }//end if-else.
    }//end method.

    @Override
    public State copy() {
        return new BeliefParticle(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BeliefParticle)) return false;
        BeliefParticle that = (BeliefParticle) o;
        return Objects.equals(getStateParticle(), that.getStateParticle()) &&
                Objects.equals(getMentalModels(), that.getMentalModels()) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStateParticle(), getMentalModels());
    }



}
