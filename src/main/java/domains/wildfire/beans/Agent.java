/**
 *  {@link domains.wildfire.beans.Agent}  is the class to describe the fire in the premise.
 *  The Fire is described in the form of Fire number, Location and intensity.
 *  This class is being used further to describe WildFire state.
 * @author : Maulik Shah.
 */
package domains.wildfire.beans;


import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.State;
import domains.wildfire.WildfireAnonymousState;
import domains.wildfire.WildfireParameters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Agent implements Serializable,ObjectInstance{
    /**
     * Describes the agent-number of the agent in the premise.
     */
    private int agentNumber;
    /**
     * Describes the agent-Location of the agent in the premise by Location object.
     */
    private Location agentLocation;
    /**
     * Agent's fire fighting power. Depends on the type of agent.
     */
    private int powerType;
    /**
     * If agent is available to fight the fire or refilling fuel.
     * Kept it integer to add more state for this describing the feature.
     */
    private int availability;
    /**
     * The group that agent belong. e.g Agents with agent number 0-99, which shares the same location and type.
     * agentGroup would be 0-99.
     */
    private String agentGroup;


    /**
     * Class name and variable keys for the state class.
     */
    public static final String AGENT_CLASS = "AGENT_CLASS";
    private static final String AGENT_NUM = "AGENT_NUM";
    private static final String AGENT_LOC = "AGENT_LOC";
    private static final String POWER_TYPE = "POWER_TYPE";
    private static final String AVL = "AVL";
    private static final String AGENT_GROUP = "AGENT_GROUP";
    private static final List<Object> VARIABLE_KEYS = Arrays.<Object>asList(AGENT_NUM, AGENT_LOC, POWER_TYPE,AVL,AGENT_GROUP);




    /**
     * Default constructor - applies -1 to every object.
     */
    public Agent() {
        this.agentNumber = -1;
        this.agentLocation = new Location();
        this.powerType = 0;
        this.availability = WildfireParameters.MAX_SUPPRESSANT_STATES -1;
        this.agentGroup = "";
    }
    /**
     * Primary Constructor to initiate the fire.
     * @param agentNumber is the index of the agent in the premise.
     * @param agentLocation is the location of the agent in the premise.
     * @param powerType is the fire-fighting powerType of the particular agent.
     * @param availability is the agent's availability.
     */
    public Agent(int agentNumber, Location agentLocation, int powerType, int availability,String agentGroup) {
        this.agentNumber = agentNumber;
        this.agentLocation = agentLocation;
        this.powerType = powerType;
        this.availability = availability;
        this.agentGroup = agentGroup;
    }


    /**
     * This constructor creates a new Agent object from the passed Agent object.
     * @param agent is the agent object to be copied into the new one.
     */
    public Agent(Agent agent) {
        this.agentNumber = agent.getAgentNumber();
        this.agentLocation = new Location(agent.getAgentLocation());
        this.powerType = agent.getPowerType();
        this.availability = agent.getAvailability();
        this.agentGroup = agent.getAgentGroup();
    }

    //Getter and Setter methods.

    public int getAgentNumber() {
        return agentNumber;
    }

    //To use the agent-number as agent name.
    public String getAgentNumberString() {
        return agentNumber+"";
    }

    public void setAgentNumber(int agentNumber) {
        this.agentNumber = agentNumber;
    }

    public Location getAgentLocation() {
        return agentLocation;
    }

    public void setAgentLocation(Location agentLocation) {
        this.agentLocation = agentLocation;
    }

    public int getPowerType() {
        return powerType;
    }

    public void setPowerType(int powerType) {
        this.powerType = powerType;
    }

    public int getAvailability() {
        return availability;
    }

    public void setAvailability(int availability) {
        this.availability = availability;
    }

    public String getAgentGroup() {
        return agentGroup;
    }

    public void setAgentGroup(String agentGroup) {
        this.agentGroup = agentGroup;
    }

    /**
     * This method will generate Agent class as String.
     * output: e.g. Agent{agentNumber=12,agentLocation=Location{x=1,y=2},powerType=2,availability=1}.
     * It also uses Location.toString() method intrinsically.
     * @return the Fire string.
     */
    @Override
    public String toString() {
        return "Agent{" +
                "agentNumber=" + agentNumber +
                ": agentLocation=" + agentLocation +
                ": powerType=" + powerType +
                ": availability=" + availability +
                ":agent group=" + agentGroup +
                '}';
    }


    /**
     * This method overrides the general equal method for comparison.
     * @param o is the object to compare with current object.
     * @return true or false based on the comparison.
     */
    @Override
    public boolean equals(Object o) {
        //Check if the passed object is the current object.
        if (this == o) return true;
        //Check if the passed object is instance of Agent or not.
        if (!(o instanceof Agent)) return false;
        //Convert the object to Agent object and compare each member.
        Agent agent = (Agent) o;
        return this.agentNumber == agent.agentNumber &&
                this.powerType == agent.powerType &&
                this.availability == agent.availability &&
                this.agentLocation.equals(agent.agentLocation) &&
                this.agentGroup.equals(agent.getAgentGroup()) ;
    }


    @Override
    public String className() {
        return Agent.AGENT_CLASS;
    }

    @Override
    public String name() {
        return this.getAgentNumberString();
    }

    @Override
    public ObjectInstance copyWithName(String objectName) {
        return null;
    }

    @Override
    public List<Object> variableKeys() {
        return Agent.VARIABLE_KEYS;
    }

    @Override
    public Object get(Object variableKey) {
        String key = (String) variableKey;

        if(Agent.AGENT_NUM.equals(key)){
            return this.getAgentNumber();
        }else if(Agent.AGENT_GROUP.equals(key)){
            return this.AGENT_GROUP;
        }else if(Agent.AGENT_LOC.equals(key)){
            return  this.agentLocation;
        }else if(Agent.AVL.equals(key)){
            return  this.availability;
        }else if(Agent.POWER_TYPE.equals(key)){
            return  this.powerType;
        }else{
            return  null;
        }//end if-else.
    }

    @Override
    public State copy() {
        return new Agent(this);
    }

    @Override
    public int hashCode() {

        return Objects.hash(agentNumber, agentLocation, powerType, availability, agentGroup);
    }
}
