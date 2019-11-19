package posg.model;

import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.pomdp.observations.DiscreteObservationFunction;
import burlap.mdp.singleagent.pomdp.observations.ObservationProbability;

import java.util.List;

/**
 * The PartialObservationFunction is an extension to the DiscreteObservationFunction, which includes methods for including,
 * previous and current state. In the current scope of the Wild Fire domain, the fire intensity difference can be calculated
 * by finding difference between previous and the current state, which would be then compared with the actual observation in
 * fire intensity difference.
 * @author Maulik Shah
 */
public interface PartialObservationFunction extends DiscreteObservationFunction {

    /**
     * This method would calculate the probability of the observation given the previous-state and the next-state.
     * @param observation is the observation got from the environment.
     * @param previousState is the previous state of the environment.
     * @param nextState is the next state after action.
     * @param action is the action performed by the current agent.
     * @return is the probability of the observation given all other params.
     */
    double probability(State observation, State previousState, State nextState, Action action);


    /**
     * Given the previous and next state, as well as the current agent's action, sample the next observation from the
     * all possible observations.
     * @param previousState is the previous state.
     * @param nextState is the next state.
     * @param action is the current agent's action.
     * @return is the sampled observation.
     */
    State sample(State previousState,State nextState,Action action);

    /**
     * This method would give us the probability distribution over the observation given the previous and next state,as
     * well as the current agent's action.
     * @param previousState is the previous state of the environment.
     * @param nextState is the next state of the environment.
     * @param action is the current agent's action.
     * @returni is the list of the probabilities over all possible observations.
     */
    List<ObservationProbability> probabilities(State previousState,State nextState,Action action);
}//end interface.
