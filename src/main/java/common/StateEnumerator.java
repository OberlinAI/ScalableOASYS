package common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import burlap.mdp.core.Domain;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;
import posg.POOOSGDomain;
import scalability.FrameActionConfiguration;


public class StateEnumerator {

    /**
     * The domain whose states will be enumerated
     */
    protected Domain domain;

    /**
     * The hashing factory used to hash states and perform equality tests
     */
    protected HashableStateFactory hashingFactory;

    /**
     * The forward state enumeration map
     */
    public Map<HashableState, Integer>  enumeration = new HashMap<HashableState, Integer>();

    /**
     * The reverse enumeration id to state map
     */
    public Map<Integer, State> reverseEnumerate = new HashMap<Integer, State>();

    protected Map<String, SGAgent> agentDefinitions;

    /**
     * Added for the anonymous agent definition.
     */
    protected Map<String, SGAgent> allAgentAnmDefinitions;

    /**
     * The id to use for the next unique state that is added
     */
    protected int nextEnumeratedID = 0;


    /**
     * Constructs
     * @param domain the domain of the states to be enumerated
     * @param hashingFactory the hashing factory to use
     */


    public StateEnumerator(Domain domain, HashableStateFactory hashingFactory, Map<String, SGAgent> agentDefinitions){
        this.domain = domain;
        this.hashingFactory = hashingFactory;
        this.agentDefinitions = agentDefinitions;
    }

    public StateEnumerator(Domain domain, HashableStateFactory hashingFactory, Map<String, SGAgent> agentDefinitions
                                                        , Map<String, SGAgent> allAgentAnmDefinitions){
        this.domain = domain;
        this.hashingFactory = hashingFactory;
        this.agentDefinitions = agentDefinitions;
        this.allAgentAnmDefinitions = allAgentAnmDefinitions;
    }


    public StateEnumerator(Domain domain, HashableStateFactory hashingFactory){
        this.domain = domain;
        this.hashingFactory = hashingFactory;
    }




    /**
     * Finds all states that are reachable from an input state and enumerates them
     * @param from the state from which all reachable states should be searched
     */
    public void findReachableStatesAndEnumerate(State from){
        Set<HashableState> reachable =
                StateReachability.getReachableHashedStates(from, (POOOSGDomain)this.domain, this.agentDefinitions, this.hashingFactory);
        for(HashableState sh : reachable){
            this.getEnumeratedID(sh);
        }
    }

    public void findReachableStatesAndEnumerate(State from, int horizon){
        Set<HashableState> reachable =
                StateReachability.getReachableHashedStates(from, (POOOSGDomain)this.domain, this.agentDefinitions,this.allAgentAnmDefinitions, this.hashingFactory, horizon);
        for(HashableState sh : reachable){
            this.getEnumeratedID(sh);
        }
    }

    /**
     * This method would get the reachable states and then enumerate it.
     * @param from is the from state.
     * @param horizon is the current horizon.
     */
    public void findReachableAnmStatesAndEnumerate(State from, FrameActionConfiguration fac, int horizon, boolean excludeSelfAgent){
        Set<HashableState> reachable =
                StateReachability.getReachableAnmHashedStates(from, fac, (POOOSGDomain)this.domain,this.allAgentAnmDefinitions, this.hashingFactory, horizon,excludeSelfAgent);
        for(HashableState sh : reachable){
            this.getEnumeratedID(sh);
        }
    }


    /**
     * Get all possible FACs given the current state and agent definitions.
     * @param from is the from state.
     * @param fac is the current FrameActionConfiguration object.
     */
    public void initializeFAC(State from, FrameActionConfiguration fac, boolean excludeSelfAgent){
        StateReachability.initializeFAC(from,fac,(POOOSGDomain) this.domain,this.allAgentAnmDefinitions,excludeSelfAgent);
    }


    /**
     * Finds all states that are reachable from an input state and enumerates them. 
     * Will not search from states that are marked as terminal states.
     * @param from the state from which all reachable states should be searched
     * @param tf the terminal function that prevents expanding from terminal states
     */
    public void findReachableStatesAndEnumerate(State from, TerminalFunction tf){
        Set<HashableState> reachable =
                StateReachability.getReachableHashedStates(from, (POOOSGDomain)this.domain, this.agentDefinitions, this.hashingFactory, tf);
        for(HashableState sh : reachable){
            this.getEnumeratedID(sh);
        }
    }

    public void findReachableStatesAndEnumerate(State from, TerminalFunction tf, int horizon){
        Set<HashableState> reachable =
                StateReachability.getReachableHashedStates(from, (POOOSGDomain)this.domain, this.agentDefinitions, this.allAgentAnmDefinitions,this.hashingFactory, tf, horizon);
        for(HashableState sh : reachable){
            this.getEnumeratedID(sh);
        }
    }

    /**
     * Get or create and get the enumeration id for a state
     * @param s the state to get the enumeration id
     * @return the enumeration id
     */
    public int getEnumeratedID(State s){
        HashableState sh = this.hashingFactory.hashState(s);
        return this.getEnumeratedID(sh);
    }

    /**
     * Returns the state associated with the given enumeration id.
     * A state must have previously be associated with the input enumeration id, or a runtime exception is thrown.
     * @param id the enumeration id
     * @return the state associated with the given enumeration id.
     */
    public State getStateForEnumerationId(int id){
        State s = this.reverseEnumerate.get(id);
        if(s == null){
            throw new RuntimeException("There is no state stored with the enumeration id: " + id);
        }
        return s;
    }

    /**
     * Returns the number of states that have been enumerated
     * @return the number of states that have been enumerated
     */
    public int numStatesEnumerated(){
        return this.enumeration.size();
    }

    /**
     * Get or create and get the enumeration id for a hashed state
     * @param sh the hashed state to get the enumeration id
     * @return the enumeration id
     */
    protected int getEnumeratedID(HashableState sh){
        Integer storedID = this.enumeration.get(sh);

        if(storedID == null){
            this.enumeration.put(sh, this.nextEnumeratedID);
            this.reverseEnumerate.put(this.nextEnumeratedID, sh.s());
            storedID = this.nextEnumeratedID;
            this.nextEnumeratedID++;
        }
//		System.out.println("Stored: "+storedID);
        return storedID;
    }

}
