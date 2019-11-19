package simulators;

import burlap.behavior.stochasticgames.GameEpisode;
import burlap.mdp.auxiliary.common.NullTermination;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.action.SimpleAction;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.agent.SGAgentBase;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.mdp.stochasticgames.world.World;
import common.POWorld;
import domains.wildfire.*;
import domains.wildfire.beans.Fire;
import posg.POOOSGDomain;
import posg.model.FACRewardFunction;


public class HeuristicBaselineSimulator {

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
        boolean isFireRandom = false;
        boolean isSuppRandom = true;
        boolean isSuppOpen = true;

        try{



            configuration = 14;
            numberOfRounds = 15;
            numberOfTrials = 5;
            experimentNumber = 0;

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
            State ms = Wildfire.getInitialMasterState(wildfireDomain ,agentAvailability,isFireRandom,isSuppRandom,isSuppOpen);

            //State File Name.
            String outFileHead = "output/Simulations/Config_"+configuration +"/Experiment_"+ experimentNumber+"/";

            //Output File Paths.
            String exptHead = outFileHead + "/HEURISTIC_BASELINE_";
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
                //Create a heuristic agent, which performs NOOP action when the suppressant level(availability) is
                // empty and random action otherwise.
                SGAgent agent = new SGAgentBase() {
                    @Override
                    public void gameStarting(World w, int agentNum) {
                        //Do Nothing.
                    }

                    @Override
                    public Action action(State s) {
                        WildfireState wildfireState = (WildfireState)s;
                        int currentAvailability = wildfireState.getAgentList().get(Integer.parseInt(this.worldAgentName)).getAvailability();

                        //If the suppressant level is nill, then do NO-operation action, otherwise
                        //choose an action randomly from the agent type.
                        if(currentAvailability == 0){
                            return  new SimpleAction(WildfireParameters.NOOP_ACTION);
                        }else{
                            int randomActionIndex = 0;
                            boolean isActionFound = false;
                            boolean isFireAlive = false;
                            //If any fire is alive in the current agent's range, then he would not do NOOP.
                            //So, set the flag isFireAlive as true if any fire is alive.
                            for(ActionType actionType : this.agentType.actions){
                                String actionName = actionType.associatedAction("Useless Argument").actionName();
                                //The action must not be the NOOP Action.
                                if(!(WildfireParameters.NOOP_ACTION
                                        .equals(actionName))){
                                    for(Fire fire : wildfireState.getFireList()){
                                        if(fire.fireActionName().equals(actionName) &&
                                                fire.getIntensity() != 0 && fire.getIntensity() != (WildfireParameters.MAX_FIRE_STATES-1)){
                                            isFireAlive = true;
                                        }//end if.
                                    }//end for.
                                }//end if action.
                            }//end for.

                            //Check if the chose action is not fighting the fire being either burned out or already
                            //extinguished.
                            while(!isActionFound){
                                randomActionIndex = (int) (Math.random() * this.agentType.actions.size());
                                //If NOOP, then there is no need to check the action.
                                //Select an action instead of NOOP.
                                if(WildfireParameters.NOOP_ACTION
                                        .equals(this.agentType.actions.get(randomActionIndex)
                                                .associatedAction("Useless Argument").actionName())){
                                    //If the fire is alive, don't do NOOP.
                                    //Else Go for NOOP.
                                    if(isFireAlive){
                                        continue;
                                    }else{
                                        break;
                                    }
                                }//end if - NOOP.

                                for(Fire fire: wildfireState.getFireList()){
                                    if(fire.fireActionName()
                                            .equals(this.agentType.actions.get(randomActionIndex)
                                                    .associatedAction("Useless Argument").actionName())
                                            && (!(fire.getIntensity() == 0 || fire.getIntensity() == WildfireParameters.MAX_FIRE_STATES -1))){
                                        isActionFound = true;
                                        break;
                                    }//end if.
                                }//end for.
                            }//end while.

                            return this.agentType.actions.get(randomActionIndex).associatedAction("Useless Argument");
                        }
                    }//end method.

                    @Override
                    public void observeOutcome(State s, JointAction jointAction, double[] jointReward, State sprime, boolean isTerminal) {
                        //Do Nothing.
                    }

                    @Override
                    public void gameTerminated() {
                        //DO nothing.
                    }
                };

                String agentGroup = wildfireDomain.agentsList.get(agentIndex).getAgentGroup();
                //Set the agent type and name details.
                //The type is set by the actions the agent can perform.
                ((SGAgentBase)agent).setAgentDetails(agentIndex+"",
                        new SGAgentType(agentGroup,
                                wildfireDomain.actionsPerGroup.get(agentGroup)));
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
                GameEpisode episode = world.runGame(numberOfRounds,ms, WildfireParameters.HEURISTIC_EXPERIMENT);
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
