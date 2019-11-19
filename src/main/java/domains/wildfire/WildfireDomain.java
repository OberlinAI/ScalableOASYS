package domains.wildfire;

import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.ActionType;
import domains.wildfire.beans.Agent;
import domains.wildfire.beans.Fire;
import domains.wildfire.beans.Location;
import posg.POOOSGDomain;

import java.util.*;

public class WildfireDomain extends POOOSGDomain {
    //List of the static class variables, which would be used throughout the operations and would be the same.
    /** X- dimension of the premise. (Width of the premise) */
    public static int premiseX;
    /** Y - dimension of the premise (Height of the premise)*/
    public static int premiseY;

    /**
     * Agents in the premise with their initial configuration.
     * Only the availability of the agent changes over the time.
     */
    public  ArrayList<Agent> agentsList;

    /**
     * Locations where agents reside.
     */
    public ArrayList<Location> agentLocations;


    /**
     * Fires in the premise with their initial configuration.
     */
    public  ArrayList<Fire> fireList;

    /**
     * Agent-Fire Neighbourhood can be defined as the agents==> fires mapping.
     * The Hash map sets key as the agents in the same group from configuration. e.g. agent number 0-99.
     * The value ArrayList against that key defines the fires represented in form of fire index.
     */
    public  HashMap<String,ArrayList<Integer>> agentFireNeighbourhood;
    /**
     * Agent Neighbourhood can be defined as the agents who can fight the same fire.
     * The Hash map sets key as the agents in the same group from configuration. e.g. agent number 0-99.
     * The inner ArrayList also defines the agents group list, who are neighbours of the current agents. e.g.{100-199}
     */
    public  HashMap<String,ArrayList<String>> agentsNeighbourhood;

    /**
     * The visible fires for an agent are  all the fires which the agent or its neighbours can see.
     * The Hash map sets key as the agents in the same group from configuration. e.g. agent number 0-99.
     * The value ArrayList against that key defines the fires represented in form of fire index.
     */
    public  HashMap<String,ArrayList<Integer>> agentVisibleFires;
    /**
     * Available types for the agents, which defines power of the agent.
     */
    public  ArrayList<Integer> agentPowerTypes;
    /**
     * Contains a map of power value for the type.Corresponds to the agentPowerTypes index for the related type.
     */
    public ArrayList<Double> agentPower;
    /**
     * List of agent groups in the domain.
     */
    public ArrayList<String> agentGroups;

    /**
     * Available types for the agents, which defines power of the agent.
     */
    public  ArrayList<Integer> fireTypes;


    /**
     * Sample Agent for the group.
     */
    public Map<String,Agent> sampleAgentPerGroup;
    /**
     * Actions available for the whole group.
     */
    public Map<String, List<ActionType>> actionsPerGroup;



    /**
     * Default Constructor.
     */
    public WildfireDomain() {
        super();

        //Initiate all the static array list.
        this.agentsList = new ArrayList<Agent>();
        this.fireList = new ArrayList<Fire>();
        this.agentFireNeighbourhood = new LinkedHashMap<>();
        this.agentsNeighbourhood = new LinkedHashMap<>();
        this.agentVisibleFires = new LinkedHashMap<>();
        this.agentPowerTypes = new ArrayList<Integer>();
        this.agentGroups = new ArrayList<>();
        this.agentPowerTypes = new ArrayList<Integer>();
        this.agentPower = new ArrayList<Double>();
        this.sampleAgentPerGroup = new  LinkedHashMap<>();
        this.actionsPerGroup = new LinkedHashMap<>();
        this.agentLocations = new ArrayList<>();
        this.fireTypes = new ArrayList<Integer>();

        //Initialize the Power-types available for the agent from 1 to Max power.
//        for(int power = 1; power<= WildfireParameters.MAX_POWER; power++ ){
//            agentPowerTypes.add(power);
//        }//end for.

    }//end constructor.

    /**
     * This method returns the number of agents in a group.
     * @param agentGroup is the group name.
     * @return is the number of agents in that group.
     */
    public static int getAgentCountInGroup(String agentGroup){
        ArrayList<String> startEndIndexes =new ArrayList<>(Arrays.asList(agentGroup.split("-")));
        //Separate indexes.
        int startIndex = Integer.parseInt(startEndIndexes.get(0));
        int endIndex = Integer.parseInt(startEndIndexes.get(1));
        return (endIndex-startIndex) + 1;
    }

}//end class.
