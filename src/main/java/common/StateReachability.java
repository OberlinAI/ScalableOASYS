package common;

import java.util.*;

import burlap.mdp.auxiliary.common.NullTermination;
import burlap.mdp.core.StateTransitionProb;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.action.SimpleAction;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.model.FullJointModel;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;
import domains.wildfire.WildfireAnonymousState;
import domains.wildfire.WildfireDomain;
import domains.wildfire.WildfireMechanics;
import domains.wildfire.beans.Agent;
import posg.POOOSGDomain;
import scalability.FrameActionConfiguration;
import scalability.beans.ConfigurationBean;

public class StateReachability {
    /**
     * The debugID used for making calls to {@link burlap.debugtools.DPrint}.
     */
    public static int			debugID = 22222;


    /**
     * Returns the list of {@link burlap.mdp.core.state.State} objects that are reachable from a source state.
     * @param from the source state
     * @param inDomain the domain of the state
     * @param usingHashFactory the state hashing factory to use for indexing states and testing equality.
     * @return the list of {@link burlap.mdp.core.state.State} objects that are reachable from a source state.
     */
    public static List <State> getReachableStates(State from, POOOSGDomain inDomain,
                                                  Map<String, SGAgent> agentDefinitions, HashableStateFactory usingHashFactory){

        return getReachableStates(from, inDomain, agentDefinitions, usingHashFactory, new NullTermination());
    }


    /**
     * Returns the list of {@link burlap.mdp.core.state.State} objects that are reachable from a source state.
     * @param from the source state
     * @param inDomain the domain of the state
     * @param usingHashFactory the state hashing factory to use for indexing states and testing equality.
     * @param tf a terminal function that prevents expansion from terminal states.
     * @return the list of {@link burlap.mdp.core.state.State} objects that are reachable from a source state.
     */
    public static List <State> getReachableStates(State from, POOOSGDomain inDomain,
                                                  Map<String, SGAgent> agentDefinitions, HashableStateFactory usingHashFactory, TerminalFunction tf){

        Set <HashableState> hashedStates = getReachableHashedStates(from, inDomain, agentDefinitions, usingHashFactory, tf);
        List <State> states = new ArrayList<State>(hashedStates.size());
        for(HashableState sh : hashedStates){
            states.add(sh.s());
        }

        return states;
    }


    /**
     * Returns the set of {@link burlap.mdp.core.state.State} objects that are reachable from a source state.
     * @param from the source state
     * @param inDomain the domain of the state Map<String, SGAgentType>
     * @param agentDefinitions for the agents in the domain 
     * @param usingHashFactory the state hashing factory to use for indexing states and testing equality.
     * @return the set of {@link burlap.mdp.core.state.State} objects that are reachable from a source state.
     */
    public static Set<HashableState> getReachableHashedStates(State from, POOOSGDomain inDomain,
                                                              Map<String, SGAgent> agentDefinitions, HashableStateFactory usingHashFactory) {

        return getReachableHashedStates(from, inDomain, agentDefinitions, usingHashFactory, new NullTermination());
    }

    public static Set<HashableState> getReachableHashedStates(State from, POOOSGDomain inDomain,
                                                              Map<String, SGAgent> agentDefinitions,Map<String, SGAgent> agentAnmDefinitions, HashableStateFactory usingHashFactory, int horizon) {

        return getReachableHashedStates(from, inDomain, agentDefinitions,agentAnmDefinitions, usingHashFactory, new NullTermination(), horizon);
    }


    //Add Null termination function to call the actual method.
    public static Set<HashableState> getReachableAnmHashedStates(State from,FrameActionConfiguration fac, POOOSGDomain inDomain,
                                                              Map<String, SGAgent> agentAnmDefinitions, HashableStateFactory usingHashFactory, int horizon,boolean excludeSelfAgent) {

        return getReachableAnmHashedStates(from, fac, inDomain,agentAnmDefinitions, usingHashFactory, new NullTermination(), horizon,excludeSelfAgent);
    }

    /**
     * Returns the set of {@link burlap.mdp.core.state.State} objects that are reachable from a source state.
     * @param from the source state
     * @param inDomain the domain of the state
     * @param agentDefinitions the domain of the state Map<String, SGAgentType>
     * @param usingHashFactory the state hashing factory to use for indexing states and testing equality.
     * @param tf a terminal function that prevents expansion from terminal states.
     * @return the set of {@link burlap.mdp.core.state.State} objects that are reachable from a source state.
     */

    public static Set<HashableState> getReachableHashedStates(State from, POOOSGDomain inDomain,
                                                              Map<String, SGAgent> agentDefinitions, HashableStateFactory usingHashFactory, TerminalFunction tf) {

        Set<HashableState> hashedStates = new HashSet<HashableState>();
        HashableState shi = usingHashFactory.hashState(from);
        FullJointModel jaModel = (FullJointModel)inDomain.getJointActionModel();
        List<JointAction> jas = JointAction.getAllJointActions(from, (List<SGAgent>)agentDefinitions.values());
        int nGenerated = 0;

        LinkedList <HashableState> openList = new LinkedList<HashableState>();
        openList.offer(shi);
        hashedStates.add(shi);
        while(openList.size() > 0){
            HashableState sh = openList.poll();

            if(tf.isTerminal(sh.s())){
                continue; //don't expand
            }

            for(JointAction ja : jas){
                List<StateTransitionProb> tps = jaModel.stateTransitions(sh.s(), ja);
                for(StateTransitionProb tp : tps){
                    HashableState nsh = usingHashFactory.hashState(tp.s);
                    nGenerated++;
                    if(!hashedStates.contains(nsh)){
                        openList.offer(nsh);
                        hashedStates.add(nsh);
                    }
                }

            }

        }

//		DPrint.cl(debugID, "Num generated: " + nGenerated + "; num unique: " + hashedStates.size());

        return hashedStates;
    }

    public static Set<HashableState> getReachableHashedStates(State from, POOOSGDomain inDomain,
                                                              Map<String, SGAgent> agentDefinitions,Map<String, SGAgent> agentAnmDefinitions, HashableStateFactory usingHashFactory, TerminalFunction tf, int horizon) {

        Set<HashableState> hashedStates = new HashSet<HashableState>();
        HashableState shi = usingHashFactory.hashState(from);
        FullJointModel jaModel = (FullJointModel)inDomain.getJointActionModel();
        List<JointAction> jas = JointAction.getAllJointActions(from, (List<SGAgent>)agentDefinitions.values());
        int nGenerated = 0;

//		LinkedList <HashableState> openList = new LinkedList<HashableState>();
//		openList.offer(shi);

        datastructures.QueueLinkedList<HashableState> openList = new datastructures.QueueLinkedList<HashableState>();
        openList.add(shi);
        hashedStates.add(shi);

        int h=0;
        while(openList.size() > 0){
            HashableState sh = openList.poll();
            h++;
            if(tf.isTerminal(sh.s())){
                continue; //don't expand
            }

            for(JointAction ja : jas){
                List<StateTransitionProb> tps = jaModel.stateTransitions(sh.s(), ja);
                for(StateTransitionProb tp : tps){
                    HashableState nsh = usingHashFactory.hashState(tp.s);
                    nGenerated++;
                    if(!hashedStates.contains(nsh) && (h <= horizon)){
                        openList.add(nsh);
                        hashedStates.add(nsh);
                    }
                }

            }

        }

//		DPrint.cl(debugID, "[FINITE] "+horizon+"-steps; Num generated: " + nGenerated + "; num unique: " + hashedStates.size());

        return hashedStates;
    }


    /**
     * This method would give you the possible states from the current state.
     * @param from is the current agent's Anonymous state.
     * @param inDomain is the current domain.
     * @param agentAnmDefinitions is the anonymous-agent definitions.
     * @param usingHashFactory hashable state factory.
     * @param tf is the terminal function.
     * @param horizon is the horizon size.
     * @return set of Hashable State.
     */
    public static Set<HashableState> getReachableAnmHashedStates(State from, FrameActionConfiguration fac, POOOSGDomain inDomain,
                                                                 Map<String, SGAgent> agentAnmDefinitions,HashableStateFactory usingHashFactory, TerminalFunction tf, int horizon, boolean excludeSelfAgent) {

        //Get possible facs from the given state.
        initializeFAC(from,fac,inDomain,agentAnmDefinitions,excludeSelfAgent);

        System.out.println("Done with FAC caching.");

        //Initialize the Hashable state.
        Set<HashableState> hashedStates = new HashSet<HashableState>();

        WildfireMechanics facModel = (WildfireMechanics)inDomain.getJointActionModel();
        List<State> possibleStates = facModel.findPossibleStates(from);

        //Add the hashed states by finding all the possible states given the current state.
        for(State possibleState: possibleStates){
            HashableState hashableState = usingHashFactory.hashState(possibleState);
            if(!hashedStates.contains(hashableState)){
                hashedStates.add(hashableState);
            }
        }

        System.out.println("No. of Hashed States:"  + hashedStates.size());

        return hashedStates;
    }//end method.



    /**
     * This method gives all possible FACs available for the given State. It updates the FAC passed in the argument.
     * @param from is from state.
     * @param fac initialized FAC.
     * @param inDomain is the current domain.
     * @param agentAnmDefinitions is the anonymous agent definition.
     * @param excludeSelfAgent is to exclude the self-agent's action in FAC. Generally done when finding the possible states.
     */
    public static void  initializeFAC(State from, FrameActionConfiguration fac, POOOSGDomain inDomain,
                                                            Map<String, SGAgent> agentAnmDefinitions, boolean excludeSelfAgent){
        //Get the wildfire domain.
        WildfireDomain domain = (WildfireDomain)inDomain;
        WildfireAnonymousState anonymousState = (WildfireAnonymousState)from;

        //Get the number of types available.
        ArrayList<Integer> typeList = new ArrayList<Integer>();

        //Calculate the typeList first by adding the type of the self-agent and all the other neighbours.
        //Ignore the types if they are duplicated.
        typeList.add(anonymousState.getSelfAgent().getPowerType());
        if(anonymousState.getanonymousAgents().length > 0){
            for(String agentGroup : anonymousState.getanonymousAgents()){
                int groupPower = domain.sampleAgentPerGroup.get(agentGroup).getPowerType();
                if(!typeList.contains(groupPower)){
                    typeList.add(groupPower);
                }//end if.
            }//end for.
        }//end.


        //Sort the type list.
        Collections.sort(typeList);

        //Find The total number of agents for each type from the current state.
        ArrayList<Integer> typeAgents = new ArrayList<Integer>(typeList.size());
        //Go through the anonymous state again and find the number of agents per each type.
        //Special concern: Add the self-agent's group members also to the list.
        for(int typeIndex = 0; typeIndex < typeList.size(); typeIndex++){
            //Get type.
            int type = typeList.get(typeIndex);
            //Initialize the number of agents with zero.
            typeAgents.add(0);
            if(anonymousState.getSelfAgent().getPowerType() == type) {
                //Get the group string.

                ArrayList<String> startEndIndexes = new ArrayList<>(Arrays.asList(anonymousState.getSelfAgent().getAgentGroup().split("-")));
                //Separate indexes.
                int startIndex = Integer.parseInt(startEndIndexes.get(0));
                int endIndex = Integer.parseInt(startEndIndexes.get(1));

                //Add the agents in the group to the list, reducing 1 for the self-agent.
                if(excludeSelfAgent){
                    typeAgents.set(typeIndex,endIndex-startIndex);
                }else{
                    typeAgents.set(typeIndex,endIndex-startIndex + 1);
                }

            }

            for(String agentGroup : anonymousState.getanonymousAgents()){
                int groupPower = domain.sampleAgentPerGroup.get(agentGroup).getPowerType();
                if(type == groupPower){
                    ArrayList<String> startEndIndexes = new ArrayList<>(Arrays.asList(agentGroup.split("-")));
                    //Separate indexes.
                    int startIndex = Integer.parseInt(startEndIndexes.get(0));
                    int endIndex = Integer.parseInt(startEndIndexes.get(1));
                    //Add the agents in the group to the list.
                     typeAgents.set(typeIndex,typeAgents.get(typeIndex) + (endIndex-startIndex+1));
                }//end if.
            }//end for.
        }//end for.

        //Convert Arraylists to int array.
        int[] typeListArray = new int[typeList.size()];
        int[] typeAgentsArray = new int[typeList.size()];
        //Type-agent array
        int[][] typeAgentArray = new int[typeList.size()][2];
        for(int index=0; index< typeList.size(); index++ ){
            //Set the types.
            typeListArray[index]  = typeList.get(index);
            typeAgentArray[index][0] =typeList.get(index);
            //Set the number of agents.
            typeAgentsArray[index] = typeAgents.get(index);
            typeAgentArray[index][1] =typeAgents.get(index);
        }

        //Set the type and agent array.
        fac.setAgentTypes(typeAgentArray);



        //This is for to calculate the number of action types available.
        //It would help us to define the number of members in fac.
        ArrayList<String> actionTypeList = new ArrayList<String>();
        Iterator agentIt = agentAnmDefinitions.entrySet().iterator();
        while(agentIt.hasNext()) {
            Map.Entry entry = (Map.Entry) agentIt.next();
            //Get the group agent value.
            SGAgent groupAnmAgent = (SGAgent) entry.getValue();
            //Get the actions.
            List<ActionType> groupActionType = domain.actionsPerGroup.get(groupAnmAgent.agentName());

            for(ActionType actionType : groupActionType){
                if(!actionTypeList.contains(actionType.typeName())){
                    actionTypeList.add(actionType.typeName());
                }//end if.
            }//end for.
        }//edn while.

        //Total FAC entries.
        int totalFAC = 0;
        //Find the number of entries for FAC by calculating number of actions for each type.
        ArrayList<String> typeActionPairs = new ArrayList<String>();
        for (int type =0 ; type < typeListArray.length; type++){
            Iterator agentTypeIt = agentAnmDefinitions.entrySet().iterator();
            while (agentTypeIt.hasNext()){
                Map.Entry entry = (Map.Entry) agentTypeIt.next();
                //Get the group agent value.
                SGAgent groupAnmAgent = (SGAgent) entry.getValue();
                if(domain.sampleAgentPerGroup.get(groupAnmAgent.agentName()).getPowerType() == typeListArray[type]){
                    for(ActionType actionType: groupAnmAgent.agentType().actions){
                        if(!typeActionPairs.contains(type +"-" +actionType.typeName())){
                            totalFAC++;
                            typeActionPairs.add(type +"-" +actionType.typeName());
                        }
                    }
                }//end if.
            }//end while.
        }//end for.
        //Setting and initializing the array again.
        fac.setConfigurationArraySize(totalFAC);


        //Get the list of the type-action-max agents from the agent definition and populate the FAC.
        //Match each type and create a configuration for that.
        //Iterating over type would reduce the effort to sort the list.
        for(Integer type: typeList){
            Iterator agentDefIt = agentAnmDefinitions.entrySet().iterator();
            while(agentDefIt.hasNext()) {
                Map.Entry entry = (Map.Entry) agentDefIt.next();
                //Get the group agent value.
                SGAgent groupAnmAgent = (SGAgent) entry.getValue();
                //Get the sample agent. Pass group name as args.
                Agent sampleAgent = domain.sampleAgentPerGroup.get(groupAnmAgent.agentName());
                //If the group has the same type as the looping agent type. Add actions to the list.
                if(sampleAgent.getPowerType() == type){
                    //Get the total number of agents from the group first.
                    ArrayList<String> startEndIndexes = new ArrayList<> (Arrays.asList(groupAnmAgent.agentName().split("-")));
                    //Separate indexes.
                    int startIndex = Integer.parseInt(startEndIndexes.get(0));
                    int endIndex = Integer.parseInt(startEndIndexes.get(1));
                    int totalCount; //Total number of agents in the group.

                    //If the self-agent group has been added, put one less count, other wise all the agent count.
                    if(groupAnmAgent.agentName().equals(anonymousState.getSelfAgent().getAgentGroup()) && excludeSelfAgent){
                        totalCount =endIndex-startIndex;
                    }else{
                        totalCount =endIndex-startIndex + 1;
                    }


                    //For all the action types in the group, add agents to fac with related entry.
                    for(ActionType actionType: groupAnmAgent.agentType().actions){
                        int facIndex = fac.search(type,actionType.typeName());
                        if(facIndex != -1){
                            //Add the counts to the original entry.
                            ConfigurationBean config = fac.getMaxConfiguration()[facIndex];
                            config.setNumberOfAgents(config.getNumberOfAgents() + totalCount);
                            fac.setMaxConfiguration(facIndex,config);
                        }else{
                            //Create a new entry for the fac.
                            fac.putMaxConfiguration(type,new SimpleAction(actionType.typeName()),totalCount);
                        }
                    }//end for action.
                }//end if.
            }//end iterator.
        }//end for type.

        //Initialize the current fac.
        fac.initializeCurrentFAC();


        //Get all the possible list of FACs and store it in the static variable.
//        try{
//            fac.findPossibleFACs(typeListArray,typeAgentsArray);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
    }

}
