package simulators;

import burlap.behavior.policy.Policy;
import burlap.behavior.stochasticgames.GameEpisode;
import burlap.mdp.auxiliary.common.NullTermination;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import common.*;
import domains.wildfire.*;
import pomcp.IPOMCP;
import posg.POOOSGDomain;
import posg.model.FACRewardFunction;
import posg.model.FullJointIPOMCPModel;
import scalability.FrameActionConfiguration;

import java.io.*;
import java.util.*;

public class IPOMCPSimulator {

    /**
     * Main method which generates the flow.
     * @param args arguments given in the following order.
     *             0: Configuration
     *             1: Gamma
     *             2: Epsilon
     *             3: Maximum Tree Height or Maximum horizon.
     *             4: Number of Rounds to Play.
     *             5: Maximum time for choosing a decision in mili-seconds.
     *             6: Branching Factor.
     *             7: Experiment Number.
     *             8: Maximum Sampling Error  ep
     *             9: Confidence Interval for the Agent sampling.
     *             10: Number of Samples multiple to number of states.
     *             11: Bandit Constant.
     */
    public static void main(String args[]){

        int configuration = 0, experimentNumber = 1;
        double gamma,epsilon,samplingErrorBound, alpha,C;
        int maxHorizons,numberOfRounds, numberOfTrials ,timeBound,particleCount;
        boolean isFireRandom = false;
        boolean isSuppRandom = true;
        boolean isAvgQActionNode = false;
        boolean isSuppOpen = true;
        boolean isSingleConfigNode = true;
        int trialSeries;


        try{

            configuration = 15;
            gamma = 0.9;
            epsilon = 0.001;
            maxHorizons = 10;
            numberOfRounds = 15;
            numberOfTrials = 3;
            timeBound = 1000;
            experimentNumber = 151;
            samplingErrorBound = 0.00001;
            alpha = (1 - 0.95);
            particleCount = 500;
            C = 80;
            trialSeries=0;


            //Get the parameters from the argument.
            //The 15 argument insert is just a printing tree trial.
            if(args != null && args.length == 17){
                try{
                    System.out.println("Print Tree Trial with 5 stages.");
                    configuration = Integer.parseInt(args[0]);
                    gamma = Double.parseDouble(args[1]);
                    epsilon = Double.parseDouble(args[2]);
                    maxHorizons = Integer.parseInt(args[3]);
//                    numberOfRounds = Integer.parseInt(args[4]);
//                    numberOfTrials = Integer.parseInt(args[5]);
                    timeBound = Integer.parseInt(args[6]);
                    experimentNumber = Integer.parseInt(args[7]);
                    samplingErrorBound = Double.parseDouble(args[8]);
                    alpha =  Double.parseDouble(args[9]);
                    particleCount = Integer.parseInt(args[10]);
                    C = Integer.parseInt(args[11]);
                    isFireRandom = Boolean.parseBoolean(args[12]);
                    isSuppRandom = Boolean.parseBoolean(args[13]);
                    isAvgQActionNode = Boolean.parseBoolean(args[14]);
                    isSuppOpen = Boolean.parseBoolean(args[15]);
                    isSingleConfigNode = Boolean.parseBoolean(args[16]);
                    trialSeries=0;
                    numberOfRounds = 5;
                    numberOfTrials = 1;
                }catch (NumberFormatException ne){
                    System.out.println("Problem in the arguments." + ne.getMessage());
                    System.exit(0);
                }//end try-catch.
            }if(args != null && args.length == 18){
                try{
                    configuration = Integer.parseInt(args[0]);
                    gamma = Double.parseDouble(args[1]);
                    epsilon = Double.parseDouble(args[2]);
                    maxHorizons = Integer.parseInt(args[3]);
                    numberOfRounds = Integer.parseInt(args[4]);
                    numberOfTrials = Integer.parseInt(args[5]);
                    timeBound = Integer.parseInt(args[6]);
                    experimentNumber = Integer.parseInt(args[7]);
                    samplingErrorBound = Double.parseDouble(args[8]);
                    alpha =  Double.parseDouble(args[9]);
                    particleCount = Integer.parseInt(args[10]);
                    C = Integer.parseInt(args[11]);
                    isFireRandom = Boolean.parseBoolean(args[12]);
                    isSuppRandom = Boolean.parseBoolean(args[13]);
                    trialSeries = Integer.parseInt(args[14]);
                    isAvgQActionNode = Boolean.parseBoolean(args[15]);
                    isSuppOpen = Boolean.parseBoolean(args[16]);
                    isSingleConfigNode = Boolean.parseBoolean(args[17]);
                }catch (NumberFormatException ne){
                    System.out.println("Problem in the arguments." + ne.getMessage());
                    System.exit(0);
                }//end try-catch.
            }else{
               //Take the defaults.
            }//end if args.

            //File Names of the Policy files.
            String policyFileHead = "output/Simulations/Config_" + configuration + "/Experiment_" + experimentNumber  + "/";

            //State File Name.
            String outFileHead = "output/Simulations/Config_"+configuration +"/Experiment_"+ experimentNumber+"/";


            //Initiate the domain parameters.
            Wildfire wildfire = new Wildfire();
            WildfireDomain wildfireDomain = (WildfireDomain) wildfire.generateDomain(configuration,false);
            FullJointIPOMCPModel jointIPOMCPModel = new WildfireMechanics(wildfireDomain);
            FACRewardFunction rewardFunction = new WildfireRewardFunction();
            TerminalFunction terminalFunction = new NullTermination();
            wildfireDomain.setPartialObservationFunction(new FireDiscreteObservation());


            //Get agent definitions
            System.out.println("Getting all agent definitions..");
            List<Map<String, SGAgent>> allAgentDefinitions = Wildfire.getAllAgentDefs( wildfireDomain);
            Map<String,Map<String, SGAgent>> allAgentAnmDefinitions = Wildfire.getAllAnmAgentDefs( wildfireDomain );


            //Get initial master state and master anonymous state.
            //Set the Suppressant levels from the beginning.
            //Agent can have [0, WildfireParameters.MAX_SUPPRESSANT_STATES - 1] availability
            //states.
            int agentAvailability = WildfireParameters.MAX_SUPPRESSANT_STATES - 1;
            State ms = Wildfire.getInitialMasterState(wildfireDomain ,agentAvailability,isFireRandom,isSuppRandom, isSuppOpen);

            HashMap<String, Policy> agentGroupPolicyMap  = new HashMap<>();
            HashMap<String, StateEnumerator> senumGroupMap  = new HashMap<>();
            HashMap<String, FrameActionConfiguration> maxConfigurations  = new HashMap<>();
            //Load Nested VI policies for all the Agent Groups, and create a hashmap for that.
            for(String agentGroup: wildfireDomain.agentGroups){
                int currentAgentNumber = wildfireDomain.sampleAgentPerGroup.get(agentGroup).getAgentNumber();
                int currentAgentType = wildfireDomain.sampleAgentPerGroup.get(agentGroup).getPowerType();

                wildfireDomain.sampleAgentPerGroup.get(agentGroup).getPowerType();
                //Get agent definition in the perspective of the current agent.
                Map<String, SGAgent> agentDefinitions = allAgentDefinitions.get(currentAgentNumber);
                Map<String, SGAgent> agentAnmDefinitions = allAgentAnmDefinitions.get(Wildfire.getAgentGroup( wildfireDomain ,currentAgentNumber));

                //Get the current agent's initial Anonymous state to generate enumerator.
                State initialState = Wildfire.createAgentStateFromMasterState( wildfireDomain , currentAgentNumber, ms);
                State initialAnmState = new WildfireAnonymousState((WildfireState)initialState, wildfireDomain );

                HashableStateFactory hf = new SimpleHashableStateFactory(true);
                StateEnumerator senum = new StateEnumerator( wildfireDomain , hf, agentDefinitions,agentAnmDefinitions);

                //Set the Maximum values for each frame-action pair for the given Anonymous initial state.
                FrameActionConfiguration possibleConfiguration = new FrameActionConfiguration();

                //If the maximum horizon is not set to a finite value then, go for it.
                //The method call finds the possible states, as well as initializes max configuration.
                if(maxHorizons > 0){
                    //Don't Exclude self agent's actions in FAC.
                    senum.findReachableAnmStatesAndEnumerate(initialAnmState,possibleConfiguration,maxHorizons,true);
                }else{
                    //Don't Exclude self agent's actions in FAC. -1 indicates infinite horizon.
                    senum.findReachableAnmStatesAndEnumerate(initialAnmState,possibleConfiguration, -1,true);//Put Horizon value as -1.
                }//end if-else.



                //A mapping of hashcode of state and state.
                Map<Integer,State> stateMapping = new HashMap<>();

                Set<HashableState> states= new HashSet<HashableState>();
                //Enumerate all the states.
                int nS = senum.numStatesEnumerated();
                for(int i=0;i<nS;i++){
                    if(i%1000 == 0){
                        System.out.print(".");
                    }
                    State st = senum.getStateForEnumerationId(i);

                    //Add it to state mapping.
                    stateMapping.put(((WildfireAnonymousState)st).hashCode(), st);

                    //Add to the hashcode thing.
                    HashableState hashedST = hf.hashState(st);
                    states.add(hashedST);
                }


                //State File Name.
                String stateFileName = outFileHead + "States_" + wildfireDomain.agentsList.size()
                        +"-WF_config"+ configuration +"_firestates"+WildfireParameters.MAX_FIRE_STATES
                        +"_current_agent"+currentAgentNumber +  "_Simulator.txt";

                //Write the states to the file as well as print.
                File stateFile;
                PrintWriter stateOut;
                try {
                    stateFile = new File(stateFileName);
                    stateFile.getParentFile().mkdirs();
                    if(!stateFile.exists()){
                        stateFile.createNewFile();
                    }
                    stateOut = new PrintWriter(new BufferedWriter(new FileWriter(stateFile.getAbsoluteFile())));
                    String output = "";

                    //Iterate through each available state.
                    for(HashableState s: states){
                        WildfireAnonymousState anm = (WildfireAnonymousState) s.s();
//                        System.out.println("State:" + anm);
                        output += anm.hashCode() + ":" +  anm;
                        output += "\n";
                    }
                    stateOut.println(output);
                    stateOut.close();
                }catch (IOException e) {
                    System.out.println("Error:" + e.getMessage());
                    e.printStackTrace();
                }//end try-catch.




                //Add State enumerator to the hashmap.
                senumGroupMap.put(agentGroup,senum);
                maxConfigurations.put(agentGroup,possibleConfiguration);


                //Get the policy of the agent group.
                Policy nestedVIPolicy = new OtherAgentsNMDPPolicy(wildfireDomain,
                        agentGroup,OtherAgentsNMDPPolicy.PolicyType.Deterministic);

                String policyFileName = wildfireDomain.agentsList.size() +"-WF_config" + configuration + "_firestates" +  WildfireParameters.MAX_FIRE_STATES
                                            +"_nmdp_policy_level0_agent" + currentAgentNumber
                                            + "_neighbor" + agentGroup + "-" + currentAgentType + "_iter10.txt";

                ((OtherAgentsNMDPPolicy)nestedVIPolicy).readPolicy(policyFileHead + policyFileName, senum.numStatesEnumerated());
                agentGroupPolicyMap.put(agentGroup,nestedVIPolicy);
            }//end for.




            //Output File Paths.
            String ipomcpHead = outFileHead + "/IPOMCP_";
            String suppressantResultsFile = "Suppressants" + "_Config"+ configuration+ "_Horizons" + maxHorizons+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials+"_BanditValue" + C + "_Trial" + trialSeries + ".csv";
            String rewardResultsFile = "Rewards" + "_Config"+ configuration+ "_Horizons" + maxHorizons +"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials+"_BanditValue" + C + "_Trial" + trialSeries+".csv";
            String actionResultsFile = "JointActions"+ "_Config"+ configuration+ "_Horizons" + maxHorizons+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials+"_BanditValue" + C + "_Trial" + trialSeries +".csv";
            String fireResultsFile = "Fires"+ "_Config"+ configuration+ "_Horizons" + maxHorizons+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials+"_BanditValue" + C + "_Trial" + trialSeries +".csv";
            String overallResultsFile = "OVERALL"+ "_Config"+ configuration+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials+".csv";
            String analyticsResultsFile = "Analytics"+ "_Config"+ configuration+ "_Horizons" + maxHorizons+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials+"_BanditValue" + C + "_Trial" + trialSeries +".csv";
            String predictionResultsFile = "Predictions"+ "_Config"+ configuration+ "_Horizons" + maxHorizons+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials+"_BanditValue" + C + "_Trial" + trialSeries +".csv";
            String configurationResultsFile = "Configuration"+ "_Config"+ configuration+ "_Horizons" + maxHorizons+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials+"_BanditValue" + C + "_Trial" + trialSeries +".csv";


            //Content Strings.
            String suppressantContent = "";
            String rewardContent = "";
            String actionContent = "";
            String fireContent = "";
            String overallContent = "";
            String analyticContent = "";


            //Play game for numberOfTrials times(episodes), and each game consist of numberOfRounds rounds.
            //Save information after wards.
            for(int trial = 0; trial <numberOfTrials ; trial++){
                double begin = System.currentTimeMillis();

                String ipomcpTreeFile = "IPOMCPTREE"+ "_Config"+ configuration+ "_Horizons" + maxHorizons+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials
                                                            +"_BanditValue" + C + "_TrialSeries" + trialSeries + "_TRIAL" + trial +".csv";
                String ipomcpParticleFile = "PARTICLES"+ "_Config"+ configuration+ "_Horizons" + maxHorizons+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials
                                                            +"_BanditValue" + C + "_TrialSeries" + trialSeries + "_TRIAL" + trial +".csv";
                System.out.println("Trial:" + trial);
                //Add Logs.
                String trialHead  = "Trial:" + trial + " \n";
                SimulatorHelper.addPrediction(trialHead);
                SimulatorHelper.addConfigInfo(trialHead);
                SimulatorHelper.addBeliefInfo(trialHead);
                SimulatorHelper.addTreeInfo(trialHead);
                SimulatorHelper.addParticleLog(trialHead);




                POWorld world = new POWorld(((POOOSGDomain) wildfireDomain),
                        ((JointRewardFunction)rewardFunction),
                        terminalFunction, wildfireDomain.getPartialObservationFunction(),
                        allAgentDefinitions,new WildfireState((WildfireState) ms), rewardFunction);

                //Add all the agents to the domain with their policy and their type.
                for( int agentIndex = 0;agentIndex < wildfireDomain.agentsList.size(); agentIndex++){
                    //Get the agent group.
                    String agentGroup = wildfireDomain.agentsList.get(agentIndex).getAgentGroup();

                    //Create an instance of the Maximum values for each frame-action pair for the given Anonymous initial state.
                    FrameActionConfiguration possibleConfiguration =
                            new FrameActionConfiguration(maxConfigurations.get(agentGroup),true);

                    //Get the current agent's initial Anonymous state to generate enumerator.
                    State initialState = Wildfire.createAgentStateFromMasterState( wildfireDomain , agentIndex, ms);
                    State initialAnmState = new WildfireAnonymousState((WildfireState)initialState, wildfireDomain );

                    //Create an IPOMCP solver object.
                    IPOMCP ipomcp = new IPOMCP(epsilon,gamma,C,maxHorizons,timeBound,particleCount,alpha,
                            samplingErrorBound,possibleConfiguration,agentGroupPolicyMap,
                            agentIndex,wildfireDomain.actionsPerGroup.get(agentGroup),
                            wildfire,wildfireDomain,rewardFunction,
                            jointIPOMCPModel,terminalFunction,senumGroupMap.get(agentGroup),
                            initialAnmState,ms,false, trial,trialSeries,isAvgQActionNode,isSuppOpen,isSingleConfigNode); //Last argument is for IPOMCP.


                    //Create a new policy based agent.
                    SGAgent agent = new IPOMCPAgent(agentIndex+"",
                            ipomcp);
                    world.join(agent);

                    System.out.println("Agent:" + agentIndex + " joined.");
                }//end for.


                //Get a new master state on each trial.
                ms = Wildfire.getInitialMasterState(wildfireDomain ,agentAvailability,isFireRandom,isSuppRandom,isSuppOpen);



                GameEpisode episode = world.runGame(numberOfRounds,ms,WildfireParameters.IPOMCP_EXPERIMENT);
                String suppressantTrial = SimulatorHelper.saveSuppressantResultsToFile(episode,trial);
                suppressantContent += suppressantTrial;
                //Dump rewards results to file
                String rewardTrial=SimulatorHelper.saveRewardResultsToFile(episode,trial);
                rewardContent += rewardTrial;
                //Dump actions results to file
                String actionTrial = SimulatorHelper.saveActionResultsToFile(episode,trial);
                actionContent += actionTrial;
                //Dump fires results to file
                String fireTrial = SimulatorHelper.saveFireResultsToFile(episode,trial);
                fireContent += fireTrial;

                //Overall Content.
                String overallTrail = SimulatorHelper.saveAllResultsToFile(episode,trial);
                overallContent += overallTrail;

                //Dump Analytical Results.
                String analyticTrial = SimulatorHelper.analyticsWithLocation(episode,wildfireDomain,trial);
                analyticContent += analyticTrial;

                if(trial < 1 && trialSeries <=1){
                    SimulatorHelper.writeOutputToFile(SimulatorHelper.ipomcpTreeLog.toString(), ipomcpHead+ipomcpTreeFile);
                    SimulatorHelper.writeOutputToFile(SimulatorHelper.particleLog.toString(), ipomcpHead+ipomcpParticleFile);
                    SimulatorHelper.ipomcpTreeLog = new StringBuilder("") ;
                    SimulatorHelper.particleLog = new StringBuilder("") ;
                }else{
                    SimulatorHelper.ipomcpTreeLog = new StringBuilder("") ;//To reduce memory use.
                    SimulatorHelper.particleLog = new StringBuilder("") ;
                }

                double end = System.currentTimeMillis();
                System.out.println("Trail " + trial + " Completed in Seconds: " + ((end-begin)/1000));
                System.out.println("Trail should have been completed in Seconds:" +
                        (wildfireDomain.agentsList.size() * numberOfRounds * timeBound / 1000));


                SimulatorHelper.writeOutputToFile(suppressantTrial, ipomcpHead+suppressantResultsFile);
                SimulatorHelper.writeOutputToFile(rewardTrial, ipomcpHead+rewardResultsFile);
                SimulatorHelper.writeOutputToFile(actionTrial, ipomcpHead+actionResultsFile);
                SimulatorHelper.writeOutputToFile(fireTrial, ipomcpHead+fireResultsFile);
                SimulatorHelper.writeOutputToFile(overallTrail, ipomcpHead+overallResultsFile);
                SimulatorHelper.writeOutputToFile(analyticTrial, ipomcpHead+analyticsResultsFile);


                SimulatorHelper.writeOutputToFile(SimulatorHelper.predictionLog, ipomcpHead+predictionResultsFile);
                SimulatorHelper.predictionLog = new String("");
                SimulatorHelper.writeOutputToFile(SimulatorHelper.configLog, ipomcpHead+configurationResultsFile);
                SimulatorHelper.configLog = new String("");
            }//end for.



        }catch (Exception e){
            System.out.println("Error:" + e.getMessage());
            e.printStackTrace();
        }//end try-catch.
    }//end method.
}//end class.
