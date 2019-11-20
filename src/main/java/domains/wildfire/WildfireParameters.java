package domains.wildfire;


import java.util.ArrayList;
import java.util.Arrays;

/**
 * The {@link WildfireParameters} class defines the parameters and constants used in
 * the wildfire domain.
 * TODO : Update the constants to be read from a text file.
 * @author Adam Eck
 * @author  Maulik Shah - Scalable OASYS.
 */
public class WildfireParameters {

    //Wildfire Domain Constants.

    //1. Parameter set to be used to impose different limits
    //over fire,agents and observations.

    /**The number of fire states to use. */
    public static int MAX_FIRE_STATES;
    /**The number of suppressant levels to use.
    Full,Half-full,Empty. */
    public static int MAX_SUPPRESSANT_STATES;
    /** The intensity difference for the observation levels. */
    public static final int OBS_DIFF = 1;
    /** The maximum range of an agent to fight a fire. */
    public static final int MAX_AGENT_RANGE = 1;
    /** Extra Agent Fire Power */
    public static final int MAX_FIRE_REDUCTION = -1;

    // 4. Other parameters.
    public static final double FIRE_MULTIPLIER = 2.0;

    /**The amount of noise to add to observations. */
    public static final double OBSERVATION_NOISE = 0.1;
    /** The penalty for trying to fight a non-existent fire. */
    public static final double NO_FIRE_PENALTY_CURRENT = -10.0;
    /** Fire extinguishing reward for small fire. */
    public static final double NO_FIRE_REWARD_SMALL = 20.0;
    /** Fire extinguishing reward for big fire. */
    public static final double NO_FIRE_REWARD_BIG = 40.0;
    /** Fire extinguishing reward for very big fire. */
    public static final double NO_FIRE_REWARD_VB = 60.0;
    /** The penalty when the fire is burned out. */
    public static final double FIRE_BURNOUT_PENALTY = -1.0;
    /** The value of AGENT_NUM for the master state (that doesn't belong to any particular agent). */
    public static final int MASTER_STATE_AGENT_NUM = -1;

    //Action Definition.
    //Rest of the Actions can be defined dynamically after setting fire
    //and agents in the domain.

    /** Action - No operation */
    public static final String NOOP_ACTION = "NOOP";

    /** Action - No observation intensity */
    public static final int NO_OBS = 3; //Quick Fix value : 3


    //2. Fire Spreading Parameters.
    /** The true probability that the suppressant level decreases after each non-NOOP action. */
    public static final double TRUE_DISCHARGE_PROB = 0.75;
    /** The average amount of time an requires for charging. */
    public static final int TRUE_CHARGING_TIME = 2;
    /** The size of a cell (in meters). */
    public static final double CELL_SIZE = 200.0;
    /** Fire Reduction Probability. */
    public static final double FIRE_REDUCTION_PROB = 0.75;
    /** Extra Agent Prob increase. */
    public static final double EXTRA_AGENT_FIRE_REDUCTION_PROB = 0.075 / FIRE_MULTIPLIER;
    /** Extra Agent Fire Power */
    public static final double EXTRA_AGENT_FIRE_POWER = 0.1;



    /** Big fire type number for the fire type parameter */
    public static final int BIG_FIRE = 1;
    /** Big fire type number for the fire type parameter */
    public static final double BIG_FIRE_THRESHOLD = 1.0 * FIRE_MULTIPLIER;

    /** Small fire type number for the fire type parameter */
    public static final int SMALL_FIRE = 2;
    /** Big fire type number for the fire type parameter */
    public static final double SMALL_FIRE_THRESHOLD = 0.5 * FIRE_MULTIPLIER;
    /** Small fire type number for the fire type parameter */
    public static final double SMALL_FIRE_TRANS_PROB = 0.075 / FIRE_MULTIPLIER;
    /** Small fire type number for the fire type parameter */
    public static final double SMALL_AGENT_FIRE_POWER = 0.1;


    /** Big fire type number for the fire type parameter */
    public static final int VERY_BIG_FIRE = 3;
    /** Big fire type number for the fire type parameter */
    public static final double VERY_BIG_FIRE_THRESHOLD = 1.5 * FIRE_MULTIPLIER;
    /** Small fire type number for the fire type parameter */
    public static final double VB_FIRE_TRANS_PROB = 0.075 / FIRE_MULTIPLIER;
    /** Small fire type number for the fire type parameter */
    public static final double VB_AGENT_FIRE_POWER = 0.1;


    /**The default wind direction.
     Note: 0 = North, PI = SOUTH, 1/2 PI = EAST, 3/2 PI = WEST */
    public static final double WIND_DIRECTION = 0.25 * Math.PI;

    /**The initial, random spread probability for dealing with location outside the frontier.
      This breaks domains.WildfireMechanics.actionHelper when RANDOM_SPREAD_PROB = 1, 0.5; */
    public static final double RANDOM_SPREAD_PROB = 0.05;


    //Constants for experiment types.
    public static final String NOOP_EXPERIMENT = "NOOP";
    public static final String IPOMCP_EXPERIMENT = "IPOMCP";
    public static final String HEURISTIC_EXPERIMENT = "HEURISTIC";
    public static final String NMDP_EXPERIMENT = "NMDP";


    //----
    //End of Constants.



    /** Configuration code for the premise. To be set run time to set the premise for the experiment. */
    public static int config;
	/** Agent number counter.*/
    public static int agentIndex;
    /** Fire number counter.*/
    public static int fireIndex;


    /** 
     * The structure of the configuration strings is be as below:
     * XY-<x>-<y> defines the length of X and Y axis of the experimental premise.
     * A-<agents>-<x>-<y>-<agent type> defines the number of agents at ```<x>-<y>``` location of the
     * <agent type> type. In the AAAI-paper experiments, the type `1` agents are ground fire fighters,
     * where as type `2` agents are helicopters.
     *  F-<x>-<y>-<fire type> defines the fire location <x>-<y> and the fire type.
     *  The constants related to the fire type are defined at the beginning of the class.
     * P-<agent type>-<agent extinguishing power> defines the extinguishing power of each agent type.
     * In the AAAI-paper experiments, the ground firefighters have extinguishing power of 0.1,
     * where as the helicopters have it of 0.2.
     * MFS-<maximum fire states> defines the maximum number of fire states of each fire.
     * Here, the 0 fire intensity indicates an extinguished fire, while  <maximum fire state> - 1 indicates a burnt location.
     * MSS-<maximum agent states> defines the agent's suppressant level.
     * Here a 0 indicates an empty suppressant level, in which the agents' actions become ineffective.
     */
    public static ArrayList<String> configStrings;


    /**
     * Configuration String for the current setting.
     */
    public static String configString;

    /**
     * Initiate the static variable arrays.
     */
    static {
        //Set the indexes to 0.
        agentIndex =0;
        fireIndex = 0;

         //Instantiate configurations.
        //TODO: Put the configurations in a text file and read from it.
        configStrings = new ArrayList<String>();
        configStrings.add("XY-3-3,A-20-0-1-1,A-20-2-1-1,F-0-0-2,F-1-1-1,F-2-0-2,F-2-2-2,P-1-0.1,MFS-5,MSS-3");
        configStrings.add("XY-3-3,A-20-0-1-1,A-20-2-1-1,F-0-0-2,F-1-1-1,F-1-2-2,F-2-0-2,P-1-0.1,MFS-5,MSS-3");
        configStrings.add("XY-3-3,A-15-0-1-1,A-15-1-2-1,A-15-2-1-1,F-0-2-1,F-1-0-1,F-2-2-1,P-1-0.1,MFS-5,MSS-3");
        configStrings.add("XY-3-3,A-30-0-1-1,A-20-2-1-2,F-0-0-1,F-1-1-3,F-2-0-1,F-2-2-1,P-1-0.1,P-2-0.2,MFS-5,MSS-3");
        configStrings.add("XY-3-3,A-30-0-1-1,A-20-2-1-2,F-0-0-1,F-1-1-3,F-1-2-1,F-2-0-1,P-1-0.1,P-2-0.2,MFS-5,MSS-3");
    }




    /**
     * The constructor initializes the game configuration parameters(configuration, numAgents(number of agents), premiseX(width of the premise)
     * , primise Y (height of the premise) given the configuration code.
     * @param config is the configuration code.
     */
	public WildfireParameters(int config) {
	    //Copy the argument to the Parameter variable.
        WildfireParameters.config = config;

        //Check if the configuration is in the range of the actual configuration.
        //Set the configuration given the configuration setting.
        if(config >=0 && config < configStrings.size()){
            configString = configStrings.get(config);
            System.out.println("Configuration Setting:" + configString);
		}else{
			System.out.println("Invalid Configuration is set!!");
			System.exit(0);
		}//end if-else if -else.


        //Set the Config String.
        setMaxFireSuppStates();
	}//end Constructor.


    /**
     * This method finds the maximum number of fire states and suppressant states from the configuration string.
     */
    private void setMaxFireSuppStates(){
        ArrayList<String> configList = new ArrayList<>(Arrays.asList(WildfireParameters.configString.split(",")));
        //For each line in configuration.
        for(String config: configList) {
            if (config.startsWith("MFS")) {
                ArrayList<String> fireStates =  new ArrayList<> (Arrays.asList(config.split("-")));
                WildfireParameters.MAX_FIRE_STATES = Integer.parseInt(fireStates.get(1));
            }
            if (config.startsWith("MSS")) {
                ArrayList<String> suppStates =  new ArrayList<> (Arrays.asList(config.split("-")));
                WildfireParameters.MAX_SUPPRESSANT_STATES = Integer.parseInt(suppStates.get(1));
            }
        }//end for.
    }//end method.
}//end class.
