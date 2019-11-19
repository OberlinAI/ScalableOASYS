/**
 * This class represents the Wildfire State, which includes fires, agents and fire observations.
 * Note that the Anonymous state is always partial,it would contain only the groups which are in the neighbourhood
 * of the self agent.
 * @author: Maulik Shah
 */

package domains.wildfire;

import burlap.mdp.core.oo.state.ObjectInstance;
import java.util.*;
import burlap.mdp.core.state.MutableState;
import burlap.mdp.core.state.State;
import domains.wildfire.beans.*;

public class WildfireAnonymousState implements ObjectInstance,MutableState {
    /**
     * Current agent.
     */
    private Agent selfAgent;

    /**
     * This is the neighbour groups of the selfAgent.
     * Note, the list does not contain selfAgent's own group, which can also be a part of neighbourhood.
     * Explicit concern should be taken while processing this state.
     */
    private String[] anonymousAgentGroups;
    /**
     * List of the fires in the premise with their information, with the fire number as the key.
     */
    private Fire[] fireList;

    /**
     * Class name and variable keys for the state class.
     */
    public static final String WILDFIRE_ANM_CLASS_NAME = "WILDFIRE_STATE";
    private static final String SELF_AGENT = "SELF_AGENT";
    private static final String ANM_AGENT_GROUP = "ANM_AGENT_GROUP";
    private static final String FIRE_LIST = "FIRE_LIST";
    private static final List<Object> VARIABLE_KEYS = Arrays.<Object>asList(SELF_AGENT, ANM_AGENT_GROUP, FIRE_LIST);
    /**
     * Default Constructor : Initiate dimensions to -1,-1 and the List of Agents/Fires/Observation to new lists.
     */
    public WildfireAnonymousState() {
        this.selfAgent = new Agent();
        this.anonymousAgentGroups = null;
        this.fireList = null;
    }

    /**
     * Primary Constructor : Creates a state objects given all the available parameters.
     * @param anonymousAgents is the agents in the premise.
     * @param fireList is the fires in the premise.
     */
    public WildfireAnonymousState( Agent selfAgent,String[] anonymousAgents, Fire[] fireList) {
        this.selfAgent = selfAgent;
        this.anonymousAgentGroups = anonymousAgents;
        this.fireList = fireList;
    }


    /**
     * This constructor creates a new WildfireAnonymousState object from the passed similar class object.
     * @param WildfireAnonymousState is the WildfireAnonymousState object to be copied into the new one.
     */
    public WildfireAnonymousState(WildfireAnonymousState WildfireAnonymousState) {
        //Create Self-agent.
        this.selfAgent = new Agent(WildfireAnonymousState.getSelfAgent());

        //Create Anonymous agent list.
        this.anonymousAgentGroups = new String[WildfireAnonymousState.getanonymousAgents().length];
        System.arraycopy(WildfireAnonymousState.getanonymousAgents(),0,this.anonymousAgentGroups,
                                                    0,WildfireAnonymousState.getanonymousAgents().length);

        //Copy fires.
        this.fireList = new Fire[WildfireAnonymousState.getFireList().length];
        int fireCount = 0;
        for(Fire fire : WildfireAnonymousState.getFireList()){
            this.fireList[fireCount] = new Fire(fire);
            fireCount++;
        }//end for.
    }


    /**
     * This constructor creates a new anonymous WildfireAnonymousState object from the WildFireState (physical state).
     * @param wildfireState is the physical state to use.
     */
    public WildfireAnonymousState(WildfireState wildfireState,WildfireDomain domain) {
        //Copy the Self-agent and the fire list.
        this.selfAgent = wildfireState.getSelfAgent();

        //Copy fires.
        this.fireList = new Fire[wildfireState.getFireList().size()];
        int fireCount = 0;
        for(Fire fire : wildfireState.getFireList()){
            this.fireList[fireCount] = new Fire(fire);
            fireCount++;
        }//end for.

        //ArrayList for groups already considered.
        ArrayList<String> groupsConsidered = new ArrayList<String>();

        //Get the list of the agent groups in the neighbourhood.
        this.anonymousAgentGroups = domain.agentsNeighbourhood.get(selfAgent.getAgentGroup()).toArray(new String[0]);

    }//end constructor.


    //Getter and Setter Methods.

    public String[] getanonymousAgents() {
        return this.anonymousAgentGroups;
    }

    public void setanonymousAgents(String[] anonymousAgentGroups) {
        this.anonymousAgentGroups = anonymousAgentGroups;
    }

    public Fire[] getFireList() {
        return fireList;
    }

    public void setFireList(Fire[] fireList) {
        this.fireList = fireList;
    }

    public Agent getSelfAgent() {
        return selfAgent;
    }

    public void setSelfAgent(Agent selfAgent) {
        this.selfAgent = selfAgent;
    }

    // Overridden methods.


    @Override
    public String className() {
        return WildfireAnonymousState.ANM_AGENT_GROUP;
    }

    @Override
    public String name() {
        return  this.getSelfAgent().getAgentNumberString();
    }

    @Override
    public ObjectInstance copyWithName(String objectName) {
        return null;
    }

    @Override
    public MutableState set(Object variableKey, Object value) {
        String key = (String) variableKey;
        if(WildfireAnonymousState.SELF_AGENT.equals(key)){
            this.setSelfAgent((Agent)value);
        }else if(WildfireAnonymousState.ANM_AGENT_GROUP.equals(key)){
            this.setanonymousAgents((String[])((ArrayList<String>)value).toArray());
        }else if(WildfireAnonymousState.FIRE_LIST.equals(key)){
            this.setFireList((Fire[])((ArrayList<Fire>)value).toArray());
        }else{
            //DO nothing.
        }//end if-else.

        return  this;
    }

    @Override
    public List<Object> variableKeys() {
        return WildfireAnonymousState.VARIABLE_KEYS;
    }

    @Override
    public Object get(Object variableKey) {
        String key = (String) variableKey;

        if(WildfireAnonymousState.SELF_AGENT.equals(key)){
            return this.getSelfAgent();
        }else if(WildfireAnonymousState.ANM_AGENT_GROUP.equals(key)){
            return new ArrayList<String>(Arrays.asList(this.anonymousAgentGroups));
        }else if(WildfireAnonymousState.FIRE_LIST.equals(key)){
            return  new ArrayList<Fire>(Arrays.asList(this.fireList));
        }else{
            return  null;
        }//end if-else.
    }

    /**
     * This method creates a copy of the current object and creates a new object.
     * @return is the copied new WildfireAnonymousState object.
     */
    public State copy() {
        return new WildfireAnonymousState(this);
    }

    /**
     * This method converts the whole state into a string object and prints it.
     * It intrinsically calls all the toString() methods of member bean classes.
     * @return is the string description of the state.
     */
    @Override
    public String toString() {
        String str =  "WildfireAnonymousState{" +
                ";selfAgent=" + selfAgent ;

        if(this.anonymousAgentGroups != null){
            for(String groups : this.anonymousAgentGroups){
                str+= "; anonymousAgents=" + groups;
            }
        }

        if(this.fireList != null){
            for(Fire f:this.fireList){
                str += ";Fire="+f.toString();
            }
        }

        str += "}";

        return  str;
    }

    /**
     * This method tries to compare two states, which is a general implementation of equals method.
     * TODO: Update the Objects.equal method to a more authentic one.
     * @param o is the WildfireObject to be compared.
     * @return true or false based on comparison.
     */
    @Override
    public boolean equals(Object o) {
        //Check if the objects are the same reference.
        if (this == o) return true;
        //Check if the object is of the WildfireAnonymousState.
        if (!(o instanceof WildfireAnonymousState)) return false;
        //Convert the object to WildfireAnonymousState and compare each members.
        WildfireAnonymousState wildfireAnonymousState = (WildfireAnonymousState) o;
        if(!(this.getSelfAgent().equals(wildfireAnonymousState.getSelfAgent()))){
            return  false;
        }

        //Compare each anonymous agent objects.
        for(int configs= 0; configs < this.anonymousAgentGroups.length ; configs++){
            if(!(this.anonymousAgentGroups[configs].equals(wildfireAnonymousState.getanonymousAgents()[configs]))){
                return false;
            }
        }

        //Compare each fire.
        for(int fire= 0; fire < this.anonymousAgentGroups.length ; fire++){
            if(!(this.fireList[fire].equals(wildfireAnonymousState.getFireList()[fire]))){
                return false;
            }
        }

        return  true;
    }//end method.

    /**
     * Find the index of the configuration of an agent by using type of the agent type and location frame.
     * @param agentType is the type of the agent.
     * @param location of the agent.
     * @return is the index of the agent.
     */
    public int search(int agentType, Location location,WildfireDomain domain) {
        int configIndex = -1; // -1 means not found.
        //Find the index of the configuration, which has the same action Type and
        //the same action.
        for(int config = 0 ; config < this.anonymousAgentGroups.length; config++){
            //Get the sample agent.
            Agent sampleAgent = domain.sampleAgentPerGroup.get(this.anonymousAgentGroups[config]);
            if(sampleAgent!= null
                    && sampleAgent.getPowerType() == agentType
                    && sampleAgent.getAgentLocation().equals(location)){
                configIndex = config;
                break;
            }
        }//end for.
        return  configIndex;
    }//end method.

    /**
     * The state index can be given as
     *index =   (Agent Supp Level) * (Max fire combinations) +  _Sum(Fires) ((Max_Fire_states)^Fire_Number  * Fire_Intensity)
     *E.G. for the state.
     *[WildfireAnonymousState{;selfAgent=Agent{agentNumber=0: agentLocation=Location{x=0: y=1}:
     // powerType=1: availability=0:agent group=0-9}; anonymousAgents=10-19;
     // Fire=Fire{fireNumber=0: fireLocation=Location{x=0: y=0}: intensity=3: fireType=2};
     // Fire=Fire{fireNumber=1: fireLocation=Location{x=1: y=1}: intensity=4: fireType=1};
     // Fire=Fire{fireNumber=2: fireLocation=Location{x=2: y=0}: intensity=1: fireType=2};
     // Fire=Fire{fireNumber=3: fireLocation=Location{x=2: y=2}: intensity=4: fireType=2}}]
     * index =  0 * (5^4) +   (5^0 * 3 + 5^1 * 4 + 5^2 * 1 + 5^3 * 4)
     * index = 548
     * @return hashcode of the state.
     */
    @Override
    public int hashCode() {
        int result = 0;
        //Suppressant term.
        result += this.getSelfAgent().getAvailability() *
                (int)Math.round(Math.pow(WildfireParameters.MAX_FIRE_STATES,this.getFireList().length));
        //Sum of the fire numbers- intensities.
        for(Fire fire: this.getFireList()){
            result +=   (int)Math.round(Math.pow(WildfireParameters.MAX_FIRE_STATES,fire.getFireNumber()))
                                    * fire.getIntensity();
        }//end for.
        return result;
    }
}//end class.
