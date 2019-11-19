package domains.wildfire;

import burlap.behavior.stochasticgames.agents.RandomSGAgent;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.agent.SGAgentBase;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import common.StateEnumerator;
import burlap.mdp.auxiliary.DomainGenerator;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.action.SimpleAction;
import burlap.mdp.core.action.UniversalActionType;
import burlap.mdp.core.Domain;
import domains.wildfire.beans.Agent;
import domains.wildfire.beans.Fire;
import domains.wildfire.beans.Location;
import burlap.mdp.core.state.*;

import java.util.*;

import org.thejavaguy.prng.generators.PRNG;
import org.thejavaguy.prng.generators.XorshiftPlus;
import posg.TabularBeliefState;
import posg.model.PartialObservationFunction;

public class Wildfire implements DomainGenerator{

    //Agent Definitions
    protected static List<Map<String, SGAgent>> allAgentDefinitions = new ArrayList<Map<String, SGAgent>>();
    //Anonymous agent Definitions
    protected static Map<String,Map<String, SGAgent>> allAgentAnmDefinitions = new HashMap<String,Map<String, SGAgent>>();
    //Is the Agent-definitions are set.
    private static boolean isSetAgentDefs = false;
    //Is the Agent-Anm-definitions are set.
    private static boolean isSetAnmAgentDefs = false;


    @Override
    public Domain generateDomain() {
        //If nothing specified, go for the default configuration.
        return generateDomain(0,false);
    }

    /**
     * This method generates the domain with the given configuration and the estimated transition function.
     * It creates the agents, fires and creates the required neighbourhoods.
     * In the end it initiates the observation and full action models to create a complete domain.
     * @param configuration is the configuration number for the configuration.
     * @param estimateTF is if we need to read the estimated transition function from the file or not.
     * @return crated domain.
     */
    public Domain  generateDomain(int configuration,boolean estimateTF) {
        boolean isEverythingFine = true;//Flag for checking the process.
        //Create an instance of the OOSADomain.
        WildfireDomain wildFireDomain = new WildfireDomain();
        //Create a new instance of the Wildfire parameters.
        //As all the parameters are Static, there is no need for the instance of the object.
        new WildfireParameters(configuration);

        try{
            //Create the premise from the configuration string and confirm that it has been created.
            isEverythingFine = createPremise(WildfireParameters.configString,wildFireDomain);
            if(!isEverythingFine)
                throw new Exception("Something went wrong with creating premise.");

            //Create neighbourhoods and visibilities.
            isEverythingFine = createNeighbourhood(wildFireDomain);
            if(!isEverythingFine)
                throw new Exception("Something went wrong with creating neighbourhood.");

            //Create Actions and add them to the domain.
            isEverythingFine = createActions(wildFireDomain);
            if(!isEverythingFine)
                throw new Exception("Something went wrong with creating neighbourhood.");

            //Add the Wildfire state to the Domain.
            wildFireDomain.addStateClass(WildfireState.WILDFIRE_CLASS_NAME,WildfireState.class);
            //Add the observation function.
            PartialObservationFunction fireOF = new FireDiscreteObservation();
            wildFireDomain.setPartialObservationFunction(fireOF);
            //Set the Joint model for the wildfire.
            WildfireMechanics wildfireJointModel = new WildfireMechanics(wildFireDomain);

            //Check if to use the transition function from file or not.
            if(estimateTF){
                //Set the path to the file.
                String outputPath = "output/FinalTransitionFns/Config_"+WildfireParameters.config+"/";
                String tfFilePath = wildFireDomain.agentsList.size() + "-WF_CONFIG" + WildfireParameters.config;
                String fileName = outputPath + tfFilePath  + "_estimatedTransitionFn_500_100_30.txt";

                //Use the TF from the file.
                wildfireJointModel.usePresumedTF = false;
                wildfireJointModel.parsedAgentTransitionFunction = WildfireMechanics.readTransitionFnFromFile(wildFireDomain,fileName);
            }else{
                wildfireJointModel.usePresumedTF = true;
            }//end if-else.
            //Set the joint action model.
            wildFireDomain.setJointActionModel(wildfireJointModel);

            //Set Agent and Anonymous agent Definitions.
            setAgentDefs(wildFireDomain);
            setAnmAgentDefs(wildFireDomain);


        }catch (Exception e){
            e.printStackTrace();
            return null;
        }


        return wildFireDomain;
    }//end method.



    /**
     * This method creates the premise, agents and fires.
     * @param configurationString is the configuration String of a particular configuration.
     * @return true everything is created as per the configuration, false otherwise.
     */
    public boolean createPremise(String configurationString, WildfireDomain domain){
        boolean isCreated = false, isAgents = false, isFires = false,isPower = false;
        try{
            ArrayList<String> configList = new ArrayList<>(Arrays.asList(configurationString.split(",")));
            //For each line in configuration.
            for(String config: configList){
                if(config.startsWith("XY")){
                    //Get all the information from XY-x-y.
                    //Set the forest size.
                    ArrayList<String> premise =  new ArrayList<> (Arrays.asList(config.split("-")));
                    WildfireDomain.premiseX = Integer.parseInt(premise.get(1));
                    WildfireDomain.premiseY = Integer.parseInt(premise.get(2));
                }else if(config.startsWith("A")){
                    //Create Agents.
                    isAgents = createAgents(config,domain);
                    //Something went wrong with agents.
                    if(!isAgents)
                        throw new Exception("Something went wrong, while creating agents.");
                }else if(config.startsWith("F")){
                    //Create Fires.
                    isFires = createFires(config,domain);
                    //Something went wrong with agents.
                    if(!isFires)
                        throw new Exception("Something went wrong, while creating fires.");
                }else if(config.startsWith("P")){
                    //Create Fires.
                    isPower = createPower(config,domain);
                    //Something went wrong with agents.
                    if(!isPower)
                        throw new Exception("Something went wrong, while creating power types.");
                }else{
                    //Something went wrong.
                    //throw new Exception("Something went wrong with configuration string.");
                }//end if-else.
            }//end for.
            //Everything went fine.
            isCreated = true;
        }catch (Exception e){
            e.printStackTrace();
            isCreated = false;
        }//end try-catch.

        return isCreated;
    }//end method.


    /**
     * This method creates agents in the premise and also adds the agent group into the neighbourhood array.
     * @param agentConfiguration is the configuration string only for agents.
     * @return true everything is created as per the configuration, false otherwise.
     */
    public boolean createAgents(String agentConfiguration,WildfireDomain domain){
        boolean isAgentCreated = false;
        try{
            //Get all the information from A-num of agents-x-y-type.
            //Parse agent information.
            ArrayList<String> agentInformation =  new ArrayList<> (Arrays.asList(agentConfiguration.split("-")));

            int agentCounts = Integer.parseInt(agentInformation.get(1)); //Number of agents.
            int agentXLoc = Integer.parseInt(agentInformation.get(2));//Agent's X location.
            int agentYLoc = Integer.parseInt(agentInformation.get(3));//Agent's Y location.
            int agentPowerType = Integer.parseInt(agentInformation.get(4));//Agent's power Type.

            //Create the agent group string. e.g. 0-99
            String agentGroup = WildfireParameters.agentIndex + "-" + (WildfireParameters.agentIndex+agentCounts-1);
            //Add the agent group Strings to all the neighbourhood arrays.
            domain.agentFireNeighbourhood.put(agentGroup,new ArrayList<Integer>());
            domain.agentsNeighbourhood.put(agentGroup,new ArrayList<String>());
            domain.agentVisibleFires.put(agentGroup,new ArrayList<Integer>());
            domain.agentGroups.add(agentGroup);


            //Create Location object for all the agents.
            Location agentLocation = new Location(agentXLoc,agentYLoc);
            //Create Agent Location.
            createAgentLocation(domain,agentLocation);


            //Get the first agent's number to create the state enumerator for the group.
            ArrayList<String> startEndIndexes =new ArrayList<>(Arrays.asList(agentGroup.split("-")));
            int sanpleAgentNumber = Integer.parseInt(startEndIndexes.get(0));
            //Put Sample agent for the group.
            Agent sampleAgent = new Agent(sanpleAgentNumber,agentLocation,agentPowerType,-1,agentGroup);
            domain.sampleAgentPerGroup.put(agentGroup,sampleAgent);


            //Create Agents with given configuration.
            for(int agentCount = 0; agentCount< agentCounts ; agentCount++){
                //Create a new agent without availability information.
                Agent newAgent = new Agent(WildfireParameters.agentIndex,agentLocation,agentPowerType,-1,agentGroup);
                //Increase the index of the agent sequence.
                WildfireParameters.agentIndex ++;
                //Add agent to the default list.
                domain.agentsList.add(newAgent);
            }//end for.

            //Agent Created.
            isAgentCreated = true;
        }catch (Exception e){
            e.printStackTrace();
        }//end if-else.

        return isAgentCreated;
    }//end method.

    /**
     * Create a set for the locations, where agents reside.
     * @param domain is the wildfire domain.
     * @param agentLocation is the location where the agent-group resides.
     */
    public void createAgentLocation(WildfireDomain domain,Location agentLocation){
        //Iterate through all the locations, and if the location does not exist than add it.
        boolean locationExist = false;
        for(Location location: domain.agentLocations){
            if(location.equals(agentLocation)){
                locationExist = true;
                break;
            }//end if.
        }//end for.
        if(!locationExist){
            domain.agentLocations.add(agentLocation);
        }//end if.
    }//end method.



    /**
     * This method creates fires in the premise.
     * @param fireConfiguration is the configuration string only for fires.
     * @return true everything is created as per the configuration, false otherwise.
     */
    public boolean createFires(String fireConfiguration,WildfireDomain domain){
        boolean isFireCreated = false;
        try{
            //Get all the information from F-x-y.
            //Parse agent information.
            ArrayList<String> fireInformation =  new ArrayList<> (Arrays.asList(fireConfiguration.split("-")));

            int fireXLoc = Integer.parseInt(fireInformation.get(1));//Fire's X location.
            int fireYLoc = Integer.parseInt(fireInformation.get(2));//Fire's Y location.
            int fireType = Integer.parseInt(fireInformation.get(3));//Fire's Y location.

            //Create Location object for all the agents.
            Location fireLocation = new Location(fireXLoc,fireYLoc);
            //Create Fire Object.
            Fire newFire = new Fire(WildfireParameters.fireIndex,fireLocation,-1,fireType);
            //Increase the fire counter.
            WildfireParameters.fireIndex++;
            //Add the fire to the list.
            domain.fireList.add(newFire);
            //Fire Created.
            isFireCreated = true;
        }catch (Exception e){
            e.printStackTrace();
        }//end if-else.

        return isFireCreated;
    }//end method.


    /**
     * This method creates power types and values.
     * @param powerConfiguration is the configuration string only for power.
     * @return true everything is created as per the configuration, false otherwise.
     */
    public boolean createPower(String powerConfiguration,WildfireDomain domain){
        boolean isPowerCreated = false;
        try{
            //Get all the information from F-x-y.
            //Parse agent information.
            ArrayList<String> powerInfo =  new ArrayList<> (Arrays.asList(powerConfiguration.split("-")));

            int powerType = Integer.parseInt(powerInfo.get(1));//Power Type
            double power= Double.parseDouble(powerInfo.get(2));//Power value.

            //Add to the power type.
            domain.agentPowerTypes.add(powerType);
            //Add the power
            domain.agentPower.add(power);

            //Power Created.
            isPowerCreated = true;
        }catch (Exception e){
            e.printStackTrace();
        }//end if-else.

        return isPowerCreated;
    }//end method.

    /**
     * This method will create 3 types of pairing between agents and fires.
     * agent-agent, agent-fire, agents - fire visibility. More explanation in methods used to
     * create such neighbourhood.
     * @return true if the neighbourhood created, false otherwise.
     */
    public boolean createNeighbourhood(WildfireDomain domain) {
        boolean isCreated = true;
        try{

            //create agent-fire neighbour.
            isCreated = agentFireNeighbourhood(domain);
            if(!isCreated)
                throw new Exception("Something wrong with agent-fire neighbourhood process.");

            //crate agent-agent neighbour.
            isCreated = agentAgentNeighbourhood(domain);
            if(!isCreated)
                throw new Exception("Something wrong with agent-agent neighbourhood process.");

            //create visible fires.
            isCreated = agentVisibleFireNeighbourhood(domain);
            if(!isCreated)
                throw new Exception("Something wrong with agent-fire visibility process.");
        }catch (Exception e){
            e.printStackTrace();
            isCreated = false;//Set the flag to false.
        }//end try-catch.

        return isCreated;
    }//end method.


    /**
     * agent - agent neighbourhood populates the {@link domains.wildfire.WildfireParameters} class's agentFireNeighbourhood
     * static variable (ArrayList).
     * This neighbourhood is defined in terms of agents and fires, which is fires an agent can fight.
     * Here, the key of the HashMap is the agent group from start index to end, while the value is the list of the
     * fires in the neighbourhood.
     * @return true if the process completes successfully, false otherwise.
     */
    public boolean agentFireNeighbourhood(WildfireDomain domain){
        boolean isNeighbourhood = true; //check if the process of creation succeeded or not.
        try{
            //Loop through each agent groups in the neighbourhood array.
            Iterator agentFireIt = domain.agentFireNeighbourhood.entrySet().iterator();
            while(agentFireIt.hasNext()){
                //Get the current entry.
                Map.Entry<String,ArrayList<Integer>> agentFireMap = (Map.Entry<String,ArrayList<Integer>>)agentFireIt.next();
                //Get the first agent of the group to fetch the location.
                Agent firstAgent = domain.agentsList
                        .get(Integer.parseInt((new ArrayList<>(Arrays.asList(agentFireMap.getKey().split("-")))).get(0)));

                ArrayList<Integer> fireListForAgents = new ArrayList<Integer>();
                //Loop through all the fires and if it is in neighbourhood then add it into the list.
                for(Fire fire: domain.fireList){
                    //If fire is in 1 horizontal or vertical or diagonal distance then add it into the list.
                    if( Math.abs(firstAgent.getAgentLocation().getX() - fire.getFireLocation().getX()) <= 1  &&
                        Math.abs(firstAgent.getAgentLocation().getY() - fire.getFireLocation().getY()) <= 1 ){
                        fireListForAgents.add(fire.getFireNumber());//Add fire number in the list.
                    }//end if.
                }//end for.

                //Set the new list as the Fire list.
                agentFireMap.setValue(fireListForAgents);
            }//end while.
        }catch (Exception e){
            e.printStackTrace();
            isNeighbourhood = false;
        }
        return isNeighbourhood;
    }//end method.

    /**
     * agent - agent neighbourhood populates the {@link domains.wildfire.WildfireParameters} class's agentsNeighbourhood
     * static variable.
     * The neighbourhood is defined as the agents who share same fires in their agent-fire neighbourhood.
     * Here, the key of the HashMap is the agent group from start index to end, while the value is the list of the other
     * agent groups in the neighbourhood.
     * @return true if the process completes successfully, false otherwise.
     */
    public boolean agentAgentNeighbourhood(WildfireDomain domain){
        boolean isNeighbourhood = true; //check if the process of creation succeeded or not.
        try{
            //Loop through each agent groups in the neighbourhood array.
            Iterator agentsIt = domain.agentsNeighbourhood.entrySet().iterator();
            Iterator agentFireIt = domain.agentFireNeighbourhood.entrySet().iterator();
            while(agentsIt.hasNext()){
                //Get the current entry from both the maps.
                Map.Entry<String,ArrayList<String>> agentsMap = (Map.Entry<String,ArrayList<String>>)agentsIt.next();
                Map.Entry<String,ArrayList<Integer>> agentFireMap = (Map.Entry<String,ArrayList<Integer>>)agentFireIt.next();


                //ArrayList object to insert into the Hash map.
                ArrayList<String> agentListForAgents = new ArrayList<String>();

                //Loop through the agentsFireList to get the pairs of the agents.
                Iterator agentFireInnerIt = domain.agentFireNeighbourhood.entrySet().iterator();
                //Iterate through the the map again to find the neighbours.
                while(agentFireInnerIt.hasNext()){
                    //Get the current entry.
                    Map.Entry<String,ArrayList<Integer>> agentFireInnerMap = (Map.Entry<String,ArrayList<Integer>>)agentFireInnerIt.next();
                    //If the agent groups are the same then skip.
                    if(agentFireMap.getKey().equals(agentFireInnerMap.getKey())){
                        continue;
                    }else{
                        //Get the List of the fire of the outer agent group.
                        ArrayList<Integer> outerFireNumbers = new ArrayList<>((ArrayList<Integer>)agentFireMap.getValue());
                        //Get the List of the fire of the inner agent group.
                        ArrayList<Integer> innerFireNumbers = new ArrayList<>((ArrayList<Integer>)agentFireInnerMap.getValue());

                        //First get all the connected or visible fires in a list for the current agent.
                        for(Integer outerFireNumber:outerFireNumbers){
                            if(innerFireNumbers.contains(outerFireNumber)){
                                //Add the fires of the neighbour's into the list of the current agent.
                                for(Integer innerFireNumber: innerFireNumbers){
                                    if(!outerFireNumbers.contains(innerFireNumber)){
                                        outerFireNumbers.add(innerFireNumber);
                                    }//end if.
                                }//end for.
                                break;
                            }//end if.
                        }//end for.


                        //Compare fire indexes of the both. If any overlap is there, add the agent-group into the
                        //agent-agent neighbourhood list.
                        for(Integer outerFireNumber:outerFireNumbers){
                            if(innerFireNumbers.contains(outerFireNumber)){
                                //Add the inner map to the list.
                                agentListForAgents.add(agentFireInnerMap.getKey());
                                break;
                            }//end if.
                        }//end for.

                    }//end if-else.
                }//end inner while.
                //Add the Agents list into the map.
                agentsMap.setValue(agentListForAgents);
            }//end while.
        }catch (Exception e){
            e.printStackTrace();
            isNeighbourhood = false;
        }

        return isNeighbourhood;
    }//end method.



    /**
     * agent - agent neighbourhood populates the {@link domains.wildfire.WildfireParameters} class's agentVisibleFires
     * static variable.
     * The neighbourhood is defined as the fires shared by an agent and its agent neighbours. For example if agent 1 can see fire 1 and 2,
     * and agent 2 who is agent-neighbour of agent-1 can see fire 2 and 3, the agent-fire visibility for agent 1 is 1,2 and 3.
     * Here, the key of the HashMap is the agent group from start index to end, while the value is the list of fires
     * that the agent and its neighbours share.
     * @return true if the process completes successfully, false otherwise.
     */
    public boolean agentVisibleFireNeighbourhood(WildfireDomain domain){
        boolean isNeighbourhood = true; //check if the process of creation succeeded or not.
        try{
            //Loop through each agent groups in the neighbourhood array.
            Iterator agentsIt = domain.agentsNeighbourhood.entrySet().iterator();
            Iterator agentFireIt = domain.agentFireNeighbourhood.entrySet().iterator();
            Iterator agentFireVisibilityIt = domain.agentVisibleFires.entrySet().iterator();
            //Loop through the agent-groups in the visibility.
            while(agentFireVisibilityIt.hasNext()){
                //Get the current entry from all the maps.
                Map.Entry<String,ArrayList<String>> agentsMap = (Map.Entry<String,ArrayList<String>>)agentsIt.next();
                Map.Entry<String,ArrayList<Integer>> agentFireMap = (Map.Entry<String,ArrayList<Integer>>)agentFireIt.next();
                Map.Entry<String,ArrayList<Integer>> agentFireVisibilityMap = (Map.Entry<String,ArrayList<Integer>>)agentFireVisibilityIt.next();


                //Create an array list object of the shared fires by initially adding the fires which are in
                //neighbourhood of the current agent.
                ArrayList<Integer> agentVisibilityList = new ArrayList<Integer>(agentFireMap.getValue());

                //Iterate through all the neighbouring list.
                for(String agentNeighbour: agentsMap.getValue()){
                    //Get the fire list for the neighbour agent group.
                    ArrayList<Integer> neighbourFires =  domain.agentFireNeighbourhood.get(agentNeighbour);
                    //Iterate through the loop and if the fire is not in the current agent's visibility,
                    //add the fire index.
                    for(Integer neighbourFire : neighbourFires){
                        if(!agentVisibilityList.contains(neighbourFire)){
                            agentVisibilityList.add(neighbourFire);//Add fire to the list.
                        }//end if.
                    }//end for.
                }//end for.

                //Add the combined fire list into the map.
                agentFireVisibilityMap.setValue(agentVisibilityList);
            }//end while.
        }catch (Exception e){
            e.printStackTrace();
            isNeighbourhood = false;
        }//end try-catch.
        return isNeighbourhood;
    }//end method.


    /**
     * This method creates action for the domain.
     * @return is, if the actions are being created or not.
     */
    private boolean createActions(WildfireDomain domain) {
        boolean isCreated = true;
        String actionStr; //Action temp variable.
        try{
            //Get the agent-fire neighbourhood iterator.
            Iterator agentFireIt = domain.agentFireNeighbourhood.entrySet().iterator();
            //Iterate through all the agent-fire neighbourhoods.
            while (agentFireIt.hasNext()){
                //Get the fire indexes
                Map.Entry<String,ArrayList<Integer>> agentFireMap = (Map.Entry<String,ArrayList<Integer>>)agentFireIt.next();
                //Get the List of the Fires.
                ArrayList<Integer> fireIndexes = (ArrayList<Integer>)agentFireMap.getValue();
                //List of the actions.
                List<ActionType> agentGroupActions = new ArrayList<ActionType>();
                //ADD NO-OP
                agentGroupActions.add(new UniversalActionType(WildfireParameters.NOOP_ACTION,new SimpleAction(WildfireParameters.NOOP_ACTION)));

                //Loop through all fires and add them to the list of actions to the domain.
                for(Integer fireIndex : fireIndexes){
                    Fire fire = domain.fireList.get(fireIndex);
                    //Create action from the fire location. e.g. X1Y2.
                    actionStr = fire.fireActionName();
                    //Add to the all actions to actions list.
                    domain.addActionType(new UniversalActionType(actionStr,new SimpleAction(actionStr)));
                    //Add action in the list for the group.
                    agentGroupActions.add(new UniversalActionType(actionStr,new SimpleAction(actionStr)));
                }//end for
                //Set Group to actions mapping.
                domain.actionsPerGroup.put(agentFireMap.getKey(),agentGroupActions);
            }//end while.
        }catch(Exception e){
            e.printStackTrace();
            isCreated = false;
        }//end try-catch.
        return isCreated;
    }//end method.


    /**
     * Find the agent group.
     * @param agentNumber is the current agent number.
     * @return is the respective agent group.
     */
    public static String getAgentGroup(WildfireDomain domain,int agentNumber){
        String agentGroup = "";
        for(String group: domain.agentGroups){
            ArrayList<String> groupLimits =  new ArrayList<> (Arrays.asList(group.split("-")));
            //If the agent number lies in this limit or not.
            if(agentNumber >= Integer.parseInt(groupLimits.get(0)) && agentNumber <= Integer.parseInt(groupLimits.get(1))){
                agentGroup = group;
                break;
            }//end if.
        }//end for.
        return agentGroup;
    }//end method.

    /**
     * Returns an empty state with the correct state variables for a particular agent.
     *
     * @param domain The {@link WildfireDomain}
     * @param agent The agent we are creating the state for
     *
     * @return An empty state for {@code agent}
     */
    public static WildfireState getCleanState(WildfireDomain domain, int agent){
        WildfireState wf = new WildfireState();
//		System.out.println(d.visibleFireIndices[agent].length + " "+d.neighbors[agent].length);
        //Get the size of the visible fire to the agent-group.
        wf.setFireList(new ArrayList<Fire>(domain.agentVisibleFires.get(getAgentGroup(domain,agent)).size()));
        //Create a new self agent.
        wf.setSelfAgent(domain.agentsList.get(agent));
        //Create other agents from the neighbourhood.
        wf.setAgentList(new ArrayList<Agent>(domain.agentsNeighbourhood.get(getAgentGroup(domain,agent)).size()));

        return wf;
    }//end method.

    /**
     * Returns an empty anonymous state with the correct state variables for a particular agent.
     *
     * @param domain The {@link WildfireDomain}
     * @param agent The agent we are creating the state for
     *
     * @return An empty state for {@code agent}
     */
    public static WildfireAnonymousState getCleanAnmState(WildfireDomain domain, int agent, int fires){
        WildfireAnonymousState wf = new WildfireAnonymousState();
//		System.out.println(d.visibleFireIndices[agent].length + " "+d.neighbors[agent].length);
        //Get the size of the visible fire to the agent-group.
        wf.setFireList(new Fire[fires]);
        //Create a new self agent.
        wf.setSelfAgent(domain.agentsList.get(agent));

        return wf;
    }//end method.

    /**
     * Returns an empty master state with the correct state variables.
     *
     * @param domain The {@link WildfireDomain}
     *
     * @return An empty master state
     */
    public static WildfireState getCleanMasterState(WildfireDomain domain){
        WildfireState wf = new WildfireState();

        //Set the number of fires in the setup
        //Get the size of the visible fire to the agent-group.
        wf.setFireList(new ArrayList<Fire>(domain.fireList.size()));
        //Create a new self agent.
        //By default it sets the required parameters of the master state.
        wf.setSelfAgent(new Agent());
        //Create other agents from the neighbourhood.
        wf.setAgentList(new ArrayList<Agent>(domain.agentsList.size()));

        return  wf;
    }//end method.


    /**
     * Returns an empty anonymous master state with the correct state variables.
     *
     * @param domain The {@link WildfireDomain}
     *
     * @return An empty master state
     */
    public static WildfireAnonymousState getCleanAnmMasterState(WildfireDomain domain,int fires){
        WildfireAnonymousState wf = new WildfireAnonymousState();

        //Set the list to accept just one.
        wf.setFireList(new Fire[fires]);
        //Create a new self agent.
        //By default it sets the required parameters of the master state.
        wf.setSelfAgent(new Agent());

        return  wf;
    }//end method.

    /**
     * Sets a fire location objects attribute values
     * @param wf is the Wildfire State.
     * @param fire is the fire to be set.
     */
    public static void setFire(WildfireState wf, Fire fire){
        //Set the fire on the particular index.
        wf.getFireList().add(fire);
    }//end method.


    /**
     * Set the fire in the anonymous state.
     * @param wf is the anonymous State.
     * @param fire is the fire to be set.
     * @param fireIndex is index of the fire to be set.
     */
    public static void setAnmFire(WildfireAnonymousState wf, Fire fire,int fireIndex){
        //Set the fire on the particular index.
        wf.getFireList()[fireIndex] = fire;
    }//end method.


    /**
     * Sets the self agent.
     * @param wf is the Wildfire State.
     * @param agent is the agent values to be set.
     */
    public static void setSelfAgent(WildfireState wf,Agent agent){
        //Set the fire on the particular index.
        wf.setSelfAgent(agent);
    }//end method.

    /**
     * Sets the self agent.
     * @param wf is the Wildfire State.
     * @param agent is the agent values to be set.
     */
    public static void setOtherAgent(WildfireState wf,Agent agent){
        //Set the fire on the particular index.
        wf.getAgentList().add(agent);
    }//end method.

    /**
     * Migrated from previous version with some changes.
     *
     * Creates an individual agent's view on the current state of the environment based on the
     * complete current master state.
     *
     * @param d The {@link WildfireDomain}
     * @param agent The agent to create a current state for
     * @param masterState The current master environment state
     *
     * @return {@code agent}'s view of {@code masterState}
     */
    public static State createAgentStateFromMasterState(WildfireDomain d, int agent, State masterState) {
        State cleanState = getCleanState(d, agent);

        // save the fires that this agent or its neighbors can see
        for (int i = 0; i < d.agentVisibleFires.get(d.agentsList.get(agent).getAgentGroup()).size(); i++) {
            int fireIndex = d.agentVisibleFires.get(d.agentsList.get(agent).getAgentGroup()).get(i);
            Fire fire = ((WildfireState)masterState).getFireList().get(fireIndex);
            setFire((WildfireState)cleanState,fire);
        }//end for.

        Agent selfAgent = ((WildfireState)masterState).getAgentList().get(agent);
        // save the agent's internal state
        setSelfAgent((WildfireState)cleanState,selfAgent);

        // save the other agents' states
        int otherAgent;
        for (int i = 0; i < d.agentsNeighbourhood.get(selfAgent.getAgentGroup()).size(); i++) {
            //Get the group.
            String groupString = d.agentsNeighbourhood.get(selfAgent.getAgentGroup()).get(i);
            //Get the group string.
            ArrayList<String> startEndIndexes =new ArrayList<> (Arrays.asList(groupString.split("-")));
            //Separate indexes.
            int startIndex = Integer.parseInt(startEndIndexes.get(0));
            int endIndex = Integer.parseInt(startEndIndexes.get(1));
            //Add agents by the indexes in the group.
            for(int index = startIndex ; index <=endIndex ; index++){
                setOtherAgent((WildfireState)cleanState, ((WildfireState)masterState).getAgentList().get(index));
            }//end for.
        }//end for.

        return (WildfireState)cleanState;
    }//end method.


    //Migrated from the previous version.
    public static State createOtherAgentStateFromLocalState(WildfireDomain d, int otherAgent, State agentState) {
        //Get wildfire state.
        WildfireState agentWFState =  ((WildfireState)agentState);

        int agentNumber = agentWFState.getSelfAgent().getAgentNumber();

        if(agentNumber == otherAgent){
            return agentState;
        }

        // NOTE: we use our own id here because we want to use our number of fires and neighbors, in case we have more or less than otherAgent
        // This is because we are making a mock state for otherAgent in our local world view
        State state = getCleanState(d, agentNumber);
        //Convert to wildfire state.
        WildfireState cleanWFState = (WildfireState) state;

        // save the original agent's internal state to other agent.
        setOtherAgent(cleanWFState, agentWFState.getSelfAgent());

//		System.out.println(fires.size());
        //Create all the fires required.
        for(Fire fire: agentWFState.getFireList()){
            setFire(cleanWFState,fire);
        }


        // find the otherAgent's internal state and set it to the self agent state if the agent number matches.
        //Otherwise set as other agents.
        //Get the agent-fire neighbourhood iterator.
        for (Agent agent : agentWFState.getAgentList()) {
            if (agent.getAgentNumber() == otherAgent) {
                setSelfAgent(cleanWFState, agent);
            }else{
                setOtherAgent(cleanWFState,agent);
            }//end if-else.
        }

        return state;
    }//end method.


    /**
     * This method converts the current anonymous state into other agent's anonymous state. The first agent of a group is
     * selected to be the self-agent and the current self-agent group is added in the neighbourhood.
     * @param domain is the domain.
     * @param otherAgentGroup is the other agent group.
     * @param agentState is the current state.
     * @return other agent's state.
     */
    public static State createOtherAgentAnmState(WildfireDomain domain, String otherAgentGroup, State agentState) {
        //Get wildfire state.
        WildfireAnonymousState anonymousState =  ((WildfireAnonymousState)agentState);

        int agentNumber = anonymousState.getSelfAgent().getAgentNumber();

        //If the agent's group and the required group matches each other.
        if(domain.agentsList.get(agentNumber).getAgentGroup().equals(otherAgentGroup)){
            State otherAgentState = new WildfireAnonymousState(anonymousState);
            return otherAgentState;
        }

        // NOTE: we use our current agent number here because we want to use our number of fires and neighbors, in case we have more or less than otherAgent
        // This is because we are making a mock state for otherAgent in our local world view
        State state = getCleanAnmState(domain,agentNumber,anonymousState.getFireList().length);
        //Convert to anonymous state.
        WildfireAnonymousState cleanAnonymousState= (WildfireAnonymousState) state;

        //Set the self-agent state.
        //Get the group string.
        ArrayList<String> startEndIndexes =new ArrayList<> (Arrays.asList(otherAgentGroup.split("-")));
        int startIndex = Integer.parseInt(startEndIndexes.get(0));
        cleanAnonymousState.setSelfAgent(new Agent(domain.agentsList.get(startIndex)));
        cleanAnonymousState.getSelfAgent().setAvailability(anonymousState.getSelfAgent().getAvailability());

        //Pass the fire information.
        //NOTE : Fire sequences are very important. Make sure to get the sequence from the
        //agentVisibleFires.
        for(int fireIndex = 0 ; fireIndex < domain.agentVisibleFires.get(otherAgentGroup).size(); fireIndex++){
            int otherFireIndex = domain.agentVisibleFires.get(otherAgentGroup).get(fireIndex);
            //Get all the fires in the current agent's anonymous state.
            //Copy the fire when the index matches.
            for(Fire currentFire: anonymousState.getFireList()){
                if(currentFire.getFireNumber() == otherFireIndex){
                    Fire otherFire = new Fire(currentFire);
                    cleanAnonymousState.getFireList()[fireIndex] = otherFire;
                    break;
                }//end if.
            }//end for inner fire.
        }//end for.

        //Get the other agent' list from the domain object.
        String[] neighbourGroups = new String[domain.agentsNeighbourhood.get(otherAgentGroup).size()];
        int count = 0;
        for(String group: domain.agentsNeighbourhood.get(otherAgentGroup)){
            neighbourGroups[count++] = group;
        }
        cleanAnonymousState.setanonymousAgents(neighbourGroups);

        return state;
    }//end method.

    //Modified as per requirements.
    public static List<State> enumerateStates(WildfireDomain d, State localWorld) {
        //Convert to the wildfire state.
        WildfireState localState = (WildfireState)localWorld;

        // first count the number of needed states
        int numStates = 1;
        int numFires = localState.getFireList().size();
        int numAgents = localState.getAgentList().size() + 1;
        for (int j = 0; j < numFires; j++) {
            numStates *= WildfireParameters.MAX_FIRE_STATES;
        }
        for (int j = 0; j < numAgents; j++) {
            numStates *= WildfireParameters.MAX_SUPPRESSANT_STATES;
        }

        int stateIndex, intensity, available;
        List<State> list = new ArrayList<State>();
        State state;
        for (int j = 0; j < numStates; j++) {
            stateIndex = j;
            state = Wildfire.getCleanState(d, localState.getSelfAgent().getAgentNumber());


            for (Fire localFire: localState.getFireList()){
                intensity = stateIndex % WildfireParameters.MAX_FIRE_STATES;
                stateIndex /= WildfireParameters.MAX_FIRE_STATES;

                localFire.setIntensity(intensity);
                Wildfire.setFire((WildfireState)state,localFire);
            }

            available = stateIndex %WildfireParameters.MAX_SUPPRESSANT_STATES;
            stateIndex /= WildfireParameters.MAX_SUPPRESSANT_STATES;
            Agent newAgent = localState.getSelfAgent();
            newAgent.setAvailability(available);

            Wildfire.setSelfAgent((WildfireState)state, newAgent);



            for (Agent agent: localState.getAgentList()){
                available = stateIndex % WildfireParameters.MAX_SUPPRESSANT_STATES;
                stateIndex /= WildfireParameters.MAX_SUPPRESSANT_STATES;

                agent.setAvailability(available);
                Wildfire.setOtherAgent((WildfireState)state,agent);
            }

            list.add(state);
        }

        return list;
    }//end method.

    /**
     * Changes : according to the change in the structure.
     * Creates a fully observable initial state for a particular agent.
     *
     * @param d The {@link WildfireDomain}
     * @param agent The agent to create the initial state for
     *
     * @return A fully observable initial state for {@code agent}
     */
    public static State getInitialState(WildfireDomain d, int agent) {
        StateEnumerator senum = d.getStateEnumerator();

        State s = getCleanState(d, agent);
        //Convert to Wildfire State
        WildfireState ws = (WildfireState)s;

        // set the initial fire states
        // NOTE: we start with no fires
        int fireIndex;
        for (int j = 0; j < d.agentVisibleFires.get(d.agentsList.get(agent).getAgentGroup()).size(); j++) {
            fireIndex = d.agentVisibleFires.get(d.agentsList.get(agent).getAgentGroup()).get(j);
            Fire fire = new Fire(d.fireList.get(fireIndex));
            fire.setIntensity(WildfireParameters.MAX_FIRE_STATES - 2);
            setFire(ws, fire);
        }

        // set the self state
        // NOTE: we assume the agent is present to start
        Agent newAgent = new Agent(d.agentsList.get(agent));
        newAgent.setAvailability(WildfireParameters.MAX_SUPPRESSANT_STATES-1);
        setSelfAgent(ws,newAgent);

        // set the other agents' states
        // NOTE: we assume each neighbor is present to start
        for (int i = 0; i < d.agentsNeighbourhood.get(newAgent.getAgentGroup()).size(); i++) {
            //Get the group.
            String groupString = d.agentsNeighbourhood.get(newAgent.getAgentGroup()).get(i);
            //Get the group string.
            ArrayList<String> startEndIndexes =new ArrayList<>(Arrays.asList(groupString.split("-")));
            //Separate indexes.
            int startIndex = Integer.parseInt(startEndIndexes.get(0));
            int endIndex = Integer.parseInt(startEndIndexes.get(1));
            //Add agents.
            for(int index = startIndex ; index <=endIndex ; index++){
                Agent otherAgent = new Agent(d.agentsList.get(i));
                otherAgent.setAvailability(WildfireParameters.MAX_SUPPRESSANT_STATES-1);
                setOtherAgent(ws, otherAgent);
            }//end for.
        }//end for.

        senum.getEnumeratedID(s);


        return s;
    }//end method.

    public static State toggleAvailabilityForAgent(State curState, int agent){
        //Create a wild fire state
        WildfireState curWSState = (WildfireState)curState;



        System.out.println("OLD STATE:\n"+ curWSState);
        int agentAvailability = curWSState.getAgentList().get(agent).getAvailability();
        System.out.println("agentAvailability: "+agentAvailability);
        int toggle  = (agentAvailability == 3) ? 2 :(agentAvailability == 2 ? 1 : 2);
        System.out.println("toggle: "+toggle);

        //Copy the state.
        WildfireState newState = (WildfireState) curWSState.copy();
        newState.getAgentList().get(agent).setAvailability(toggle);
        System.out.println("OLD STATE:\n"+curWSState);
        System.out.println("NEW STATE:\n"+newState);

        return newState;

    }//end method.

    /**
     * This method gives the initial belief state given the initial master state. The initial belief state
     * just contains one master state with a full probability.
     * @param wildfireDomain is the domain object.
     * @param currentAgent is the agent-number of the current agent.
     * @param senum is the State enumerator, which oontains all the possible states.
     * @param masterAnmState is the master anonymous state for the current agent.
     * @return is the belief state.
     */
    public static TabularBeliefState getInitialBeliefState(WildfireDomain wildfireDomain, int currentAgent, StateEnumerator senum, State masterAnmState) {

        List<Integer> sIDList = new ArrayList<Integer>();

        //Add the state to the list.
        State beliefState = new WildfireAnonymousState((WildfireAnonymousState)masterAnmState);
        sIDList.add(senum.getEnumeratedID(beliefState));

        //Create an object of a tabular belief to represent a full probability.
        TabularBeliefState initBeliefState = new TabularBeliefState(wildfireDomain,senum,currentAgent+"");
        initBeliefState.initializeBeliefsUniformly(sIDList);

        return initBeliefState;
    }//end method.


    //Changed according to manage structure.
    public static State getInitialMasterState(WildfireDomain d) {
        State s = getCleanMasterState(d);
        //Convert to Wildfire State
        WildfireState ws = (WildfireState)s;

        // set the initial fire states
        // NOTE: we assume there are no fires to start
        for (int fireIndex = 0; fireIndex < d.fireList.size(); fireIndex++) {
            Fire fire = new Fire(d.fireList.get(fireIndex));
            fire.setIntensity(WildfireParameters.MAX_FIRE_STATES - 2);
            setFire(ws,fire);
        }

        // NOTE: the self state is set in getCleanMasterState

        // set the  agents' states
        // NOTE: we assume each agent is present to start
        for (int i = 0; i < d.agentsList.size(); i++) {
            Agent newAgent = new Agent(d.agentsList.get(i));
            newAgent.setAvailability(WildfireParameters.MAX_SUPPRESSANT_STATES - 1);
            setOtherAgent(ws, newAgent);
        }//end for.

        return s;
    }


    /**
     * This method is overloaded for the NestedVI to run only for a particular type of suppresent level.
     * @param d is the domain.
     * @param agentAvailability is the agent availability to set.
     * @param isFireRandom if the fires starts with random values except burned out or extinguished.
     * @param isSuppRandom if the suppressant values follows a random distribution except having empty or zero state.
     * @param isSuppOpen if the suppressant values are following openness settings.
     * @return master state.
     */
    public static State getInitialMasterState(WildfireDomain d, int agentAvailability,boolean isFireRandom, boolean isSuppRandom, boolean isSuppOpen) {
        State s = getCleanMasterState(d);
        //Convert to Wildfire State
        WildfireState ws = (WildfireState)s;

        // set the initial fire states
        // NOTE: we assume there are no fires to start
        for (int fireIndex = 0; fireIndex < d.fireList.size(); fireIndex++) {
            Fire fire = new Fire(d.fireList.get(fireIndex));
            //If the isRandom is true than generate a random fire intensity and apply.
            //Max-1 otherwise.
            if(isFireRandom){
                int fireIntensity = (int) Math.round(Math.random()*(WildfireParameters.MAX_FIRE_STATES-3)) + 1;
                fire.setIntensity(fireIntensity);
            }else{
                fire.setIntensity(WildfireParameters.MAX_FIRE_STATES - 3);
            }//end if -else.

            setFire(ws,fire);
        }

        // NOTE: the self state is set in getCleanMasterState
        PRNG.Smart generator =  new XorshiftPlus.Smart(new XorshiftPlus());

        // set the  agents' states
        // NOTE: we assume each agent is present to start
        for (int i = 0; i < d.agentsList.size(); i++) {
            Agent newAgent = new Agent(d.agentsList.get(i));
            //If the suppressant values need to be random. Get it after sampling from the distribution.
            //Otherwise put them at given values.
            if(isSuppRandom && agentAvailability != 1){

                if(isSuppOpen){

                    //Hardcoded for the Setup-14.
                    if(i < 10){
                        newAgent.setAvailability(2);
                    }else if(i < 20){
                        newAgent.setAvailability(1);
                    }else{
                        newAgent.setAvailability(0);
                    }//end if-else.
                }else{
                    //Distribution for the suppressant levels. 0.667 for the 2 and 0.333 for the 1. Nothing at level 0.
                    //Set the availability from this distribution.
                    //Note: Make it dynamic in future.
                    double[] suppressantDistribution = {0.5,0.5};
                    double distributionSum = 0.0;

                    double randomNumber = generator.nextDouble();
                    for(int availability = 1 ; availability <= suppressantDistribution.length; availability++ ){
                        distributionSum += suppressantDistribution[availability-1];
                        if(randomNumber <= distributionSum){
                            newAgent.setAvailability(availability);
                            break;
                        }//end if.
                    }//end for.
                }//end if-else.
            }else{
                newAgent.setAvailability(agentAvailability);
            }//end if-else.

            setOtherAgent(ws, newAgent);
        }//end for.

        return s;
    }


    /**
     * Get anonymous state for all the groups in the domain.
     * @param wildfireDomain is the domain object.
     * @return is the hashmap of the group and state.
     */
    public static HashMap<String,State> getAnmStateForAllGroups(WildfireDomain wildfireDomain, State jointState){
        HashMap<String,State> anonymousStates = new HashMap<>();
        //Go through all the agent-groups, and create a sample anonymous state for each group.
        //Populate the hashmap.
        for(String group : wildfireDomain.agentGroups){
            int agentNumber = wildfireDomain.sampleAgentPerGroup.get(group).getAgentNumber();
            WildfireAnonymousState anonymousState = new WildfireAnonymousState(
                    (WildfireState) Wildfire.createAgentStateFromMasterState(wildfireDomain,agentNumber,jointState),wildfireDomain);
            anonymousStates.put(group,anonymousState);
        }//end for.
        return anonymousStates;
    }//end method.


    /**
     * Creates a new observation.
     *
     * @param domain The {@link WildfireDomain}
     * @param intensityChange The observed intensity difference
     *
     * @return An observation for {@code intensityChange}
     */
    public static State createObservation(WildfireDomain domain, int intensityChange) {
        FireObservation fob = new FireObservation();
        fob.setFireDifference(intensityChange);
        return fob;
    }


    /**
     * Returns the agent definition of all the agents.
     * @param wildfireDomain is the domain.
     * @return agent definitions.
     */
    public static List<Map<String, SGAgent>> getAllAgentDefs(WildfireDomain wildfireDomain){
        if(Wildfire.isSetAgentDefs ){
            return Wildfire.allAgentDefinitions;
        }else{
            throw new UnsupportedOperationException("Agent Definitions are not set!");
        }
    }


    /**
     * Returns the agent anonymous definition of all the agents.
     * @param wildfireDomain is the domain.
     * @return agent definitions.
     */
    public static Map<String,Map<String, SGAgent>> getAllAnmAgentDefs(WildfireDomain wildfireDomain){
        if(Wildfire.isSetAnmAgentDefs ){
            return Wildfire.allAgentAnmDefinitions;
        }else{
            throw new UnsupportedOperationException("Agent Definitions are not set!");
        }
    }

    /**
     * Set the agent definitions.
     * @param wildfireDomain is the domain.
     */
    public void setAgentDefs(WildfireDomain wildfireDomain){
        SGAgentBase sgAgent = null;
        SGAgentType sgAgentType = null;
        for(Agent agent: wildfireDomain.agentsList){
            sgAgentType = new SGAgentType(WildfireState.WILDFIRE_CLASS_NAME,
                                        getAgentActions(wildfireDomain,agent.getAgentNumber()));
            sgAgent = new RandomSGAgent();
            sgAgent.init(wildfireDomain,agent.getAgentNumber()+"",sgAgentType);

            Map<String, SGAgent> agentDefinitions = new HashMap<String, SGAgent>();
            //Add current agent.
            agentDefinitions.put(agent.getAgentNumber() +"", sgAgent);
            //Get all neighbours fire list.
            for(String neighbourGroup : wildfireDomain.agentsNeighbourhood.get(agent.getAgentGroup())){

                ArrayList<String> startEndIndexes =new ArrayList<>(Arrays.asList(neighbourGroup.split("-")));
                //Separate indexes.
                int startIndex = Integer.parseInt(startEndIndexes.get(0));
                int endIndex = Integer.parseInt(startEndIndexes.get(1));

                for(int index = startIndex ; index <=endIndex ; index++){
                    //Set the neighbours to the list.
                    Agent neighbour = wildfireDomain.agentsList.get(index);
                    sgAgentType = new SGAgentType(WildfireState.WILDFIRE_CLASS_NAME,
                            getAgentActions(wildfireDomain,neighbour.getAgentNumber()));

                    //Initiate the agent.
                    sgAgent = new RandomSGAgent();
                    sgAgent.init(wildfireDomain,agent.getAgentNumber()+"",sgAgentType);

                    //Add neighbour.
                    agentDefinitions.put(neighbour.getAgentNumber() +"", sgAgent);
                }//end for.
            }//end for.
            Wildfire.allAgentDefinitions.add(agentDefinitions);
        }
        Wildfire.isSetAgentDefs = true;
    }

    /**
     * It creates the anonymous agent definitions.
     * The definition has its first member as self-agent group and the rest of them are neighbour agent groups.
     * @param wildfireDomain is the domain.
     */
    public void setAnmAgentDefs(WildfireDomain wildfireDomain){
        SGAgentBase sgAgent = null;
        SGAgentType sgAgentType = null;
        for(String agentGroup: wildfireDomain.agentGroups){
            //Get the agent types and Random agent information.
            sgAgentType = new SGAgentType(WildfireState.WILDFIRE_CLASS_NAME,
                    getAgentActions(wildfireDomain,agentGroup));
            sgAgent = new RandomSGAgent();
            sgAgent.init(wildfireDomain,agentGroup,sgAgentType);

            Map<String, SGAgent> agentDefinitions = new HashMap<String, SGAgent>();
            //Add current agent.
            agentDefinitions.put(agentGroup, sgAgent);
            //Get all neighbour groups and add them into the definition.
            for(String neighbourGroup : wildfireDomain.agentsNeighbourhood.get(agentGroup)){
                sgAgentType = new SGAgentType(WildfireState.WILDFIRE_CLASS_NAME,
                        getAgentActions(wildfireDomain,neighbourGroup));

                //Initiate the agent per each group.
                sgAgent = new RandomSGAgent();
                sgAgent.init(wildfireDomain,neighbourGroup,sgAgentType);
                agentDefinitions.put(neighbourGroup, sgAgent);
            }//end for.
            Wildfire.allAgentAnmDefinitions.put(agentGroup,agentDefinitions);
        }
        Wildfire.isSetAnmAgentDefs = true;
    }//end method.

    /**
     * This method creates a list of agent actions for each agent.
     * @param wildfireDomain is the domain.
     * @param agentNumber is the agent Number.
     * @return List of the actions.
     */
    public ArrayList<ActionType> getAgentActions(WildfireDomain wildfireDomain,int agentNumber){
        //Call the method which gets the action of the group.
        return getAgentActions(wildfireDomain,wildfireDomain.agentsList.get(agentNumber).getAgentGroup());
    }//end if.


    /**
     * This method creates a list of agent actions for each agent-group.
     * @param wildfireDomain is the domain.
     * @param agentGroup is the group of the agent.
     * @return List of the actions.
     */
    public ArrayList<ActionType> getAgentActions(WildfireDomain wildfireDomain,String agentGroup){
        ArrayList<ActionType> actionList = new ArrayList<ActionType>();
        actionList.add(new UniversalActionType(WildfireParameters.NOOP_ACTION, new SimpleAction(WildfireParameters.NOOP_ACTION)));
        //Get the list of the fire.
        ArrayList<Integer> fireList = wildfireDomain.agentFireNeighbourhood.get(agentGroup);

        //Add all the fire actions.
        for(Integer fireNumber: fireList){
            String fireAction = wildfireDomain.fireList.get(fireNumber).fireActionName();
            actionList.add(wildfireDomain.getActionType(fireAction));
        }

        return actionList;
    }//end if.


//    //For testing purpose only.
    public static void main(String args[]){
//        WildfireAnonymousState anm = new WildfireAnonymousState();
//        anm.setSelfAgent(new Agent(1,new Location(0,1),1,2,"0-3"));
//        anm.setanonymousAgents(new String[]{"4-7"});
//        anm.setFireList(new Fire[]{new Fire(1,new Location(1,0),3)});
//
//        State s1 = anm;
//
//        WildfireAnonymousState anm1 = new WildfireAnonymousState();
//        anm1.setSelfAgent(new Agent(1,new Location(0,1),1,2,"0-3"));
//        anm1.setanonymousAgents(new String[]{"4-7"});
//        anm1.setFireList(new Fire[]{new Fire(1,new Location(1,0),3)});
//
//        State s2 = anm1;
//
//        WildfireAnonymousState anm2 = new WildfireAnonymousState();
//        anm2.setSelfAgent(new Agent(1,new Location(0,1),1,2,"0-3"));
//        anm2.setanonymousAgents(new String[]{"4-7"});
//        anm2.setFireList(new Fire[]{new Fire(1,new Location(1,0),3)});
//
//
//        State s3 = anm2;
//
//        List<State> st = new ArrayList<State>(){{add(s1);add(s2);}};
//
//
//        System.out.println(s1.equals(s2));
//        System.out.println(s2.equals(s3));
//        System.out.println(st.contains(s3));
//
//        String st1 = "test";
//        String st2 = "test2";
//        String st3 = "test2";
//
//        List<String> str = new ArrayList<String>(){{add(st1);add(st2);}};
//        System.out.println(str.contains(st3));
        //Get the jvm heap size.
        long heapSize = Runtime.getRuntime().maxMemory();

        //Print the jvm heap size.
        System.out.println("Heap Size = " + heapSize);
    }
}//end class.

