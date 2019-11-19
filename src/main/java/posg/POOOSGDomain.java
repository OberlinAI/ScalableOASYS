package posg;

import common.StateEnumerator;


import burlap.mdp.stochasticgames.oo.OOSGDomain;
import posg.model.PartialObservationFunction;


/**
 * This class is created to implement the POMDP structure of the burlap. It is the copy of the {@link burlap.mdp.singleagent.pomdp.PODomain}
 * which implements the SADomain , which is a single-agent domain.
 */


public class POOOSGDomain extends OOSGDomain {

    /**
     * The observation function
     */
    protected PartialObservationFunction obsevationFunction;

    /**
     * The underlying MDP state enumerator
     */
    protected StateEnumerator		stateEnumerator;


    /**
     * Sets the {@link PartialObservationFunction} used by the domain.
     * @param PartialObservationFunction the {@link PartialObservationFunction} to be used by the domain.
     */
    public void setPartialObservationFunction(PartialObservationFunction PartialObservationFunction){
        this.obsevationFunction = PartialObservationFunction;
    }

    /**
     * Returns the {@link PartialObservationFunction} used by this domain.
     * @return the {@link PartialObservationFunction} used by this domain.
     */
    public PartialObservationFunction getPartialObservationFunction(){
        return this.obsevationFunction;
    }


    /**
     * Indicates whether this domain has a {@link burlap.behavior.singleagent.auxiliary.StateEnumerator} defined for it.
     * If true, then it does provide a {@link burlap.behavior.singleagent.auxiliary.StateEnumerator}, if false, then
     * it does not. POMDP algorithms that require access to a {@link burlap.behavior.singleagent.auxiliary.StateEnumerator}
     * should always query this method to check, because querying the {@link #getStateEnumerator()} when one is not provided
     * by this domain will result in a runtime exception.
     * @return True if this POMDP domain provides a {@link burlap.behavior.singleagent.auxiliary.StateEnumerator}, false otherwise.
     */
    public boolean providesStateEnumerator(){ return this.stateEnumerator != null; }

    /**
     * Gets the {@link burlap.behavior.singleagent.auxiliary.StateEnumerator} used by this domain to enumerate all underlying MDP states.
     * If no {@link burlap.behavior.singleagent.auxiliary.StateEnumerator} is provided by this domain, then a runtime exception
     * will be thrown. To check if a {@link burlap.behavior.singleagent.auxiliary.StateEnumerator} is provided, use the
     * {@link #providesStateEnumerator()} method.
     * @return the {@link burlap.behavior.singleagent.auxiliary.StateEnumerator} used by this domain to enumerate all underlying MDP states.
     */
    public StateEnumerator getStateEnumerator() {
        if(this.stateEnumerator == null){
            throw new RuntimeException("This domain cannot return a StateEnumerator because one is not defined for it. " +
                    "Use the providesStateEnumerator() method to check if one is provided in advance.");
        }
        return stateEnumerator;
    }


    /**
     * Sets the {@link burlap.behavior.singleagent.auxiliary.StateEnumerator} used by this domain to enumerate all underlying MDP states.
     * @param stateEnumerator the {@link burlap.behavior.singleagent.auxiliary.StateEnumerator} used by this domain to enumerate all underlying MDP states.
     */
    public void setStateEnumerator(StateEnumerator stateEnumerator) {
        this.stateEnumerator = stateEnumerator;
    }



}
