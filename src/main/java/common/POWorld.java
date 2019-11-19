package common;

import burlap.behavior.stochasticgames.GameEpisode;
import burlap.behavior.stochasticgames.JointPolicy;
import burlap.debugtools.DPrint;
import burlap.mdp.auxiliary.StateGenerator;
import burlap.mdp.auxiliary.StateMapping;
import burlap.mdp.auxiliary.common.ConstantStateGenerator;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.mdp.stochasticgames.world.World;
import burlap.mdp.stochasticgames.world.WorldObserver;
import domains.wildfire.*;
import posg.POOOSGDomain;
import posg.TabularBeliefState;
import posg.model.FACRewardFunction;
import posg.model.PartialObservationFunction;
import scalability.FrameActionConfiguration;
import simulators.SimulatorHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class provides a means to have agents play against each other and synchronize all of their actions and observations.
 * Any number of agents can join a World instance and they will be told when a game is starting, when a game ends, when
 * they need to provide an action, and what happened to all agents after every agent made their action selection. The world
 * may also make use of an optional {@link burlap.mdp.auxiliary.StateMapping} object so that agents are provided an
 * abstract and simpler representation of the world. A game can be run until a terminal state is hit, or for a specific
 * number of stages, the latter of which is useful for repeated games.
 * @author James MacGlashan
 *
 */
public class POWorld extends World {

    /* --Super attributes for reference --
    protected SGDomain domain;
    protected State                             currentState;
    protected List <SGAgent>                  agents;
    protected HashedAggregator<String>            agentCumulativeReward;
    protected JointModel worldModel;
    protected JointRewardFunction jointRewardFunction;
    protected TerminalFunction                  tf;
    protected StateGenerator initialStateGenerator;
    protected StateMapping                  abstractionForAgents;
    protected JointAction lastJointAction;
    protected List<WorldObserver>             worldObservers;
    protected GameEpisode currentGameEpisodeRecord;
    protected boolean                           isRecordingGame = false;
    protected int                               debugId;
    protected double[]                          lastRewards;

    */
    /**
     * Added class attributes.
     */
    protected POOOSGDomain pooosgDomain;
    protected PartialObservationFunction  observationFunction; //Observation Function.
    protected Map<String, TabularBeliefState> beliefStateMap; // Belief State Map for the agents, where the key is agent group or an agent.
    protected List<Map<String, SGAgent>> 	allAgentDefinitions;
    protected FACRewardFunction rewardFunction;


    /**
     * This constructor initializes the IPOMCP solver.
     * @param pooosgDomain is the domain object.
     * @param jr is the joint reward function.
     * @param tf is the terminal function.
     * @param obs is the observation function.
     * @param allAgentDefinitions is the agent definition object.
     * @param initialState is the initial anonymous state of the agent.
     * @param rewardFunction is the reward function for the agent.
     */
    public POWorld(POOOSGDomain pooosgDomain, JointRewardFunction jr,
                   TerminalFunction tf, PartialObservationFunction obs,
                   List<Map<String, SGAgent>> allAgentDefinitions,State initialState, FACRewardFunction rewardFunction ){
        super(pooosgDomain,jr, tf, new ConstantStateGenerator(initialState));
        this.init(pooosgDomain, obs,allAgentDefinitions, rewardFunction);
    }


    /**
     * Initializes the world.
     * @param pooosgDomain the SGDomain the world will use
     * @param jr the joint reward function
     * @param tf the terminal function
     * @param sg a state generator for generating initial states of a game
     */
    public POWorld(POOOSGDomain pooosgDomain, JointRewardFunction jr, TerminalFunction tf, StateGenerator sg,
                   PartialObservationFunction obs,List<Map<String, SGAgent>> allAgentDefinitions, FACRewardFunction rewardFunction){
        super(pooosgDomain,jr, tf,sg);
        this.init(pooosgDomain, obs,allAgentDefinitions, rewardFunction);
    }


    /**
     * Initializes the world
     * @param pooosgDomain the POOOSGDomain the world will use
     * @param jr the joint reward function
     * @param tf the terminal function
     * @param sg a state generator for generating initial states of a game
     * @param abstractionForAgents the abstract state representation that agents will be provided
     */
    public POWorld(POOOSGDomain pooosgDomain, JointRewardFunction jr, TerminalFunction tf, StateGenerator sg,
                                    StateMapping abstractionForAgents,PartialObservationFunction obs,
                                    List<Map<String, SGAgent>> allAgentDefinitions, FACRewardFunction rewardFunction){
        super(pooosgDomain,jr, tf,sg, abstractionForAgents);
        this.init(pooosgDomain, obs, allAgentDefinitions, rewardFunction);
    }

    protected void init(POOOSGDomain pooosgDomain,PartialObservationFunction observationFunction,
                        List<Map<String, SGAgent>> allAgentDefinitions, FACRewardFunction rewardFunction){
        this.pooosgDomain = pooosgDomain;
        this.observationFunction = observationFunction;
        this.beliefStateMap = new HashMap<>();
        this.allAgentDefinitions = allAgentDefinitions;

        this.rewardFunction = rewardFunction;
    }

    //Getter and Setter methods for the newly added objects.

    public POOOSGDomain getPooosgDomain() {
        return pooosgDomain;
    }

    public void setPooosgDomain(POOOSGDomain pooosgDomain) {
        this.pooosgDomain = pooosgDomain;
    }

    public PartialObservationFunction getObservationFunction() {
        return observationFunction;
    }

    public void setObservationFunction(PartialObservationFunction observationFunction) {
        this.observationFunction = observationFunction;
    }

    public Map<String, TabularBeliefState> getBeliefStateMap() {
        return beliefStateMap;
    }

    public void setBeliefStateMap(Map<String, TabularBeliefState> beliefStateMap) {
        this.beliefStateMap = beliefStateMap;
    }

    /**
     * Manually attempts to execute a joint action in the current world state, if a game is currently not running.
     * If a game is running, then no action will be taken. Additionally, if the world is currently in a terminal
     * state, then no action will be taken either.
     * @param ja the {@link JointAction} to execute.
     */
    public void executeJointAction(JointAction ja){

        if(!this.gameIsRunning()){
            if(tf.isTerminal(currentState)){
                return ; //cannot continue this game
            }

            State oldState = this.currentState;
            this.currentState = this.worldModel.sample(this.currentState, ja);
            double[] rewards = this.jointRewardFunction.reward(oldState, ja, this.currentState);
            this.lastRewards = rewards;
            this.lastJointAction = ja;

            for(WorldObserver ob : this.worldObservers){
                ob.observe(oldState, ja, rewards, this.currentState);
            }

        }

    }

    /**
     * Runs a game until a terminal state is hit.
     * @return a {@link GameEpisode} of the game.
     */
    public GameEpisode runGame(){
        return this.runGame(-1);
    }

    /**
     * Runs a game until a terminal state is hit for maxStages have occurred
     * @param maxStages the maximum number of stages to play in the game before its forced to end. If set to -1, then run until a terminal state is hit.
     * @return a {@link GameEpisode} of the game.
     */
    public GameEpisode runGame(int maxStages){

        return this.runGame(maxStages, initialStateGenerator.generateState());

    }


    /**
     * Runs a game starting in the input state until a terminal state is hit.
     * @param maxStages the maximum number of stages to play in the game before its forced to end. If set to -1, then run until a terminal state is hit.
     * @param s the input {@link State} from which the game will start
     * @param experimentType is the string representing type of the experiment.
     * @return a {@link GameEpisode} of the game.
     */
    public GameEpisode runGame(int maxStages, State s, String experimentType){

        int aid = 0;
        for(SGAgent a : agents){
            a.gameStarting(this, aid);
            aid++;
        }

        //Create a copy of the state, so that the modifications does not impact.
        currentState = new WildfireState((WildfireState) s);
        this.currentGameEpisodeRecord = new GameEpisode(currentState);
        this.isRecordingGame = true;

        int t = 0;



        for(WorldObserver wob : this.worldObservers){
            wob.gameStarting(this.currentState);
        }

        while(!tf.isTerminal(currentState) && (t < maxStages || maxStages == -1)){
            String predictionLogString = "Stage: " + t + "\n";
            predictionLogString  += "Agent Number,Agent Group,State,Agent Actions," +
                    "Predictions(Visit- Weighted Q Value pair)," +
                    "Chosen Action  \n";
            SimulatorHelper.addPrediction(predictionLogString);

            String configLogString = "\nStage: " + t + "\n";
            configLogString += "Predicted Configurations:\n";
            SimulatorHelper.addConfigInfo(configLogString);

            String treeLogString = "\nStage: " + t + "\n";
            SimulatorHelper.addParticleLog(treeLogString);

            treeLogString += "IPOMCP Trees:\n";
            SimulatorHelper.addTreeInfo(treeLogString);




//            String beliefLogString = "Stage: " + t + "\n";
//            beliefLogString += "Actual State: \n";
//            SimulatorHelper.addConfigInfo(beliefLogString);



            System.out.println("Stage:" + t);


            //Check the type of experiment and run the runStage method accordingly.
            if(experimentType.equals(WildfireParameters.IPOMCP_EXPERIMENT)){
                this.runStage();
                SimulatorHelper.addPrediction("\n");
                SimulatorHelper.addTreeInfo("\n");
                SimulatorHelper.addParticleLog("\n");
            }else{
                this.runStage(experimentType);
            }
            t++;
        }

        for(SGAgent a : agents){
            a.gameTerminated();
        }

        for(WorldObserver wob : this.worldObservers){
            wob.gameEnding(this.currentState);
        }

        DPrint.cl(debugId, currentState.toString());

        this.isRecordingGame = false;

        return this.currentGameEpisodeRecord;
    }






    /**
     * Rollsout a joint policy until a terminate state is reached for a maximum number of stages.
     * @param jp the joint policy to rollout
     * @param maxStages the maximum number of stages
     * @return a {@link GameEpisode} that has recorded the result.
     */
    public GameEpisode rolloutJointPolicy(JointPolicy jp, int maxStages){
        currentState = initialStateGenerator.generateState();
        this.currentGameEpisodeRecord = new GameEpisode(currentState);
        this.isRecordingGame = true;
        int t = 0;

        while(!tf.isTerminal(currentState) && t < maxStages){
            this.rolloutOneStageOfJointPolicy(jp);
            t++;
        }

        this.isRecordingGame = false;

        return this.currentGameEpisodeRecord;
    }



    /**
     * Rollsout a joint policy from a given state until a terminate state is reached for a maximum number of stages.
     * @param jp the joint policy to rollout
     * @param s the state from which the joint policy should be rolled out
     * @param maxStages the maximum number of stages
     * @return a {@link GameEpisode} that has recorded the result.
     */
    public GameEpisode rolloutJointPolicyFromState(JointPolicy jp, State s, int maxStages){
        currentState = s;
        this.currentGameEpisodeRecord = new GameEpisode(currentState);
        this.isRecordingGame = true;
        int t = 0;

        while(!tf.isTerminal(currentState) && t < maxStages){
            this.rolloutOneStageOfJointPolicy(jp);
            t++;
        }

        this.isRecordingGame = false;

        return this.currentGameEpisodeRecord;
    }

    /**
     * Runs a single stage of this game.
     */
    public void runStage(String experimentType){

        //Agent Group states in case of Nested VI Experiment.
        HashMap<String,State> agentGroupStates = null;

        if(tf.isTerminal(currentState)){
            return ; //cannot continue this game
        }


        //Get the anonymous initial state for all the sample agent  in each group.
        if(experimentType.equals(WildfireParameters.NMDP_EXPERIMENT)){
            State masterState = new WildfireState((WildfireState) this.currentState);
            agentGroupStates = Wildfire.getAnmStateForAllGroups((WildfireDomain) this.pooosgDomain,masterState);
        }//end if.


        JointAction ja = new JointAction();

        //For each agent find the action and add it to the joint action.
        //In case of Nested VI experiment, convert current state into anonymous state.
        for(int agentIndex = 0 ; agentIndex < this.agents.size(); agentIndex++){
            if(experimentType.equals(WildfireParameters.NMDP_EXPERIMENT)){
                //This would create an anonymous state for a particular group with
                //the first agent in the group as the self-agent.

                //Note: The policies are built from the states having an anonymous view from the first agent of the group.
                //So, the state is kept with the first agent's view, but the suppressant level is matched with that
                //of the current agent.
                State anmState = agentGroupStates.get(((WildfireDomain)this.pooosgDomain).agentsList.get(agentIndex).getAgentGroup());

                //Set the availability that of the current agent.
                int availability = ((WildfireState)this.currentState).getAgentList().get(agentIndex).getAvailability();
                ((WildfireAnonymousState)anmState).getSelfAgent().setAvailability(availability);

                ja.addAction(this.agents.get(agentIndex).action(anmState));
            }else{
                ja.addAction(this.agents.get(agentIndex).action(this.currentState));
            }//end if-else.
        }//end for.
        this.lastJointAction = ja;


        DPrint.cl(debugId, ja.toString());


        //now that we have the joint action, perform it
        State sp = worldModel.sample(new WildfireState((WildfireState) this.currentState), ja);
//        State abstractedPrime = this.abstractionForAgents.mapState(sp);
        double[] jointReward = jointRewardFunction.reward(currentState, ja, sp);

        DPrint.cl(debugId, jointReward.toString());

        //index reward
        for(int i = 0; i < jointReward.length; i++){
            String agentName = this.agents.get(i).agentName();
            agentCumulativeReward.add(agentName, jointReward[i]);
        }


        //tell all the agents about it
        for(SGAgent a : agents){
            a.observeOutcome(null, ja, jointReward, sp, tf.isTerminal(sp));
        }

        //tell observers
        for(WorldObserver o : this.worldObservers){
            o.observe(currentState, ja, jointReward, sp);
        }

        //update the state
        currentState = sp;
        this.lastRewards = jointReward;

        //record events
        if(this.isRecordingGame){
            this.currentGameEpisodeRecord.transition(this.lastJointAction, this.currentState, jointReward);
        }

    }


    /**
     * Runs a stage with given current agent's index and best action learned by an algorithm(e.g. IPOMCP).
     */
    public void runStage(){

        if(tf.isTerminal(currentState)){
            return ; //cannot continue this game
        }

        //Current agent's action in the given situation.
        Action bestSearchedAction = null;


        JointAction ja = new JointAction();

        //Get the anonymous states of the agent.
        //There is a bug in the getAnmStateForAllGroups, which modifies the passed state object.
        //So, passing a new wildfire state object created from the currentState.
        State masterState = new WildfireState((WildfireState) this.currentState);
        HashMap<String,State> agentGroupStates = Wildfire.getAnmStateForAllGroups((WildfireDomain) this.pooosgDomain,masterState);


        //Iterate through all the agents, from the anonymous state they are having, find the best action from either IPOMCP
        //or according to their policy and create a joint action in the end.
        for(int agentIndex = 0 ; agentIndex < this.agents.size(); agentIndex++){
            State anmState = agentGroupStates.get(((WildfireDomain)this.pooosgDomain).agentsList.get(agentIndex).getAgentGroup());
            //Set the availability and agentIndex of the current agent.
            //This would save a bit overhead in creating, a new object.
            int availability = ((WildfireState)this.currentState).getAgentList().get(agentIndex).getAvailability();
            ((WildfireAnonymousState)anmState).getSelfAgent().setAvailability(availability);
            ((WildfireAnonymousState)anmState).getSelfAgent().setAgentNumber(agentIndex);

            try{
                ja.addAction(this.agents.get(agentIndex).action(anmState));
            }catch (Exception e){
                System.out.println("Problem in IPOMCP:" + e.getMessage());
                System.exit(0);//Exit.
            }//end try catch.
        }//end for.
        this.lastJointAction = ja;


        DPrint.cl(debugId, ja.toString());


        //NOTE:Debugging Code.
        WildfireDomain wildfireDomain = (WildfireDomain)this.pooosgDomain;
        HashMap<String, Integer> actionMap = new HashMap<>();
        for(int agentIndex = 0 ; agentIndex < ja.size(); agentIndex++){
            Action action = ja.action(agentIndex);
            int agentType = wildfireDomain.agentsList.get(agentIndex).getPowerType();

            String configLable = action.actionName()+ "-" + agentType;

            if(actionMap.get(configLable) == null){
                actionMap.put(configLable,0);
            }
            actionMap.put(configLable, actionMap.get(configLable) + 1);
        }//end for.
        String configLogString = "\nActual Configuration:" + "\n";
        for(Map.Entry<String,Integer> entry: actionMap.entrySet()){
//            System.out.println("Chosen Actions:");
//            System.out.println("Action:" + entry.getKey() + " Value:" + entry.getValue());
            configLogString += entry.getKey() + "-" + entry.getValue() + ",";
        }//end for.

        configLogString += "\n";
        SimulatorHelper.addConfigInfo(configLogString);



        //now that we have the joint action, perform it
        State sp = worldModel.sample(currentState, ja);

        // Get the joint reward for each agent.
        double[] jointReward = rewardFunction.reward(currentState, ja, sp);

        DPrint.cl(debugId, jointReward.toString());

        //index reward
        for(int i = 0; i < jointReward.length; i++){
            String agentName = this.agents.get(i).agentName();
            agentCumulativeReward.add(agentName, jointReward[i]);
        }


        //Each agent observes all the fires in their neighbourhood at the end of the round.
        agentGroupStates = Wildfire.getAnmStateForAllGroups((WildfireDomain) this.pooosgDomain,sp);


        State fireObservation  = null;
        WildfireState previousState = (WildfireState)this.currentState;
        WildfireState nextState = (WildfireState)sp;


        //NOTE: Remove comments if it is needed for multiple agents to observe the results.
        for(int agentIndex = 0 ; agentIndex < this.agents.size(); agentIndex++){
            IPOMCPAgent ipomcpAgent = (IPOMCPAgent) this.agents.get(agentIndex);

            //Get the anonymous agent state.
            WildfireAnonymousState anmState = (WildfireAnonymousState) agentGroupStates.get(((WildfireDomain)this.pooosgDomain).
                                                                                agentsList.get(agentIndex).getAgentGroup());
            //Get the action of the current agent.
            bestSearchedAction = ja.action(agentIndex);


            //Create a fire observation for the agent for the fire it is fighting, No observation in case
            // of no action.
            if(bestSearchedAction.actionName().equals(WildfireParameters.NOOP_ACTION)){
                fireObservation = new FireObservation(WildfireParameters.NO_OBS);
            }else{
                for(int fire=0; fire < anmState.getFireList().length; fire++){
                    if(anmState.getFireList()[fire].fireActionName().equals(bestSearchedAction.actionName())){
                        int fireDiff = nextState.getFireList().get(anmState.getFireList()[fire].getFireNumber()).getIntensity()
                                                - previousState.getFireList().get(anmState.getFireList()[fire].getFireNumber()).getIntensity() ;
                        fireObservation = new FireObservation(fireDiff);
                        break;
                    }//end if.
                }//end for.
            }//end if-else.



            //Observe the outcome. Currently this thing does nothing.
            this.agents.get(agentIndex).observeOutcome(this.currentState, ja, jointReward, sp, tf.isTerminal(sp));


            // If the exploration is for the IPOMCP, then do the belief update.
            ipomcpAgent.ipomcp.beliefUpdate(fireObservation,bestSearchedAction);
        }
        //tell observers
        for(WorldObserver o : this.worldObservers){
            o.observe(currentState, ja, jointReward, sp);
        }

        //update the state
        currentState = sp;
        this.lastRewards = jointReward;


        //record events
        if(this.isRecordingGame){
            this.currentGameEpisodeRecord.transition(this.lastJointAction, this.currentState, jointReward);
        }
    }//end method.



    /**
     * Runs a single stage following a joint policy for the current world state
     * @param jp the joint policy to follow
     */
    protected void rolloutOneStageOfJointPolicy(JointPolicy jp){

        if(tf.isTerminal(currentState)){
            return ; //cannot continue this game
        }

        this.lastJointAction = (JointAction)jp.action(this.currentState);

        DPrint.cl(debugId, this.lastJointAction.toString());


        //now that we have the joint action, perform it
        State sp = worldModel.sample(currentState, this.lastJointAction);
        double[] jointReward = jointRewardFunction.reward(currentState, this.lastJointAction, sp);

        DPrint.cl(debugId, jointReward.toString());

        //index reward
        for(int i = 0; i < jointReward.length; i++){
            String agentName = this.agents.get(i).agentName();
            agentCumulativeReward.add(agentName, jointReward[i]);
        }


        //tell observers
        for(WorldObserver o : this.worldObservers){
            o.observe(currentState, this.lastJointAction, jointReward, sp);
        }

        //update the state
        currentState = sp;
        this.lastRewards = jointReward;

        //record events
        if(this.isRecordingGame){
            this.currentGameEpisodeRecord.transition(this.lastJointAction, this.currentState, jointReward);
        }

    }

    /**
     * @return the agent definitions for the agents registered in this world.
     */
    public List<SGAgentType> getAgentDefinitions(){
        List<SGAgentType> defs = new ArrayList<SGAgentType>(this.agents.size());
        for(SGAgent a : this.agents){
            defs.add(a.agentType());
        }
        return defs;
    }


    /**
     * Returns the player index for the agent with the given name.
     * @param aname the name of the agent
     * @return the player index of the agent with the given name.
     */
    public int getPlayerNumberForAgent(String aname){
        for(int i = 0; i < agents.size(); i++){
            SGAgent a = agents.get(i);
            if(a.agentName().equals(aname)){
                return i;
            }
        }
        return -1;
    }//end method.
}