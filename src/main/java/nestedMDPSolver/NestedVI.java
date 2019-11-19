package nestedMDPSolver;

import burlap.behavior.policy.CachedPolicy;
import burlap.behavior.policy.EnumerablePolicy;
import burlap.behavior.policy.GreedyQPolicy;
import burlap.behavior.policy.Policy;
import burlap.behavior.policy.support.ActionProb;
import burlap.behavior.singleagent.MDPSolver;
import burlap.behavior.singleagent.planning.Planner;
import burlap.behavior.stochasticgames.agents.SetStrategySGAgent;
import burlap.behavior.valuefunction.QProvider;
import burlap.behavior.valuefunction.QValue;
import burlap.debugtools.DPrint;
import burlap.debugtools.MyTimer;
import burlap.mdp.auxiliary.common.NullTermination;
import burlap.mdp.core.StateTransitionProb;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.oo.OOSGDomain;
import burlap.mdp.stochasticgames.world.World;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;
import burlap.behavior.valuefunction.ConstantValueFunction;

import java.io.*;
import java.util.*;


import burlap.statehashing.simple.SimpleHashableStateFactory;
import common.RandomPolicy;
import common.StateEnumerator;
import datastructures.QueueLinkedList;
import domains.wildfire.*;
import posg.model.FACRewardFunction;
import posg.model.FullJointIPOMCPModel;
import scalability.FrameActionConfiguration;
import scalability.beans.ConfigurationBean;


public class NestedVI extends MDPSolver implements Planner,QProvider{

    public static final String WF = "wf";
    public static final String GG = "gg";
    public static final String CN = "cn";
    private static boolean finite = true;
    public enum dom {WF,GG,CN};
    protected static Set<HashableState> states = new LinkedHashSet<>();
    protected static double maxDelta;
    protected static int maxIterations;
    protected static int debugCode = 23456789;
    protected static boolean verbosemode;
    protected static boolean testingmode;
    protected static boolean debugmode = false;
    protected Map<HashableState, Double> valueFunction;
    //Temporary Map for storing the current horizon's V values.
    protected Map<HashableState, Double> currentHorizonValueFunction;
    protected ConstantValueFunction vinit;
    protected static FullJointIPOMCPModel facModel;
    protected static FACRewardFunction facRewardFunction;
    protected World w;
    protected static TerminalFunction tf;
    protected static double gamma;
    protected static HashableStateFactory hashingFactory;
    private static Map<String, SGAgent> agentDefinitions;
    private static Map<String, SGAgent> agentAnmDefinitions;
    protected boolean planningStarted = false;
    protected int currentAgentIndex;
    protected String currentAgentName;
    protected Map<String, Policy> otherAgentsPolicyMap;
    protected int maxLevel;
    protected static int numPlayers;
    protected static Map<Integer, String> agIDtoName;
    protected static OOSGDomain domain;
    protected static StateEnumerator senum;
    private static boolean initNestedVI = false;
    public static boolean doreach = false;
    protected static List<String> agentsInWorld;
    public static FrameActionConfiguration fac;
    public static FrameActionConfiguration facOthers;
    public static double[] facUniformPriors;
    public static int zeroCounter = 0;
    public static boolean isBackedUp;
    public static Map<State,List<QValue>> qValueMap;
    public static ArrayList<int[]> filteredFACs;
    public static ArrayList<Double> filteredFACProbabilities;
    public static double epselone;
    public static double qValueErrorMargin; //For resolving bug of q-values fraction inaccuracy.




    public NestedVI(ConstantValueFunction vinit, int subjectAgentIndex,
                    Map<String, Policy> otherAgentsPolicies, int maxLevel2) {

        if(initNestedVI){
            this.valueFunction = new HashMap<HashableState, Double>();
            this.vinit = vinit;
            this.currentAgentIndex = subjectAgentIndex;
            this.currentAgentName = agIDtoName.get(this.currentAgentIndex);
            this.otherAgentsPolicyMap = otherAgentsPolicies;
            this.maxLevel = maxLevel2;
        }else{
            System.out.println("NestedVI planner not initialized..");
            System.exit(0);
        }
    }

    public static boolean initNestedVI(OOSGDomain d, StateEnumerator senum1, Map<String, SGAgent> agentDefs,
                                       Map<Integer, String> agIDtoN, FACRewardFunction facRewardFunction1, TerminalFunction tf1,
                                       HashableStateFactory hashingFactory1, double gamma1, double maxDelta1,
                                       int maxIterations1, List<String> agentsInWorld1, int numP, boolean finite1) {

        domain = d;
        senum = senum1;
        agentDefinitions = agentDefs;
        agIDtoName = agIDtoN;
        facRewardFunction = facRewardFunction1;
        tf = tf1;
        hashingFactory = hashingFactory1;
        gamma = gamma1;
        maxDelta = maxDelta1;
        maxIterations = maxIterations1;
        agentsInWorld = agentsInWorld1;
        numPlayers = numP;
        finite = finite1;
        facModel = (FullJointIPOMCPModel) d.getJointActionModel();
        qValueMap = new HashMap<>();

        return true;
    }

    /**
     * This method creates a list of the Q-Value for all applicable actions for current agent.
     * @param s is the state.
     * @return List of QValues.
     */
    @Override
    public List<QValue> qValues(State s) {
        String actingAgent = agIDtoName.get(this.currentAgentIndex);


        //This would save a bit more time, while writing the policy.
        if(isBackedUp)
            return NestedVI.qValueMap.get(s);

        //Get all the actions for the agent.
        List<ActionType> allsgActions = agentDefinitions.get(actingAgent).agentType().actions;
        List<Action> allAgActions = SGtoGroundedSG(allsgActions,actingAgent);

        if(testingmode){
            dprint(actingAgent);
        }
        List<QValue> qs = new ArrayList<QValue>(allAgActions.size());


        //Find Q Value for all applicable actions to the agent.
        for(Action action: allAgActions){
            qs.add(new QValue(s,action,this.qValue(s,action)));
        }//end for.

        if(testingmode){
            dprint("got all Q(S,a) values");
        }

        //Replace the previous Q values with the new ones.
        NestedVI.qValueMap.put(s,qs);

        return qs;
    }//end method.


    /**
     * This method calculates Q-Value for each action performed given state.
     * @param s current state.
     * @param a performed action.
     * @return Q-Value.
     */
    @Override
    public double qValue(State s, Action a) {
        double sumQ = 0.;

//        double cumOthersJAProb = 1.;
//        for(int probIndex = 0; probIndex < NestedVI.facUniformPriors.length ; probIndex++){
//            cumOthersJAProb *= NestedVI.facUniformPriors[probIndex];
//        }

        //For all possible combinations of the configurations.
        //Reset the fac.


        //For all the FACs.
        for(int facCount = 0 ; facCount < NestedVI.filteredFACs.size() ; facCount++){

            //Set the Current FAC values in the facOthers from the stored list of agent values.
            int[] agentCounts = NestedVI.filteredFACs.get(facCount);
            for(int pairNumber = 0 ; pairNumber < agentCounts.length ; pairNumber++){
                NestedVI.facOthers.getCurrentConfiguration()[pairNumber].setNumberOfAgents(agentCounts[pairNumber]);
            }
            //Create a temporary FAC.
            FrameActionConfiguration tempFAC = new FrameActionConfiguration(NestedVI.facOthers,false);
            //Sum the Q-value function.
            sumQ += NestedVI.filteredFACProbabilities.get(facCount) * getQValueForAgentFAC(s,a,tempFAC);
        }//end config.

        if(NestedVI.zeroCounter > 0){
            System.out.println("Total Zeroes in state transition in  one FAC rounds:" + NestedVI.zeroCounter);
        }

        return sumQ;
    }


    /**
     * Q-Value for the current agent action and the FAC.
     * @param s is the current state.
     * @param currentAgentAction is the current Agent action.
     * @param facQ is the FAC for the transition.
     * @return is the Q-Value of the joint-action.
     */
    public Double getQValueForAgentFAC(State s, Action currentAgentAction, FrameActionConfiguration facQ) {

        FACTransitions facTransitions = new FACTransitions(s,currentAgentAction,facQ);

        double sumQ = 0.;
        if(!tf.isTerminal(s)){
            List<StateTransitionProb> tps = facTransitions.tps;
            for(int i = 0; i < tps.size(); i++){
                StateTransitionProb tp = tps.get(i);
                double p = tp.p;
                HashableState sh = hashingFactory.hashState(tp.s);
                //The reward list for the given state transition.
                double r = facTransitions.rewards.get(i);
                double vprime = this.getValue(sh);

                //TODO: Correct, but written a bit different from the equation in the OASYS paper.
                double contribution = r + gamma*vprime;
                double weightedContribution = p*contribution;

                sumQ += weightedContribution;
            }//end for.
        }//end if.

        return sumQ;
    }


    @Override
    public double value(State s) {
        Double d = this.valueFunction.get(hashingFactory.hashState(s));
        if(d == null){
            return vinit.value(s);
        }
        return d;
    }

    @Override
    public void resetSolver() {
        this.valueFunction.clear();
    }


    @Override
    public Policy planFromState(State initialState) {

        HashableState hashedInitialState = hashingFactory.hashState(initialState);
        if(this.valueFunction.containsKey(hashedInitialState)){
            return new GreedyQPolicy(this); //already performed planning here!
        }


        //Filter all the FACs by calculating their multi nomial probability.
        facFilter(facRewardFunction.getRewardUpperBound(initialState,NestedVI.domain,gamma),NestedVI.epselone);


        int maxIter = maxIterations;
//        if(finite){
//            if(!doreach) doreach = this.perform_H_StepReachabilityFrom(initialState);
//            maxIter = maxIterations;
//        }else{
//            if(!doreach) doreach = this.performReachabilityFrom(initialState);
//            maxIter = maxIterations;
//        }

        if(doreach){
            int iter=0;
            double maxChange;

            currentHorizonValueFunction = new HashMap<>();
            MyTimer timer = new MyTimer();
            do{
                timer.start();
                maxChange = Double.NEGATIVE_INFINITY;
                for(HashableState sh : states){

                    if(!this.valueFunction.containsKey(sh)){
                        setValue(sh, this.vinit.value(sh.s()));
                    }
                    double change = this.backupAgentValueFunction(sh.s());
                    maxChange = Math.max(change, maxChange);
                }

                //Set the value of the actual V for the use in all the equations after the current
                //horizon computation ends.
                for(HashableState sh: states){
                    setValue(sh, this.currentHorizonValueFunction.get(sh));
                }

                timer.stop();

                DPrint.cl(NestedVI.debugCode , "Finished VI backup: "+ iter +" in: "
                        +timer.getTime()+ "s w/ max change: " + maxChange);
                iter++;
            }while((maxChange >= maxDelta) && (iter < maxIter));
            DPrint.cl(NestedVI.debugCode, "Performed " + iter + " backups..in "
                    +timer.getTotalTime()+"s Avg time/backup = "+timer.getAvgTime()+"s");
        }

        //Compare Q-Value map for each state and update them if there are marginal errors in the Q-values.
        System.out.println("---------------Q-Value Logs---------------");
        for(HashableState sh: states){
            System.out.println("State:" + sh.s());
            List<QValue> qValues = NestedVI.qValueMap.get(sh.s());
            for(int qCount = 0; qCount <  qValues.size() ; qCount++){
                System.out.print("Action:Q-Value::" + qValues.get(qCount).a.actionName() + ":" + qValues.get(qCount).q +",");
            }//end for.
        }//end for states.
        System.out.println("---------------Q-Value Logs---------------");

        return new GreedyQPolicy(this);
    }

    private double backupAgentValueFunction(State s) {
        List<QValue> QValues = qValues(s);

        if(testingmode){
            dprint("Got Qs (inside backupAgentValueFunction)");
        }


        //The below code block fixes the fractional value errors due to processor inaccuracies.
        List<QValue> qValues = NestedVI.qValueMap.get(s);
        boolean isModified = false;
        //For each Q-value for an action compare it against other Q-values for the state.
        //If the difference between them is less than the threshold, then make them equal. zz
        for(int qCount = 0; qCount <  qValues.size() ; qCount++){
            List<QValue> compareQValues = new ArrayList<>(qValues);//Comparison Q-values.
            for(int compCount = 0; compCount <  compareQValues.size() && qCount != compCount ; compCount++){
                QValue compareQValue = compareQValues.get(compCount);
                double qValueDiff = Math.abs(qValues.get(qCount).q - compareQValue.q);
                if(qValueDiff != 0 && qValueDiff < NestedVI.qValueErrorMargin){
                    qValues.get(qCount).q = compareQValue.q;
                    isModified = true;
//                    System.out.println("Q-Value Fix:" + qValues.get(qCount).q);
                }//end if.
            }//end for.
        }//end for.
        //If modified for the error, update the Q-Value map.
        if(isModified){
            NestedVI.qValueMap.put(s,qValues);
        }//end if.


        HashableState sh = hashingFactory.hashState(s);
        double maxChange = Double.NEGATIVE_INFINITY;
        double maxQ = Double.NEGATIVE_INFINITY;

        double oldVal = getValue(sh);

        for(QValue qv : QValues){
            maxQ = Math.max(maxQ, qv.q);
        }
        double newVal = maxQ;

        maxChange = Math.max(maxChange, Math.abs(newVal-oldVal));
        this.currentHorizonValueFunction.put(sh, newVal);
        return maxChange;
    }


    public double getValue(HashableState sh){
        Double stored = this.valueFunction.get(sh);
        if(stored != null){
            return stored;
        }
        double v = 0.;
        if(!tf.isTerminal(sh.s())){
            v = this.vinit.value(sh.s());
        }
        this.valueFunction.put(sh, v);
        return v;
    }

    public void setValue(HashableState sh, double v){
        this.valueFunction.put(sh, v);
    }


    /**
     * NOTE: Change in Uniform prior.
     * This method calculates the uniform prior for the agents.
     * which is P(theta,a) = 1/N_theta * (SUM i = 1 to k) n_i / A_i
     * N_theta = number of agents of type theta in the current state.
     * n_i is the number of agents at location i of type theta.
     * A_i is the number of actions which the agent at location i and type theta can perform.
     * @param agentsInTheWorld is the list of the agent groups.
     * @param currentAgentGroup is the current agent group.
     * @param d is the Wildfire domain object.
     * @param isInflatedDistribution is the parameter to decide the base distribution for finding agents at level-0. If true, then
     *                               the agents would follow inflated distribution for actions, uniform random otherwise.
     */
    public static void calculateUniformPrior(List<String> agentsInTheWorld,String currentAgentGroup, WildfireDomain d,
                                             boolean isInflatedDistribution){
        //Create the probability array for the uniform priors.
        NestedVI.facUniformPriors = new double[NestedVI.facOthers.getMaxConfiguration().length];
        Map<Integer,Double> typeAlphas = new HashMap<>();

        //Count through all the theta-action pair and find the groups which are suitable to this pair.
        //Then, sum the agent numbers.
        //And find the ratio for particular group size/no. of actions.
        //Multiply (1/total agents) * (Sum of the ratios above).
        int configCount = 0;
        for(ConfigurationBean config : facOthers.getMaxConfiguration()){
            ArrayList<SGAgent> actingAgents = new ArrayList<>();

            //Find the agents who can perform the action of the config's type.
            for(String agentGroup: agentsInTheWorld) {
                SGAgent groupAgent = NestedVI.agentAnmDefinitions.get(agentGroup);
                for (ActionType groupAction : groupAgent.agentType().actions) {
                    //Action Type name and the action names are same in general.
                    if (groupAction.typeName().equals(config.getAction().actionName())
                            && d.sampleAgentPerGroup.get(groupAgent.agentName()).getPowerType() == config.getAgentType()) {
                        actingAgents.add(groupAgent);
                        break;
                    }//end if.
                }//end for.
            }//end for.

            double sumOfRatios = 0.0;
            for(SGAgent agent: actingAgents){

                //If the NOOP Action is there, the probability is 0.1 or 0.2
                // Hard coded for the fulfillment purpose.
                double probWeight;

                //If the base distribution is the inflated distribution than reduce the probability for the NOOPs.
                //Use the uniform random otherwise.
                if(isInflatedDistribution){
                    if(config.getAction().actionName().equals(WildfireParameters.NOOP_ACTION)){
                        if(agent.agentType().actions.size()== 3){
                            probWeight = 0.2;
                        }else if(agent.agentType().actions.size() == 4){
                            probWeight = 0.1;
                        }else{
                            probWeight = 0.1;
                        }
                    }else{
                        if(agent.agentType().actions.size() == 3){
                            probWeight = (1 - 0.2)/(agent.agentType().actions.size()-1);
                        }else if(agent.agentType().actions.size() == 4){
                            probWeight = (1 - 0.1)/(agent.agentType().actions.size()-1);
                        }else{
                            probWeight = (1 - 0.1)/(agent.agentType().actions.size()-1);
                        }
                    }
                }else{
                    probWeight = 1.0/agent.agentType().actions.size();
                }//end if-else.

                ArrayList<String> startEndIndexes =new ArrayList<>(Arrays.asList(agent.agentName().split("-")));
                //Separate indexes.
                int startIndex = Integer.parseInt(startEndIndexes.get(0));
                int endIndex = Integer.parseInt(startEndIndexes.get(1));

                double total = (endIndex-startIndex);
                if(!agent.agentName().equals(currentAgentGroup)){
                    total +=1;
                }
                //Add number of agents to total agents.
                sumOfRatios += (total * probWeight /agent.agentType().actions.size());
            }//end for.

            //Find the number of the agents of a particular type in the configuration
            //of the same type as the current type-action pair.
            int totalTypeAgents = 0;
            for(int typeIndex = 0; typeIndex < NestedVI.facOthers.getAgentTypes().length; typeIndex++){
                if(config.getAgentType() == NestedVI.facOthers.getAgentTypes()[typeIndex][0]){
                    totalTypeAgents = NestedVI.facOthers.getAgentTypes()[typeIndex][1];
                }//end if.
            }//end for.

            //Find the final probability after the division by the N_type.
            NestedVI.facUniformPriors[configCount++] = sumOfRatios/totalTypeAgents;

            //Calculate the alpha's to normalize.
            if (typeAlphas.get(config.getAgentType()) == null){
                typeAlphas.put(config.getAgentType(),sumOfRatios/totalTypeAgents);
            }else{
                typeAlphas.put(config.getAgentType(),typeAlphas.get(config.getAgentType()) + sumOfRatios/totalTypeAgents);
            }//end if-else.

        }//end for config.

        //Normalize the probabilities.
        configCount = 0;
        for(ConfigurationBean config : facOthers.getMaxConfiguration()){
            NestedVI.facUniformPriors[configCount] = NestedVI.facUniformPriors[configCount] / typeAlphas.get(config.getAgentType());
            configCount++;
        }//end for.

    }//end method.

    public static List<Action> SGtoGroundedSG(
            List<ActionType> allsgActions, String actingAgent) {
        List<Action> allAgActions = new ArrayList<Action>();
        for(int i=0;i<allsgActions.size();i++){
            allAgActions.add(allsgActions.get(i).associatedAction(actingAgent));
        }
        return allAgActions;
    }


    public List<Action> getAllGroundedActionsForAgentID(State s, int agentIndex){
        String actingAgent = agIDtoName.get(agentIndex);
        List<ActionType> allsgActions = agentDefinitions.get(actingAgent).agentType().actions;
        List<Action> allAgActions = SGtoGroundedSG(allsgActions,actingAgent);
        return allAgActions;
    }

    public Map<Action,List<JointAction>> getGAJAMap(State s, List<JointAction> allJAs, int agentIndex){
        Map<Action,List<JointAction>> gaJAMap = new HashMap<Action, List<JointAction>>();
        List<Action> allAgActions = getAllGroundedActionsForAgentID(s,agentIndex);

        for(Action ga:allAgActions){
            gaJAMap.put(ga, getJAListforGA(ga,allJAs,agentIndex));
        }

        return gaJAMap;
    }

    public List<JointAction> getJAListforGA(Action ga, List<JointAction> allJAs, int agentIndex) {
        List<JointAction> jaList = new ArrayList<JointAction>();
        for(JointAction ja: allJAs){
            if(ja.action(agentIndex).actionName() == ga.actionName()){
                jaList.add(ja);
            }
        }
        return jaList;
    }


    /**
     * Inner class for calculating the Rewards and StateTransition Probs.
     */
    public class FACTransitions{
        public Action currentAgentAction;
        public FrameActionConfiguration facIn;
        public List<StateTransitionProb> tps;
        public ArrayList<Double> rewards;

        /**
         * Given the state, current Agent Action and configuration, calculate the state transition probability for the
         * next states and also calculate rewards.
         * @param s is the current state.
         * @param currentAgentAction is the action done by current agent.
         * @param facIn is the FAC for the other agents.
         */
        public FACTransitions(State s,Action currentAgentAction, FrameActionConfiguration facIn) {
            this.currentAgentAction = currentAgentAction;
            this.facIn = facIn;

            //Add current agent to the action list.
            WildfireAnonymousState was = (WildfireAnonymousState)s;
            int facIndex;

            //Get anonymous state and check if the current agent's suppressant level is 0 or not.
            //If it's zero, then add 1 to the NOOP, otherwise find the index of the current agent's action to
            //merge it with current configuration.
            if(was.getSelfAgent().getAvailability() == 0){
                //Add current action to the configuration and create a new inclusive fac.
                facIndex =  this.facIn.search(was.getSelfAgent().getPowerType(),WildfireParameters.NOOP_ACTION);
            }else{
                //Add current action to the configuration and create a new inclusive fac.
                facIndex =  this.facIn.search(was.getSelfAgent().getPowerType(),currentAgentAction.actionName());
            }



            if(facIndex != -1){
                int numOfAgents = this.facIn.getCurrentConfiguration()[facIndex].getNumberOfAgents();
                this.facIn.getCurrentConfiguration()[facIndex].setNumberOfAgents( numOfAgents + 1);
            }//end if.

            //NOTE : Sanity check. Just in case the transition method is modifying the state.
            WildfireAnonymousState anonymousState = new WildfireAnonymousState(was);
            this.tps = facModel.stateTransitions(anonymousState, this.facIn);

            if(tps.size() == 0){
                NestedVI.zeroCounter++;
//                System.out.println("State Transition with 0 next states.");
//                System.out.println( "State:" + ((WildfireAnonymousState)s).toString() + " action:" + currentAgentAction.actionName()
//                        + " fac:" + Arrays.toString(facIn.currentConfiguration));

            }
            this.rewards = new ArrayList<Double>();

            for(StateTransitionProb tp : this.tps){
                double reward = facRewardFunction.reward(s,currentAgentAction,this.facIn, tp.s) ;
                this.rewards.add(reward);
            }//end for.
        }//end constructor.

        //Same as previous except, there is an input variable for state transition.
        public FACTransitions(State s, Action currentAgentAction, FrameActionConfiguration facIn, List<StateTransitionProb> tps){
            this.currentAgentAction = currentAgentAction;
            this.facIn = facIn;
            this.tps = tps;
            for(StateTransitionProb tp : this.tps){
                double reward = facRewardFunction.reward(s,currentAgentAction,this.facIn, tp.s);
                this.rewards.add(reward);
            }//end for.
        }
    }//end class.

    public static void dprint(String toPrint){
        DPrint.cl(NestedVI.debugCode, toPrint);
    }

    public void setAgentDefinitions(Map<String, SGAgent> agentDefs){

        if(this.planningStarted){
            throw new RuntimeException("Cannot reset the agent definitions after planning has already started.");
        }

        if(agentDefinitions == null){
            return;
        }

        if(agentDefinitions == agentDefs){
            return ;
        }

        agentDefinitions = agentDefs;

    }


    public static void printAgLvlMap(HashMap<String, ArrayList<Integer>> hmap) {
        Set<Map.Entry<String, ArrayList<Integer>>> set = hmap.entrySet();
        Iterator<Map.Entry<String, ArrayList<Integer>>> iterator = set.iterator();
        while(iterator.hasNext()) {
            @SuppressWarnings("rawtypes")
            Map.Entry mentry = (Map.Entry)iterator.next();
            System.out.print("Agent ID: "+ mentry.getKey() + " & Level-Policies to be computed: ");
            System.out.println(mentry.getValue());
        }
    }

    public static HashMap<Integer, ArrayList<Integer>> makeAgLvlMap(int level, int currentAgentIndex) {
        HashMap<Integer, ArrayList<Integer>> agLvlMap = new HashMap<Integer, ArrayList<Integer>>();
        ArrayList<Integer> lvlList;
        int curPlayerLevel = level;
        int otherPlayerLevel = level-1;

        for(int i=0;i<numPlayers;i++){
            if(level==0) break;
            lvlList = new ArrayList<Integer>();
            for(int k=0;k<=level;k++){
                if(i==currentAgentIndex){
                    if(k==otherPlayerLevel) continue;
                    lvlList.add(k);
                }else{
                    if(k==curPlayerLevel) continue;
                    lvlList.add(k);
                }
            }
            agLvlMap.put(i, lvlList);
        }
        return agLvlMap;
    }


    public static Map<String,Integer> readMDPActionNameToID(String filename) {

        Map<String,Integer> actionNameToID = new HashMap<String,Integer>();
        try {
            Scanner s = new Scanner(new File(filename));
            String actionNames = s.nextLine();
            String[] actionNamesArray = actionNames.split(",");
            for(int i=0;i<actionNamesArray.length;i++){
                actionNameToID.put(actionNamesArray[i], i);
            }

            s.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return actionNameToID;
    }

    @SuppressWarnings("unused")
    private static void writeActions(OOSGDomain d, StateEnumerator senum, Map<Integer, Action> mdpActions, String filename) {
        boolean writeToFile = true;
        if(writeToFile){
            //Write alphavector actions
            try {
                File file = new File(filename);
                // if file doesnt exists, then create it
                if (!file.exists()) {
                    file.createNewFile();
                }

                String content = "";//Integer.toString(alphaVectorActions .size());
                for(int i=0;i<senum.numStatesEnumerated();i++){
                    content = content + Integer.toString(i) + "," + mdpActions.get(i).actionName() + "\n" ;
                }


                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(content);
                bw.close();

                System.out.println("Done writing MDP Policy Actions..");

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    public static Policy runVI(State s, OOSGDomain d, StateEnumerator senum1,
                               Map<String, SGAgent> agentDefs, Map<Integer, String> agIDtoN,
                               FACRewardFunction fc1, TerminalFunction tf1, HashableStateFactory hf,
                               double gamma1, double maxDelta1, int maxIterations1, int maxLevel,
                               List<String> agentsInWorld1, int currentAgentIndex, boolean finite1) {

        int numP = agentsInWorld1.size();
        //Initialize the Nested VI.
        initNestedVI = initNestedVI(d, senum1, agentDefs, agIDtoN, fc1, tf1, hf,
                gamma1, maxDelta1, maxIterations1, agentsInWorld1, numP, finite1);

        //The following is just used to generate uniformly random policies of others for level-0 planning
        Map<String,Policy> otherAgentsPoliciesForLvl0Planning = new HashMap<String,Policy>();
        otherAgentsPoliciesForLvl0Planning = generateOthersUniformPolicy(d,s,currentAgentIndex);

        //The main method for computing all level policies for all agents from state
        Map<Integer,Map<Integer,Policy>> allLevelPolicyList = new HashMap<Integer,Map<Integer,Policy>>();
        allLevelPolicyList = computeAgLvlPoliciesFromState(s, currentAgentIndex, otherAgentsPoliciesForLvl0Planning, maxLevel);


        return allLevelPolicyList.get(maxLevel).get(currentAgentIndex);
    }

    private static Map<Integer, Map<Integer, Policy>> computeAgLvlPoliciesFromState(State s,
                                                                                    int subjectAgentIndex, Map<String, Policy> otherAgentsPoliciesForLvl0Planning, int maxLevel) {

        if(maxLevel==0){

            Map<Integer,Policy> agPolicyList = new HashMap<Integer,Policy>();
            Map<Integer,Map<Integer,Policy>> allLevelAgPolicyList = new HashMap<Integer,Map<Integer,Policy>>();

            NestedVI nVI = new NestedVI(new ConstantValueFunction(0.0),
                    subjectAgentIndex, otherAgentsPoliciesForLvl0Planning, maxLevel);

            Policy agentPol = nVI.planFromState(s);
            Policy cAgentPol = new CachedPolicy(hashingFactory,(EnumerablePolicy) agentPol);
            agPolicyList.put(subjectAgentIndex, cAgentPol);

            allLevelAgPolicyList.put(maxLevel, agPolicyList);
            return allLevelAgPolicyList;

        }


//        ArrayList<Integer> agLvlList;
//        HashMap<String, ArrayList<Integer>> agLvlMap = new HashMap<String, ArrayList<Integer>>();
//
//
//        //For the current agent, put the level which is max.
//        //For all others keep it one level below.
//        for(String agent:agentsInWorld){
//            agLvlList = new ArrayList<Integer>();
//            for(int i=0;i<=maxLevel;i++){
//                if (agent.equals(subjectAgentIndex+"")){
//                    if (i==(maxLevel-1)) continue;
//                    agLvlList.add(i);
//                }else{
//                    if (i==maxLevel) continue;
//                    agLvlList.add(i);
//                }
//            }
//            agLvlMap.put(agent, agLvlList);
//        }
//
//        if(verbosemode){
//            printAgLvlMap(agLvlMap);
//        }

        //All Level policy for the current agent.
        Map<Integer,Map<Integer,Policy>> allLevelAgPolicyList = new HashMap<Integer,Map<Integer,Policy>>();

//        for(int level=0; level<=maxLevel; level++){
//            if (level == 0){
//                Map<Integer,Policy> agPolicyList = new HashMap<Integer,Policy>();
//                for(String agent:agentsInWorld){
//                    if(agLvlMap.get(agent).contains(level)){
//                        dprint("Computing Agent "+agent+"'s Level-"+Integer.toString(level)+" policy:");
//                        NestedVI nVI = new NestedVI(new ConstantValueFunction(0.0),
//                                agentID, otherAgentsPoliciesForLvl0Planning, maxLevel);
//
//                        System.out.println("Number of other agent lvl 0 policies: "+Integer.toString(otherAgentsPoliciesForLvl0Planning.size()));
//                        Policy agPol = null;
////						if(agentID==subjectAgentIndex){
////							agPol = nVI.planFromState(s);
////						}else{
////							State oState = Wildfire.createOtherAgentStateFromLocalState((WildfireDomain) domain,agentID, s);
////							agPol = nVI.planFromState(oState);
////						}
//                        agPol = nVI.planFromState(s);
////						Policy cAgentPol = new CachedPolicy(hashingFactory,agPol);
//                        agPolicyList.put(agentID, agPol);
//
//                    }else{
//                        continue;
//                    }
//                }
//                allLevelAgPolicyList.put(level,agPolicyList);
//
//            }else{
//                Map<Integer,Policy> agPolicyList = new HashMap<Integer,Policy>();
//                for(int agentID:agentsInWorld){
//                    if(agLvlMap.get(agentID).contains(level)){
//                        dprint("Computing Agent "+Integer.toString(agentID)+"'s Level-"+Integer.toString(level)+" policy:");
//                        //get all other agent's policies at immediate lower level l-1
//                        Map<Integer,Policy> allOtherAgPolicyList = new HashMap<Integer,Policy>();
//                        for(Map.Entry<Integer,Policy> e:allLevelAgPolicyList.get(level-1).entrySet()){
//                            if(e.getKey() == agentID){
//                                continue;
//                            }
//                            allOtherAgPolicyList.put(e.getKey(),e.getValue());
//                        }
//
//                        NestedVI nVI = new NestedVI(new ConstantValueFunction(0.0),
//                                agentID, allOtherAgPolicyList, maxLevel);
//
//                        Policy cAgentPol = new CachedPolicy(hashingFactory,(EnumerablePolicy) nVI.planFromState(s));
//                        agPolicyList.put(agentID, cAgentPol);
//
//                    }else{
//                        continue;
//                    }
//                }
//                allLevelAgPolicyList.put(level,agPolicyList);
//            }
//        }

        return allLevelAgPolicyList;
    }

    //Updated to return the Policy for the whole group.
    public static Map<String,Policy> generateOthersUniformPolicy(OOSGDomain d, State s, Integer subjectAgent) {
        //Convert to wildfire domain.
        WildfireDomain wf = (WildfireDomain)d;

        Map<String,Policy> otherAgentsPoliciesForLvl0Planning = new HashMap<String,Policy>();

        if(numPlayers > 1){
            //Generate uniform policy for each agent groups.
            for(String agent:agentsInWorld){

                List<ActionType> allsgActions = agentAnmDefinitions.get(agent).agentType().actions;
                List<Action> allAgActions = SGtoGroundedSG(allsgActions,agent);

                if(debugmode){
                    System.out.println("GroundedSGAgentActions for "+agent+":");
                    printList(allAgActions);
                    System.out.println("SGAgentActions for "+agent+":");
                    printList3(allsgActions);
                }

                Policy randPolicy = new RandomPolicy(d, allsgActions);
                otherAgentsPoliciesForLvl0Planning.put(agent,randPolicy);
            }
        }
//        if(agentsInWorld.size() != (otherAgentsPoliciesForLvl0Planning.size()+1)){
//            dprint("The number of other agent policies -"+otherAgentsPoliciesForLvl0Planning.size()+"- doesn't makes sense");
//        }

        return otherAgentsPoliciesForLvl0Planning;
    }




    public static void printList(List<Action> allAgActions) {
        for(int i=0;i<allAgActions.size();i++){
            System.out.print("  "+ allAgActions.get(i).actionName());
        }
        System.out.println();
    }


    private static void printList3(List<ActionType> allSGActions) {
        for(int i=0;i<allSGActions.size();i++){
            System.out.print("  "+ allSGActions.get(i).toString());
        }
        System.out.println();
    }


    /**
     * This method filter outs the FACs, which are supposed to be removed due to very less utility value.
     * The FACs are being iterated using the FrameActionConfiguration class.
     * The calculation done in a few steps.
     * 1. Get the FAC log probabilities.
     * 2. Compare the values with epselone :
     *     Check if ( Log(P(FAC)) + Log(Reward Upper Bound) >= Log(epselone))
     *     If Yes, add the FAC to the list and add the P(FAC) value against that entry.
     * @param rewardUpperBound is the calculated reward upper bound for the domain.
     * @param epselone is the epselone- threshold value.
     */
    public static void facFilter(double rewardUpperBound ,double epselone){
        //Filtered FAC objects.
        NestedVI.filteredFACs = new ArrayList<>();
        NestedVI.filteredFACProbabilities = new ArrayList<>();

        //Initialize the fac for iteration.
        facOthers.initializeCurrentFAC();

        //Total FAC count
        int counter = 0;
        //Get the log values for 1 to total number of agents.
        double[] logValues = factorialLogValues(facOthers.getTotalAgents());

        double probLogValue = 0;//Variable for calculation.
        double overallSum = 0;//Sum the probabilities for sanity check.
        //Search through all the FACs.
        while (facOthers.hasNext()){
            counter++;

            //Get the ln(p(FAC))
            probLogValue = getFACLogProbs(facOthers,logValues);
            //Sum up the log probabilities.
            overallSum += Math.exp(probLogValue);

            //Add the FAC to the list.
            if(( probLogValue + Math.log(rewardUpperBound))>= Math.log(epselone)){
                //Create an array of the counts.
                int[] configCounters = new int[facOthers.getCurrentConfiguration().length];
                for(int configCount = 0 ; configCount < facOthers.getCurrentConfiguration().length ; configCount++){
                      configCounters[configCount] = facOthers.getCurrentConfiguration()[configCount].getNumberOfAgents();
                }//end for.

                NestedVI.filteredFACs.add(configCounters);
                //Add probabilities
                NestedVI.filteredFACProbabilities.add(Math.exp(probLogValue));
            }//end if.
        }//end while.
        System.out.println("Out of " + counter + " FACs, only " + NestedVI.filteredFACs.size() + " selected.");
        System.out.println("Sum of Probabilities: " + overallSum);

    }//end method.


    /**
     * Get the probability of the configuration using the Multi-nomial PDF function:
     * P(FAC) =  _product [theta= 1 to k] ((Total Num Agents of type theta)! / _product (Agents in each type-action pair of type theta)!)
     *                                      * (_product (probability of type-action of type theta)^(Number of agents doing that action of type theta))
     * If we convert this into log format.
     * LOG (P (FAC)) = _sum [theta= 1 to k] LOG((Total Num Agents of type theta)!) - (_SUM (LOG(Agents in each type-action pair of type theta)!))
     *                      +    (_SUM (Num of agent in type-action pair of type theta * Log(probability of the action of type theta)))
     * @param probFAC is the Frame Action Configuration for which the probability should be calculated.
     * @param logValues is an array of log values of factorials maximum size up to number of agents.
     * @return log probability value for the FAC.
     */
    public static double getFACLogProbs(FrameActionConfiguration probFAC, double[] logValues){
        double probLogSum = 0; //Probabilities log sum.

        for(int typeIndex = 0 ; typeIndex < probFAC.getAgentTypes().length;typeIndex++){
            double typeSum = 0;
            int type = probFAC.getAgentTypes()[typeIndex][0];
            //First set the sum as  the N! log value.
            typeSum += logValues[probFAC.getAgentTypes()[typeIndex][1] - 1];
            //Now subtract all the agent number factorial log values.
            // and add the  num of agents * log(type-action probability).
            for(int configCount = 0 ; configCount < probFAC.getCurrentConfiguration().length ; configCount++){
                //Make sure to not have log(0) error and the type matches.
                if(type ==  probFAC.getCurrentConfiguration()[configCount].getAgentType() &&
                        probFAC.getCurrentConfiguration()[configCount].getNumberOfAgents() != 0){
                    //Factorial value sum.
                    typeSum += (-1) * logValues[probFAC.getCurrentConfiguration()[configCount].getNumberOfAgents() - 1];
                    //Add the probability values.
                    typeSum +=  probFAC.getCurrentConfiguration()[configCount].getNumberOfAgents() *
                            Math.log(NestedVI.facUniformPriors[configCount]);
                }//end if.
            }//end for - config Count.

            probLogSum += typeSum;
        }//end for - type.



        return probLogSum;
    }//end method.



    /**
     * This method calculates the log values of the factorial until the maximum value is reached.
     * E.g. the maxValue is 5. The return value would contain [log(1), log(2), log(3),log(4), log(5)].
     * @param maxValue is the maximum value calculation.
     * @return is an array of the log values.
     */
    public static double[] factorialLogValues(int maxValue){
        double[] logValues = new double[maxValue];
        double logSum = 0;
        for(int value = 1 ; value <= maxValue ; value++){
            logSum += Math.log(value);
            logValues[value-1] = logSum;
        }//end for.
        return  logValues;
    }//end method.

    public static void main(String[] args) {

        //example command line argument
        /*
         * order of command line arguments: gamma maxDelta maxIterations conf experiment
         *
         * [mkran@erebus oasys-git]$  java -cp "bin/:lib/burlap.jar:lib/libpomdp.jar:external/*.jar" nestedMDPSolver/NestedVI 0.9 0.1 50 1 5 > output/Simulations/Config_1_Results/Experiment_5/RUN_2-WF_config1_NestedMDP_experiment5_50iters.txt
         *
         */

        //Domain selection Wildfire. ENUM.
        dom dom = NestedVI.dom.WF;

        int maxLevel = 0; //Max Level for I-POMDP Lite.
        double gamma = 0.9; //Gamma value(Discounting Factor) for calculating the VI function in the I-POMDP Lite.
        double maxDelta = 0.001;//Delta
        int maxIterations = 3;//VI count
        int conf = 14; //Test Configuration.
        int experiment = 1;//Experiment number.
        boolean estimateTF = false; //Keep it false.
        int agentAvailability = WildfireParameters.MAX_SUPPRESSANT_STATES - 1; //Full 0-1-2 are 3 suppressant states.
        String currentGroupToBKP = "0-9";//Agent Group for the perspective.
        String neighbourGroupToBKP = "0-9";//neighbour to model for the CurrentGroupToBKP.

        NestedVI.epselone = 0.99;
        NestedVI.qValueErrorMargin = 0.001;

//        String outFileHead = "D:\\MyWork\\scalableoasys\\output\\Simulations\\Config_"+ WildfireParameters.config+"_Results\\Experiment_"+experiment+"\\";

        //If you want to run the whole Nested VI modelling in sequence, pass only 7 arguments.
        //Else if you want to create policy for a particular set pass the current agent group and the neighbour group to map. This can run in parallel with
        //bash script.
		if(args.length == 8){
            conf = Integer.parseInt(args[0]);
            gamma = Double.parseDouble(args[1]);
            maxLevel = Integer.parseInt(args[2]);
			maxDelta = Double.parseDouble(args[3]);
			maxIterations = Integer.parseInt(args[4]);
            NestedVI.epselone = Double.parseDouble(args[5]);
			experiment = Integer.parseInt(args[6]);
            currentGroupToBKP = null;
            neighbourGroupToBKP = null;
            NestedVI.qValueErrorMargin = Double.parseDouble(args[7]);
		}else if (args.length == 10){
            conf = Integer.parseInt(args[0]);
            gamma = Double.parseDouble(args[1]);
            maxLevel = Integer.parseInt(args[2]);
            maxDelta = Double.parseDouble(args[3]);
            maxIterations = Integer.parseInt(args[4]);
            NestedVI.epselone = Double.parseDouble(args[5]);
            experiment = Integer.parseInt(args[6]);
            currentGroupToBKP = args[7];
            neighbourGroupToBKP = args[8];
            NestedVI.qValueErrorMargin = Double.parseDouble(args[9]);
        }


        String outFileHead = "output/Simulations/Config_"+conf +"/Experiment_"+experiment+"/";
        //Testing arguments
        System.out.print(gamma + " " + maxDelta + " " + maxIterations + " " + conf + " " + experiment);

        State ms = null;//Master State.
        WildfireDomain d = null; //Domain.
        int numPlayers = 0; //Players.
        FACRewardFunction facr = null;
        TerminalFunction tf = null;
        List<Map<String, SGAgent>> allAgentDefinitions = new ArrayList<Map<String, SGAgent>>();
        Map<String,Map<String, SGAgent>> allAgentAnmDefinitions = new HashMap<String,Map<String, SGAgent>>();
        Map<Integer, String> agIDtoName = new HashMap<Integer, String>();
        Map<Integer,Map<String,Policy>> agFinalPolicyMap = new HashMap<Integer,Map<String,Policy>>();





        MyTimer senumTimer = new MyTimer();

        boolean vmode = true;
        boolean dmode = false;
        boolean tmode = false;
        boolean finite = true;

        verbosemode = vmode;
        testingmode = tmode;
        debugmode = dmode;
        DPrint.toggleCode(debugCode,verbosemode);

        if(dom.equals(NestedVI.dom.WF)){
            if(estimateTF)
                System.out.println("Transition function has been estimated!");

            System.out.println("Generating the domain..");
            Wildfire wf = new Wildfire();
            d = (WildfireDomain) wf.generateDomain(conf,estimateTF);
            facr = new WildfireRewardFunction();
            tf = new NullTermination();

            //Just need one agent from each agent-group.
            numPlayers = d.agentGroups.size();



            //1. Get agent definitions
            System.out.println("Getting all agent definitions..");
            allAgentDefinitions = Wildfire.getAllAgentDefs((WildfireDomain) d);
            allAgentAnmDefinitions = Wildfire.getAllAnmAgentDefs((WildfireDomain) d);

            //2. Get agent id-name mapping
            System.out.println("Getting agent id-name mappings..");
            for(int n=0;n<numPlayers;n++){
                //Take the first agent of each group.
                //Get the group string.
                ArrayList<String> startEndIndexes =new ArrayList<> (Arrays.asList(d.agentGroups.get(n).split("-")));
                //Separate indexes.
                int startIndex = Integer.parseInt(startEndIndexes.get(0));
                agIDtoName.put(startIndex, d.agentsList.get(startIndex).getAgentGroup());
            }

            //3. Get initial master state and master anonymous state.
            //Set the Suppressant levels from the beginning.
            //No random start for fires.
            ms = Wildfire.getInitialMasterState((WildfireDomain) d,agentAvailability,false,false,false);


            //Iterate through all the groups and run the Nested VI for the first agent of each group.
            for(int currentGroup = 0; currentGroup < numPlayers; currentGroup++){ //numPlayers


                //Run for just one Group currentGroupToBKP. Otherwise continue.
                if(currentGroupToBKP != null && !("".equals(currentGroupToBKP)) && !(d.agentGroups.get(currentGroup).equals(currentGroupToBKP))){
                    continue;
                }


                System.out.println("==Start=>---------------------------------------------------");
                System.out.println("                  Subject agent: "+currentGroup);
                System.out.println("---------------------------------------------------");

                //1. find neighbors of subject agent
                ArrayList<String> neighbors = ((WildfireDomain) d).agentsNeighbourhood.get(d.agentGroups.get(currentGroup));

                //Get the first agent of the group.
                ArrayList<String> startEndIndexes =new ArrayList<>(Arrays.asList(d.agentGroups.get(currentGroup).split("-")));
                //Separate indexes.
                int firstAgentIndex = Integer.parseInt(startEndIndexes.get(0));

                //Add the agents and their neighbours to the world.
                //The list is heterogeneous and would contain the current agent number and neighbour groups. e.g. [1,5-10]
                List<String> agentsInWorld = new ArrayList<String>();
                agentsInWorld.add(d.agentGroups.get(currentGroup));
                //Add neighbours.
                for(String neighbourGroup : neighbors){
                    agentsInWorld.add(neighbourGroup);
                }

                int numAgentsInSubjectAgentWorld = agentsInWorld.size();
                System.out.println("Total Number of agents and agent-groups in "+currentGroup+"'s world: "+ numAgentsInSubjectAgentWorld);

                //2. set agent definitions of agents in the local neighborhood of subject agent
                System.out.println("Getting agent definitions for all agents in subject agent's world..");
                NestedVI.agentDefinitions = allAgentDefinitions.get(firstAgentIndex);
                NestedVI.agentAnmDefinitions = allAgentAnmDefinitions.get(d.agentGroups.get(currentGroup));


                //3. create state of subject agent from master state and then convert it into the Anonymous state.
                System.out.println("Getting subject agent's initial state and it's anonymous version..");
                State initialState = Wildfire.createAgentStateFromMasterState(d, firstAgentIndex, ms);
                State initialAnmState = new WildfireAnonymousState((WildfireState)initialState,d);

                //4. Setting state enumerator for subject agent's world..
                //Updated to take the Anonymous definitions of the state.
                System.out.println("Setting state enumerator for subject agent's world..");
                //NOTE: BURLAP 2 and 3 implementations are different. They have
                HashableStateFactory hf = new SimpleHashableStateFactory(true);




                //Clear the FAC every time planning for a new agent group.
                NestedVI.fac = new FrameActionConfiguration();
                System.out.println("Caching all enumerated states reachable from subject agent's initial state..");


                Map<String,Policy> neighborPolicies = new HashMap<String,Policy>();

                //Now, create policy for the random agents.
                for(String neighbourTOMap: agentsInWorld){

                    //Run for just one Group neighbourGroupToBKP. Otherwise continue.
                    if(neighbourGroupToBKP != null && !("".equals(neighbourGroupToBKP)) && !(neighbourTOMap.equals(neighbourGroupToBKP))){
                        continue;
                    }

                    //Reset the Backup flag.
                    isBackedUp = false;

                    int neighborType = ((WildfireDomain) d).sampleAgentPerGroup.get(neighbourTOMap).getPowerType();
                    System.out.println("Getting neighbor agent "+neighbourTOMap+"'s local state from subject agent "+firstAgentIndex+"'s world..");

                    //Change the state type if the agent-group in process is not of the current agent's group.
                    State neibourState;
                    if(neighbourTOMap.equals(d.agentGroups.get(currentGroup))){
                        neibourState = initialAnmState;
                    }else{
                        neibourState = Wildfire.createOtherAgentAnmState((WildfireDomain) d,neighbourTOMap, initialAnmState);
                    }





                    // Caching all enumerated states..
                    //Get all possible reachable states from the current states in infinite/<horizon value> horizons.
                    senumTimer.start();
                    states = new HashSet<HashableState>();
                    d.setStateEnumerator(new StateEnumerator(d, hf, agentDefinitions,agentAnmDefinitions));


                    System.out.println("Getting state enumerator for subject agent's world..");
                    StateEnumerator senum = d.getStateEnumerator();
                    if(finite){
                        //Don't Exclude self agent's actions in FAC.
                        senum.findReachableAnmStatesAndEnumerate(neibourState,NestedVI.fac,maxIterations,false); //remove horizons later
                    }else{
                        //Don't Exclude self agent's actions in FAC.
                        senum.findReachableAnmStatesAndEnumerate(neibourState,NestedVI.fac, -1,false);//Put Horizon value as -1.
                    }
                    //Enumerate all the states.
                    int nS = senum.numStatesEnumerated();
                    for(int i=0;i<nS;i++){
                        if(i%1000 == 0){
                            System.out.print(".");
                        }
                        State st = senum.getStateForEnumerationId(i);
                        HashableState hashedST = hf.hashState(st);
                        states.add(hashedST);
                    }



                    //State File Name.
                    String stateFileName = outFileHead + "States_" + d.agentsList.size()
                            +"-WF_config"+ conf +"_firestates"+WildfireParameters.MAX_FIRE_STATES
                            +"_current_agent"+firstAgentIndex + "_neighbour_" + neighbourTOMap + ".txt";

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


                    //Stop Timer.
                    System.out.println("[Done] in "+senumTimer.getTime()+"s; "+states.size()+" unique states found.");
                    System.out.println("States are written to:" + stateFileName);


                    doreach = true;
                    senumTimer.stop();




                    //Find the Possible FACs for the current agent, if the other agent is not from the same group.
                    NestedVI.facOthers = new FrameActionConfiguration();
                    senum.initializeFAC(neibourState,NestedVI.facOthers,true);//Exclude self agent's actions in FAC.

                    System.out.println("Neighbouring State Description:" + neibourState.toString());
                    System.out.println("Planning from neighbor agent "+neibourState+"'s local state..");

                    ArrayList<String> neighbourIndexes =new ArrayList<> (Arrays.asList(neighbourTOMap.split("-")));
                    //Separate indexes.
                    int neighborAgent = Integer.parseInt(neighbourIndexes.get(0));


                    //Calculate the Random prior for the agent action.
                    //The method takes all static parameters to calculate the prior.
                    calculateUniformPrior(agentsInWorld,neighbourTOMap,d,false);//use Uniform random probabilities.

                    //Calculate the policy.
                    Policy nPol =  NestedVI.runVI(neibourState,d,senum,agentAnmDefinitions,agIDtoName,facr,tf,hf,
                            gamma,maxDelta,maxIterations,maxLevel,agentsInWorld,neighborAgent,finite);

                    //Set the flag to done,so that no more qValue calculations.
                    isBackedUp = true;

                    neighborPolicies.put(neighbourTOMap,nPol);//Save the policy on group's name.



                    System.out.println("Writing neighbor agent "+neighbourTOMap+"'s policy to file..");
                    //Writing policy to file
                    //Set the agent type from the anonymous agent definitions.
                    SetStrategySGAgent.SetStrategyAgentFactory agentFactory = new SetStrategySGAgent.SetStrategyAgentFactory(d, nPol);
                    SGAgent MDPagent = agentFactory.generateAgent(neighbourTOMap,
                            agentAnmDefinitions.get(neighbourTOMap).agentType());
                    World w = new World(d, facr, tf, neibourState);
                    w.join(MDPagent);

                    //TODO: Organize the file names through-out the domain.
                    String filename = outFileHead + d.agentsList.size()
                                               +"-WF_config"+ conf +"_firestates"+WildfireParameters.MAX_FIRE_STATES
                                                +"_nmdp_policy_level"+maxLevel+"_agent"+firstAgentIndex+"_neighbor"+neighbourTOMap
                                                +"-"+neighborType+"_iter"+ maxIterations+".txt";
                    String fileNameQ = outFileHead + d.agentsList.size()
                            +"-WF_config"+ conf +"_firestates"+WildfireParameters.MAX_FIRE_STATES
                            +"_nmdp_QValues_level"+maxLevel+"_agent"+firstAgentIndex+"_neighbor"+neighbourTOMap
                            +"-"+neighborType+"_iter"+ maxIterations+".txt";


                    System.out.println("FileName:" + filename);
                    File outputFile,outputFileQ;
                    PrintWriter out,outQ;
                    try {
                        outputFile = new File(filename);
                        outputFileQ = new File(fileNameQ);

                        outputFile.getParentFile().mkdirs();
                        outputFileQ.getParentFile().mkdirs();

                        if(!outputFile.exists()){
                            outputFile.createNewFile();
                        }

                        if(!outputFileQ.exists()){
                            outputFileQ.createNewFile();
                        }

                        out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile.getAbsoluteFile())));
                        outQ = new PrintWriter(new BufferedWriter(new FileWriter(outputFileQ.getAbsoluteFile())));

                        int c=0;
                        String output = "";
                        String outputQ = "";

                        for(HashableState state: states){
                            Action mdpaction = w.getRegisteredAgents().get(0).action  (state.s());
                            List<ActionProb> aps = new ArrayList<ActionProb>();
                            aps = ((CachedPolicy)nPol).policyDistribution(state.s());
                            String apsString = "";
                            String actionStr = "";
                            for(ActionProb ap: aps){
                                if(c<1) actionStr += ap.ga.actionName()+",";
                                apsString += Double.toString(ap.pSelection) +",";
                            }

                            List<QValue> qValues = NestedVI.qValueMap.get(state.s());
                            outputQ += ((WildfireAnonymousState)state.s())
                                    + " : "  + ((WildfireAnonymousState)state.s()).hashCode() + ": \n";
                            for(int qCount = 0; qCount <  qValues.size() ; qCount++){
                                outputQ +=  qValues.get(qCount).a.actionName() + ":" + qValues.get(qCount).q +",";
                            }//end for.
                            outputQ += "\n";


                            if(c<1) actionStr += "\n";
                            output +=actionStr+ ((WildfireAnonymousState)state.s()).hashCode() + ":" +mdpaction.actionName()+":"+apsString+"\n";
                            c++;
                        }
                        out.println(output);
                        outQ.println(outputQ);
                        System.out.println("Neighbor agent "+neighbourTOMap+"'s policy is stored in file.."+filename);

                        out.close();
                        outQ.close();
                    }catch (IOException e) {
                        System.out.println("Error:" + e.getMessage());
                        e.printStackTrace();
                    }
                }//end for.

                agFinalPolicyMap.put(firstAgentIndex,neighborPolicies);


                System.out.println("Agent ["+firstAgentIndex+"] has " + agFinalPolicyMap.get(firstAgentIndex).size()+" neighbor policies..");
            }
            System.out.println("Number of Agents: "+ agFinalPolicyMap.size());
        }
        System.out.println("Done!!");
    }//end main.
}//end class.
