/**
 *  {@link domains.wildfire.FireDiscreteObservation}  is the class to get all the discrete fire observation for one stage.
 *  The class provides all the fires with the current state and difference in the intensity from the last stage.
 *  difference = Last Stage intensity - current Stage intensity.
 *  It would be further used to get and set the overall observations for the stage.
 *  @author : Maulik Shah.
 */
package domains.wildfire;


import burlap.mdp.core.action.*;
import burlap.mdp.core.state.*;
import burlap.mdp.singleagent.pomdp.observations.*;
import domains.wildfire.beans.Fire;
import posg.model.PartialObservationFunction;

import org.thejavaguy.prng.generators.PRNG;
import org.thejavaguy.prng.generators.XorshiftPlus;

import java.util.*;

public class FireDiscreteObservation implements PartialObservationFunction{

    /**
     * List of the fire observations available in each stage.
     */
    List<State> fireObservations;

    /**
     * Random Number Generator.
     */
    PRNG.Smart generator;


    /**
     * This is the default constructor generating the null list for the observations.
     */
    public FireDiscreteObservation() {
        fireObservations = new ArrayList<State>();
        //Set the possible fire observation.
        //Loop through all the intensity from least to Max intensity which is 2-max states. e.g. 2-5 = -3.
        //So, fire differences would be 3, 2, 1, 0,-1 where 3 is NO_OBS. Fires can go from 0 to 2, so 2 is a valid observation. The fire difference
        //can not be more than -1 for now.
        for (int intensity = WildfireParameters.NO_OBS; intensity >= WildfireParameters.MAX_FIRE_REDUCTION; intensity--) {
            fireObservations.add(new FireObservation(intensity));
        }

        //Initialize generator with the XorShift ones.
        generator =  new XorshiftPlus.Smart(new XorshiftPlus());
    }

    /**
     * Create a new list from the given list.
     * @param fireObservations is the list to be copied to the current one.
     */
    public FireDiscreteObservation(List<State> fireObservations) {
        this.fireObservations = fireObservations;
    }

    //Getter-Setter Methods.
    public List<State> getFireObservations() {
        return fireObservations;
    }

    public void setFireObservations(List<State> fireObservations) {
        this.fireObservations = fireObservations;
    }


    /**
     * Returns all the possible observation values.
     * @return is the list of the observation states.
     */
    @Override
    public List<State> allObservations() {
        return fireObservations;
    }


    @Override
    public List<ObservationProbability> probabilities(State state, Action action) {
        return null;
    }

    @Override
    public double probability(State observation, State state, Action action) {
        return 0;
    }

    @Override
    public State sample(State state, Action action) {
        return null;
    }

    /**
     * Given the current and next wildfire states,and based on the current agent's action, the
     * method would calculate the probability of the fire-difference observation.
     * @param observation is the observation got from the environment.
     * @param previousState is the previous state of the environment.
     * @param nextState is the next state after action.
     * @param action is the action performed by the current agent.
     * @return is the probability of the observation value.
     */
    @Override
    public double probability(State observation, State previousState, State nextState, Action action) {
        double obsProbability = 0;

        int fireNumber = -1; //Fire Number fought by @param action.
        int fireDifference = 100; // Fire Difference.


        //Get the information about the next state and the fires.

        FireObservation fireObservation = (FireObservation) observation;

        //Check for the NO-OP or NO-OBS fire differences.
        //If either of them matches then the probability would be 1.0, and 0 otherwise.
        if(WildfireParameters.NO_OBS == fireObservation.getFireDifference()){
            return  WildfireParameters.NOOP_ACTION.equals(action.actionName()) ? 1.0 : 0.0;
        }//end if.

        //If the No-op action equals the action name, then 1 if NO_OBS observation and 0 if something else.
        if(WildfireParameters.NOOP_ACTION.equals(action.actionName())){
            return  WildfireParameters.NO_OBS == fireObservation.getFireDifference() ? 1.0 : 0.0;
        }


        //Check the type of the State is being received in the argument.
        //Find the fire number and fire difference accordingly.
        if(previousState instanceof WildfireState && nextState instanceof WildfireState){
            WildfireState wfPreviousState = (WildfireState)previousState;
            WildfireState wfNextState = (WildfireState)nextState;
            //Find the fire, which current agent fought from the previous state.
            for(Fire fire : wfPreviousState.getFireList()){
                if(fire.fireActionName().equals(action.actionName())){
                    fireNumber = fire.getFireNumber();
                }
            }//end while.

            //Get the fire difference from the current and the next state.
            fireDifference = wfNextState.getFireList().get(fireNumber).getIntensity() - wfPreviousState.getFireList().get(fireNumber).getIntensity() ;
        }else if (previousState instanceof WildfireAnonymousState && nextState instanceof WildfireAnonymousState){
            WildfireAnonymousState wfPreviousState = (WildfireAnonymousState)previousState;
            WildfireAnonymousState wfNextState = (WildfireAnonymousState)nextState;
            //Find the fire, which current agent fought from the previous state.
            for(Fire fire : wfPreviousState.getFireList()){
                if(fire.fireActionName().equals(action.actionName())){
                    fireNumber = fire.getFireNumber();
                }
            }//end while.

            //Get the fire difference from the current and the next state.
            fireDifference = wfNextState.getFireList()[fireNumber].getIntensity()
                              - wfPreviousState.getFireList()[fireNumber].getIntensity();
        }else {
            System.err.println("FireDiscreteObservation: State objects are not of correct type.");
            return -1;
        }//end if-else ladder.



        //If the fire exist in the premise(It must!!) and the observation matches the actual difference.
        if(fireNumber != -1 && fireDifference == fireObservation.getFireDifference()){
            obsProbability = 1 - WildfireParameters.OBSERVATION_NOISE;
        }else{
            //Observation probability excluding the correct observation and NO_OBS.
            obsProbability = WildfireParameters.OBSERVATION_NOISE / (this.fireObservations.size()-2);
        }

        return  obsProbability;
    }//end method.

    /**
     * This method samples the observation from converting a probability density function of an observation into a CDF.
     * @param previousState is the previous state.
     * @param nextState is the next state.
     * @param action is the current agent's action.
     * @return is the sampled observation.
     */
    @Override
    public State sample(State previousState, State nextState, Action action) {
        //Get the list of observation probabilities for each and every observation.
        List<ObservationProbability> obProbs = this.probabilities(previousState,nextState, action);


        double r = this.generator.nextDouble();
        double sumProb = 0.;

        //Iterate through all possible observations.
        for(ObservationProbability op : obProbs){
            sumProb += op.p;
            if(r < sumProb){
                return op.observation;
            }
        }//end for.

        throw new RuntimeException("Could not sample observation because observation probabilities did not sum to 1; they summed to " + sumProb);
    }

    /**
     * This method calculates the probability of each fire-difference observation.
     * @param previousState is the previous state of the environment.
     * @param nextState is the next state of the environment.
     * @param action is the current agent's action.
     * @return is the probability distribution over all observations.
     */
    @Override
    public List<ObservationProbability> probabilities(State previousState, State nextState, Action action) {
        //Get all observations.
        List<State> possibleObservations = this.allObservations();
        //List of probabilities for observation.
        List<ObservationProbability> probs = new ArrayList<ObservationProbability>(possibleObservations.size());

        double sumP=0;
        String probStr="[";

        //Go through all possible observations.
        for(State obs : possibleObservations){
            double p = probability(obs,previousState,nextState,action);
            probStr += " "+p;
            if(p != 0){
                sumP+=p;
                probs.add(new ObservationProbability(obs, p));
            }//end if.
        }//end for.
        //If sum does not equal to one.
        if(sumP<1.){

            if(previousState instanceof WildfireState && nextState instanceof WildfireState){
                System.out.println( "Previous State:" + (WildfireState)previousState
                        + "Next State:" + (WildfireState)nextState
                        + "Action: " + action);
            }else{
                System.out.println( "Previous State:" + (WildfireAnonymousState)previousState
                        + "Next State:" + (WildfireAnonymousState)nextState
                        + "Action: " + action);
            }

            System.out.println(" Total Obs Prob: "+probStr+" = "+sumP);
        }

        return probs;
    }//end method.
}
