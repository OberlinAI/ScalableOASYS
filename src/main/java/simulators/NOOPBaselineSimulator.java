package simulators;

import burlap.behavior.policy.Policy;
import burlap.behavior.stochasticgames.GameEpisode;
import burlap.mdp.auxiliary.common.NullTermination;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.SimpleAction;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.agent.SGAgentBase;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.mdp.stochasticgames.world.World;
import common.POWorld;
import common.PlannedPOSGAgent;
import domains.wildfire.*;
import posg.POOOSGDomain;
import posg.model.FACRewardFunction;


public class NOOPBaselineSimulator {

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
        int numberOfRounds, numberOfTrials ;



        try{



            configuration = 3;
            numberOfRounds = 15;
            numberOfTrials = 5;
            experimentNumber = 0;
            boolean isFireRandom = false;
            boolean isSuppRandom = true;
            boolean isSuppOpen = false;

            //Get the parameters from the argument.
            if(args != null && args.length == 7){
                try{
                    configuration = Integer.parseInt(args[0]);
                    numberOfRounds = Integer.parseInt(args[1]);
                    numberOfTrials = Integer.parseInt(args[2]);
                    experimentNumber = Integer.parseInt(args[3]);
                    isFireRandom = Boolean.parseBoolean(args[4]);
                    isSuppRandom = Boolean.parseBoolean(args[5]);
                    isSuppOpen = Boolean.parseBoolean(args[6]);
                }catch (NumberFormatException ne){
                    System.out.println("Problem in the arguments." + ne.getMessage());
                    System.exit(0);
                }//end try-catch.
            }else{
                //Take the defaults.
            }//end if args.


            //Initiate the domain parameters.
            Wildfire wildfire = new Wildfire();
            WildfireDomain wildfireDomain = (WildfireDomain) wildfire.generateDomain(configuration,false);
            FACRewardFunction rewardFunction = new WildfireRewardFunction();
            TerminalFunction terminalFunction = new NullTermination();
            wildfireDomain.setPartialObservationFunction(new FireDiscreteObservation());



            //Get initial master state and master anonymous state.
            //Set the Suppressant levels from the beginning.
            //Agent can have [0, WildfireParameters.MAX_SUPPRESSANT_STATES - 1] availability
            //states.
            int agentAvailability = WildfireParameters.MAX_SUPPRESSANT_STATES - 1;

            State ms = Wildfire.getInitialMasterState( wildfireDomain ,agentAvailability, isFireRandom, isSuppRandom, isSuppOpen);

            //State File Name.
            //State File Name.
            String outFileHead = "output/Simulations/Config_"+configuration +"/Experiment_"+ experimentNumber+"/";

            //Output File Paths.
            String exptHead =  outFileHead + "/NOOP_BASELINE_";
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
                //Create an inline class to create a NOOP action agent.
                //Create a new policy based agent.
                SGAgent agent = new SGAgentBase() {
                    @Override
                    public void gameStarting(World w, int agentNum) {
                        //Do Nothing.
                    }

                    @Override
                    public Action action(State s) {
                        return new SimpleAction(WildfireParameters.NOOP_ACTION);
                    }

                    @Override
                    public void observeOutcome(State s, JointAction jointAction, double[] jointReward, State sprime, boolean isTerminal) {
                        //Do Nothing.
                    }

                    @Override
                    public void gameTerminated() {
                        //DO nothing.
                    }
                };
                //Set agent name.
                ((SGAgentBase)agent).setAgentDetails(agentIndex+"",null);
                world.join(agent);
            }//end for.


            //Content Strings.
            String suppressantContent = "";
            String rewardContent = "";
            String actionContent = "";
            String fireContent = "";
            String overallContent = "";


            //Play game for numberOfTrials times(episodes), and each game consist of numberOfRounds rounds.
            //Save information after wards.
            for(int trial = 1; trial <=numberOfTrials ; trial++){
                GameEpisode episode = world.runGame(numberOfRounds,ms, WildfireParameters.NOOP_EXPERIMENT);
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
            }//end for.


            SimulatorHelper.writeOutputToFile(suppressantContent, exptHead+suppressantResultsFile);
            SimulatorHelper.writeOutputToFile(rewardContent, exptHead+rewardResultsFile);
            SimulatorHelper.writeOutputToFile(actionContent, exptHead+actionResultsFile);
            SimulatorHelper.writeOutputToFile(fireContent, exptHead+fireResultsFile);
            SimulatorHelper.writeOutputToFile(overallContent, exptHead+overallResultsFile);

            System.out.println("---DONE!!--");
        }catch (Exception e){
            System.out.println("Error:" + e.getMessage());
            e.printStackTrace();
        }//end try-catch.
    }//end method.


}
