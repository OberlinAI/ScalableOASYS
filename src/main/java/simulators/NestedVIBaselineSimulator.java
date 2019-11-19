package simulators;

import burlap.behavior.policy.Policy;
import burlap.behavior.stochasticgames.GameEpisode;
import burlap.mdp.auxiliary.common.NullTermination;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import common.OtherAgentsNMDPPolicy;
import common.POWorld;
import common.PlannedPOSGAgent;
import common.StateEnumerator;
import domains.wildfire.*;
import posg.POOOSGDomain;
import posg.model.FACRewardFunction;
import scalability.FrameActionConfiguration;


import java.io.*;
import java.util.*;


public class NestedVIBaselineSimulator {

    /**
     * Main method which generates the flow.
     * @param args arguments given in the following order.
     *             0: Configuration
     *             1: Number of Rounds to Play.
     *             2: Number of Trials
     *             3: Experiment Number.
     *
     */
    public static void main(String args[]){

        int configuration , experimentNumber ;
        int numberOfRounds, numberOfTrials,maxHorizons ;


        try{

            configuration = 14;
            numberOfRounds = 15;
            numberOfTrials = 30;
            experimentNumber = 143;
            maxHorizons = 10;
            boolean isFireRandom = false;
            boolean isSuppRandom = true;
            boolean isSuppOpen = true;


            //Get the parameters from the argument.
            if(args != null && args.length == 8){
                try{
                    configuration = Integer.parseInt(args[0]);
                    numberOfRounds = Integer.parseInt(args[1]);
                    numberOfTrials = Integer.parseInt(args[2]);
                    maxHorizons = Integer.parseInt(args[3]);
                    experimentNumber = Integer.parseInt(args[4]);
                    isFireRandom = Boolean.parseBoolean(args[5]);
                    isSuppRandom = Boolean.parseBoolean(args[6]);
                    isSuppOpen = Boolean.parseBoolean(args[7]);
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
            State ms = Wildfire.getInitialMasterState( wildfireDomain ,agentAvailability, isFireRandom,isSuppRandom,isSuppOpen);

            HashMap<String, Policy> agentGroupPolicyMap  = new HashMap<>();
            //Load Nested VI policies for all the Agent Groups, and create a hashmap for that.
            for(String agentGroup: wildfireDomain.agentGroups){
                int currentAgentNumber = wildfireDomain.sampleAgentPerGroup.get(agentGroup).getAgentNumber();
                int currentAgentType = wildfireDomain.sampleAgentPerGroup.get(agentGroup).getPowerType();

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



                //Get the policy of the agent group.
                Policy nestedVIPolicy = new OtherAgentsNMDPPolicy(wildfireDomain,
                                    agentGroup,OtherAgentsNMDPPolicy.PolicyType.Deterministic);

                String policyFileName = wildfireDomain.agentsList.size() +"-WF_config" + configuration + "_firestates" +  WildfireParameters.MAX_FIRE_STATES
                        +"_nmdp_policy_level0_agent" + currentAgentNumber
                        + "_neighbor" + agentGroup + "-" + currentAgentType + "_iter10.txt";

                ((OtherAgentsNMDPPolicy)nestedVIPolicy).readPolicy(policyFileHead + policyFileName,senum.numStatesEnumerated());
                agentGroupPolicyMap.put(agentGroup,nestedVIPolicy);
            }//end for.





            //Output File Paths.
            String exptHead =  outFileHead + "/NESTEDVI_BASELINE_";
            String suppressantResultsFile = "Suppressants"+ "_Config"+ configuration+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials+".csv";
            String rewardResultsFile = "Rewards"+ "_Config"+ configuration+"_maxStages"+numberOfRounds+"_maxTrials"+ numberOfTrials +".csv";
            String actionResultsFile = "JointActions"+ "_Config"+ configuration+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials+".csv";
            String fireResultsFile = "Fires"+ "_Config"+ configuration+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials+".csv";
            String overallResultsFile = "OVERALL"+ "_Config"+ configuration+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials+".csv";
            String analyticsResultsFile = "Analytics"+ "_Config"+ configuration+"_maxStages"+numberOfRounds+"_maxTrials"+numberOfTrials +".csv";

            POWorld world = new POWorld(((POOOSGDomain) wildfireDomain),
                    ((JointRewardFunction)rewardFunction),
                    terminalFunction, wildfireDomain.getPartialObservationFunction(),
                    null,ms, rewardFunction);

            //Add all the agents to the domain with their policy and their type.
            for( int agentIndex = 0;agentIndex < wildfireDomain.agentsList.size(); agentIndex++){
                //Get the group type of the current agent.
                String agentGroup = wildfireDomain.agentsList.get(agentIndex).getAgentGroup();

                //Create an agent with the Nested VI policy.
                SGAgent agent = new PlannedPOSGAgent(agentIndex+"",
                        new SGAgentType(agentGroup,
                        wildfireDomain.actionsPerGroup.get(agentGroup)),
                        agentGroupPolicyMap.get(agentGroup),
                        rewardFunction);

                //Agent joins the world.
                world.join(agent);
            }//end for.


            //Content Strings.
            String suppressantContent = "";
            String rewardContent = "";
            String actionContent = "";
            String fireContent = "";
            String overallContent = "";
            String analyticContent = "";



            //Play game for numberOfTrials times(episodes), and each game consist of numberOfRounds rounds.
            //Save information after wards.
            for(int trial = 1; trial <=numberOfTrials ; trial++){
                System.out.println("Trial:" + trial);
                GameEpisode episode = world.runGame(numberOfRounds,ms, WildfireParameters.NMDP_EXPERIMENT);

                //Generate the master state again.
                ms = Wildfire.getInitialMasterState( wildfireDomain ,agentAvailability, isFireRandom,isSuppRandom,isSuppOpen);

                //Gather the information dump.
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

                String overallTrail = SimulatorHelper.saveAllResultsToFile(episode,trial);
                overallContent += overallTrail;

                //Dump Analytical Results.
                String analyticTrial = SimulatorHelper.analyticsWithLocation(episode,wildfireDomain,trial);
                analyticContent += analyticTrial;
            }//end for.


            SimulatorHelper.writeOutputToFile(suppressantContent, exptHead+suppressantResultsFile);
            SimulatorHelper.writeOutputToFile(rewardContent, exptHead+rewardResultsFile);
            SimulatorHelper.writeOutputToFile(actionContent, exptHead+actionResultsFile);
            SimulatorHelper.writeOutputToFile(fireContent, exptHead+fireResultsFile);
            SimulatorHelper.writeOutputToFile(overallContent, exptHead+overallResultsFile);
            SimulatorHelper.writeOutputToFile(analyticContent, exptHead+analyticsResultsFile);
            System.out.println("---DONE!!--");
        }catch (Exception e){
            System.out.println("Error:" + e.getMessage());
            e.printStackTrace();
        }//end try-catch.
    }//end method.


}
