package domains.wildfire;

import burlap.mdp.core.Domain;
import burlap.mdp.core.StateTransitionProb;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.SimpleAction;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import datastructures.Triple;
import domains.wildfire.beans.Agent;
import domains.wildfire.beans.Fire;
import org.thejavaguy.prng.generators.PRNG;
import org.thejavaguy.prng.generators.XorshiftPlus;
import pomcp.beans.TreeNode;
import posg.model.FullJointIPOMCPModel;
import scalability.FrameActionConfiguration;


public class WildfireMechanics implements FullJointIPOMCPModel {
    /** The {@link WildfireDomain}. */
    private WildfireDomain domain;

    /**
     * Spreading model of the fire.
     */
    private WildfireSpreadModel wildfireSpreadModel;
    /**
     * Use the presumed transition function or not.
     */
    public boolean usePresumedTF;
    /**
     * In case of reading the transition function, this is the object which would contain the parsed transition function.
     */
    public Map<Integer, Map<Integer, Map<Triple<Integer, SimpleAction, Integer>, Double>>> parsedAgentTransitionFunction;

    /**
     * Random Number Generator.
     */
    PRNG.Smart generator;

    int totalCounter;
    int highCount;

    /**
     * Default Constructor.
     * Put the presumed TF as false.
     */
    public WildfireMechanics(WildfireDomain domain) {
        //Set the domain.
        this.domain  = domain;

        // Create the spreading model object.
        wildfireSpreadModel = new WildfireSpreadModel(WildfireParameters.CELL_SIZE,WildfireParameters.WIND_DIRECTION);

        //Initialize generator with the XorShift ones.
        generator =  new XorshiftPlus.Smart(new XorshiftPlus());
        //Use the presumed Transition function.
        boolean usePresumedTF = false;
        //Transition Function Parsed.
        parsedAgentTransitionFunction =	new HashMap<Integer, Map<Integer, Map<Triple<Integer, SimpleAction, Integer>, Double>>>();

        totalCounter = 0;
        highCount = 0;
    }


    /**
     * NOTE: Don't use it. Need to fix.
     * This method converts the JointAction object into a FrameActionConfiguration.
     * JointAction object has an array list of actions and the index of this array list is the agent number, while
     * the FrameActionConfiguration object would contain an array of the (Action-Agent Type) pairs and their counts.
     * @param ja is the JointAction object for the domain.
     * @param d is the current domain object.
     * @return FrameActionConfiguration with required conversation.
     */
    @Override
    public FrameActionConfiguration convertJAtoFAC(JointAction ja, Domain d){
        //Convert it to wildfire domain
        WildfireDomain wf = (WildfireDomain)d;
        //Frame Action Configuration Object.
        FrameActionConfiguration fac = new FrameActionConfiguration();
        //Temporary variables to map.
        int agentType,numberOfAgents;
        //Count each agent-type and fire combinations and sum them up.
        for(int agentNum = 0; agentNum < ja.size() ; agentNum++){
            //Power is the agent-type.
            agentType = wf.agentsList.get(agentNum).getPowerType();
            //Get the current action. If the action is null then skip.
            if(ja.action(agentNum) == null){
                continue;
            }

            int searchConfig = fac.search(agentType,ja.action(agentNum).actionName());
            //If map already exist add one to the current numbers.
            if(searchConfig == -1){
                //Start the key-value pair with 1.
                fac.putCurrentConfiguration(agentType,ja.action(agentNum),1);
            }else{
                //Add 1 to the number of agents.
                numberOfAgents = fac.getCurrentConfiguration()[searchConfig].getNumberOfAgents();
                fac.putCurrentConfiguration(agentType,ja.action(agentNum),numberOfAgents+1);
            }//end if-else.
        }//end for.
        return  fac;
    }//end method.


    /**
     *  As the {@link WildfireState} is a factored state, all the factors in the state are sampled separately. So, all
     *  the fires, current agent's next suppressant level and other agents' suppressant levels are sampled separately
     *  on individual bases.
     * {@link WildfireState} is a master state without any self-agent information in current experiment of IPOMCP.
     * @param s is the current state.
     * @param ja is the current joint action.
     * @return next sampled state.
     */
    @Override
    public State sample(State s, JointAction ja) {
        //Get the current state.
        WildfireState currentState = (WildfireState)s;
        //Create a new state from the current state.
        WildfireState nextState = currentState.getSelfAgent().getAgentNumber() > WildfireParameters.MASTER_STATE_AGENT_NUM
                        ? Wildfire.getCleanState(this.domain, currentState.getSelfAgent().getAgentNumber())
                        : Wildfire.getCleanMasterState(this.domain);

        //Sample the fire intensity for the fires in the next state.
        for(Fire currentFire : currentState.getFireList()){
            nextState.getFireList().add( sampleFire(currentState,new Fire(currentFire),ja));
        }//end if.

        //Sample the current agent's availability transition, if it is not the master state.
        if(currentState.getSelfAgent().getAgentNumber() > WildfireParameters.MASTER_STATE_AGENT_NUM){
            nextState.setSelfAgent(sampleAgent(currentState.getSelfAgent(),ja.getActions().get(currentState.getSelfAgent().getAgentNumber())));
        }//end if.

        //Sample the other agent's availabilities in the next stage.
        for(Agent agent : currentState.getAgentList()){
            nextState.getAgentList().add(sampleAgent(new Agent(agent),ja.getActions().get(agent.getAgentNumber())));
        }//end for.

       return nextState;
    }//end method.


    /**
     * After applying joint action on the current fire, this method calculates probabilities of possible next fire intensities,
     * and samples a fire intensity level out of it.
     * @param currentState is the current WildFire state.
     * @param currentFire is the current fire object.
     * @param jointAction is the joint action by all the agents.
     * @return is the sampled next fire.
     */
    public Fire sampleFire(WildfireState currentState, Fire currentFire, JointAction jointAction){
        Fire nextFire = new Fire(currentFire);

        //Possible next fires.
        ArrayList<Fire> nextFires = new ArrayList<Fire>();

        // this fire burned out. it's intensity doesn't change
        // So, return the fire in the burned out state.
        if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
            System.out.println("Fire Number" + currentFire.getFireNumber() + " Intensity:" + currentFire.getIntensity());
            System.out.println("Next Fire Intensity:" + nextFire.getIntensity() + " Prob : 1.0" );
            return nextFire;
        }//end if.

        //Get the powers applied to the current fire.
        double powers = 0.0;
        for(int agentPower = 0; agentPower < jointAction.size(); agentPower++){
            //If the action is towards the current fire then add the powers.
            if(currentFire.fireActionName().equals(jointAction.getActions().get(agentPower).actionName())) {
                int powerType = domain.agentsList.get(agentPower).getPowerType();
                powers += domain.agentPower.get(powerType - 1);
            }//end if.
        }//end for.

        //NOTE: Due to discrepancies in the floating point sums.
        int powerInt = (int)powers;
        double powerFract = powers - (double)powerInt;
        if(powerFract > 0.99){
            powers += 0.01;
//            System.out.println("POWER Bug fix:" + powers);
        }


        //If someone is fighting the fire.
        if (((currentFire.getFireType()== WildfireParameters.BIG_FIRE && powers >= WildfireParameters.BIG_FIRE_THRESHOLD) ||
                (currentFire.getFireType()== WildfireParameters.SMALL_FIRE && powers >= WildfireParameters.SMALL_FIRE_THRESHOLD) ||
                (currentFire.getFireType()== WildfireParameters.VERY_BIG_FIRE && powers >= WildfireParameters.VERY_BIG_FIRE_THRESHOLD))
                && currentFire.getIntensity() > 0) {
            // Update the fire intensity.
            //NOTE: The conversion should discard the fractional part, which is the idea for adjusting fire.

            //The only fires are possible after the changes made is either with min intensity as shown in the
            //below equation or minIntensity + 1.

            int minIntensity = currentFire.getIntensity() - 1;

            nextFire.setIntensity(minIntensity);
            nextFires.add(nextFire);

            Fire nextFire1= new Fire(currentFire);
            nextFire1.setIntensity(minIntensity + 1);
            nextFires.add(nextFire1);

        } else {
            // no one worked on this location
            // was it already on fire?
            //Add the same state or something else
            if(currentFire.getIntensity() == 0){
                //Add the current state to the list.
                nextFires.add(currentFire);
                // this location was not on fire, but it might start on fire
                nextFire.setIntensity(Math.min(2,WildfireParameters.MAX_FIRE_STATES -2));
                nextFires.add(nextFire);
            }else if(currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES -2){
                //Add the current state to the list.
                nextFires.add(currentFire);
                //There are some chances that the fire can get burned out.
                nextFire.setIntensity(currentFire.getIntensity() + 1);
                nextFires.add(nextFire);
            }else{
                //The intensity would increase by 1 and return it.
                nextFire.setIntensity(currentFire.getIntensity() + 1);
                System.out.println("Fire Number" + currentFire.getFireNumber() + " Intensity:" + currentFire.getIntensity());
                System.out.println("Next Fire Intensity:" + nextFire.getIntensity() + " Prob : 1.0" );
                return  nextFire;
            }//end if-else if -else.
        }//end if-else. power.



        //Generate random probabilities and sum.
        double randomNumber = randomNumberGenerator();
        System.out.println("Fire Number" + currentFire.getFireNumber() + " Intensity:" + currentFire.getIntensity() + "Random Number:" + randomNumber
            + "Fire Type:" + currentFire.getFireType());
        //Cumulative probability after summing each fire intensity probability.
        double cumulativeProbs = 0.0;
        for(Fire fire : nextFires){
            double transProb = computeFireTransitionProbability(currentState,currentFire,jointAction,fire);
            System.out.println("Next Fire Intensity:" + fire.getIntensity() + " Trans Prob:" + transProb);
            cumulativeProbs += transProb;
            if(randomNumber <= cumulativeProbs){
                return fire;
            }//end if.
        }//end for.

        //If not generated properly throw exception.
        throw new RuntimeException("Could not sample next Fire because transition probabilities did not sum to 1; they summed to " + cumulativeProbs);
    }//end method.

    public Agent sampleAgent(Agent currentAgent, Action action){
        Agent nextAgent = new Agent(currentAgent);


        //Generate random probabilities and sum.
        double randomNumber = randomNumberGenerator();
        //Cumulative probability after summing each agent suppressant probability.
        double cumulativeProbs = 0.0;


        for(int nextAvailability = 0 ; nextAvailability < WildfireParameters.MAX_SUPPRESSANT_STATES; nextAvailability++){
            cumulativeProbs += computeInternalTransitionProbability(currentAgent.getAvailability(),nextAvailability,action);
            if(randomNumber <= cumulativeProbs){
                nextAgent.setAvailability(nextAvailability);
                return nextAgent;
            }//end if.
        }//end for.

        //If not generated properly throw exception.
        throw new RuntimeException("Could not sample next Agent availability because transition probabilities did not sum to 1; they summed to " + cumulativeProbs);
    }//end method.



    /**
     * Replacement of the actionHelper method in the previous version.
     * Sample the next output state by generating the transition probability randomly and use the FrameActionConfiguration
     * instead of the Joint Action.
     * @param s is the current state.
     * @param fac is the FrameActionConfiguration.
     * @return next sampled state.
     */
    @Override
    public State sample(State s, FrameActionConfiguration fac) {
        List<StateTransitionProb> transProbs = this.stateTransitions(s,fac);

        //Generate random probabilities and sum.
        double r = randomNumberGenerator();
        double sumProb = 0.0;
        for(StateTransitionProb trans : transProbs){
            sumProb += trans.p;
            if(r < sumProb){
                return trans.s;
            }
        }//end for.

        if(sumProb > 1.01 || sumProb < 0.99){
            System.out.println("Problem in the Transition: PROBSUM:" + sumProb);
        }

        System.out.println("Could not sample next state because transition probabilities did not sum to 1; they summed to " + sumProb);
        //If not generated properly throw exception.
        throw new RuntimeException("Could not sample next state because transition probabilities did not sum to 1; they summed to " + sumProb);
    }//end method.


    /**
     * This method samples the next state given the current state, current-agent's action and the
     * FAC of other agents(branchNumber and actionNumber are index to FAC and Action). The
     * method also updates the current node with the observation and reward value.
     * @param state is the current state.
     * @param node is the current belief-node.
     * @param branchNumber is the configuration branch.
     * @param actionNumber is the index of the action being taken for this FAC.
     * @return next state after sampling the possible states.
     */
    public State sample(State state, TreeNode node, int branchNumber, int actionNumber){
        //Find the next state.
        State nextState = null;

        try{
            //Get the Frame Action Configuration and the current agent's action.
            FrameActionConfiguration fac = new FrameActionConfiguration(node.getHistoryElement().get(branchNumber).getConfigurationNode().getFac(),false);
            Action action = node.getHistoryElement().get(branchNumber).getActionNodes().get(actionNumber).getAction();

            //Get anonymous state and check if the current agent's suppressant level is 0 or not.
            //If it's zero, then add 1 to the NOOP, otherwise find the index of the current agent's action to
            //merge it with current configuration.
            WildfireAnonymousState anonymousState = (WildfireAnonymousState)state;
            int configIndex = -1;

            if(anonymousState.getSelfAgent().getAvailability() == 0){
                //Add current action to the configuration and create a new inclusive fac.
                configIndex = fac.search(((WildfireAnonymousState)state)
                        .getSelfAgent().getPowerType(), WildfireParameters.NOOP_ACTION);
            }else{
                //Add current action to the configuration and create a new inclusive fac.
                configIndex = fac.search(((WildfireAnonymousState)state)
                        .getSelfAgent().getPowerType(), action.actionName());
            }


            //Add action to the FAC.
            if(configIndex != -1){
                int numOfAgents = fac.getCurrentConfiguration()[configIndex].getNumberOfAgents();
                fac.getCurrentConfiguration()[configIndex].setNumberOfAgents( numOfAgents + 1);
            }//end if.

            //Sample the next state after the transition.
            nextState = this.sample(state,fac);
        }catch (Exception e){
            e.printStackTrace();
        }

        return nextState;
    }

    /**
     * Replacement of the actionHelper method in the previous version.
     * Sample the next output state by generating the transition probability randomly and use the FrameActionConfiguration
     * instead of the Joint Action.
     * @param currentState is the current state.
     * @param action is the action performed by the agent.
     * @return sampled next Internal state from the transition.
     */

    public int sampleInternalTransition(int currentState, Action action) {

        //Transition Probs for the internal transition.
        ArrayList<Double> sampledTransition = new ArrayList<>();
        //Get probability for next internal transition.
        for(int availability = 0 ; availability < WildfireParameters.MAX_SUPPRESSANT_STATES ; availability++){
            sampledTransition.add(internalTransitionProbs(currentState,action,availability));
        }


        //Generate random probabilities and sum.
        double r = randomNumberGenerator();
        double sumProb = 0.0;
        for(int availability = 0 ; availability < WildfireParameters.MAX_SUPPRESSANT_STATES ; availability++){
            sumProb += sampledTransition.get(availability);
            if(r < sumProb){
                return availability;
            }
        }//end for.

        if(sumProb > 1.01 || sumProb < 0.99){
            System.out.println("Problem in the Transition: PROBSUM:" + sumProb);
        }

        //If not generated properly throw exception.
        throw new RuntimeException("Could not sample next state because transition probabilities did not sum to 1; they summed to " + sumProb);
    }//end method.

    /**
     * NOTE: Not used.
     * Replacement for transitionProbsFor method in Burlap 2.
     * This method returns the state transition probabilities for the next state given the current state and the
     * joint action for a {@link WildfireState} master state . The method converts the joint action to
     * FrameActionConfiguration and then proceeds ahead.
     * @param s is the current state.
     * @param ja is the joint action.
     * @return probabilities for the next state.
     */
    @Override
    public List<StateTransitionProb> stateTransitions(State s, JointAction ja) {
        //Transition probability for the next state.
        List<StateTransitionProb> ret = new ArrayList<StateTransitionProb>();

        // save each possible transition
        List<State> nextStates = generateNextStates(s, ja);
//		System.out.println("Number of next states: "+nextStates.size());
        double prob;
		double sum = 0;
        for (State nextState : nextStates) {
            prob = computeStateTransitionProbability(s, ja, nextState);
			sum +=prob;
            if (prob > 0.0) {
                ret.add(new StateTransitionProb(nextState, prob));
            }
        }

        if(sum > 1.01 || sum < 0.99){
            System.out.println("Problem in the Transition: PROBSUM:" + sum);
        }

        return ret;
    }//end method.


    /**
     * This method returns the state transition probabilities for the next state given the current state and the
     * FAC. The method first calculates the Fire Transition and then the self-agent transitions.
     * @param s is the current state.
     * @param fac is the Frame-Action-Configuration happened.
     * @return probabilities for the next state.
     */
    @Override
    public List<StateTransitionProb> stateTransitions(State s, FrameActionConfiguration fac) {
        //List of State Transition probability.
        List<StateTransitionProb> ret = new ArrayList<StateTransitionProb>();

        // save each possible transition
        List<State> nextStates = generateAnonymousNextStates(s, fac);
//		for(State state : nextStates){
//		    WildfireAnonymousState anm = (WildfireAnonymousState)state;
//		    System.out.println("State Transition:" + anm);
//        }

        double prob;
		double sum = 0;
        for (State nextState : nextStates) {
            prob = computeAnmStateTransitionProbability(s, fac, nextState);
			sum +=prob;
            if (prob > 0.0) {
                ret.add(new StateTransitionProb(nextState, prob));
            }
        }

        if(sum > 1.01 || sum < 0.99){
            System.out.println("Problem in Transition. Prob Sum:" + sum);
        }
        return ret;
    }//end method.





    /**
     * This method generates the next state for the {@link WildfireState} master state, which contains
     * availability information of all the agents in the premise.
     * @param state is the current master state.
     * @param jointAction is the joint action by all the agents.
     * @return list of possible states.
     */
    public List<State> generateNextStates(State state, JointAction jointAction) {
        //List of next states.
        List<State> nextStates = new ArrayList<State>();

        // get the agent number
        WildfireState wildfireState= (WildfireState)state;
        int agentNum = wildfireState.getSelfAgent().getAgentNumber();


        //Power to fight the fire.
        double powers = 0;

        //Create a new fire list.
        ArrayList<ArrayList<Fire>> nextFiresList = new ArrayList<ArrayList<Fire>>();

        //Go through each current fire and try to make next fire list.
        for(Fire fire: wildfireState.getFireList()){

            ArrayList<Fire> nextFires = new ArrayList<Fire>();
            Fire nextFire = new Fire(fire);
            // this fire burned out. it's intensity doesn't change
            if (fire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                //Add it into the next fire list.
                nextFires.add(nextFire);
                nextFiresList.add(nextFires);
                //Nothing to do.
                continue;
            }//end if.


            //Calculate the fighting power added by agents.
            for(int agentPower = 0; agentPower < jointAction.size(); agentPower++){
                if(!(jointAction.action(agentPower).actionName().equals(WildfireParameters.NOOP_ACTION))) {
                    int powerType = domain.agentsList.get(agentPower).getPowerType();
                    powers += domain.agentPower.get(powerType - 1);
                }//end if.
            }//end for.

             //NOTE: Due to discrepancies in the floating point sums.
            int powerInt = (int)powers;
            double powerFract = powers - (double)powerInt;
            if(powerFract > 0.99){
                powers += 0.01;
//                System.out.println("POWER Bug fix:" + powers);
            }



            //If someone is fighting the fire.
            if (((fire.getFireType()== WildfireParameters.BIG_FIRE && powers >= WildfireParameters.BIG_FIRE_THRESHOLD) ||
                    (fire.getFireType()== WildfireParameters.SMALL_FIRE && powers >= WildfireParameters.SMALL_FIRE_THRESHOLD) ||
                    (fire.getFireType()== WildfireParameters.VERY_BIG_FIRE && powers >= WildfireParameters.VERY_BIG_FIRE_THRESHOLD))
                    && fire.getIntensity() > 0) {


                //below equation or minIntensity + 1.
                int minIntensity = fire.getIntensity() - 1;

                nextFire.setIntensity(minIntensity);
                Fire nextFire1 = new Fire(fire);
                nextFire1.setIntensity(minIntensity + 1);

                nextFires.add(nextFire);
                nextFires.add(nextFire1);

                nextFiresList.add(nextFires);
            } else {
                // no one worked on this location

                // was it already on fire?
                //Add the same state or something else
                if(fire.getIntensity() == 0){
                    //Add the current state to the list.
                    nextFires.add(fire);
                    // this location was not on fire, but it might start on fire
                    nextFire.setIntensity(Math.min(2,WildfireParameters.MAX_FIRE_STATES -2));
                    nextFires.add(nextFire);

                    //Add the fires into the next fire list.
                    nextFiresList.add(nextFires);
                }else if(fire.getIntensity() == WildfireParameters.MAX_FIRE_STATES -2){
                    //Add the current state to the list.
                    nextFires.add(fire);
                    // this location was not on fire, but it might start on fire
                    nextFire.setIntensity(fire.getIntensity() + 1);
                    nextFires.add(nextFire);

                    //Add the fires into the next fire list.
                    nextFiresList.add(nextFires);
                }else{
                    nextFire.setIntensity(fire.getIntensity() + 1);
                    nextFires.add(nextFire);
                    //Add the fires into the next fire list.
                    nextFiresList.add(nextFires);
                }//end if-else if -else.
            }//end if-else. power.
        }//end for.

        // count the number of possible fire states
        int numFireStates = 1;
        for (int i = 0; i < nextFiresList.size(); i++) {
            numFireStates *= nextFiresList.get(i).size();
        }//end for.

        // count the number of possible agent states
        int numAgentStates = agentNum == WildfireParameters.MASTER_STATE_AGENT_NUM ? 1 : WildfireParameters.MAX_SUPPRESSANT_STATES;

        //For all the agents calculate the number of states available.
        for (int i = 0; i < wildfireState.getAgentList().size(); i++) {
            numAgentStates *= WildfireParameters.MAX_SUPPRESSANT_STATES;
        }

        //Get the number of agents.
        int numAgents = agentNum == WildfireParameters.MASTER_STATE_AGENT_NUM
                ? wildfireState.getAgentList().size()
                : wildfireState.getAgentList().size() + 1;

        //Get the next state.
        WildfireState nextState;

        int fireStateNum, agentStateNum, agentIndex, available;

        //Go through all the possible fire-states available.
        for (int fireNumber = 0; fireNumber < numFireStates; fireNumber++) {
            //For each fire state find the number of agent states.
            for (int agentNumber = 0; agentNumber < numAgentStates; agentNumber++) {
                // Start the state.
                nextState = agentNum > WildfireParameters.MASTER_STATE_AGENT_NUM
                        ? Wildfire.getCleanState(this.domain, agentNum)
                        : Wildfire.getCleanMasterState(this.domain);
                //Add the subjective state into the list.
                nextStates.add(nextState);

                fireStateNum = fireNumber;
                for (int nextFire = 0; nextFire < nextFiresList.size(); nextFire++) {
                    //If there is one deterministic state then set it to fire.
                    if (nextFiresList.get(nextFire).size() == 1) {
                        Wildfire.setFire(nextState,nextFiresList.get(nextFire).get(0));
                    } else {
                        // assign the intensity from this fireStateNum count
                        // Get the fires one by one in the list.
                        Fire fireIn  = nextFiresList.get(nextFire).get(fireStateNum % nextFiresList.get(nextFire).size());
                        //Assign a fire state number.
                        fireStateNum /= nextFiresList.get(nextFire).size();

                        // assign the intensity
                        Wildfire.setFire(nextState,fireIn);
                    }//end if-else.
                }//end for.

                // assign the agent values
                agentStateNum = agentNumber;

                //count the agent numbers. The actions are in sequence of agent number.
                for (int k = 0, agentListIndex = 0; k < numAgents && agentListIndex < wildfireState.getAgentList().size(); k++) {

                    // figure out the availability
                    available  = agentStateNum % WildfireParameters.MAX_SUPPRESSANT_STATES;
                    agentStateNum /= WildfireParameters.MAX_SUPPRESSANT_STATES;

                    agentIndex = agentNum == WildfireParameters.MASTER_STATE_AGENT_NUM? k : k - 1;

                    //Check the agent index and create agents.
                    if (agentIndex == -1) {

                        //Create a new Agent.
                        Agent newAgent = wildfireState.getSelfAgent();
                        newAgent.setAvailability(available);
                        // this is the self agent
                        // For next-state, new agent object.
                        Wildfire.setSelfAgent(nextState,new Agent(newAgent));
                    } else {
                        // this is another agent
                        wildfireState.getAgentList().get(agentListIndex).setAvailability(available);
                        //Set the other agent.
                        // For next-state, new agent object.
                        Wildfire.setOtherAgent(nextState,new Agent(wildfireState.getAgentList().get(agentListIndex)));
                        agentListIndex++;
                    }//end if-else. agent index.
                }//Go through all agents.
            }//end for each agent-available-state
        }//end for fire states.


        return nextStates;
    }//end method.



    /**
     * Generate a list of new states from the given state and the FAC.
     * @param state is the current state.
     * @param fac FrameActionConfiguration for the particular state.
     * @return list of possible states.
     */
    public List<State> generateAnonymousNextStates(State state, FrameActionConfiguration fac) {
        //List of next states.
        List<State> nextStates = new ArrayList<State>();

        // get the agent number
        WildfireAnonymousState anonymousState= (WildfireAnonymousState)state;
        int agentNum = anonymousState.getSelfAgent().getAgentNumber();


        //Create a new fire 2D array of the same size as previous state.
        Fire[][] nextFiresList = new Fire[anonymousState.getFireList().length][];


        for(int fireIndex = 0 ; fireIndex < anonymousState.getFireList().length ; fireIndex++){
            //Power to fight the fire.
            double powers = 0;
            Fire fire = anonymousState.getFireList()[fireIndex];
            //Create new fire Array.
            Fire[] nextFires;
            Fire nextFire = new Fire(fire);
            // this fire burned out. it's intensity doesn't change
            if (fire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                nextFires = new Fire[1];
                //Add it into the next fire list.
                nextFires[0] = nextFire;
                nextFiresList[fireIndex] = nextFires;
            }else{
                //For each power type find the agentPower-fire list. Then search that configuration in the
                //configuration and sum the powers.
                for(int agentPowerType=1 ; agentPowerType <= this.domain.agentPowerTypes.size();agentPowerType++){
                    int configIndex = fac.search(agentPowerType,fire.fireActionName());
                    //If any agent-fire configuration exist then sum up the powers by adding (power*agent) value.
                    if(configIndex != -1 && !(fac.getCurrentConfiguration()[configIndex] == null)){
                        powers += (fac.getCurrentConfiguration()[configIndex].getNumberOfAgents() * domain.agentPower.get(agentPowerType-1));
                    }//end if.
                }//end for.

                //NOTE: Due to discrepancies in the floating point sums.
                int powerInt = (int)powers;
                double powerFract = powers - (double)powerInt;
                if(powerFract > 0.99){
                    powers += 0.01;
//                    System.out.println("POWER Bug fix:" + powers);
                }





                //If someone is fighting the fire.
                if (((fire.getFireType()== WildfireParameters.BIG_FIRE && powers >= WildfireParameters.BIG_FIRE_THRESHOLD) ||
                        (fire.getFireType()== WildfireParameters.SMALL_FIRE && powers >= WildfireParameters.SMALL_FIRE_THRESHOLD) ||
                        (fire.getFireType()== WildfireParameters.VERY_BIG_FIRE && powers >= WildfireParameters.VERY_BIG_FIRE_THRESHOLD))
                        && fire.getIntensity() > 0) {
                    //The only fires are possible after the changes made is either with min intensity as shown in the
                    //below equation or minIntensity + 1.
                    int minIntensity;

                    minIntensity = fire.getIntensity() - 1;
                    nextFires = new Fire[2];

                    nextFire.setIntensity(minIntensity);
                    nextFires[0] = nextFire;

                    Fire nextFire1 = new Fire(fire);
                    nextFire1.setIntensity(minIntensity + 1);
                    nextFires[1] = nextFire1;

                    nextFiresList[fireIndex] = nextFires;
                } else {
                    // no one worked on this location


                    // was it already on fire?
                    //Add the same state or something else
                    if(fire.getIntensity() == 0){
                        nextFires = new Fire[2];

                        //Add it into the next fire list.
                        nextFires[0] = nextFire;


                        // this location was not on fire, but it might start on fire
                        Fire newNextFire = new Fire(nextFire);
                        newNextFire.setIntensity(Math.min(2,WildfireParameters.MAX_FIRE_STATES -2));
                        nextFires[1] = newNextFire;

                        //Add the list to the overall list.
                        nextFiresList[fireIndex] = nextFires;
                    }else if(fire.getIntensity() == WildfireParameters.MAX_FIRE_STATES -2){

                        nextFires = new Fire[2];
                        //Add it into the next fire list.
                        nextFires[0] = nextFire;


                        //this location can get burned out.
                        Fire newNextFire = new Fire(nextFire);
                        newNextFire.setIntensity(fire.getIntensity()+1);
                        nextFires[1] = newNextFire;

                        //Add the list to the overall list.
                        nextFiresList[fireIndex] = nextFires;
                    }else{
                        nextFires = new Fire[1];
                        // this location was not on fire, but it might start on fire
                        nextFire.setIntensity(fire.getIntensity()+1);
                        nextFires[0] = nextFire;

                        //Add the list to the overall list.
                        nextFiresList[fireIndex] = nextFires;
                    }//end if-else fire intensity.
                }//end if-else. power.
            }//end if -else Burned out location. .
        }//end for.


        // count the number of possible fire states
        int numFireStates = 1;
        for (int i = 0; i < nextFiresList.length; i++) {
            numFireStates *= nextFiresList[i].length;
        }//end for.

        //Calculate the number of next states which is
        // Number of Fire states * Self agent suppressant levels.

        //Next state.
        WildfireAnonymousState nextState;
        //Temporary variables.
        int fireStateNum;
        //Go through all the possible fire-states available.
        for (int fireNumber = 0; fireNumber < numFireStates; fireNumber++) {
            //For each fire state find the number of agent states.
            for(int agentAvailability=0 ; agentAvailability < WildfireParameters.MAX_SUPPRESSANT_STATES;agentAvailability++){
                // Start the state.
                nextState = agentNum > WildfireParameters.MASTER_STATE_AGENT_NUM
                        ? Wildfire.getCleanAnmState(this.domain, agentNum,anonymousState.getFireList().length)
                        : Wildfire.getCleanAnmMasterState(this.domain,anonymousState.getFireList().length);
                //Add the subjective state into the list.
                nextStates.add(nextState);

                fireStateNum = fireNumber;
                for (int nextFire = 0; nextFire < nextFiresList.length; nextFire++) {
                    //If there is one deterministic state then set it to fire.
                    if (nextFiresList[nextFire].length == 1) {
                        Wildfire.setAnmFire(nextState,nextFiresList[nextFire][0],nextFire);
                    } else {
                        // assign the intensity from this fireStateNum count
                        // Get the fires one by one in the list.
                        Fire fireIn  = nextFiresList[nextFire][fireStateNum % nextFiresList[nextFire].length];
                        //Assign a fire state number.
                        fireStateNum /= nextFiresList[nextFire].length;

                        // assign the intensity
                        Wildfire.setAnmFire(nextState,fireIn,nextFire);
                    }//end if-else.
                }//end for.

                //The anonymous list is same for all the agents.
                nextState.setanonymousAgents(anonymousState.getanonymousAgents());

                //Set the agent's suppressant levels to all possible combinations.
                Agent selfAgent = new Agent(anonymousState.getSelfAgent());
                selfAgent.setAvailability(agentAvailability);
                nextState.setSelfAgent(selfAgent);
            }//end for each agent-available-state
        }//end for fire states.

        return nextStates;
    }//end method.


    /** Calculates the probability of a state transition between two states.
     *@param state The current state of the environment
     * @param jointAction is the joint action by the agents.
     * @param nextState The next state of the environment
     *
     * @return The probability that the environment goes from {@code state}
     *      to {@code nextState} after the agents perform {@code ja}
     */
    public double computeStateTransitionProbability(State state,
                                                       JointAction jointAction, State nextState) {
        //Compute Internal Transition Probability.
        double totalProb = computeFireTransitionProbability(state, jointAction, nextState);
//        System.out.println("fire prob: " + totalProb);

        double internalProb = computeInternalTransitionProbability(state, jointAction, nextState);
        totalProb *= internalProb;
//        }

        return totalProb;
    }//end method.


    /** Calculates the probability of a state transition between two states.
     *@param state The current state of the environment
     * @param fac The frame-action-configuration for the state.
     * @param nextState The next state of the environment
     *
     * @return The probability that the environment goes from {@code state}
     *      to {@code nextState} after the agents perform {@code ja}
     */
    public double computeAnmStateTransitionProbability(State state,
                                                    FrameActionConfiguration fac, State nextState) {
        //Compute Internal Transition Probability.
        double totalProb = computeAnmFireTransitionProbability(state, fac, nextState);
//        System.out.println("fire prob: " + totalProb);
        double internalProb = computeAnmInternalTransitionProbability(state, fac, nextState);
//			System.out.println("internal prob: " + internalProb);
        totalProb *= internalProb;

        return totalProb;
    }//end method.


    /**
     * This method computes transition probability of a single fire from the current intensity to the
     * next intensity after doing the joint action.
     * @param currentState is the current Wildfire State.
     * @param currentFire is the current fire object.
     * @param jointAction is the joint action by all agents.
     * @param nextFire is the next possible fire.
     * @return is the probability of transition from currentFire to nextFire.
     */
    private double computeFireTransitionProbability(State currentState, Fire currentFire,
                                                    JointAction jointAction, Fire nextFire) {

        // use the probability for each location
        double power = 0;
        double totalProb = 1.0;


        for(int agentPower = 0; agentPower < jointAction.size(); agentPower++){
            //If the action is towards the current fire then add the powers.
            if(currentFire.fireActionName().equals(jointAction.getActions().get(agentPower).actionName())) {
                int powerType = domain.agentsList.get(agentPower).getPowerType();
                power += domain.agentPower.get(powerType - 1);
            }//end if.
        }//end for.

        //NOTE: Due to discrepancies in the floating point sums.
        int powerInt = (int)power;
        double powerFract = power - (double)powerInt;
        if(powerFract > 0.99){
            power += 0.01;
            powerInt += 1;
//            System.out.println("POWER Bug fix:" + power);
        }


        if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
            if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                // this is an absorbing state
                totalProb *= 1.0;
            } else {
                // this isn't possible
                totalProb = 0.0;
            }
        }else{
            if(currentFire.getFireType() == WildfireParameters.BIG_FIRE){
                //If the burned out location is there. There can not be any other possibility.
                if (power < WildfireParameters.BIG_FIRE_THRESHOLD || currentFire.getIntensity() == 0) {
                    // no one is fighting this fire, so we expect it to increase
                    if (currentFire.getIntensity() == 0) {
                        double spread = probabilitySpread((WildfireState) currentState, currentFire.getFireNumber());
                        if (nextFire.getIntensity() == 0) {
                            // this is the inverse of the ignition probability
                            totalProb *= (1.0 - spread);
                        } else if (nextFire.getIntensity() == Math.min(2,  WildfireParameters.MAX_FIRE_STATES - 2)) {
                            // this is the ignition probability
                            totalProb *= spread;
                        } else {
                            // this isn't possible
                            totalProb = 0.0;
                        }
                    } else if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                        if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                            // this is the burnout probability
                            totalProb *= this.wildfireSpreadModel.burnoutProb;
                        } else if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                            // the fire didn't burn out
                            totalProb *= (1.0 - this.wildfireSpreadModel.burnoutProb);
                        } else {
                            // this isn't possible
                            totalProb = 0.0;
                        }
                    } else if (currentFire.getIntensity() == nextFire.getIntensity() - 1) {
                        // the fire intensity should increase by 1
                        totalProb *= 1.0;
                    } else {
                        // this isn't possible
                        totalProb = 0.0;
                    }
                } else {
                    int expNext =  currentFire.getIntensity()-1;

                    //For each extra agent than minimum probability would increase the probability
                    //of fire reduction by  EXTRA_AGENT_FIRE_REDUCTION_PROB.
                    double extraAgentProbs = 0;
                    if (power > WildfireParameters.BIG_FIRE_THRESHOLD) {
                        double extraPower = power - WildfireParameters.BIG_FIRE_THRESHOLD;
                        extraAgentProbs = WildfireParameters.EXTRA_AGENT_FIRE_REDUCTION_PROB
                                * (extraPower / WildfireParameters.EXTRA_AGENT_FIRE_POWER);
                    }

                    double effectiveProb = 0;

                    //If prob of fire reducing to the expected next fire increases with number of
                    //extra agent and is capped at 1. Same way the expected Next + 1 fire intensity
                    //probability is capped at 0.
                    if (nextFire.getIntensity() == expNext) {
                        // this is what we expect
                        effectiveProb = (WildfireParameters.FIRE_REDUCTION_PROB + extraAgentProbs);
                        if (effectiveProb > 1.0)
                            effectiveProb = 1.0;
                    } else if (nextFire.getIntensity() == expNext + 1) {
                        effectiveProb = (1 - (WildfireParameters.FIRE_REDUCTION_PROB + extraAgentProbs));
                        if (effectiveProb < 0.0)
                            effectiveProb = 0.0;
                    } else {
                        // this isn't possible
                        effectiveProb = 0.0;
                    }//end if -else if -else.

                    totalProb *= effectiveProb;
                }//end if-else. power Int.
            }else if (currentFire.getFireType() == WildfireParameters.SMALL_FIRE){
                //If the burned out location is there. There can not be any other possibility.
                if (power < WildfireParameters.SMALL_FIRE_THRESHOLD || currentFire.getIntensity() == 0) {
                    // no one is fighting this fire, so we expect it to increase
                    if (currentFire.getIntensity() == 0) {
                        double spread = probabilitySpread((WildfireState) currentState, currentFire.getFireNumber());
                        if (nextFire.getIntensity() == 0) {
                            // this is the inverse of the ignition probability
                            totalProb *= (1.0 - spread);
                        } else if (nextFire.getIntensity() == Math.min(2,  WildfireParameters.MAX_FIRE_STATES - 2)) {
                            // this is the ignition probability
                            totalProb *= spread;
                        } else {
                            // this isn't possible
                            totalProb = 0.0;
                        }
                    } else if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                        if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                            // this is the burnout probability
                            totalProb *= this.wildfireSpreadModel.burnoutProb;
                        } else if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                            // the fire didn't burn out
                            totalProb *= (1.0 - this.wildfireSpreadModel.burnoutProb);
                        } else {
                            // this isn't possible
                            totalProb = 0.0;
                        }
                    } else if (currentFire.getIntensity() == nextFire.getIntensity() - 1) {
                        // the fire intensity should increase by 1
                        totalProb *= 1.0;
                    } else {
                        // this isn't possible
                        totalProb = 0.0;
                    }
                } else {
                   int expNext =  currentFire.getIntensity()-1;

                    double effectiveProb = 0;
                    //If prob of fire reducing to the expected next fire increases with number of
                    //extra agent and is capped at 1. Same way the expected Next + 1 fire intensity
                    //probability is capped at 0.
                    if (nextFire.getIntensity() == expNext) {
                        // this is what we expect
                        effectiveProb = (power/WildfireParameters.SMALL_AGENT_FIRE_POWER) * WildfireParameters.SMALL_FIRE_TRANS_PROB ;
                        if(effectiveProb > 1.0)
                            effectiveProb = 1.0;
                    } else if(nextFire.getIntensity() == expNext + 1) {
                        effectiveProb = (1 - ((power/WildfireParameters.SMALL_AGENT_FIRE_POWER) * WildfireParameters.SMALL_FIRE_TRANS_PROB));
                        if(effectiveProb < 0.0)
                            effectiveProb = 0.0;
                    }else{
                        // this isn't possible
                        effectiveProb = 0.0;
                    }//end if -else if -else.

                    totalProb *= effectiveProb;
                }//end if-else. power
            }else if (currentFire.getFireType() == WildfireParameters.VERY_BIG_FIRE){
                //If the burned out location is there. There can not be any other possibility.
                if (power < WildfireParameters.VERY_BIG_FIRE_THRESHOLD || currentFire.getIntensity() == 0) {
                    // no one is fighting this fire, so we expect it to increase
                    if (currentFire.getIntensity() == 0) {
                        double spread = probabilitySpread((WildfireState) currentState, currentFire.getFireNumber());
                        if (nextFire.getIntensity() == 0) {
                            // this is the inverse of the ignition probability
                            totalProb *= (1.0 - spread);
                        } else if (nextFire.getIntensity() == Math.min(2,  WildfireParameters.MAX_FIRE_STATES - 2)) {
                            // this is the ignition probability
                            totalProb *= spread;
                        } else {
                            // this isn't possible
                            totalProb = 0.0;
                        }
                    } else if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                        if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                            // this is the burnout probability
                            totalProb *= this.wildfireSpreadModel.burnoutProb;
                        } else if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                            // the fire didn't burn out
                            totalProb *= (1.0 - this.wildfireSpreadModel.burnoutProb);
                        } else {
                            // this isn't possible
                            totalProb = 0.0;
                        }
                    } else if (currentFire.getIntensity() == nextFire.getIntensity() - 1) {
                        // the fire intensity should increase by 1
                        totalProb *= 1.0;
                    } else {
                        // this isn't possible
                        totalProb = 0.0;
                    }
                } else {
                    int expNext =  currentFire.getIntensity()-1;

                    double effectiveProb = 0;
                    //If prob of fire reducing to the expected next fire increases with number of
                    //extra agent and is capped at 1. Same way the expected Next + 1 fire intensity
                    //probability is capped at 0.
                    if (nextFire.getIntensity() == expNext) {
                        // this is what we expect
                        effectiveProb = (power/WildfireParameters.VB_AGENT_FIRE_POWER) * WildfireParameters.VB_FIRE_TRANS_PROB ;
                        if(effectiveProb > 1.0)
                            effectiveProb = 1.0;
                    } else if(nextFire.getIntensity() == expNext + 1) {
                        effectiveProb = (1 - ((power/WildfireParameters.VB_AGENT_FIRE_POWER) * WildfireParameters.VB_FIRE_TRANS_PROB));
                        if(effectiveProb < 0.0)
                            effectiveProb = 0.0;
                    }else{
                        // this isn't possible
                        effectiveProb = 0.0;
                    }//end if -else if -else.

                    totalProb *= effectiveProb;
                }//end if-else. power
            }else{

            }//end if-else. Fire Type.
        }//end if-else. Burnout.



        return totalProb;
    }//end method.






    /**
     * Computes the probability of a transition in the state of fires in an
     * agent's neighborhood.
     *
     * @param state The current state of the environment
     * @param jointAction is the joint action by the agents.
     * @param nextState The next state of the environment
     *
     * @return The probability that the fire portion of {@code state}
     *      changes to {@code nextState} after the agents perform
     *      {@code ja}
     */
    private double computeFireTransitionProbability(State state,
                                                    JointAction jointAction, State nextState) {

        //Convert states into Wildfire states.
        WildfireState ws = (WildfireState)state;
        WildfireState wsNext = (WildfireState)nextState;

        // use the probability for each location
        double power;
        double totalProb = 1.0;


        //For each fire in the current state.
      for(Fire currentFire : ws.getFireList()){
            Fire nextFire = wsNext.getFireList().get(currentFire.getFireNumber());

            power = 0;
            //Find powers by summing up the overall frame-action configuration.
            //Calculate the overall power.
            for(int agentPower = 0; agentPower < jointAction.size(); agentPower++){
                if(!jointAction.getActions().get(agentPower).actionName().equals(WildfireParameters.NOOP_ACTION)){
                    power += domain.agentPowerTypes.get(domain.agentsList.get(agentPower).getPowerType());
                }
            }

          //NOTE: Due to discrepancies in the floating point sums.
          int powerInt = (int)power;
          double powerFract = power - (double)powerInt;
          if(powerFract > 0.99){
              power += 0.01;
              powerInt += 1;
//              System.out.println("POWER Bug fix:" + power);
          }

            //If the burned out location is there. There can not be any other possibility.
            if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                    // this is an absorbing state
                    totalProb *= 1.0;
                } else {
                    // this isn't possible
                    totalProb = 0.0;
                    break;
                }
            }else{
                if(currentFire.getFireType() == WildfireParameters.BIG_FIRE){
                    if (power < WildfireParameters.BIG_FIRE_THRESHOLD || currentFire.getIntensity() == 0) {
                        // no one is fighting this fire, so we expect it to increase
                        if (currentFire.getIntensity() == 0) {
                            double spread = probabilitySpread(ws, currentFire.getFireNumber());
                            if (nextFire.getIntensity() == 0) {
                                // this is the inverse of the ignition probability
                                totalProb *= (1.0 - spread);
                            } else if (nextFire.getIntensity() == Math.min(2,  WildfireParameters.MAX_FIRE_STATES - 2)) {
                                // this is the ignition probability
                                totalProb *= spread;
                            } else {
                                // this isn't possible
                                totalProb = 0.0;
                                break;
                            }
                        } else if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                            if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                                // this is the burnout probability
                                totalProb *= this.wildfireSpreadModel.burnoutProb;
                            } else if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                                // the fire didn't burn out
                                totalProb *= (1.0 - this.wildfireSpreadModel.burnoutProb);
                            } else {
                                // this isn't possible
                                totalProb = 0.0;
                                break;
                            }
                        } else if (currentFire.getIntensity() == nextFire.getIntensity() - 1) {
                            // the fire intensity should increase by 1
                            totalProb *= 1.0;
                        } else {
                            // this isn't possible
                            totalProb = 0.0;
                            break;
                        }
                    } else {
                        int expNext = currentFire.getIntensity()-1;

                        //For each extra agent than minimum probability would increase the probability
                        //of fire reduction by  EXTRA_AGENT_FIRE_REDUCTION_PROB.
                        double extraAgentProbs = 0;
                        if (power > WildfireParameters.BIG_FIRE_THRESHOLD) {
                            double extraPower = power - WildfireParameters.BIG_FIRE_THRESHOLD;
                            extraAgentProbs = WildfireParameters.EXTRA_AGENT_FIRE_REDUCTION_PROB
                                    * (extraPower / WildfireParameters.EXTRA_AGENT_FIRE_POWER);
                        }

                        double effectiveProb = 0;

                        //If prob of fire reducing to the expected next fire increases with number of
                        //extra agent and is capped at 1. Same way the expected Next + 1 fire intensity
                        //probability is capped at 0.
                        if (nextFire.getIntensity() == expNext) {
                            // this is what we expect
                            effectiveProb = (WildfireParameters.FIRE_REDUCTION_PROB + extraAgentProbs);
                            if (effectiveProb > 1.0)
                                effectiveProb = 1.0;
                        } else if (nextFire.getIntensity() == expNext + 1) {
                            effectiveProb = (1 - (WildfireParameters.FIRE_REDUCTION_PROB + extraAgentProbs));
                            if (effectiveProb < 0.0)
                                effectiveProb = 0.0;
                        } else {
                            // this isn't possible
                            effectiveProb = 0.0;
                        }//end if -else if -else.

                        totalProb *= effectiveProb;

                    }
                }else if(currentFire.getFireType() == WildfireParameters.SMALL_FIRE){
                    //If the burned out location is there. There can not be any other possibility.
                    if (power < WildfireParameters.SMALL_FIRE_THRESHOLD || currentFire.getIntensity() == 0) {
                        // no one is fighting this fire, so we expect it to increase
                        if (currentFire.getIntensity() == 0) {
                            double spread = probabilitySpread(ws, currentFire.getFireNumber());
                            if (nextFire.getIntensity() == 0) {
                                // this is the inverse of the ignition probability
                                totalProb *= (1.0 - spread);
                            } else if (nextFire.getIntensity() == Math.min(2,  WildfireParameters.MAX_FIRE_STATES - 2)) {
                                // this is the ignition probability
                                totalProb *= spread;
                            } else {
                                // this isn't possible
                                totalProb = 0.0;
                            }
                        } else if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                            if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                                // this is the burnout probability
                                totalProb *= this.wildfireSpreadModel.burnoutProb;
                            } else if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                                // the fire didn't burn out
                                totalProb *= (1.0 - this.wildfireSpreadModel.burnoutProb);
                            } else {
                                // this isn't possible
                                totalProb = 0.0;
                            }
                        } else if (currentFire.getIntensity() == nextFire.getIntensity() - 1) {
                            // the fire intensity should increase by 1
                            totalProb *= 1.0;
                        } else {
                            // this isn't possible
                            totalProb = 0.0;
                        }
                    } else {
                        int expNext =  currentFire.getIntensity()-1;

                        double effectiveProb = 0;
                        //If prob of fire reducing to the expected next fire increases with number of
                        //extra agent and is capped at 1. Same way the expected Next + 1 fire intensity
                        //probability is capped at 0.
                        if (nextFire.getIntensity() == expNext) {
                            // this is what we expect
                            effectiveProb = (power/WildfireParameters.SMALL_AGENT_FIRE_POWER) * WildfireParameters.SMALL_FIRE_TRANS_PROB ;
                            if(effectiveProb > 1.0)
                                effectiveProb = 1.0;
                        } else if(nextFire.getIntensity() == expNext + 1) {
                            effectiveProb = (1 - ((power/WildfireParameters.SMALL_AGENT_FIRE_POWER) * WildfireParameters.SMALL_FIRE_TRANS_PROB));
                            if(effectiveProb < 0.0)
                                effectiveProb = 0.0;
                        }else{
                            // this isn't possible
                            effectiveProb = 0.0;
                        }//end if -else if -else.

                        totalProb *= effectiveProb;
                    }//end if-else. power
                }else if(currentFire.getFireType() == WildfireParameters.VERY_BIG_FIRE){
                    //If the burned out location is there. There can not be any other possibility.
                    if (power < WildfireParameters.VERY_BIG_FIRE_THRESHOLD || currentFire.getIntensity() == 0) {
                        // no one is fighting this fire, so we expect it to increase
                        if (currentFire.getIntensity() == 0) {
                            double spread = probabilitySpread(ws, currentFire.getFireNumber());
                            if (nextFire.getIntensity() == 0) {
                                // this is the inverse of the ignition probability
                                totalProb *= (1.0 - spread);
                            } else if (nextFire.getIntensity() == Math.min(2,  WildfireParameters.MAX_FIRE_STATES - 2)) {
                                // this is the ignition probability
                                totalProb *= spread;
                            } else {
                                // this isn't possible
                                totalProb = 0.0;
                            }
                        } else if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                            if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                                // this is the burnout probability
                                totalProb *= this.wildfireSpreadModel.burnoutProb;
                            } else if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                                // the fire didn't burn out
                                totalProb *= (1.0 - this.wildfireSpreadModel.burnoutProb);
                            } else {
                                // this isn't possible
                                totalProb = 0.0;
                            }
                        } else if (currentFire.getIntensity() == nextFire.getIntensity() - 1) {
                            // the fire intensity should increase by 1
                            totalProb *= 1.0;
                        } else {
                            // this isn't possible
                            totalProb = 0.0;
                        }
                    } else {
                        int expNext =  currentFire.getIntensity()-1;

                        double effectiveProb = 0;
                        //If prob of fire reducing to the expected next fire increases with number of
                        //extra agent and is capped at 1. Same way the expected Next + 1 fire intensity
                        //probability is capped at 0.
                        if (nextFire.getIntensity() == expNext) {
                            // this is what we expect
                            effectiveProb = (power/WildfireParameters.VB_AGENT_FIRE_POWER) * WildfireParameters.VB_FIRE_TRANS_PROB ;
                            if(effectiveProb > 1.0)
                                effectiveProb = 1.0;
                        } else if(nextFire.getIntensity() == expNext + 1) {
                            effectiveProb = (1 - ((power/WildfireParameters.VB_AGENT_FIRE_POWER) * WildfireParameters.VB_FIRE_TRANS_PROB));
                            if(effectiveProb < 0.0)
                                effectiveProb = 0.0;
                        }else{
                            // this isn't possible
                            effectiveProb = 0.0;
                        }//end if -else if -else.

                        totalProb *= effectiveProb;
                    }//end if-else. power
                }else{

                }//end if-else Fire type.
            }//end if-else Burnout.
        }//end for-fire.

        return totalProb;
    }//end method.



    /**
     * Computes the probability of a transition in the state of fires in an
     * agent's neighborhood.
     *
     * @param state current anonymous state.
     * @param fac The frame-action-configuration for the state.
     * @param nextState is the next anonymous state.
     *
     * @return The probability that the fire portion of {@code state}
     *      changes to {@code nextState} after the agents perform
     *      {@code ja}
     */
    private double computeAnmFireTransitionProbability(State state,
                                                    FrameActionConfiguration fac, State nextState) {

        //Convert states into Wildfire states.
        WildfireAnonymousState ws = (WildfireAnonymousState)state;
        WildfireAnonymousState wsNext = (WildfireAnonymousState)nextState;

        // use the probability for each location
        double power;
        double totalProb = 1.0;


        //For each fire in the current state.

        for(int fireIndex = 0 ; fireIndex < ws.getFireList().length ; fireIndex++){
            Fire currentFire = ws.getFireList()[fireIndex];
            Fire nextFire = wsNext.getFireList()[fireIndex];

            power = 0;
            //Find powers by summing up the overall frame-action configuration.
            for(int agentPowerType=1 ; agentPowerType <= this.domain.agentPowerTypes.size();agentPowerType++){
                int configIndex = fac.search(agentPowerType,currentFire.fireActionName());
                //If any agent-fire configuration exist then sum up the powers by adding (power*agent) value.
                if(configIndex != -1 && !(fac.getCurrentConfiguration()[configIndex] == null)){
                    power += (fac.getCurrentConfiguration()[configIndex].getNumberOfAgents() * domain.agentPower.get(agentPowerType-1));
                }//end if.
            }//end for.

            //NOTE: Due to discrepancies in the floating point sums.
            int powerInt = (int)power;
            double powerFract = power - (double)powerInt;
            if(powerFract > 0.99){
                power += 0.01;
                powerInt +=1;
//                System.out.println("POWER Bug fix:" + power);
            }

            //If the burned out location is there. There can not be any other possibility.
            if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                    // this is an absorbing state
                    totalProb *= 1.0;
                } else {
                    // this isn't possible
                    totalProb = 0.0;
                    break;
                }
            }else{
                if(currentFire.getFireType() == WildfireParameters.BIG_FIRE){
                    if (power < WildfireParameters.BIG_FIRE_THRESHOLD || currentFire.getIntensity() == 0) {
                        // no one is fighting this fire, so we expect it to increase
                        if (currentFire.getIntensity() == 0) {
                            double spread = probabilityAnmSpread(ws, currentFire.getFireNumber());
                            if (nextFire.getIntensity() == 0) {
                                // this is the inverse of the ignition probability
                                totalProb *= (1.0 - spread);
                            } else if (nextFire.getIntensity() == Math.min(2,  WildfireParameters.MAX_FIRE_STATES - 2)) {
                                // this is the ignition probability
                                totalProb *= spread;
                            } else {
                                // this isn't possible
                                totalProb = 0.0;
                                break;
                            }
                        } else if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                            if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                                // this is the burnout probability
                                totalProb *= this.wildfireSpreadModel.burnoutProb;
                            } else if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                                // the fire didn't burn out
                                totalProb *= (1.0 - this.wildfireSpreadModel.burnoutProb);
                            } else {
                                // this isn't possible
                                totalProb = 0.0;
                                break;
                            }
                        } else if (currentFire.getIntensity() == nextFire.getIntensity() - 1) {
                            // the fire intensity should increase by 1
                            totalProb *= 1.0;
                        } else {
                            // this isn't possible
                            totalProb = 0.0;
                            break;
                        }
                    } else {

                        //If the current fire gets reduced to zero for sure, then return 1.0.
                        int expNext = currentFire.getIntensity() - 1;

                        //For each extra agent than minimum probability would increase the probability
                        //of fire reduction by  EXTRA_AGENT_FIRE_REDUCTION_PROB.
                        double extraAgentProbs = 0;
                        if (power > WildfireParameters.BIG_FIRE_THRESHOLD) {
                            double extraPower = power - WildfireParameters.BIG_FIRE_THRESHOLD;
                            extraAgentProbs = WildfireParameters.EXTRA_AGENT_FIRE_REDUCTION_PROB
                                    * (extraPower/ WildfireParameters.EXTRA_AGENT_FIRE_POWER);
                        }

                        double effectiveProb = 0;

                        //If prob of fire reducing to the expected next fire increases with number of
                        //extra agent and is capped at 1. Same way the expected Next + 1 fire intensity
                        //probability is capped at 0.
                        if (nextFire.getIntensity() == expNext) {
                            // this is what we expect
                            effectiveProb = (WildfireParameters.FIRE_REDUCTION_PROB + extraAgentProbs);
                            if (effectiveProb > 1.0)
                                effectiveProb = 1.0;
                        } else if (nextFire.getIntensity() == expNext + 1) {
                            effectiveProb = (1 - (WildfireParameters.FIRE_REDUCTION_PROB + extraAgentProbs));
                            if (effectiveProb < 0.0)
                                effectiveProb = 0.0;
                        } else {
                            // this isn't possible
                            effectiveProb = 0.0;
                        }//end if -else if -else.

                        totalProb *= effectiveProb;
                    }//end else.
                }else if(currentFire.getFireType() == WildfireParameters.SMALL_FIRE){
                    //If the burned out location is there. There can not be any other possibility.
                    if (power < WildfireParameters.SMALL_FIRE_THRESHOLD || currentFire.getIntensity() == 0) {
                        // no one is fighting this fire, so we expect it to increase
                        if (currentFire.getIntensity() == 0) {
                            double spread = probabilityAnmSpread(ws, currentFire.getFireNumber());
                            if (nextFire.getIntensity() == 0) {
                                // this is the inverse of the ignition probability
                                totalProb *= (1.0 - spread);
                            } else if (nextFire.getIntensity() == Math.min(2,  WildfireParameters.MAX_FIRE_STATES - 2)) {
                                // this is the ignition probability
                                totalProb *= spread;
                            } else {
                                // this isn't possible
                                totalProb = 0.0;
                            }
                        } else if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                            if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                                // this is the burnout probability
                                totalProb *= this.wildfireSpreadModel.burnoutProb;
                            } else if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                                // the fire didn't burn out
                                totalProb *= (1.0 - this.wildfireSpreadModel.burnoutProb);
                            } else {
                                // this isn't possible
                                totalProb = 0.0;
                            }
                        } else if (currentFire.getIntensity() == nextFire.getIntensity() - 1) {
                            // the fire intensity should increase by 1
                            totalProb *= 1.0;
                        } else {
                            // this isn't possible
                            totalProb = 0.0;
                        }
                    } else {
                        int expNext =  currentFire.getIntensity()-1;

                        double effectiveProb = 0;
                        //If prob of fire reducing to the expected next fire increases with number of
                        //extra agent and is capped at 1. Same way the expected Next + 1 fire intensity
                        //probability is capped at 0.
                        if (nextFire.getIntensity() == expNext) {
                            // this is what we expect
                            effectiveProb = (power/WildfireParameters.SMALL_AGENT_FIRE_POWER) * WildfireParameters.SMALL_FIRE_TRANS_PROB ;
                            if(effectiveProb > 1.0)
                                effectiveProb = 1.0;
                        } else if(nextFire.getIntensity() == expNext + 1) {
                            effectiveProb = (1 - ((power/WildfireParameters.SMALL_AGENT_FIRE_POWER) * WildfireParameters.SMALL_FIRE_TRANS_PROB));
                            if(effectiveProb < 0.0)
                                effectiveProb = 0.0;
                        }else{
                            // this isn't possible
                            effectiveProb = 0.0;
                        }//end if -else if -else.

                        totalProb *= effectiveProb;
                    }//end if-else. power
                }else if(currentFire.getFireType() == WildfireParameters.VERY_BIG_FIRE){
                    //If the burned out location is there. There can not be any other possibility.
                    if (power < WildfireParameters.VERY_BIG_FIRE_THRESHOLD || currentFire.getIntensity() == 0) {
                        // no one is fighting this fire, so we expect it to increase
                        if (currentFire.getIntensity() == 0) {
                            double spread = probabilityAnmSpread(ws, currentFire.getFireNumber());
                            if (nextFire.getIntensity() == 0) {
                                // this is the inverse of the ignition probability
                                totalProb *= (1.0 - spread);
                            } else if (nextFire.getIntensity() == Math.min(2,  WildfireParameters.MAX_FIRE_STATES - 2)) {
                                // this is the ignition probability
                                totalProb *= spread;
                            } else {
                                // this isn't possible
                                totalProb = 0.0;
                            }
                        } else if (currentFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                            if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 1) {
                                // this is the burnout probability
                                totalProb *= this.wildfireSpreadModel.burnoutProb;
                            } else if (nextFire.getIntensity() == WildfireParameters.MAX_FIRE_STATES - 2) {
                                // the fire didn't burn out
                                totalProb *= (1.0 - this.wildfireSpreadModel.burnoutProb);
                            } else {
                                // this isn't possible
                                totalProb = 0.0;
                            }
                        } else if (currentFire.getIntensity() == nextFire.getIntensity() - 1) {
                            // the fire intensity should increase by 1
                            totalProb *= 1.0;
                        } else {
                            // this isn't possible
                            totalProb = 0.0;
                        }
                    } else {
                        int expNext =  currentFire.getIntensity()-1;

                        double effectiveProb = 0;
                        //If prob of fire reducing to the expected next fire increases with number of
                        //extra agent and is capped at 1. Same way the expected Next + 1 fire intensity
                        //probability is capped at 0.
                        if (nextFire.getIntensity() == expNext) {
                            // this is what we expect
                            effectiveProb = (power/WildfireParameters.VB_AGENT_FIRE_POWER) * WildfireParameters.VB_FIRE_TRANS_PROB ;
                            if(effectiveProb > 1.0)
                                effectiveProb = 1.0;
                        } else if(nextFire.getIntensity() == expNext + 1) {
                            effectiveProb = (1 - ((power/WildfireParameters.VB_AGENT_FIRE_POWER) * WildfireParameters.VB_FIRE_TRANS_PROB));
                            if(effectiveProb < 0.0)
                                effectiveProb = 0.0;
                        }else{
                            // this isn't possible
                            effectiveProb = 0.0;
                        }//end if -else if -else.

                        totalProb *= effectiveProb;
                    }//end if-else. power
                }else{

                }//end if-else. Fire type.
            }//end if-else. Burnout.
        }//end for.

        return totalProb;
    }//end method.


    /**
     * Given the action of an agent, the method finds probability of moving from current availability to
     * next availability.
     * @param current is the current availability of the agent.
     * @param next is the next availability of the agent.
     * @param action is the action performed by the agent.
     * @return is the probability of the next availability.
     */
    private double computeInternalTransitionProbability(int current, int next, Action action){
        double prob = 1.0;

        if (WildfireParameters.NOOP_ACTION.equals(action.actionName())) {
            if (current > 0 && current == next) {
                // the state doesn't change, as required
                prob *= 1.0;
            } else  if (current == 0 && next == 0) {
                prob *= (1.0 - (1.0 / WildfireParameters.TRUE_CHARGING_TIME));
            } else if (current == 0 && next == WildfireParameters.MAX_SUPPRESSANT_STATES - 1) {
                prob *= (1.0 / WildfireParameters.TRUE_CHARGING_TIME);
            } else {
                // this isn't allowed
                prob *= 0.0;
            }
        } else {
            if (current == 0 && next == 0) {
                prob *= (1.0 - (1.0 / WildfireParameters.TRUE_CHARGING_TIME));
            } else if (current == 0 && next == WildfireParameters.MAX_SUPPRESSANT_STATES - 1) {
                prob *= (1.0 / WildfireParameters.TRUE_CHARGING_TIME);
            } else if (current == next + 1) {
                prob *= WildfireParameters.TRUE_DISCHARGE_PROB;
            } else if (current == next) {
                prob *= 1.0 - WildfireParameters.TRUE_DISCHARGE_PROB;
            } else {
                // this isn't allowed
                prob *= 0.0;
            }
        }//end if-else.


        return  prob;
    }//end method.

    /**
     * Computes the probability of a transition in the internal state portion
     * of a the environment state.
     *
     * @param state The current state of the environment
     * @param nextState The next state of the environment
     *
     * @return The probability that the internal state portion of {@code state}
     *      changes to {@code nextState}
     */
    private double computeInternalTransitionProbability(State state,
                                                        JointAction ja, State nextState) {
        //Convert states into Wildfire states.
        WildfireState ws = (WildfireState)state;
        WildfireState wsNext = (WildfireState)nextState;


        // check each agent
        int current, next;
        double prob = 1.0;

        // first check the self agent
        String agent;
        SimpleAction action;

        // Check if the the current state is the master state or not, and find the probability value
        // for the transition.
        if(ws.getSelfAgent().getAgentNumber() == WildfireParameters.MASTER_STATE_AGENT_NUM){

            // now check the other agents
            for (Agent currentAgent: ws.getAgentList()) {


                action = (SimpleAction)ja.action(currentAgent.getAgentNumber());
                //Get availability

                current = currentAgent.getAvailability();
                next = wsNext.getAgentList().get(currentAgent.getAgentNumber()).getAvailability();

                if (WildfireParameters.NOOP_ACTION.equals(action.actionName())) {
                    if (current > 0 && current == next) {
                        // the state doesn't change, as required
                        prob *= 1.0;
                    } else  if (current == 0 && next == 0) {
                        prob *= (1.0 - (1.0 / WildfireParameters.TRUE_CHARGING_TIME));
                    } else if (current == 0 && next == WildfireParameters.MAX_SUPPRESSANT_STATES - 1) {
                        prob *= (1.0 / WildfireParameters.TRUE_CHARGING_TIME);
                    } else {
                        // this isn't allowed
                        prob *= 0.0;
                    }
                } else {
                    if (current == 0 && next == 0) {
                        prob *= (1.0 - (1.0 / WildfireParameters.TRUE_CHARGING_TIME));
                    } else if (current == 0 && next == WildfireParameters.MAX_SUPPRESSANT_STATES - 1) {
                        prob *= (1.0 / WildfireParameters.TRUE_CHARGING_TIME);
                    } else if (current == next + 1) {
                        prob *= WildfireParameters.TRUE_DISCHARGE_PROB;
                    } else if (current == next) {
                        prob *= 1.0 - WildfireParameters.TRUE_DISCHARGE_PROB;
                    } else {
                        // this isn't allowed
                        prob *= 0.0;
                    }
                }

                if (prob == 0.0) return prob;
            }

        }else{

            if (ws.getSelfAgent().getAgentNumber() > WildfireParameters.MASTER_STATE_AGENT_NUM) {
                // get the agent's action
                action = (SimpleAction)ja.action(ws.getSelfAgent().getAgentNumber());
                //Get the availability.
                current = ws.getSelfAgent().getAvailability();
                next = wsNext.getSelfAgent().getAvailability();


                if (WildfireParameters.NOOP_ACTION.equals(action.actionName())) {
                    if (current > 0 && current == next) {
                        // the state doesn't change, as required
                        prob *= 1.0;
                    } else  if (current == 0 && next == 0) {
                        prob *= (1.0 - (1.0 / WildfireParameters.TRUE_CHARGING_TIME));
                    } else if (current == 0 && next == WildfireParameters.MAX_SUPPRESSANT_STATES - 1) {
                        prob *= (1.0 / WildfireParameters.TRUE_CHARGING_TIME);
                    } else {
                        // this isn't allowed
                        prob *= 0.0;
                    }
                } else {
                    if (current == 0 && next == 0) {
                        prob *= (1.0 - (1.0 / WildfireParameters.TRUE_CHARGING_TIME));
                    } else if (current == 0 && next == WildfireParameters.MAX_SUPPRESSANT_STATES - 1) {
                        prob *= (1.0 / WildfireParameters.TRUE_CHARGING_TIME);
                    } else if (current == next + 1) {
                        prob *= WildfireParameters.TRUE_DISCHARGE_PROB;
                    } else if (current == next) {
                        prob *= 1.0 - WildfireParameters.TRUE_DISCHARGE_PROB;
                    } else {
                        // this isn't allowed
                        prob *= 0.0;
                    }
                }

                if (prob == 0.0) return prob;
            }

            int selfAgentID = ws.getSelfAgent().getAgentNumber();

            // now check the other agents

            for(Agent otherAgent : ws.getAgentList()){
                // get the agent's action
                int otherAgentID = otherAgent.getAgentNumber();
                agent = otherAgentID + "";
                action = (SimpleAction)ja.action(otherAgentID);

                current = otherAgent.getAvailability();
                next = wsNext.getAgentList().get(otherAgent.getAgentNumber()).getAvailability();

                //Agent Action.
                SimpleAction act = new SimpleAction(action.actionName());

                Triple<Integer,SimpleAction,Integer> trip = new Triple<Integer, SimpleAction, Integer>(current,act,next);


                if (WildfireParameters.NOOP_ACTION.equals(action.actionName())) {
                    if (current > 0 && current == next) {
                        // the state doesn't change, as required
                        prob *= 1.0;
                    } else  if (current == 0 && next == 0) {
                        prob *= (1.0 - (1.0 / WildfireParameters.TRUE_CHARGING_TIME));
                    } else if (current == 0 && next == WildfireParameters.MAX_SUPPRESSANT_STATES - 1) {
                        prob *= (1.0 / WildfireParameters.TRUE_CHARGING_TIME);
                    } else {
                        // this isn't allowed
                        prob *= 0.0;
                    }
                } else {
                    if (current == 0 && next == 0) {
                        prob *= (1.0 - (1.0 / WildfireParameters.TRUE_CHARGING_TIME));
                    } else if (current == 0 && next == WildfireParameters.MAX_SUPPRESSANT_STATES - 1) {
                        prob *= (1.0 / WildfireParameters.TRUE_CHARGING_TIME);
                    } else if (current == next + 1) {
                        prob *= WildfireParameters.TRUE_DISCHARGE_PROB;
                    } else if (current == next) {
                        prob *= 1.0 - WildfireParameters.TRUE_DISCHARGE_PROB;
                    } else {
                        // this isn't allowed
                        prob *= 0.0;
                    }
                }

                if (prob == 0.0) return prob;
            }//end for.
        }//end if-else.

        return prob;
    }//end method.


    /**
     * Computes the probability of a transition of self-agent in the anonymous state.
     * @param state current Anonymous state.
     * @param action performed by the self-agent.
     * @param nextState next Anonymous state.
     *
     * @return The probability that the internal state portion of {@code state}
     * changes to {@code nextState}
     */
    private double computeAnmInternalTransitionProbability(State state, Action action, State nextState) {
        //Convert states into Wildfire states.
        WildfireAnonymousState ws = (WildfireAnonymousState)state;
        WildfireAnonymousState wsNext = (WildfireAnonymousState)nextState;

        // Get the availabilities from the current and next states.
        int current = ws.getSelfAgent().getAvailability();
        int next = wsNext.getSelfAgent().getAvailability();

        //Get the probability of the internal transition.
        return internalTransitionProbs(current,action,next);
    }//end method.

    /**
     * This method calculates the internal state transition probability given the action.
     * @param currentAvailability is the current Availability of an agent.
     * @param action is the action performed by that agent.
     * @param nextAvailability is the next Availability of an agent.
     * @return is the probability of the transition given the action.
     */
    private double internalTransitionProbs(int currentAvailability, Action action, int nextAvailability) {
        double prob = 1.0;

        //Find the probabilities based on the suppressant transition assumptions.
        if (WildfireParameters.NOOP_ACTION.equals(action.actionName())) {
            if (currentAvailability > 0 && currentAvailability == nextAvailability) {
                // the state doesn't change, as required
                prob *= 1.0;
            } else  if (currentAvailability == 0 && nextAvailability == 0) {
                prob *= (1.0 - (1.0 / WildfireParameters.TRUE_CHARGING_TIME));
            } else if (currentAvailability == 0 && nextAvailability == WildfireParameters.MAX_SUPPRESSANT_STATES - 1) {
                prob *= (1.0 / WildfireParameters.TRUE_CHARGING_TIME);
            } else {
                // this isn't allowed
                prob *= 0.0;
            }
        } else {
            if (currentAvailability == 0 && nextAvailability == 0) {
                prob *= (1.0 - (1.0 / WildfireParameters.TRUE_CHARGING_TIME));
            } else if (currentAvailability == 0 && nextAvailability == WildfireParameters.MAX_SUPPRESSANT_STATES - 1) {
                prob *= (1.0 / WildfireParameters.TRUE_CHARGING_TIME);
            } else if (currentAvailability == nextAvailability + 1) {
                prob *= WildfireParameters.TRUE_DISCHARGE_PROB;
            } else if (currentAvailability == nextAvailability) {
                prob *= 1.0 - WildfireParameters.TRUE_DISCHARGE_PROB;
            } else {
                // this isn't allowed
                prob *= 0.0;
            }
        }//end if -else.

        return prob;
    }//end method.



    /**
     * Same as previous method. Changes in the object classes.
     * Calculates the probability of a fire spreading to a given location.
     *
     * @param state The current state of fires in the agent's
     *      neighborhood
     * @param position The position of the fire to calculation an ignition
     *      probability
     *
     * @return The probability that the location in {@code position} starts
     *      on fire.
     */
    private double probabilitySpread(WildfireState state, int position) {
        // start with the initial, random spread probability for dealing with
        // locations outside the frontier
        double prob = WildfireParameters.RANDOM_SPREAD_PROB;

       //Get fire list.
        Fire currentFire = state.getFireList().get(position);

        int xDiff, yDiff, neighborFire;
        for(Fire fire : state.getFireList()){
            //If the same fire number is there, skip.
            if (fire.getFireNumber() == position) continue;

            // only consider locations on fire

            neighborFire = fire.getIntensity();
            //If the neighbour is on fire and not burned out.
            if (neighborFire > 0 && neighborFire < WildfireParameters.MAX_FIRE_STATES - 1) {
                

                // which direction is this?
                xDiff = currentFire.getFireLocation().getX() - fire.getFireLocation().getX();
                yDiff = currentFire.getFireLocation().getY() - fire.getFireLocation().getY();

                // add the ignition probability
                if (xDiff == 0 && yDiff == 1) {
                    prob += this.wildfireSpreadModel.northIgnitionProb;
                } else if (xDiff == 0 && yDiff == -1) {
                    prob += this.wildfireSpreadModel.southIgnitionProb;
                } else if (xDiff == 1 && yDiff == 0) {
                    prob += this.wildfireSpreadModel.eastIgnitionProb;
                } else if (xDiff == -1 && yDiff == 0) {
                    prob += this.wildfireSpreadModel.westIgnitionProb;
                }//end if-else.
            }//end if fire intensity.
        }//end for loop.
        return prob;
    }//end method.


    /**
     * Same as previous method. Changes in the object classes.
     * Calculates the probability of a fire spreading to a given location.
     *
     * @param state The current state of fires in the agent's
     *      neighborhood
     * @param position The position of the fire to calculation an ignition
     *      probability
     *
     * @return The probability that the location in {@code position} starts
     *      on fire.
     */
    private double probabilityAnmSpread(WildfireAnonymousState state, int position) {
        // start with the initial, random spread probability for dealing with
        // locations outside the frontier
        double prob = WildfireParameters.RANDOM_SPREAD_PROB;

        //Get fire list.
        Fire currentFire = state.getFireList()[position];

        int xDiff, yDiff, neighborFire;
        for(int fireIndex = 0 ; fireIndex < state.getFireList().length ; fireIndex++){
            //Current fire state.
            Fire fire = state.getFireList()[fireIndex];

            //If the same fire number is there, skip.
            if (fire.getFireNumber() == position) continue;

            // only consider locations on fire

            neighborFire = fire.getIntensity();
            //If the neighbour is on fire and not burned out.
            if (neighborFire > 0 && neighborFire < WildfireParameters.MAX_FIRE_STATES - 1) {


                // which direction is this?
                xDiff = currentFire.getFireLocation().getX() - fire.getFireLocation().getX();
                yDiff = currentFire.getFireLocation().getY() - fire.getFireLocation().getY();

                // add the ignition probability
                if (xDiff == 0 && yDiff == 1) {
                    prob += this.wildfireSpreadModel.northIgnitionProb;
                } else if (xDiff == 0 && yDiff == -1) {
                    prob += this.wildfireSpreadModel.southIgnitionProb;
                } else if (xDiff == 1 && yDiff == 0) {
                    prob += this.wildfireSpreadModel.eastIgnitionProb;
                } else if (xDiff == -1 && yDiff == 0) {
                    prob += this.wildfireSpreadModel.westIgnitionProb;
                }//end if-else.
            }//end if fire intensity.
        }//end for loop.
        return prob;
    }//end method.

    /**
     * Copied from the previous version - Changes are only from SGAgentAction to SimpleAction. And other constant name changes.
     * TODO: Check if the SGAgentAction is the correct replacement or not.
     * This method reads transition function given in the file.
     * @param d is the domain.
     * @param filename is the file name to read.
     * @return Transition function map.
     */
    public static Map<Integer, Map<Integer, Map<Triple<Integer, SimpleAction, Integer>, Double>>> readTransitionFnFromFile(WildfireDomain d,
                                                                                                                           String filename) {

        ArrayList<String> list = new ArrayList<String>();

        try {
            Scanner s = new Scanner(new File(filename));

            while (s.hasNext()){
                list.add(s.next());

            }
            //Added By Maulik
            System.out.println("List Transition:" + list);
            s.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        //Line 0 = agIDtoName
        String[] agentNames = list.get(0).split(",");
        int numAgents = agentNames.length;

        Map<Integer, String> agIDtoName = new HashMap<Integer, String>();

        for(int i=0;i<numAgents;i++){
            agIDtoName.put(i,agentNames[i]);
        }

        //Line 1 = oaSizes
        String[] oaSizeStr = list.get(1).split(",");
        int[] oaSizes = new int[agIDtoName.size()];
        for(int i=0;i<agIDtoName.size();i++){
            oaSizes[i]= Integer.parseInt(oaSizeStr[i]);
        }

        //Line 2 = oaIDs
        String[] oaIDStrs = list.get(2).split(":");
        List<List<Integer>> oaIDs = new ArrayList<List<Integer>>();
        for(int k=0;k<oaIDStrs.length;k++){
            String[] oaIDStr = oaIDStrs[k].split(",");
            List<Integer> oaIDtemp = new ArrayList<Integer>();
            for(int j=0;j<oaIDStr.length;j++){
                int temp = Integer.parseInt(oaIDStr[j]);
                oaIDtemp.add(temp);
            }
            oaIDs.add(oaIDtemp);
        }


        //Line 3 = agentActions
        String[] agentActionStrs = list.get(3).split(":");
        Map<String, ArrayList<SimpleAction>> agentActions = new HashMap<String, ArrayList<SimpleAction>>();
        for(int k=0;k<agentActionStrs.length;k++){
            ArrayList<SimpleAction> agActions = new ArrayList<SimpleAction>();
            String[] agentActionStr = agentActionStrs[k].split(",");
            for(int a=0;a<agentActionStr.length;a++){
                SimpleAction tempAct = new SimpleAction(agentActionStr[a]);
                agActions.add(tempAct);
            }
            agentActions.put(agIDtoName.get(k),agActions);
        }

        Map<Integer, Map<Integer, Map<Triple<Integer, SimpleAction, Integer>, Double>>> estimatedAgentTransitionFunction=
                new HashMap<Integer, Map<Integer, Map<Triple<Integer, SimpleAction, Integer>, Double>>>();

        int line=4;
        for(int i=0;i<agIDtoName.size();i++){
//			System.out.println("Agent "+i+" ");
            Map<Integer, Map<Triple<Integer, SimpleAction, Integer>, Double>> localProbabilityPerAgent=
                    new HashMap<Integer, Map<Triple<Integer, SimpleAction, Integer>, Double>>();

            int oaSize = oaSizes[i];
            for(int j=0;j<oaSize;j++){
                int oaID = oaIDs.get(i).get(j);
//				System.out.println("Neighbor "+oaID+" ");
                List<SimpleAction> agActions = agentActions.get(agIDtoName.get(oaID));

                Map<Triple<Integer, SimpleAction, Integer>, Double> prob = initCountVectorDouble(WildfireParameters.MAX_SUPPRESSANT_STATES,agActions);

                for(SimpleAction a: agActions){
                    for(int x=0;x<WildfireParameters.MAX_SUPPRESSANT_STATES;x++){
                        for(int xp=0;xp<WildfireParameters.MAX_SUPPRESSANT_STATES;xp++){
                            Triple<Integer,SimpleAction,Integer> trip = new Triple<Integer, SimpleAction, Integer>(x,a,xp);
                            double temp = Double.parseDouble(list.get(line));
                            prob.put(trip, temp);
                            line++;
                        }
                    }
                }
                localProbabilityPerAgent.put(oaID, prob);
            }
            estimatedAgentTransitionFunction.put(i, localProbabilityPerAgent);
        }

        return estimatedAgentTransitionFunction;
    }//end method.


    /**
     * Initiate the vector.
     * Only change from previous method is the SGAgentAction ==> SimpleAction.
     * @param numAvailableStates is the num of available states.
     * @param agActions is the List of actions.
     * @return Map of the agent-action.
     */
    private static Map<Triple<Integer, SimpleAction, Integer>, Double> initCountVectorDouble(
            int numAvailableStates, List<SimpleAction> agActions) {

        Map<Triple<Integer,SimpleAction,Integer>,Double> localCountVector = new HashMap<Triple<Integer,SimpleAction,Integer>,Double>();
        for(int x=0;x<numAvailableStates;x++){
            for(SimpleAction a: agActions){
                for(int xp=0;xp<numAvailableStates;xp++){
                    Triple<Integer,SimpleAction,Integer> trip = new Triple<Integer, SimpleAction, Integer>(x,a,xp);
                    localCountVector.put(trip, 0.);
                }
            }
        }
        return localCountVector;
    }//end method.

    /**
     * This method finds all the possible states given the current state.
     * In the wildfire domain the possible number of states can be
     * (No. of availability states for self-agent) * (No. of fires) * (No. of fire intensity states).
     * @param selfState is the self-state.
     * @return is the list of the possible states.
     */
    public List<State> findPossibleStates(State selfState){
        WildfireAnonymousState wf = (WildfireAnonymousState)selfState;
        List<State> possibleStates = new ArrayList<State>();

        ArrayList<int[]> totalFireCombinations = new ArrayList<int[]>();
        //Create a default 0 array. Each element defines the intensity of the fire.
        int[] elementArray = new int[wf.getFireList().length];
        //Find total fire combinations.
        possibleCombinations (elementArray,0,WildfireParameters.MAX_FIRE_STATES,totalFireCombinations);

        //Go through all availability values and fire combination to produce possible number of states.
        for(int availability=0 ; availability < WildfireParameters.MAX_SUPPRESSANT_STATES ; availability++){
            for(int[] fireCombination: totalFireCombinations){
                WildfireAnonymousState newState = new WildfireAnonymousState(wf);
                //Set the new state availability.
                newState.getSelfAgent().setAvailability(availability);

                //Set fire intensity.
                for(int fireNumber=0; fireNumber < wf.getFireList().length ; fireNumber++){
                    newState.getFireList()[fireNumber].setIntensity(fireCombination[fireNumber]);
                }//end for.

                possibleStates.add(newState);
            }//end for.
        }//end for availability.

        System.out.println("No. of possible States:"  + possibleStates.size());
        return  possibleStates;
    }//end method.


    /**
     * This method recursively finds all possible fire combinations.
     * @param elementArray is the initial array assuming all 0s.
     * @param currentElement is the current element pointer, should start with 0.
     * @param maxLimit is the maximum limit an element can take.
     * @param combinations final output.
     */
    private static void possibleCombinations(int[] elementArray,int currentElement,int maxLimit,ArrayList<int[]> combinations){
        //If the current element is last, then add the members to the arrayList.
        if(currentElement == elementArray.length -1){
            //Loop through the elements. Get the final array and add it to the ArrayList.
            for(int element=0;element < maxLimit; element++){
                elementArray[currentElement] = element;
                //Create new instance of the current combination.
                int[] newCombination = new int[elementArray.length];
                System.arraycopy(elementArray, 0, newCombination, 0, elementArray.length);
                //Add to the list.
                combinations.add(newCombination);
            }//end for.
        }else{
            //Recursively call the current method to get the next combination.
            for(int element = 0; element <maxLimit ; element++){
                elementArray[currentElement] = element;
                possibleCombinations(elementArray,currentElement+1,maxLimit,combinations);
            }//end for.
        }//end if-else.
    }//end method.


    /**
     * Random Number Generator to replace the Java's Random Number Generator API.
     * @return a new random number between 0 and 1.
     */
    public double randomNumberGenerator(){
        double randomNumber = this.generator.nextDouble();
//        this.totalCounter++;
//        if(randomNumber > 0.95){
//            this.highCount++;
//        }
//
//        if(totalCounter%100 == 0){
//            System.out.println("Sampling Counts:" + this.totalCounter + " High Count:" + this.highCount);
//        }
//

        return randomNumber;
    }
//Testing code for the method.
//    public  static  void main(String args[]){
//        ArrayList<int[]> test = new ArrayList<int[]>();
//        int[] input = new int[4];
//        possibleCombinations(input,0,5,test);
//
//        for(int[] t:test){
//            System.out.println(Arrays.toString(t));
//        }
//
//        System.out.println(test.size());
//    }
}
