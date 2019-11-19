package pomcp;


import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.MDPSolver;
import burlap.behavior.singleagent.planning.Planner;
import burlap.behavior.valuefunction.QProvider;
import burlap.behavior.valuefunction.QValue;

import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.state.State;
import burlap.statehashing.HashableState;
import common.StateEnumerator;
import domains.wildfire.*;

import domains.wildfire.beans.Agent;
import domains.wildfire.beans.Fire;
import org.apache.commons.math3.distribution.TDistribution;
import org.thejavaguy.prng.generators.PRNG;
import org.thejavaguy.prng.generators.XorshiftPlus;
import pomcp.beans.*;
import posg.model.FACRewardFunction;
import posg.model.FullJointIPOMCPModel;
import scalability.FrameActionConfiguration;
import simulators.SimulatorHelper;

import java.util.*;


public class IPOMCP extends MDPSolver implements Planner,QProvider {
    /**
     * Maximum allowed error.
     */
    private double epsilon;
    /**
     * Discounting Factor.
     */
    private double gamma;

    /**
     * Bandit Constant.
     */
    private double C;


    /**
     * Maximum number of horizons allowed to explore the tree.
     */
    private int maxHorizons;

    /**
     * Maximum allowed time per tree exploration. .
     */
    private int timeBound;

    /**
     * Number of particles in the root node.
     */
    private int particleCount;

    /**
     * Alpha value for the Confidence in the Sampling of the Agent.
     */
    private double alpha;
    /**
     * Sampling Error bound.
     */
    private double samplingErrorBound;

    /**
     * Given the current domain configuration and the anonymous current state, this defines the
     * maximum agents allowed per frame-action pair.
     */
    private FrameActionConfiguration possibleMaxConfiguration;

    /**
     * Agent Policy for each agent group in the specific configuration.
     */
     private HashMap<String,Policy> agentPolicies;

    /**
     * Current Agent's number.
     */
    private int currentAgentNumber;
    /**
     * Set of actions current agent can do.
     */
    private ActionType[] currentAgentAction;

    /**
     * Object of the main Wildfire class.
     */
    private Wildfire wildfire;

    /**
     * Domain.
     */
    private WildfireDomain wildfireDomain;

    /**
     * Reward Function.
     */
    private FACRewardFunction rewardFunction;

    /**
     * Transition Function.
     */
    private FullJointIPOMCPModel jointIPOMCPModel;

    /**
     * Getting Terminal Function.
     */
    private TerminalFunction terminalFunction;


    /**
     * Total number of agents per type. The types index is same as the possibleConfigurations.
     */
    private int[] sampledAgentsPerType;

    /**
     * Sample the agents from the given error rate. a
     */
    private ArrayList<Agent> sampledAgents;

    /**
     * Root node to start making the tree.
     */
    private TreeNode rootNode;

    /**
     * This is the tree, where all the belief nodes are saved. To say it as a tree is a bit misguiding, but it is
     * useful anyways.
     */
    private IPOMCPTree ipomcpTree;

    /**
     * If the IPOMCP is done by considering other agents as random agents.
     */
    private boolean isRandom;

    /**
     * A static variable to keep track of the current horizon.
     */
    int currentHorizon;


    /**
     * Map of the weighted Q-value.
     */
    private double[] weightedQValue;//Weighted Q-Value. // Would follow the sequence of Agent Actions for the Q-values.

    /**
     * Map of the overall visits.
     */
    private int[] overallVisits;//Overall visits.// Would follow the sequence of Agent Actions for the Q-values.
    /**
     * State Enumerator.
     */
    private StateEnumerator senum;

    /**
     * State Enumerator.
     */
    private Map<Integer,ArrayList<Double>> rolloutDistribution;

    /**
     * Random Number generator.
     */
    private PRNG.Smart generator;

    /**
     * If the I-POMCP action node should be initialized with the averaged Q-value at root or not.
     */
    boolean isAvgQActionNode;


    /**
     * Keeping track of the stages for printing IPOMCP tree.
     */
    private int stages;

    /**
     * Keeping track of the stages for printing IPOMCP tree.
     */
    private int trials;
    private int trialSeries;

    /**
     * For setting the initial suppressants with openness setting.
     */
    boolean isSuppOpen;


    /**
     * If the tree comprises of only one configuration node after a belief node in the tree.
     */
    boolean isSingleConfigNode;


    /**
     * total visit count.
     */
    int count;
    int rolloutRewardCount;
    int treeRewardCount;

    private Map<Integer, ArrayList<BeliefParticle>> nextParticles;


    //Default Constructor.
    private IPOMCP() {
        this.currentAgentAction = new ActionType[1];
        this.sampledAgents = new ArrayList<>();
        this.ipomcpTree = new IPOMCPTree();
        this.agentPolicies = new HashMap<>();
    }


    public IPOMCP(double epsilon,double gamma,double C,int maxHorizons,int timeBound,int particleCount,double alpha,
                    double samplingErrorBound, FrameActionConfiguration possibleMaxConfiguration,
                  HashMap<String,Policy> agentPolicies, int currentAgentNumber,List<ActionType> currentAgentAction,
                  Wildfire wildfire,WildfireDomain wildfireDomain,FACRewardFunction rewardFunction,
                  FullJointIPOMCPModel jointIPOMCPModel,
                  TerminalFunction terminalFunction,StateEnumerator senum,State initialAnmState,State masterState, boolean isRandom,
                  int trials, int trialSeries, boolean isAvgQActionNode, boolean isSuppOpen, boolean isSingleConfigNode){
        this();

        //Initialize the constant values for the IPOMCP.
        this.epsilon = epsilon;
        this.gamma = gamma;
        this.C = C;
        this.maxHorizons = maxHorizons;
        this.timeBound = timeBound;
        this.particleCount = particleCount;
        this.alpha = alpha;
        this.samplingErrorBound = samplingErrorBound;

        //Initialize the domain values for the IPOMCP.
        this.possibleMaxConfiguration = possibleMaxConfiguration;
        this.agentPolicies = agentPolicies;
        this.currentAgentNumber = currentAgentNumber;
        this.wildfire = wildfire;
        this.wildfireDomain = wildfireDomain;
        this.jointIPOMCPModel = jointIPOMCPModel;
        this.rewardFunction = rewardFunction;
        this.terminalFunction = terminalFunction;
        this.isRandom = isRandom;
        this.stages = 0;
        this.trials = trials;
        this.trialSeries = trialSeries;
        this.isAvgQActionNode = isAvgQActionNode;
        this.isSuppOpen = isSuppOpen;
        this.isSingleConfigNode = isSingleConfigNode;
        //Random number generator.
        generator =  new XorshiftPlus.Smart(new XorshiftPlus());

        //Convert the action list into arrays.
        this.currentAgentAction = new ActionType[currentAgentAction.size()];
        for(int actionBranch = 0 ; actionBranch < currentAgentAction.size(); actionBranch++){
            this.currentAgentAction[actionBranch] = currentAgentAction.get(actionBranch);
        }//end for.


        //Sample the agents for modeling.
        sampleAgents(this.wildfireDomain);

        //Randomly pick some of the mental models from the all possible options.
        ArrayList<BeliefParticle> beliefParticles = sampleBeliefParticles(initialAnmState,masterState,null) ;
        //Initialize the initial root of the I-POMCP Tree.
        this.rootNode = new TreeNode(); //Use the initial state instead.
        this.rootNode.setBeliefParticles(beliefParticles);
        //Generate the IPOMCP Tree and add the initial node into that.
        this.ipomcpTree = new IPOMCPTree();
        this.ipomcpTree.setNode(rootNode);
        this.currentHorizon = 0;

        //Initialize maps.
        weightedQValue = new double[this.currentAgentAction.length];//Weighted Q-Value.
        overallVisits = new int[this.currentAgentAction.length];//Overall visits.

        //Updated distribution for the roll out.
        this.senum = senum;
        this.rolloutDistribution = new HashMap<>();
        rolloutDistribution(senum);

        rolloutRewardCount = 0;
        treeRewardCount = 0;
        this.nextParticles = new HashMap<>();
    }

    /**
     * This method generates distribution for the rollout method reducing the likelihood of the wrong actions and uses softmax
     * distribution over the reward values generated by each state.
     * The method first initializes each action with the weight 0 and then adds the "wrong actions' penalty" value
     * to all the actions which are the wrong actions. In the end the Softmax function is used for generating a distribution.
     *
     * @param senum is the state enumerator containing all the states.
     */
    private void rolloutDistribution(StateEnumerator senum) {
        //Distribution Temp variable.
        ArrayList<Double> distribution ;

        //Weights Temp Variable.
        ArrayList<Double> actionWeights;


        //For each state
        for(Map.Entry<HashableState,Integer> stateEntry: senum.enumeration.entrySet()){

            //Initialize weights and distribution.
            distribution = new ArrayList(this.currentAgentAction.length);
            for(ActionType action: this.currentAgentAction){
                distribution.add(0.0);
            }//end for.
            actionWeights = new ArrayList(this.currentAgentAction.length);
            for(ActionType action: this.currentAgentAction){
                actionWeights.add(0.0);
            }//end for.

            //Get State and enumeration ID.
            WildfireAnonymousState anonymousState = (WildfireAnonymousState)stateEntry.getKey().s();
            Integer stateID = stateEntry.getValue();

            //If the agent is at zero suppressant, then add  -1 * penalty value to the NOOP.
            if(anonymousState.getSelfAgent().getAvailability() == 0){
                for(int actionIndex = 0; actionIndex < this.currentAgentAction.length ; actionIndex++){
                    ActionType actionType = this.currentAgentAction[actionIndex];
                    if(!actionType.associatedAction("Useless Argument").actionName().equals(WildfireParameters.NOOP_ACTION)){
                        actionWeights.set(actionIndex, actionWeights.get(actionIndex) +  WildfireParameters.NO_FIRE_PENALTY_CURRENT);
                    }//end if.
                }//end for.
            }else{
                //Add the burned out fires' or extinguished fires within the reach of the current agent.
                ArrayList<String> fireActions = new ArrayList<>();
                for(Fire fire: anonymousState.getFireList()){
                    if(fire.getIntensity() == WildfireParameters.MAX_FIRE_STATES-1 || fire.getIntensity() == 0){
                        for(ActionType actionType : this.currentAgentAction){
                            if(actionType.associatedAction("Useless Argument").actionName().equals(fire.fireActionName())){
                                fireActions.add(fire.fireActionName());
                                break;
                            }//end if.
                        }//end for.
                    }//end if.
                }//end for.

                //Update the weights if there is any burned out fire. Add -1 * Penalty value for each fire which is not
                //burnt out.
                if(fireActions.size() > 0){
                    for(int actionIndex = 0; actionIndex < this.currentAgentAction.length ; actionIndex++){
                        ActionType actionType = this.currentAgentAction[actionIndex];
                        boolean isWrongAction = false;
                        for(String fireAction : fireActions){
                            if(actionType.associatedAction("Useless Argument").actionName().equals(fireAction)){
                                isWrongAction = true;
                                break;
                            }//end if.
                        }//end for.

                        //If it is not wrong action add the value.
                        if(isWrongAction){
                            actionWeights.set(actionIndex, actionWeights.get(actionIndex) +  WildfireParameters.NO_FIRE_PENALTY_CURRENT);
                        }
                    }//end for.
                }//end if.
            }//end if-else.

            //Get the total Weight and find the distribution.
            double totalWeight = 0.0;
            for(Double weight: actionWeights){
                totalWeight += Math.exp(weight);
            }

            for(int actionIndex = 0; actionIndex < this.currentAgentAction.length ; actionIndex++){
                distribution.set(actionIndex, Math.exp(actionWeights.get(actionIndex))/totalWeight);
            }//end for.


            //Add state id and distribution to the map.
            rolloutDistribution.put(stateID,distribution);
        }//end for.
    }//end.


    /**
     * This method is the primary method to create and search the IPOMCP - tree for finding the best action given the
     * current state.
     * @param anonymousState is the reference state for the current agent to plan for.
     * @return is the best action for the current agent in the given given circumstances.
     */
    public  Action searchTree(State anonymousState) throws Exception{


        String predictionLogString = "";
        predictionLogString += this.currentAgentNumber + ",";
        predictionLogString += this.wildfireDomain.agentsList.get(currentAgentNumber).getAgentGroup() + ",";
        predictionLogString += ((WildfireAnonymousState)anonymousState).toString() + ",";

        String configLog = "";


        //Add the tree Logging information.
        String treeLog = "\nCurrent Agent Info:";
        treeLog += this.currentAgentNumber + ",";
        treeLog += "Agent Group:" + this.wildfireDomain.agentsList.get(currentAgentNumber).getAgentGroup() + "\n";
        SimulatorHelper.addParticleLog(treeLog);
        SimulatorHelper.addParticleLog("Particle Count Before Resizing:" + this.rootNode.getBeliefParticles().size());
        treeLog += "Current State:" + ((WildfireAnonymousState)anonymousState).toString() + "\n";



        //Get total number of configuration sampled from root.
        double maxQValue = -100000;

        Action bestAction = null;
        //Get the current time.
        long startTime = System.currentTimeMillis();
        this.count = 0;

//        System.out.println("TIME Budget:" + this.timeBound);

        //Run the loop until the timeout.
        //NOTE: Because of uneven performance of the machine for creating a tree for each agent, counter might be helpful.
        //NOTE: Time bound would help to put at least some more nodes than the uneven performance of the machine is generating.
        while((System.currentTimeMillis() - startTime) < this.timeBound){
            this.count++;//Increase the counter.

            if(this.rootNode == null){
                throw new Exception("Root for the new exploration is null");
            }else{

                if(this.isAvgQActionNode){
                    //Update the Q-values every time a new run is starting.
                    weightedQValue = new double[this.currentAgentAction.length];
                    overallVisits = new int[this.currentAgentAction.length];
                    //Find the average Q-value by summing the weighted Q-Value of the all action nodes below the root node.
                    //The weight is the likelihood of the Configuration (C_i / C).
                    for(HistoryElement historyElement : this.rootNode.getHistoryElement()){
                        // C_i / C  = current configuration visits / total configuration sampled.
                        // The probability to choose the best action is 1, so is not included.

                        int configurationSampled =  this.rootNode.countSampledConfigs();
                        double configRatio = (double)historyElement.getConfigurationNode().getNodeVisit() / (double)configurationSampled;

                        //Iterate over all the action branches.
                        for(int actionBranch=0; actionBranch < historyElement.getActionNodes().size(); actionBranch++){

//                        //Add if the best action value does not exist.
//                        if(weightedQValue.size() == 0 ||  weightedQValue.size() <= actionBranch) {
//                            weightedQValue.add(actionBranch, 0.0);
//                            overallVisits.add(actionBranch,0);
//                        }//end if.

                            //Add the Config-Ratio * Q-Value for each branch.
                            weightedQValue[actionBranch] =
                                    weightedQValue[actionBranch]
                                            + configRatio * historyElement.getActionNodes().get(actionBranch).getNodeQValue();

                            //Add the Number of visits to the current branch.
                            overallVisits[actionBranch] =
                                    overallVisits[actionBranch]
                                            +  historyElement.getActionNodes().get(actionBranch).getNodeVisit();

                        }//end for.
                    }//end for.
                }//end if. Is average-Q action node.


//                if(counter == 1){
//                    //Print the Particles into the log file.
//                    SimulatorHelper.addParticleLog("\nParticles at the Beginning:\n");
//                    printParticles();
//                }

                //If it is not initial state, sample particles using the anonymous state and merged belief state.
                // and recreate the root node.
                if(!(this.rootNode.getBeliefParticles().size() == this.particleCount)){
                    ArrayList<BeliefParticle> beliefParticles = sampleBeliefParticles(anonymousState,null,this.rootNode.getBeliefParticles()) ;
                    this.rootNode.setBeliefParticles(beliefParticles);
                }//end if.
                //Sample a particle from the tree node.
                BeliefParticle sampledParticle = this.rootNode.sampleParticle();
                //Reset the horizon value.
                this.currentHorizon = 0;
                //Simulate the actions.
                simulate(sampledParticle, this.rootNode, this.wildfireDomain);
            }//end if.
        }//end while.

        System.out.println("Total time taken for creating tree TIME:" + (System.currentTimeMillis() - startTime));
        System.out.println("Total Visits to the tree:" + this.count);
        System.out.println("Rollout Reward COUNTER:" + this.rolloutRewardCount);
        System.out.println("Tree Reward COUNTER:" + this.treeRewardCount);

        long choosingActionStart = System.currentTimeMillis();


        //Find the best action from the root node.
        //Log the final configurations.
        if(this.currentAgentNumber%8 == 0){
            configLog += "\nCurrent Agent Info:";
            configLog += this.currentAgentNumber + ",";
            configLog += this.wildfireDomain.agentsList.get(currentAgentNumber).getAgentGroup() + "\n";
            configLog += "Agent's Prediction Configs:\n";
            for(HistoryElement historyElement : this.rootNode.getHistoryElement()) {
                configLog += historyElement.getConfigurationNode().getFac().printCurrentConfig() + ","
                        + historyElement.getConfigurationNode().getNodeVisit() + "\n";
            }
        }


        //Calculate the average Q-value just once if the action nodes are starting from zero.
        if(!this.isAvgQActionNode){
            //Update the Q-values every time a new run is starting.
            weightedQValue = new double[this.currentAgentAction.length];
            overallVisits = new int[this.currentAgentAction.length];
            //Find the average Q-value by summing the weighted Q-Value of the all action nodes below the root node.
            //The weight is the likelihood of the Configuration (C_i / C).
            for(HistoryElement historyElement : this.rootNode.getHistoryElement()){
                // C_i / C  = current configuration visits / total configuration sampled.
                // The probability to choose the best action is 1, so is not included.

                int configurationSampled =  this.rootNode.countSampledConfigs();
                double configRatio = (double)historyElement.getConfigurationNode().getNodeVisit() / (double)configurationSampled;

                //Iterate over all the action branches.
                for(int actionBranch=0; actionBranch < historyElement.getActionNodes().size(); actionBranch++){

//                        //Add if the best action value does not exist.
//                        if(weightedQValue.size() == 0 ||  weightedQValue.size() <= actionBranch) {
//                            weightedQValue.add(actionBranch, 0.0);
//                            overallVisits.add(actionBranch,0);
//                        }//end if.

                    //Add the Config-Ratio * Q-Value for each branch.
                    weightedQValue[actionBranch] =
                            weightedQValue[actionBranch]
                                    + configRatio * historyElement.getActionNodes().get(actionBranch).getNodeQValue();

                    //Add the Number of visits to the current branch.
                    overallVisits[actionBranch] =
                            overallVisits[actionBranch]
                                    +  historyElement.getActionNodes().get(actionBranch).getNodeVisit();

                }//end for.
            }//end for.
        }//end if Is-Average Q.


        String actionString ="";
        String predictionString ="";
        int bestActionIndex = -1;
//        System.out.println("Agent:" + currentAgentNumber );
        //Evaluate the action with the current maximum and choose the best action.
        for(int actionBranch = 0 ; actionBranch <  this.currentAgentAction.length ; actionBranch++){

            actionString += this.currentAgentAction[actionBranch].associatedAction("Useless Argument").actionName()+ ";";
            predictionString += this.overallVisits[actionBranch] + " : " + String.format("%.2f",this.weightedQValue[actionBranch]) + "; ";
//            System.out.println( visitMap.getValue() + " : " + String.format("%.2f",weightMap.getValue()) + "; ");

            if(this.weightedQValue[actionBranch] >= maxQValue){
                bestAction = this.currentAgentAction[actionBranch].associatedAction("Useless Argument");
                bestActionIndex = actionBranch;
                maxQValue = this.weightedQValue[actionBranch];
            }//end if.
        }//end for.

        // save the possible next particle filters for the next beliefUpdate step (so that we can clear the tree to save memory)
        this.nextParticles.clear();
        Integer fireDifference;
        int hashcode;
        for (ObservationNode onode : rootNode.getHistoryElement().get(0).getObservationNodes().get(bestActionIndex)) {
            // get the observation
            fireDifference = ((FireObservation) onode.getObservation()).getFireDifference();

            // create the list of next particles
            ArrayList<BeliefParticle> beliefParticles = new ArrayList<>();
            this.nextParticles.put(fireDifference, beliefParticles);

            // fill the list of next particles
            hashcode = onode.getChildHashCode();
            for (BeliefParticle particle : this.ipomcpTree.getBeliefTree().get(hashcode).getBeliefParticles()) {
                beliefParticles.add(particle);
            }
        }


        //Print the Particles into the log file.
        SimulatorHelper.addParticleLog("\nParticles at the End:\n");
        printParticles();

        //This method prints the entire tree into the string.
        //Print the tree only for one trial and one set of experiment.
        //Only print for the first agent in the group.
        if(this.stages < 2 && this.trials < 1
                    && this.trialSeries == 0
                    && this.currentAgentNumber == wildfireDomain.sampleAgentPerGroup.get(this.wildfireDomain.agentsList.get(currentAgentNumber).getAgentGroup()).getAgentNumber()){
            System.out.println("Stage:" + this.stages + " Trial:" + this.trials);
            SimulatorHelper.addTreeInfo(treeLog);
            printIPOMCPTree();
            this.stages++;
        }

        //Prediction String.
        predictionLogString += actionString + ",";
        predictionLogString += predictionString + ",";
        predictionLogString += bestAction.actionName() + "\n";


        //Simulator Helper to keep track of the things.
        SimulatorHelper.addPrediction(predictionLogString);

        if(this.currentAgentNumber%8 == 0){
            configLog += "Chosen Action:" + bestAction.actionName() + "\n";
            SimulatorHelper.addConfigInfo(configLog);
        }

//        System.out.println("Chose action in TIME:" + (System.currentTimeMillis() - choosingActionStart));

        System.out.println(actionString);
        System.out.println(predictionString);

        // clear the tree to save memory
        this.ipomcpTree.clear();

        return bestAction;
    }//end search method.


    /**
     * Prints particles in the beginning while creating tree.
     */
    public void printParticles(){
//        System.out.println("Printing Particles For the Agent:" + this.currentAgentNumber);

        SimulatorHelper.addParticleLog("Size:" + this.rootNode.getBeliefParticles().size()+ "\n");
        long start = System.currentTimeMillis();
        HashMap<String,Integer> suppLevels = new HashMap<>();
        HashMap<String,Integer> agentStates = new HashMap<>();

        for(BeliefParticle particle : this.rootNode.getBeliefParticles()){
            int count[] = new int[WildfireParameters.MAX_SUPPRESSANT_STATES];
            for(Integer suppLevel : particle.getMentalModels()){
                count[suppLevel] +=1;
            }//end for.
            String suppCounts = "";
            for(int supp: count){
                suppCounts += supp + " - ";
            }//end for.

            if(suppLevels.get(suppCounts) == null){
                suppLevels.put(suppCounts,1);
            }else{
                suppLevels.put(suppCounts,suppLevels.get(suppCounts) + 1);
            }//end if-else.
        }//end for.


        for (Map.Entry<String,Integer> suppLevel: suppLevels.entrySet()){
            SimulatorHelper.addParticleLog(suppLevel.getKey() + "," + suppLevel.getValue()+ "\n");
        }//end for.

        SimulatorHelper.addParticleLog("\n\n");

        for(BeliefParticle particle : this.rootNode.getBeliefParticles()){
            String stateString = "";
            stateString += "S-" + ((WildfireAnonymousState)particle.getStateParticle()).getSelfAgent().getAvailability() + "-";
            for(Fire fire : ((WildfireAnonymousState)particle.getStateParticle()).getFireList()){
                stateString += "F" + fire.getFireNumber()+ "-" + fire.getIntensity() + "-";
            }//end for.

            if(agentStates.get(stateString) == null){
                agentStates.put(stateString,1);
            }else{
                agentStates.put(stateString,agentStates.get(stateString) + 1);
            }//end if-else.
        }//end for.


        for (Map.Entry<String,Integer> agentState: agentStates.entrySet()){
            SimulatorHelper.addParticleLog(agentState.getKey() + "," + agentState.getValue()+ "\n");
        }//end for.


        long end = System.currentTimeMillis();
//        System.out.println("It took this many MS for backup TIME:" + (end-start));
    }//end method.



    /**
     * Print the entire IPOMCP Tree and add the contents to the simulator's tree log.
     */
    public void printIPOMCPTree(){
        System.out.println("Printing Tree For the Agent:" + this.currentAgentNumber);
        long start = System.currentTimeMillis();

        String printString = "Tree Size:" + this.ipomcpTree.getBeliefTree().size();
        ArrayList<TreeNode> printStack = new ArrayList();
        ArrayList<Integer> levelIndex = new ArrayList<>();
        printStack.add(this.rootNode);
        int nodeAddCounter = 1;//Root node is added.
        levelIndex.add(nodeAddCounter);
        //Get all the tree nodes in the tree until level 2 by getting hascodes from the observation nodes.
        for(int level=0; level < 5; level++){
            int startIndex = printStack.size()-nodeAddCounter;
            int endIndex = printStack.size();
            levelIndex.add(endIndex);//Add new level index.
            nodeAddCounter = 0;
            for(int printStackIndex = startIndex ; printStackIndex < endIndex; printStackIndex++){
                for(HistoryElement history: printStack.get(printStackIndex).getHistoryElement()){
                    //Get the list of observation node arrays.
                    for(ArrayList<ObservationNode> obsNodes: history.getObservationNodes()){
                        for(ObservationNode obsNode: obsNodes){
                            if(this.ipomcpTree.getNode(obsNode.getChildHashCode()) != null){
                                printStack.add(this.ipomcpTree.getNode(obsNode.getChildHashCode()));
                                nodeAddCounter++;
                            }//end if.
                        }//end for.
                    }//end for.
                }//end for.
            }//end for.
        }//end for.

        long end = System.currentTimeMillis();
//        System.out.println("It took this many MS for searching the nodes TIME:" + (end-start));
        //Print all the nodes to the tree string.
        int levelPrint = 0;
        for(int stackIndex =0 ; stackIndex < printStack.size() ; stackIndex++){

            //Print Level of the tree as well.
            if(stackIndex == levelIndex.get(levelPrint)){
                printString += "\n\nLevel:" + (levelPrint+1);
                levelPrint++;
            }//end if.

            TreeNode printNode = printStack.get(stackIndex);
            printString += "\n\n" +  printNode.toString();
            for(HistoryElement historyElement : printNode.getHistoryElement()){
                printString += "\n\n" + historyElement;
            }//end for.
        }//end for.

        end = System.currentTimeMillis();
        SimulatorHelper.addTreeInfo(printString);
        System.out.println("It took this many MS for backup TIME:" + (end-start));
    }//end method.


    /**
     * This method explores and simulates the tree until the maximum allowed depth and adds the rewards to the
     * tree by exploring each node.
     * @param beliefParticle is the particle to simulate.
     * @param currentNode is the current root node for the tree search.
     * @param wildfireDomain is the domain object.
     * @return is the reward at the current level. Apparently, it is useless.
     */
    public double simulate(BeliefParticle beliefParticle, TreeNode currentNode,WildfireDomain wildfireDomain){
        double reward = 0.0;
        int branchNumber = -1;

        // If gamma ^ depth < epsilon
        // Then return zero. The tree would not search further below the epsilon error.
        if(Math.pow(this.gamma,this.currentHorizon) < this.epsilon){
            return  reward;
        }

        //The tree would not search further than the maximum allowed horizon.
        if(this.currentHorizon >= this.maxHorizons){
            return reward;
        }


        //If the path does not exist in the tree, then go for roll out.
        if( this.ipomcpTree.getBeliefTree().get(Objects.hashCode(currentNode.getPath())) == null){
            //Create just a simple node copy with belief particles. No history element is added into the tree for this node.
            TreeNode nodeToAdd = new TreeNode(currentNode,false);
            //Add the node to the tree.
            this.ipomcpTree.setNode(nodeToAdd);

            //Get the reward and return the value.
            return rollOut(beliefParticle,currentNode,wildfireDomain);
        }else{
            try{
                //Sample the FAC.
                FrameActionConfiguration fac  = sampleFAC(beliefParticle,wildfireDomain);
                //Check if the configuration already exist in the belief node or not.
                branchNumber = currentNode.configExist(fac,this.isSingleConfigNode);


                //Explore this node by adding a history element into the current node, if the FAC node did not exist.
                if(branchNumber == -1){
                   branchNumber = exploreNode(currentNode,fac,beliefParticle);
                }else{
                    if(this.isSingleConfigNode){
                        currentNode.getHistoryElement().get(branchNumber).getConfigurationNode().setFac(fac);
                    }//end if.
                }//end if-else.

                //Find the best action using the Q-Value of the Bandit Action.
                int bestActionIndex = getBestBanditAction(currentNode,branchNumber);

//                System.out.println("Path:" + currentNode.getPath() + " FAC:" + branchNumber + " Best Action:" + bestActionIndex);
                //Simulate the environment. No new node added.
                TreeNode nextTreeNode = blackBoxSimulator(beliefParticle,currentNode,branchNumber,bestActionIndex,false);

                //Initialize the action node with the reward value.
                if(currentNode.getHistoryElement().get(branchNumber)
                        .getActionNodes().get(bestActionIndex).getNodeVisit() == 0){
                    currentNode.getHistoryElement().get(branchNumber)
                            .getActionNodes().get(bestActionIndex).setNodeQValue(nextTreeNode.getValue());
                }//end if.


                //Increase the counters by 1.
                currentNode.getHistoryElement().get(branchNumber)
                        .getConfigurationNode().incrementVisit();
                currentNode.getHistoryElement().get(branchNumber)
                        .getActionNodes().get(bestActionIndex).incrementVisit();



                //Increase the horizon value.
                this.currentHorizon++;

                if(nextTreeNode.getValue() > 0){
//                    System.out.println("Visit:" + this.count + " Path:" + nextTreeNode.getPath());
                    this.treeRewardCount++;
                }

                //Recursively iterate through the tree to calculate reward of the next nodes.
                reward = nextTreeNode.getValue()
                        + this.gamma * simulate(nextTreeNode.getBeliefParticles().get(nextTreeNode.getBeliefParticles().size()-1),
                        nextTreeNode, wildfireDomain);

                //Update Q-Value.
                currentNode.getHistoryElement().get(branchNumber).getActionNodes().get(bestActionIndex).incrementQValue(reward);
            }catch (Exception e){
                System.out.println("IPOMCP Simulator, creating the tree:" + e.getMessage());
                e.printStackTrace();
                System.exit(0);
            }


        }//end if-else.

        return reward;
    }//end Simulate.


    /**
     * This method is used for exploring the current node for an FAC. It would create a new HistoryElement in the current
     * node and add the config as the FACNode, while explore all the actions that current agent can do and add them to
     * the ActionNode after initiating them. In the end the index of the history element would be returned.
     * The Action Nodes are initialized with the R value after first simulation.
     * @param currentNode is the current node to explore.
     * @param fac is the sampled fac to add in the node history element.
     * @param beliefParticle is the belief particle to explore the node.
     * @return branch number of the added history element.
     */
    public int exploreNode (TreeNode currentNode,FrameActionConfiguration fac,BeliefParticle beliefParticle){
        //Create the History element.
        HistoryElement historyElement = new HistoryElement();

        //Create FAC node and add it to the history element.
        FACNode facNode = new FACNode();
        facNode.setFac(fac);
        historyElement.setConfigurationNode(facNode);

        //Explore All the actions that the current agent can do,  add an Action node to history.
        for(int actionBranch = 0 ; actionBranch <  this.currentAgentAction.length ; actionBranch++){
            Action action = this.currentAgentAction[actionBranch].associatedAction("Useless argument");
            //Create action node and add it to the history element.
            ActionNode actionNode = new ActionNode();
            actionNode.setAction(action);

            //Add action and observation nodes.
            historyElement.getActionNodes().add(actionNode);
            historyElement.getObservationNodes().add(new ArrayList<>());
        }//end for.

        //Add the history element to the current node.
        currentNode.getHistoryElement().add(historyElement);


        //Initialize the node with the immediate R-value.
        //Explore All the actions that the current agent can do,  add an Action node to history.
        for(int actionBranch = 0 ; actionBranch <  this.currentAgentAction.length ; actionBranch++){
            //Create mergedFAC.
            FrameActionConfiguration mergedFAC = new FrameActionConfiguration(fac,false);
            WildfireAnonymousState anonymousState = (WildfireAnonymousState)beliefParticle.getStateParticle();
            int facIndex = -1;
            if(anonymousState.getSelfAgent().getAvailability() == 0){
                //Add current action to the configuration and create a new inclusive fac.
                facIndex =  mergedFAC.search(anonymousState.getSelfAgent().getPowerType(),WildfireParameters.NOOP_ACTION);
            }else{
                //Add current action to the configuration and create a new inclusive fac.
                facIndex =  mergedFAC.search(anonymousState.getSelfAgent().getPowerType(),
                        this.currentAgentAction[actionBranch].associatedAction("Useless Argument").actionName());
            }
            if(facIndex != -1){
                int numOfAgents = mergedFAC.getCurrentConfiguration()[facIndex].getNumberOfAgents();
                mergedFAC.getCurrentConfiguration()[facIndex].setNumberOfAgents( numOfAgents + 1);
            }//end if.

            State nextState = this.jointIPOMCPModel.sample(beliefParticle.getStateParticle(),mergedFAC);
            double reward = this.rewardFunction.reward(beliefParticle.getStateParticle(),
                    currentNode.getHistoryElement().get(currentNode.getHistoryElement().size()-1)
                            .getActionNodes().get(actionBranch).getAction(),
                    mergedFAC,
                    nextState);
            currentNode.getHistoryElement().get(currentNode.getHistoryElement().size()-1)
                    .getActionNodes().get(actionBranch).setNodeQValue(reward);
        }



        return  (currentNode.getHistoryElement().size()-1);
    }//end method.

    /**
     * This method implements the rollout functionality of the IPOMCP, where it selects the action totally randomly
     * from the number of actions available to the agent.
     * @param beliefParticle is the belief particle to process with.
     * @param node is the node to explore using rollout.
     * @param wildfireDomain is the domain object.
     * @return is the value of the current node after exploring till max horizons.
     */
    public double rollOut(BeliefParticle beliefParticle, TreeNode node, WildfireDomain wildfireDomain){
        double reward = 0;
        FrameActionConfiguration rolloutFAC = null;
        HistoryElement rolloutHistoryElement = null;
        ArrayList<Double> distribution = null;
        double randomNumber;
        double distributionSum = 0;
        int randomActionIndex = 0;
        Action randomAction;

        try{
            // If gamma ^ depth < epsilon
            // Then return zero. The tree would not search further below the epsilon error.
            if(Math.pow(this.gamma,this.currentHorizon) < this.epsilon){
                return  reward;
            }
            //The tree would not search further than the maximum allowed horizon.
            if(this.currentHorizon >= this.maxHorizons){
                return reward;
            }


            //Sample FAC, and action and simulate.

            //Crate a history element.
            rolloutHistoryElement = new HistoryElement();
            //Sample a new FAC and create an FAC node.
            rolloutFAC = sampleFAC(beliefParticle,wildfireDomain);
            FACNode rolloutFACNode = new FACNode();
            rolloutFACNode.setFac(rolloutFAC);
            rolloutHistoryElement.setConfigurationNode(rolloutFACNode);


            //Choose the Action Node from the distribution for the random actions.

            //Anonymous state is needed because the State enumerators are created only once for a group.
            WildfireAnonymousState anonymousState = new WildfireAnonymousState((WildfireAnonymousState) beliefParticle.getStateParticle());
            int sampleAgentNumber = wildfireDomain.sampleAgentPerGroup.get(anonymousState.getSelfAgent().getAgentGroup()).getAgentNumber();
            anonymousState.getSelfAgent().setAgentNumber(sampleAgentNumber);

            distribution = this.rolloutDistribution.get(this.senum.getEnumeratedID(anonymousState));
            randomNumber= Math.random();

            //Select the action based on distribution and random number.

            for(int actionIndex = 0 ; actionIndex < this.currentAgentAction.length ; actionIndex++){
                distributionSum += distribution.get(actionIndex);
                if(distributionSum > randomNumber){
                    randomActionIndex = actionIndex;
                    break;
                }//end if.
            }//end for


            //Choose the action finally.
            randomAction= this.currentAgentAction[randomActionIndex].associatedAction("Useless Argument");


            //Add action nodes to the tree node, if it has not already been initialized.
            //Else just update the counter.
            ArrayList<ActionNode> actionNodes = new ArrayList<>();
            ArrayList<ArrayList<ObservationNode>> obsNodes = new ArrayList<>();
            //Add an action node.
            ActionNode actionNode = new ActionNode();
            actionNode.setAction(randomAction);
            actionNode.setNodeQValue((!isAvgQActionNode) ? 0.0 : this.weightedQValue[randomActionIndex]);

            actionNodes.add(actionNode);
            rolloutHistoryElement.setActionNodes(actionNodes);

            //Add an observation node.
            obsNodes.add(new ArrayList<>());
            rolloutHistoryElement.setObservationNodes(obsNodes);

            //Add the history element to the tree node.
            ArrayList<HistoryElement> historyElements = new ArrayList<>();
            historyElements.add(rolloutHistoryElement);
            node.setHistoryElement(historyElements);


            // Find the next node after performing fac and random action on the current belief sample.
            // No Addition of any nodes.
            TreeNode nextBeliefNode = blackBoxSimulator(beliefParticle,node,0,0,true);


            if(nextBeliefNode.getValue() > 0){
//                System.out.println("In Rollout: Visit:" + this.count + " Path:" + node.getPath());
                this.rolloutRewardCount++;
            }

            //Increment the horizon value.
            this.currentHorizon++;

            //Reward value calculation.
            //There is only one particle, which can be sampled here. So, nothing to worry about.
            reward = nextBeliefNode.getValue() + this.gamma * rollOut(nextBeliefNode.sampleParticle(),nextBeliefNode,wildfireDomain);

        }catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        //Return Reward.
        return  reward;
    }//end method.

    /**
     * This method finds the best action for the current agent by iterating through all the actions availabie to
     * that agent and calculating the Q-Value for each.
     *  Argmax_a  Q(p,a,C_h) +  C * ( log(C_h count) / (A_i count))^1/2
     * @param node is the current node being explored.
     * @param branch is the history element of the node being explored.
     * @return is the index of the best action available at the current node.
     */
    public int getBestBanditAction(TreeNode node,int branch){
        int bestActionIndex = -1;
        double bestActionValue = -1000000;


        //Get information of the configuration node.
        FACNode facNode = node.getHistoryElement().get(branch).getConfigurationNode();
        //Get all the action nodes.
        ArrayList<ActionNode> actionNodes = node.getHistoryElement().get(branch).getActionNodes();

        //Choose the best action
        for(int actionNode = 0 ; actionNode < actionNodes.size(); actionNode++){

            //As the counter of the action is zero, the Q-value would be infinite.
            //Else choose an action with the highest value.
            if(actionNodes.get(actionNode).getNodeVisit() == 0){
                bestActionIndex = actionNode;
                break;
            }else{
                double banditQValue = actionNodes.get(actionNode).getNodeQValue() +
                                        this.C * Math.sqrt( (Math.log(facNode.getNodeVisit())/ actionNodes.get(actionNode).getNodeVisit()));
                if( banditQValue > bestActionValue){
                    bestActionIndex = actionNode;
                    bestActionValue = banditQValue;
                }//end if.
            }//end if-else actionNodes.
        }//end for.

        if(bestActionIndex == -1){
            System.out.println("Problem");
        }
        return  bestActionIndex;
    }

    /**
     * Samples a set of neighbors to model from the {@link WildfireDomain}.
     *
     * @param wildfireDomain The {@link WildfireDomain} object storing the neighborhoods
     */
    public void sampleAgents(WildfireDomain wildfireDomain) {
        // first, create the list of fire neighborhoods
        List<List<Integer>> fireNeighborhoods = new ArrayList<List<Integer>>();
        for (int i = 0; i < wildfireDomain.fireList.size(); i++) {
            fireNeighborhoods.add(new ArrayList<Integer>());
        }

        List<Integer> fires;
        int start, end;
        String[] split;
        for (String agentGroup : wildfireDomain.agentGroups) {
            split = agentGroup.split("-");
            start = Integer.parseInt(split[0]);
            end = Integer.parseInt(split[1]);

            // get the fires this group can fight
            fires = wildfireDomain.agentFireNeighbourhood.get(agentGroup);

            for (Integer fire : fires) {
                for (int agent = start; agent <= end; agent++) {
                    if (agent != this.currentAgentNumber) {
                        fireNeighborhoods.get(fire).add(agent);
                    }
                }
            }
        }

        // sort the fires neighborhoods by size
        // NOTE: after this, the index into the outer list is no longer the fire number
        for (int i = 0; i < fireNeighborhoods.size(); i++) {
            int j = i;
            List<Integer> temp;
            while (j > 0 && fireNeighborhoods.get(j - 1).size() < fireNeighborhoods.get(j).size()) {
                temp = fireNeighborhoods.remove(j-1);
                fireNeighborhoods.add(j, temp);
            }
        }

        // sample the agents to model
        this.sampledAgents.clear();

        int N, n;
        List<Integer> selected;
        for (List<Integer> fire : fireNeighborhoods) {
            N = fire.size();
            n =  getNumberOfSamples(N);

            sampleNeighbors(fire, n);
        }
    }

    /**
     * Samples a set of neighbors from a fire's neighborhood.
     *
     * @param fire The list of agent numbers who can fight a particular fire
     * @param n The number of neighbors in {@code fire} to sample
     */
    private void sampleNeighbors(List<Integer> fire, int n) {
        List<Integer> modeled = new ArrayList<Integer>();
        List<Integer> notModeled = new ArrayList<Integer>();
        boolean found;
        for (Integer agentNum : fire) {
            found = false;
            for (Agent agent : this.sampledAgents) {
                if (agentNum == agent.getAgentNumber()) {
                    found = true;
                    break;
                }
            }

            if (found) {
                modeled.add(agentNum);
            } else {
                notModeled.add(agentNum);
            }
        }

        int randIndex, agentNum;
        Random rng = new Random();
        while (modeled.size() < n && notModeled.size() > 0) {
            randIndex =  rng.nextInt(notModeled.size());
            agentNum = notModeled.remove(randIndex);
            modeled.add(agentNum);
            this.sampledAgents.add(new Agent(wildfireDomain.agentsList.get(agentNum)));
        }
    }


     /**
     * This method calculates the number of agents to sample given the confidence interval and allowed error rate in the
     * samping of the agents.
      * The method uses the equation n >= ( (N * ((t_n-1,(alpha/2))/ 2* e_p)^2 ) / (N - 1 + ((t_n-1,(alpha/2))/ 2* e_p)^2)
      * for satisfying the number of agents to sample.
     * @param typeAgents is the number of agents of a particular type in the population.
     * @return number of agents to sample given the criteria.
     */
     public int getNumberOfSamples(int typeAgents){
         int sampleSize = 0;


         //In case of no extrapolation, return the same number of agents.
         if(this.alpha == 0 && this.samplingErrorBound == 0){
             return typeAgents;
         }//end if.

         for(int n=2; n<=typeAgents; n++){
             //Get the t-value.
             TDistribution t = new TDistribution( n - 1);
             double tValue = t.inverseCumulativeProbability(1 - (this.alpha / 2));

             //Calculate the ratio.
             double ratio = Math.pow((tValue/(2 * this.samplingErrorBound)),2);

             //Calculate the overall value.
             double overallRatio =  (typeAgents * ratio) / (typeAgents - 1 + ratio);

             //Put the comparison condition for the n.
             if(n >= overallRatio){
                 sampleSize = n;
                 break;
             }//end if.
         }//end for.

         return sampleSize;
     }


    /**
     * Sample the particleCount number of particles from the list of particles available in the root node.
     * If no particles available, sample them randomly.
     * @param initialState is the initial Wildfire state.
     * @param masterState is initial Master state.
     * @param currentParticles is the belief particles in the root node.
     * @return is the sampled particles.
     */
     public ArrayList<BeliefParticle> sampleBeliefParticles(State initialState,State masterState, ArrayList<BeliefParticle> currentParticles){

         //Belief Particles.
         ArrayList<BeliefParticle> beliefParticles = new ArrayList<BeliefParticle>();
         int randomParticle = -1;

         try{
             //Sample particles up to maximum limit.
             for(int particle=0; particle < this.particleCount; particle++){
                 //Copy the initial state to all the particles.
                 State beliefStateParticle = new WildfireAnonymousState((WildfireAnonymousState) initialState);

                 ArrayList<Integer> mentalModel = null;
                 //If the particles are already available after belief update, choose one from them. Sample
                 //totally random otherwise.
                 if(currentParticles != null && currentParticles.size() != 0){
                     randomParticle = (int)Math.round(Math.random() * (currentParticles.size()-1));
                     mentalModel = currentParticles.get(randomParticle).getMentalModels();
                 }else{
                     mentalModel = sampleRandMentalModel();
                 }//end if-else.


                 //Add the particle into the list.
                 beliefParticles.add(new BeliefParticle(beliefStateParticle,mentalModel));
             }//end for.
         }catch (Exception e){
             System.out.println("Error while sampling particles:" + e.getMessage());
             System.out.println("Random Number:" + randomParticle + " Sample size:" + currentParticles.size());
             e.printStackTrace();
             System.exit(0);
         }

         return  beliefParticles;
     }//end for.


    /**
     * This method generates a sample for the mental model of the sampled agents.
     * @return is one sampled Mental model of the fire fighter agents.
     */
     public ArrayList<Integer> sampleRandMentalModel(){

         //Create a list of mental model for the sampled agents.
         ArrayList<Integer> mentalModel = new ArrayList<Integer>(this.sampledAgents.size());



         if(this.isSuppOpen){
             for(int agent=0; agent< this.sampledAgents.size(); agent++){
                 int agentMentalModel = 2;//Agent's state.
                 //Based on the setup-14.
                 if(this.sampledAgents.get(agent).getAgentNumber() < 10){
                     agentMentalModel = 2;
                 }else if(this.sampledAgents.get(agent).getAgentNumber() < 20){
                     agentMentalModel = 1;
                 }else{
                     agentMentalModel = 0;
                 }//end if-else.

                 mentalModel.add(agentMentalModel);
             }//end for.
         }else{
             //Suppressant distribution for level-1 and 2.
             double[] suppressantDistribution = {0.5,0.5};

             for(int agent=0; agent< this.sampledAgents.size(); agent++){
                 double distributionSum = 0.0;
                 int agentMentalModel = 2; //Default suppressant level is 2.

                 //Generate the suppressant level of the agents using the 2/3 to 1/3 distribution for level 2 and level 1.
                 double randomNumber = generator.nextDouble();
                 for(int availability = 1 ; availability <= suppressantDistribution.length; availability++ ){
                     distributionSum += suppressantDistribution[availability-1];
                     if(randomNumber <= distributionSum){
                         agentMentalModel = availability;
                         break;
                     }//end if.
                 }//end for.
                 mentalModel.add(agentMentalModel);
             }//end for.
         }//end if-else isSuppOpen.


         return mentalModel;
     }//end method.


    /**
     * This method samples the Frame Action Configuration given the mental models of the sampled agents and the
     * sampled state. To find the action of the agent in current circumstances, the policies of the agents are
     * being loaded into the IPOMCP.agentPolicies variable, while the method uses IPOMCP.possibleConfiguration
     * for finding the type-total agent pair to extrapolate the sampled action from the policy. In the end the
     * overall FAC is returned.
     * @param beliefParticle contains the state and mental models for calculating the fac.
     * @param wildfireDomain is the wildfireDomain object.
     * @return is the sampled FAC.
     */
    public FrameActionConfiguration sampleFAC(BeliefParticle beliefParticle, WildfireDomain wildfireDomain){
         //Create a copy of the possible configuration available.
         FrameActionConfiguration sampledFAC = new FrameActionConfiguration(this.possibleMaxConfiguration,false);
         FrameActionConfiguration extraPolatedFAC = new FrameActionConfiguration(this.possibleMaxConfiguration,false);
         Map<Integer,Integer> sampledAgentsPerType = new HashMap<>();
         double[] actionDistribution = new double[this.possibleMaxConfiguration.getMaxConfiguration().length];

         //For each sampled agent, find the best action from the policy and mental model.
         for(int agent=0; agent < this.sampledAgents.size() ; agent++){
             //Create Other agent's state.
             State otherAgentState = Wildfire.createOtherAgentAnmState(wildfireDomain,this.sampledAgents.get(agent).getAgentGroup(),
                     beliefParticle.getStateParticle());
             //Set the Availability according to the sampled Mental model.
             ((WildfireAnonymousState)otherAgentState).getSelfAgent().setAvailability(beliefParticle.getMentalModels().get(agent));
             //Also, set the agent's self agent as the first agent in the group to find the action from the policy.
             ArrayList<String> startEndIndexes =new ArrayList<> (Arrays.asList(this.sampledAgents.get(agent).getAgentGroup().split("-")));
             int startIndex = Integer.parseInt(startEndIndexes.get(0));
             ((WildfireAnonymousState)otherAgentState).getSelfAgent().setAgentNumber(startIndex);

             //Get the Action from the policy file.
             Action action = this.agentPolicies
                                .get(((WildfireAnonymousState) otherAgentState).getSelfAgent().getAgentGroup())
                                .action(otherAgentState);
             //Set the sampled agent action for a help in mental models.
             beliefParticle.getSampledActions().add(action);


             //If the configuration exist, then get the configuration and add 1 to the value.
             int typeActionIndex = sampledFAC.search(((WildfireAnonymousState) otherAgentState).getSelfAgent().getPowerType(),action.actionName());
             //If the pair does not exist than start with 1, otherwise add 1 to the current pair.
             if(typeActionIndex == -1){
                 sampledFAC.putCurrentConfiguration(((WildfireAnonymousState) otherAgentState).getSelfAgent().getPowerType(),
                         action,1);
             }else{
                 int numOfAgents = sampledFAC.getCurrentConfiguration()[typeActionIndex].getNumberOfAgents();

                 sampledFAC.putCurrentConfiguration(((WildfireAnonymousState) otherAgentState).getSelfAgent().getPowerType(),
                         action,numOfAgents+1);
             }//end if-else.

             //Get the number of sampled agents per type. Create a new map of the agents, if not created or add one.
             if(sampledAgentsPerType.get(this.sampledAgents.get(agent).getPowerType()) == null){
                 sampledAgentsPerType.put(this.sampledAgents.get(agent).getPowerType(),1);
             }else{
                 sampledAgentsPerType.put(this.sampledAgents.get(agent).getPowerType(),sampledAgentsPerType.get(this.sampledAgents.get(agent).getPowerType()) + 1);
             }//end if -else.
         }//end loop.

        //Create the action distribution type-wise. The sum of type-action pair would be 1 for each type of configuration.
        for(int configCount = 0; configCount < sampledFAC.getMaxConfiguration().length ; configCount++){
             actionDistribution[configCount] = (double)sampledFAC.getCurrentConfiguration()[configCount].getNumberOfAgents() /
                                                        (double)sampledAgentsPerType.get(sampledFAC.getCurrentConfiguration()[configCount].getAgentType());
        }//end for.


        int counter = 0;
        for(int typeIndex = 0 ; typeIndex < extraPolatedFAC.getAgentTypes().length ; typeIndex++){
             //Get the type of the agents.
             int type = extraPolatedFAC.getAgentTypes()[typeIndex][0];
             //Iterate through actions for each agents.
            for(int agentIndex = 0 ; agentIndex < extraPolatedFAC.getAgentTypes()[typeIndex][1];){
                double randomNumber = Math.random();

                double cumulativeSum = 0;
                for(int configCount = 0; configCount < extraPolatedFAC.getMaxConfiguration().length ; configCount++){
                    if(extraPolatedFAC.getMaxConfiguration()[configCount].getAgentType() == type){
                        //Current action.
                        Action action = extraPolatedFAC.getMaxConfiguration()[configCount].getAction();
                        cumulativeSum += actionDistribution[configCount];
                        if(cumulativeSum >= randomNumber){
                            //If the configuration exist, then get the configuration and add 1 to the value.
                            int typeActionIndex = extraPolatedFAC.search(type,action.actionName());

                            //If the agents are already touching the max then regenerate the random number.
                            if(extraPolatedFAC.getCurrentConfiguration()[typeActionIndex].getNumberOfAgents()
                                    == extraPolatedFAC.getMaxConfiguration()[typeActionIndex].getNumberOfAgents()){
                                break;
                            }

                            agentIndex++;
                            counter++;
                            //If the pair does not exist than start with 1, otherwise add 1 to the current pair.
                            if(typeActionIndex == -1){
                                extraPolatedFAC.putCurrentConfiguration(type,action,1);
                            }else{
                                int numOfAgents = extraPolatedFAC.getCurrentConfiguration()[typeActionIndex].getNumberOfAgents();
                                extraPolatedFAC.putCurrentConfiguration(type,action,numOfAgents+1);
                            }//end if-else.
                            break;
                        }//end if.
                    }//end if.
                }//end for.
            }//end for.
        }//end for.

        if(counter != extraPolatedFAC.getTotalAgents()){
            System.out.println("Problem in sampling FAC.");
            System.exit(0);
        }

         return extraPolatedFAC;
    }



    /**
     * Get the value of the current node from the tree and calculate the reward using the UCT.
     * The value function has an constant to add in every values is C* (( log N(h)/ N(hb)) ^ 1/2).
     * The overall value function can be represented by V(h-action) + Heuristic Value.
     * @param s is the current state.
     * @param a is the action to perform.
     * @return the reward after performing the action on the state.
     */
    @Override
    public double qValue(State s, Action a) {
        return 0;
    }


    /**
     * The Black-Box Simulator simulates the action into the current state and samples an observation.
     * Given all this it returns a new node with all the new state.
     * @param beliefParticle is the belief particle to use for simulation.
     * @param treeNode is the current belief node in exploration.
     * @param facIndex is the index of the FAC (History Element) used for simulation in the treeNode.
     * @param actionIndex is the index of the action index against the FAC in the tree node.
     * @param isRollout if the simulator is only called for rollout.
     * @return is the new node simulated with a sample observation.
     */
    public TreeNode blackBoxSimulator(BeliefParticle beliefParticle, TreeNode treeNode,
                                                        int facIndex,int actionIndex, boolean isRollout){
        //next Belief node.
        TreeNode nextBeliefNode = null;
        //Create a new Belief Particle.
        BeliefParticle nextBeliefParticle = null;
        //Fire Observation.
        FireObservation fireObservation = null;
        //Path
        String path = null;
        try{
            //Get the observation, next state and the reward.

            //Find the next state after transition.
            State nextState = this.jointIPOMCPModel.sample(beliefParticle.getStateParticle(),treeNode,facIndex,actionIndex);

            //Create a new mental model.
            //Get the next mental model after the transition.
            ArrayList<Integer> nextMentalModels = new ArrayList<>();
            for(int mentalModel = 0 ; mentalModel < beliefParticle.getMentalModels().size() ; mentalModel++){
                nextMentalModels.add(
                        this.jointIPOMCPModel.sampleInternalTransition(beliefParticle.getMentalModels().get(mentalModel)
                                ,beliefParticle.getSampledActions().get(mentalModel)));
            }

            //Sample the observation and then create the Observation node.
            fireObservation = (FireObservation) (this.wildfireDomain.getPartialObservationFunction().sample
                    (beliefParticle.getStateParticle(), nextState,
                            treeNode.getHistoryElement().get(facIndex).getActionNodes()
                                    .get(actionIndex).getAction()));


            //Get the Path string
            path = treeNode.getPath() + "-" + facIndex + "-" + actionIndex ;

            //Add the observation index to path.



            boolean createNewNode = false;

            //If it's not roll out, search for the nodes in the existing tree.
            if(!isRollout){
                int obsIndex = treeNode.obsExist(facIndex,actionIndex,fireObservation);
                if(obsIndex != -1 ){
                    path += "-" + obsIndex;
                    //Get the existing node if it exist, and add the belief particle as well as increment the visit value.
                    nextBeliefNode = this.ipomcpTree.getNode(Objects.hashCode(path));
                    nextBeliefParticle = new BeliefParticle(nextState,nextMentalModels);
                    nextBeliefNode.getBeliefParticles().add(nextBeliefParticle);
                }else{
                    path += "-" + (treeNode.getHistoryElement().get(facIndex).getObservationNodes().get(actionIndex).size());
                    createNewNode = true;
                }//end if-else.
            }//end if.



            if(createNewNode || isRollout){//In case of roll out.
                //Create a new Node, but don't add it to the tree.
                nextBeliefNode = new TreeNode();
                nextBeliefParticle = new BeliefParticle(nextState,nextMentalModels);
                nextBeliefNode.getBeliefParticles().add(nextBeliefParticle);

                //Set the path.
                nextBeliefNode.setPath(path);


                //Don't add any observation on the maximum horizon.
                if(this.currentHorizon != this.maxHorizons-1){
                    //Set the observation node reference.
                    ObservationNode observationNode = new ObservationNode(fireObservation,nextBeliefNode.hashCode());
                    //Add the observation node to the current tree.
                    treeNode.getHistoryElement().get(facIndex).getObservationNodes().get(actionIndex).add(observationNode);
                }//end if.
            }//end if.

            //Set Reward for the next node.
            nextBeliefNode.setValue(this.rewardFunction.reward(beliefParticle.getStateParticle(),
                    treeNode.getHistoryElement().get(facIndex).getActionNodes().get(actionIndex).getAction(),
                    treeNode.getHistoryElement().get(facIndex).getConfigurationNode().getFac(),
                    nextState));
        }catch (Exception e){
            System.out.println("Node:" + nextBeliefNode);
            System.out.println("Path" + path + " Hashcode:" + Objects.hashCode(path));
        }



        return nextBeliefNode;
    }//end method.

    /**
     * Belief update in IPOMCP is merging all the nodes immediate from the root node obtained after performing the real action ,
     * and real observation. As the real next state would be available from simulator, the actual belief update would only
     * merge all the mental models of the other agents.
     * @param realObservation is the actual observation received from game engine.
     * @param realAction is the last action performed.
     */
    public void beliefUpdate(State realObservation,Action realAction){
        long start = System.currentTimeMillis();
        FireObservation fireObservation = (FireObservation)realObservation;

        // grab the correct particle filter
        ArrayList<BeliefParticle> beliefParticles = this.nextParticles.get(fireObservation.getFireDifference());
        System.out.println(this.currentAgentNumber +": " + beliefParticles.size());

        //Set it to the new root node.
        TreeNode newRootNode = new TreeNode();
        newRootNode.setBeliefParticles(beliefParticles);

        //Trash the IPOCMP Tree, and create a new instance.
        ipomcpTree = new IPOMCPTree();

        //Set the root to the new root node.
        this.rootNode = newRootNode;
        this.ipomcpTree.setNode(rootNode);
        long end = System.currentTimeMillis();
//        System.out.println("Belief Update in TIME:" + (end-start));
    }




    @Override
    public void resetSolver() {

    }

    @Override
    public Policy planFromState(State initialState) {
        return null;
    }

    @Override
    public List<QValue> qValues(State s) {
        return null;
    }



    @Override
    public double value(State s) {
        return 0;
    }


    //Getter and Setter methods.
    public int getCurrentAgentNumber() {
        return currentAgentNumber;
    }

    public void setCurrentAgentNumber(int currentAgentNumber) {
        this.currentAgentNumber = currentAgentNumber;
    }

    public ActionType[] getCurrentAgentAction() {
        return this.currentAgentAction;
    }

    public void setCurrentAgentAction(ActionType[] currentAgentAction) {
        this.currentAgentAction = currentAgentAction;
    }


    public boolean isRandom() {
        return isRandom;
    }

    public void setRandom(boolean random) {
        isRandom = random;
    }




//    public static void main(String args[]){
//        TDistribution t = new TDistribution(32);
//        double tValue = t.inverseCumulativeProbability(1 - (0.05/2));
//
//        System.out.println("TValue:" + tValue);
//
//        //Calculate the ratio.
//        double ratio = Math.pow((tValue/(2 * 0.1)),2);
//
//
//        System.out.println("Ratio:" + ratio);
//        //Calculate the overall value.
//
//        double overallRatio =  (50 * ratio) / (50 - 1 + ratio);
//
//
//        System.out.println("Overall Ratio:" + overallRatio);
//
//        //Put the comparison condition for the n.
//        if(33 >= overallRatio){
//            System.out.println("DONE");
//        }//end if.
//    }
}//end class.
