/**
 * This class represents the Wildfire State, which includes fires, agents and fire observations.
 * @author: Maulik Shah
 */

package domains.wildfire;



import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.MutableState;
import java.util.*;

import burlap.mdp.core.state.State;
import domains.wildfire.beans.*;

public class WildfireState implements ObjectInstance,MutableState {

    /**
     * Current agent.
     */
    private Agent selfAgent;


    /**
     * List of the agents in the premise with their information, with the agent number as the key.
     */
    private ArrayList<Agent> agentList;

    /**
     * List of the fires in the premise with their information, with the fire number as the key.
     */
    private ArrayList<Fire> fireList;

    /**
     * Class name and variable keys for the state class.
     */
    public static final String WILDFIRE_CLASS_NAME = "WILDFIRE_STATE";
    private static final String SELF_AGENT = "SELF_AGENT";
    private static final String AGENT_LIST = "AGENT_LIST";
    private static final String FIRE_LIST = "FIRE_LIST";
    private static final List<Object> VARIABLE_KEYS = Arrays.<Object>asList(SELF_AGENT, AGENT_LIST, FIRE_LIST);

    /**
     * Default Constructor : Initiate dimensions to -1,-1 and the List of Agents/Fires/Observation to new lists.
     */
    public WildfireState() {
        this.selfAgent = new Agent();
        this.agentList = new ArrayList<>();
        this.fireList = new ArrayList<>();
    }

    /**
     * Primary Constructor : Creates a state objects given all the available parameters.
     * @param agentList is the agents in the premise.
     * @param fireList is the fires in the premise.
     */
    public WildfireState(Agent selfAgent,ArrayList<Agent> agentList, ArrayList<Fire> fireList) {
        this.selfAgent = selfAgent;
        this.agentList = agentList;
        this.fireList = fireList;
    }


    /**
     * This constructor creates a new WildfireState object from the passed similar class object.
     * @param wildfireState is the WildFireState object to be copied into the new one.
     */
    public WildfireState(WildfireState wildfireState) {
        this.selfAgent = new Agent(wildfireState.getSelfAgent());

        //Copy Agents.
        this.agentList = new ArrayList<>();
        for(Agent agent:wildfireState.getAgentList()){
            this.agentList.add(new Agent(agent));
        }

        //Copy Fires.
        this.fireList = new ArrayList<>();
        for(Fire fire : wildfireState.getFireList()){
            this.fireList.add(fire);
        }//end for.
    }

    //Getter and Setter Methods.

    public ArrayList<Agent> getAgentList() {
        return agentList;
    }

    public void setAgentList(ArrayList<Agent> agentList) {
        this.agentList = agentList;
    }

    public ArrayList<Fire> getFireList() {
        return fireList;
    }

    public void setFireList(ArrayList<Fire> fireList) {
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
        return WildfireState.WILDFIRE_CLASS_NAME;
    }

    @Override
    //Just putting down.
    public String name() {
        return this.getSelfAgent().getAgentNumberString();
    }

    @Override
    public ObjectInstance copyWithName(String objectName) {
        return null;
    }

    @Override
    public MutableState set(Object variableKey, Object value) {
        String key = (String) variableKey;
        if(WildfireState.SELF_AGENT.equals(key)){
             this.setSelfAgent((Agent)value);
        }else if(WildfireState.AGENT_LIST.equals(key)){
             this.setAgentList((ArrayList<Agent>)value);
        }else if(WildfireState.FIRE_LIST.equals(key)){
            this.setFireList((ArrayList<Fire>)value);
        }else{
            //DO nothing.
        }//end if-else.

        return  this;
    }

    @Override
    public List<Object> variableKeys() {
        return WildfireState.VARIABLE_KEYS;
    }

    @Override
    public Object get(Object variableKey) {
        String key = (String) variableKey;

        if(WildfireState.SELF_AGENT.equals(key)){
            return this.getSelfAgent();
        }else if(WildfireState.AGENT_LIST.equals(key)){
            return this.getAgentList();
        }else if(WildfireState.FIRE_LIST.equals(key)){
            return this.getFireList();
        }else{
            return  null;
        }//end if-else.
    }

    /**
     * This method creates a copy of the current object and creates a new object.
     * @return is the copied new WildfireState object.
     */
    public State copy() {
        return new WildfireState(this);
    }

    /**
     * This method converts the whole state into a string object and prints it.
     * It intrinsically calls all the toString() methods of member bean classes.
     * @return is the string description of the state.
     */
    @Override
    public String toString() {
        return "WildfireState{" +
                ",selfAgent=" + selfAgent +
                ", agentList=" + agentList +
                ", fireList=" + fireList +
                '}';
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
        //Check if the object is of the WildfireState.
        if (!(o instanceof WildfireState)) return false;
        //Convert the object to WildfireState and compare each members.
        WildfireState wildfireState = (WildfireState) o;
        return  this.getSelfAgent().equals(wildfireState.getSelfAgent()) &&
                Objects.equals(agentList, wildfireState.agentList) &&
                Objects.equals(fireList, wildfireState.fireList) ;
    }

}
