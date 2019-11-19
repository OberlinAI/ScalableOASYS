/**
 * Simplified Definition: Frame Action Neighbourhood can be defined as groups of similar type of agent performing same action in the context.
 * {@Ref Sonu, Ekhlas, Yingke Chen, and Prashant Doshi. "Decision-Theoretic Planning under Anonymity in Agent Populations."
 * Journal of Artificial Intelligence Research 59 (2017): 725-770.}
 *
 *  In terms of WildFire Domain, the configuration can be defined as the set of the same-typed agent fighting the same fire.
 *  C = (N{theta i, fire j}, N{theta i+1,fire j}.........N{theta n,fire k}), where theta is agent-type or agent-power of agents
 *  and k is the number of fires in neighbourhood of an agent A. Here, the N can vary on the basis of number of agents being
 *  modelled.
 *
 *  This class provides a comprehensive utility for managing such Frame Action Configuration.
 */
package scalability;

import burlap.mdp.core.action.Action;
import java.util.*;
import java.util.function.Consumer;
import scalability.beans.ConfigurationBean;


/**
 * The FrameActionConfiguration class is a representation of the Configuration made up of the types-action pair according to the Anonimity(TODO: Put right reference)
 * paper. The {@link scalability.beans.ConfigurationBean} configuration bean class contains a single element of this
 * configuration, which is a pair of agnet frame- action - number of agents fall in this criteria. Here, the arrays
 * of such configuration represents all the available configuration in the given anonymous state with a list of
 * maximum number of agents can be in an frame-action pair. Also, the class contains an array representing the number of agents
 *  in a particular type and total number of agents in the state.
 */
public class FrameActionConfiguration  implements Action, Iterable<Action>{

    /**
     * This would define the actual frame-action-configuration. The set would remove the duplicates if any.
     * Here, the configurationBean must contain the maximum number of agents for the particular action.
     */
    public  ConfigurationBean[] maxConfiguration;
    /**
     * This would define the current FAC. To remove caching of all the configuration, this variable would serve as the
     * current configuration for reference.
     */
    public  ConfigurationBean[] currentConfiguration;

    /**
     * This variable would store the number of types and  number of agent for each type.
     * The array should contain two columns, of which first column should be the agent-type and the second should be the number of agents.
     */
    private int[][] agentTypes;


    /**
     * Total number of agent in the Anonymous state.
     */
    private int totalAgents;


    /**
     * This constructor just instantiates the configuration object.
     */
    public FrameActionConfiguration() {
        maxConfiguration = new ConfigurationBean[1];
    }

    /**
     * Flag for stopping the recursive call.
     */
    boolean stopRecursive = false;

    /**
     * This constructor just instantiates the configuration object.
     * @param configurationSize is the number of different types that can be created from the combination of
     *                          agent-frames (which is agent type and actions).
     *
     */
    public FrameActionConfiguration(int configurationSize) {
        this.maxConfiguration = new ConfigurationBean[configurationSize];
        this.currentConfiguration = new ConfigurationBean[configurationSize];
        this.totalAgents = 0;
    }

    /**
     * Create configuration from the list already available.
     * @param configuration is a configuration.
     */
    public FrameActionConfiguration(ConfigurationBean[] configuration,int[][] agentTypes) {
        this.maxConfiguration = configuration;
        this.currentConfiguration = new ConfigurationBean[configuration.length];
        this.agentTypes = agentTypes;
        //Sum the agents for each type.
        this.totalAgents = 0;
        for(int[] numAgents : agentTypes){
            this.totalAgents += numAgents[1];
        }

        //Initialize the current FAC.
        initializeCurrentFAC();
    }//end constructor.


    /**
     * Create configuration from the list already available.
     * @param  fac is the FAC to copy.
     * @param isDeepCopyWhole is indicator if entire FAC need to be deep copied or not.
     */
    public FrameActionConfiguration(FrameActionConfiguration fac, boolean isDeepCopyWhole) {

        //Deep copy the max configuration and agent types only if needed.
        if(isDeepCopyWhole){
            //Copy MaxConfigration.
            this.maxConfiguration = new ConfigurationBean[fac.getMaxConfiguration().length];
            for(int i=0 ; i < this.maxConfiguration.length ; i++){
                this.maxConfiguration[i] = new ConfigurationBean(fac.getMaxConfiguration()[i]);
            }

            //Copy types.
            this.agentTypes = new int[fac.getAgentTypes().length][fac.getAgentTypes()[0].length];
            for(int agentCount = 0 ; agentCount < fac.getAgentTypes().length; agentCount++){
                System.arraycopy(fac.getAgentTypes()[agentCount],0,this.agentTypes[agentCount],0,fac.getAgentTypes()[agentCount].length);
            }
        }else{
            this.maxConfiguration = fac.getMaxConfiguration();
            this.agentTypes = fac.getAgentTypes();
        }


        this.currentConfiguration = new ConfigurationBean[fac.getCurrentConfiguration().length];
        for(int i=0 ; i < this.currentConfiguration.length ; i++){
            this.currentConfiguration[i] = new ConfigurationBean(fac.getCurrentConfiguration()[i]);
        }
        this.totalAgents = fac.getTotalAgents();
    }


    public ConfigurationBean[] getMaxConfiguration() {
        return this.maxConfiguration;
    }

    public ConfigurationBean[] getCurrentConfiguration() {
        return this.currentConfiguration;
    }

    public int[][] getAgentTypes() {
        return agentTypes;
    }

    public void setAgentTypes(int[][] agentTypes) {
        this.agentTypes = agentTypes;
        //Calculate total agents also.
        this.totalAgents = 0;
        for(int i = 0 ; i < this.agentTypes.length ; i++){
            totalAgents += this.agentTypes[i][1];
        }
    }

    public int getTotalAgents() {
        return totalAgents;
    }

    public void setTotalAgents(int totalAgents) {
        this.totalAgents = totalAgents;
    }

    /**
     * This method adds one configuration to the configuration array. It would first check the array to match the agent-type and
     * action, if any match found then it would overwrite the content by calling the set method, otherwise it would add it to the array.
     * @param agentType is the agent type.
     * @param action is the action.
     * @param numberOfAgents is the number of agents.
     */
    public void putMaxConfiguration(int agentType,Action action,int numberOfAgents) {
        ConfigurationBean newConfig = new ConfigurationBean(agentType,action,numberOfAgents);
        putMaxConfiguration(newConfig);

    }//end method.

    /**
     * This method adds one configuration to the configuration hashmap. It would over-write the records with
     * same agent Type - action pair.
     * @param configurationBean is the configuration Bean object being passed.
     * */
    public void putMaxConfiguration(ConfigurationBean configurationBean) {
        boolean configFound = false;
        int firstNull = -1;
        //If the configuration has length more than 0 then find appropriate location to set.
        if(this.maxConfiguration.length > 0){
            for(int config=0 ; config < this.getMaxConfiguration().length ; config++){
                //Check if it is the same type of maxConfiguration in the fac, then update the number of agents.
                if(this.maxConfiguration[config] != null
                        && this.maxConfiguration[config].getAgentType() == configurationBean.getAgentType()
                        && this.maxConfiguration[config].getAction().actionName().equals(configurationBean.getAction().actionName())){
                    this.maxConfiguration[config].setNumberOfAgents(configurationBean.getNumberOfAgents());
                    configFound = true;
                    break;
                }//end if
                //Store the first Null value pointer.
                if(firstNull == -1 && this.maxConfiguration[config] == null){
                    firstNull = config;
                }
            }//end for.
        }//end if.


        //Assumption. The configuration numbers in the Frame-Action-Configuration is known priorly.
        //If there is any extra configuration other than the initial numbers, everything would be written to 0.
        if(!configFound){
            setMaxConfiguration(firstNull,configurationBean);
        }//end if.
    }

    /**
     * Set the configuration to the given location.
     * @param index is the index of the configuration.
     * @param configurationBean is the configuration Bean object being passed.
     * */
    public void setMaxConfiguration(int index,ConfigurationBean configurationBean) {
        this.maxConfiguration[index] = configurationBean;
    }



    /**
     * This method adds one configuration to the currentConfiguration array. It would first check the array to match the agent-type and
     * action, if any match found then it would overwrite the content by calling the set method, otherwise it would add it to the array.
     * @param agentType is the agent type.
     * @param action is the action.
     * @param numberOfAgents is the number of agents.
     */
    public void putCurrentConfiguration(int agentType,Action action,int numberOfAgents) {
        ConfigurationBean newConfig = new ConfigurationBean(agentType,action,numberOfAgents);
        putCurrentConfiguration(newConfig);

    }//end method.

    /**
     * This method adds one configuration to the currentConfiguration array. It would over-write the records with
     * same agent Type - action pair.
     * @param configurationBean is the configuration Bean object being passed.
     * */
    public void putCurrentConfiguration(ConfigurationBean configurationBean) {
        boolean configFound = false;
        int firstNull = 0;
        //If the configuration has length more than 0 then find appropriate location to set.
        if(this.currentConfiguration.length > 0){
            for(int config=0 ; config < this.getMaxConfiguration().length ; config++){
                //Check if it is the same type of configuration in the fac, then update the number of agents.
                if(this.currentConfiguration[config] != null
                        && this.currentConfiguration[config].getAgentType() == configurationBean.getAgentType()
                        && this.currentConfiguration[config].getAction().actionName().equals(configurationBean.getAction().actionName())){
                    this.currentConfiguration[config].setNumberOfAgents(configurationBean.getNumberOfAgents());
                    configFound = true;
                    break;
                }//end if
                //Store the first Null value pointer.
                if(firstNull ==0 && this.currentConfiguration[config] == null){
                    firstNull = config;
                }
            }//end for.
        }//end if.


        //Assumption. The configuration numbers in the Frame-Action-Configuration is known priorly.
        //If there is any extra configuration other than the initial numbers, everything would be written to 0.
        if(!configFound){
            setCurrentConfiguration(firstNull,configurationBean);
        }//end if.
    }

    /**
     * Set the configuration to the given location.
     * @param index is the index of the configuration.
     * @param configurationBean is the configuration Bean object being passed.
     * */
    public void setCurrentConfiguration(int index,ConfigurationBean configurationBean) {
        this.currentConfiguration[index] = configurationBean;
    }


    /**
     * Set the configuration to the given location.
     * @param noOfConfigs is no. of configs to create a new array.
     * */
    public void setConfigurationArraySize(int noOfConfigs) {
        this.maxConfiguration = new ConfigurationBean[noOfConfigs];
        this.currentConfiguration = new ConfigurationBean[noOfConfigs];
    }


    /**
     * Returns the number of configuration in the overall Frame action configuration.
     * @return number of joint action configuration.
     */
    public int size(){
        return this.maxConfiguration.length;
    }


    @Override
    public String toString() {
        return "FrameActionConfiguration{" +
                "configuration=" + maxConfiguration +
                "Current Configuration=" + currentConfiguration+
                '}';
    }


    /**
     * This method prints the current configurations of FAC.
     * @return current configuration String.
     */
    public String printCurrentConfig(){
        String currentConfig = "";

        for(ConfigurationBean config : this.currentConfiguration){
            currentConfig += config.toString() + ":";
        }

        return currentConfig;
    }


    @Override
    public Iterator iterator() {
        return null;
    }


    /**
     * Copy the current configuration and return a fully new object.
     * @return is the newly created object.
     */
    public FrameActionConfiguration copy() {
        FrameActionConfiguration fac = new FrameActionConfiguration(this,true);
        return fac;
    }

    /**
     * Compares two FACs and returns the states accordingly.
     * The equals method does not compare current configuration for comparing the objects.
     * @param o the FAC to compare.
     * @return true or false based on the result.
     */
    @Override
    public boolean equals(Object o){

        //Compare the instances.
        if(!(o instanceof FrameActionConfiguration)){
            return false;
        }

        //Compare size of the iterators.
        FrameActionConfiguration faco = (FrameActionConfiguration)o;
        if(faco.getMaxConfiguration().length != this.getMaxConfiguration().length){
            return false;
        }

        //Compare each object one by one.

        for(int config = 0 ; config < faco.maxConfiguration.length ; config++){

            if(!(this.maxConfiguration[config].equals(faco.getMaxConfiguration()[config]))){
                return  false;
            }//end if
        }
        return true;
    }//end method.


    /**
     * Get the configuration name of the ocnfiguration, which is 'agentType'-'action'.
     * @param index is the index of the configuration.
     * @return config-string.
     */
    public String actionName(int index) {
        return this.maxConfiguration[index].getAgentType() + "-" + this.maxConfiguration[index].getAction().actionName();
    }


    /**
     * Find the index of the type of the configuration. The method will return -1 in case of not found.
     * @param agentType is the type of the agent.
     * @param action is the action of the config.
     * @return is the index of the agent.
     */
    public int search(int agentType, String action) {
        int configIndex = -1; // -1 means not found.
        //Find the index of the configuration, which has the same action Type and
        //the same action.
        for(int config = 0 ; config < this.maxConfiguration.length; config++){
            //Check for the Action comparison. If there is any implementation of the comparison.
            if(this.maxConfiguration[config] != null
                    && this.maxConfiguration[config].getAgentType() == agentType
                    && this.maxConfiguration[config].getAction().actionName().equals(action)){
                configIndex = config;
                break;
            }
        }//end for.
        return  configIndex;
    }//end method.


    /**
     * This method returns the possible combination of the configuration given the FAC information.
     * Here, the primary reference is the configuration object of this class for the type of the agent and the
     * fire it is fighting with. The outcome ArrayList would be the possible combinations of the configuration, where
     * each element is an integer array of the size of the static variable configuration and the number on a particular position
     * refer to config information for the configuration-type.
     * @param typesList is the list of types in the state. e.g. [1,3,4]
     * @param typeAgents is type to no. of agent mapping, where the index is the agent-type-index and the value is no. of agents.
     *
     * PS: It's weird to use ArrayList<int[]>, but the inner array has fixed size and the outer ArrayList size can not
     * be determined. It might boost performance.
     */
//    public void findPossibleFACs(int typesList[],int typeAgents[]) throws Exception {
//
//        ArrayList<ArrayList<int[]>> facPerType = new ArrayList<ArrayList<int[]>>();//This is the possible combination of each configuration.
//        ArrayList<int[]> possibleFACs = new ArrayList<int[]>();//Final list.
//
//        //Get the maximum number of agent each configuration can hold.
//        int[]  maxAgents = new int[this.configuration.length];
//        // Array for a type of agent can fight how many fires. index is agent type and value is number of fire-actions.
//        int[]  typeSize = new int[typeAgents.length];
//        for(int config = 0 ; config < this.configuration.length ; config++){
//            //Set the limits.
//            maxAgents[config] = this.configuration[config].getNumberOfAgents();
//
//            //Search the index of a particular type in the typeList. If not found through an exception.
//            int lookType = this.configuration[config].getAgentType();
//            int lookupTypeMatch = -1;
//            for(int typeSearch = 0 ; typeSearch < typesList.length ; typeSearch++){
//                if(typesList[typeSearch] == lookType){
//                    lookupTypeMatch = typeSearch;
//                    break;
//                }//end if.
//            }//end for.
//
//            if(lookupTypeMatch == -1){
//                throw new Exception("Inconsistency in FAC and the State anonimity.");
//            }
//            //Increase counter for the type.
//            typeSize[lookupTypeMatch] +=1;
//        }//end for.
//
//
//        //Iterating through the configuration.
//        int configCounter =0;
//        //Calculate possible combinations of the configurations by finding possible combination for each type first and then
//        //combining them.
//        for(int type = 0; type < typeAgents.length; type ++){
//            //System.out.println("Type:"+type);
//            ArrayList<int[]> currentTypeFACs = new ArrayList<int[]>();//current type FACs.
//
//            //Create the initial array of the typeSize, which would indeed be used for creating configuration.
//            int[] configInit = new int[typeSize[type]];
//            for(int con=0 ; con< configInit.length;con++){
//                configInit[con] = -1;
//            }
//
//            //Set current config to the index of the initial position.
//            int currentConfig = 0;
//
//            //current maximum value of the agent for the current type-fire.
//            int currentMax = maxAgents[configCounter];
//
//            //Maximum allowed agent of type 'type' for respective fires.
//            int[] configMaxAgents = new int[typeSize[type]];
//            //Create Maximum agent array for the whole configs who have same type.
//            //It uses the maxAgent array.
//            for(int tm=0; tm< configMaxAgents.length;tm++ ){
//                configMaxAgents[tm] = maxAgents[configCounter];
//                configCounter++;
//            }
//
//
//            //Call recursive method to find the FAC for the type.
//            exploreFAC(configInit,currentConfig,currentMax,typeAgents[type],configMaxAgents,currentTypeFACs);
//
//            //Add it to the list.
//            facPerType.add(currentTypeFACs);
//
//        }
//
//        //Merge the arrays found from configurations.
//        mergeArrays(new int[1],0,possibleFACs,facPerType);
//
//        System.out.println("Merged Count:" + possibleFACs.size());
//
//
//        //Set it for further usage.
//        this.possibleConfigurations = possibleFACs;
//
//    }//end method.

    /**
     * This method finds all possible combination of the configuration for the current agent type. The method is recursive in
     * nature, which iterates through all the action of a type given that agent type values. e.g. current configInit is [5,2,-1] and the third
     * location has maximum 7 possible values available , it would check [5,2,0]....[5,2,7] and try to apply the constraints of total sums.
     * @param configInit is the initial array with all -1s for the first time and then current configuration.
     * @param curConfig is initiated with a 0 and it would be increased in the step by step.
     * @param curMax is the maximum-agent-counts for the type-action pair.
     * @param totalAgents is the total number of agents in the premise.
     * @param configMax is the  maximum agent available for type-action pair from the premise configuration.
     * @param currentTypeFACs is the list to add all the configuration.
     */
    public void exploreFAC(int[] configInit,int curConfig,int curMax, int totalAgents, int[] configMax,ArrayList<int[]> currentTypeFACs){
        //If the last element in the type.
        if(curConfig == configInit.length - 1){
            //Try to get all the possible combinations and if it sums up to the total number
            //then add to the list.
            for(int count=0; count <= curMax && count <= configMax[curConfig]; count++) {
                configInit[curConfig] = count;
                //If the configuration qualifies as correct then add it to the list.
                if(intArraySum(configInit) == totalAgents) {
                    int[] tempArray = new int[configInit.length]; //So that references does not mess up.
                    System.arraycopy(configInit, 0, tempArray, 0, configInit.length);
                    currentTypeFACs.add(tempArray);
//                    System.out.println("Configuration Size" + currentTypeFACs.size());
                }//end if.
            }//end for.
        }else{
            //Otherwise recursively call the method for finding all possible combination for the next type given the current type.
            for(int count = 0;count <= curMax && count <= configMax[curConfig];count++ ){
                configInit[curConfig] = count;
                exploreFAC(configInit,curConfig + 1,totalAgents-count,totalAgents,configMax,currentTypeFACs);
            }//end for.
        }//end else.
    }//end method.

    /**
     * This method sums up all the elements of an array.
     * @param sumArray integer array.
     * @return sum of the values.
     */
    private int intArraySum(int[] sumArray){
        int sum =0;
        for(int i=0 ; i < sumArray.length ; i++)
            sum+=sumArray[i];
        return sum;
    }//end.


    /**
     * This method merges the configurations received from each type.
     * @param currentArray is the initial array of size 1.
     * @param currentType is the current type of the agent.
     * @param possibleFACs is the list of possible FACs to be generated.
     * @param facPerType is the available list of all the configuration per type.
     */
    private void mergeArrays(int[] currentArray, int currentType,ArrayList<int[]> possibleFACs,ArrayList<ArrayList<int[]>> facPerType){
        //Merge everything using type.
        for(int config = 0; config <facPerType.get(currentType).size(); config++ ){
            //Create a merged array with size of both individual arrays.
            int[] tempArray;
            int copyIndex = 0; //Index to copy the array.
            //The initialized current array would be of size 1 to avoid any null pointers.
            if(currentArray.length ==1){
                tempArray  = new int[facPerType.get(currentType).get(config).length];

            }else{
                tempArray  = new int[currentArray.length + facPerType.get(currentType).get(config).length];
                System.arraycopy(currentArray, 0, tempArray, 0, currentArray.length);
                copyIndex = currentArray.length;
            }

            System.arraycopy(facPerType.get(currentType).get(config), 0, tempArray, copyIndex, facPerType.get(currentType).get(config).length);
            //If the level is the final level then add it to the final list.
            //Otherwise recursively call method to add the information into the list from other types.
            if(currentType == facPerType.size()-1){
                possibleFACs.add(tempArray);
//                System.out.println("Merged:" + tempArray);
            }else{
                mergeArrays(tempArray,currentType+1,possibleFACs,facPerType);
            }//end if-else.
        }//end for.
    }//end.

    /**
     * This method generates a new FAC given the currentConfiguration and agent-type array and copies into the currentConfiguration
     * values, if exist.
     * Algorithm: Iterate in the reverse order of the types from the current configuration. It first checks if there exist a scope of
     * updating the value of type-action pair with updating the number of agents. The iteration is done from the max value
     * to the minimum value for the number of agents in the respective order of type-action pairs. The sums of each agent
     * in type-action pair must sum up to the type-totals for their respective type.
     * E.g. for the Configuration with max agents [5 12 7] and total-agents 12, if the current value is [0 5 7], the next
     * values can be [1 11 0], [1 10 1],.....[1 5 7] , etc.
     * (Aggregating types, the first type-action would be the NO-OP and would be initialized with the maximum value.
     * The next numOfAgents values would also be initialized with maximum possible values and would be updated accordingly.)
     *
     * NOTE: Contradicting to other methods hasNext actually change the underlying under consideration value.
     * @return true if there exist any new configuration and false otherwise.
     */
    public boolean hasNext(){
        boolean nextFACExist= false; //If any next element exist or not.
        boolean resetValueFlag = false;//Reset the values after change in the upper type config.
        //Iterate through each type and agents.
        for(int type = this.agentTypes.length-1 ; type >= 0 ;type--){
            ArrayList<Integer> config = new ArrayList<Integer>();
            ArrayList<Integer> maxAgents = new ArrayList<Integer>();
            int startIndex = -1, size = 0;
            // For each configuration in the list.
            for(int index = 0 ; index < this.currentConfiguration.length; index++){
                if(this.currentConfiguration[index].getAgentType() == this.agentTypes[type][0]){
                    //Update the start index.
                    if(startIndex == -1)
                        startIndex = index;
                    //Add the number of agents to the list.
                    config.add(this.currentConfiguration[index].getNumberOfAgents());
                    maxAgents.add(this.maxConfiguration[index].getNumberOfAgents());
                    size++; //Increase the size of number of agents.
                }//end if.
            }//end for.

            //If after going through the previous type, there is no change in the FAC, then reset the values to count again.
            boolean resetValue = false;
            if(resetValueFlag){
                resetValue = true;

                if(this.agentTypes.length-1 == type){
                    //Put Reset value flag to false, so that it can be updated later if needed.
                    resetValueFlag = false;
                }
            }

            //Convert the Array list into integer arrays.
            int configRefArray[] = new int[config.size()]; //To compare the results with the previous FAC.
            int configArray[] = new int[config.size()];
            int maxArray[] = new int[config.size()];
            boolean reset[] = new boolean[config.size()]; //Reset array for resetting the type-action counts.
            for(int index=0; index< config.size();index++){
                if(!resetValue){
                    configRefArray[index] = config.get(index);
                    configArray[index] = config.get(index);
                }else{
                    configRefArray[index] = 0;
                    configArray[index] = 0;
                }
                maxArray[index] = maxAgents.get(index);
                reset[index] = resetValue;
            }//end for.


            //Get the next FAC and reset the flag.
            getNextFAC(configArray,reset,configRefArray,0,maxArray[0], this.agentTypes[type][1], maxArray, startIndex);
            this.stopRecursive = false;
            //Check if it is the same, then go to next type or break.
            for(int index = 0 ; index < size ; index++){
                if(this.currentConfiguration[index+startIndex].getNumberOfAgents() != configRefArray[index]){

                    //Find total number of agents in current configuration.
                    int currentAgentSum = 0;
                    for(int sumIndex=0 ; sumIndex < this.currentConfiguration.length; sumIndex++){
                        currentAgentSum += this.currentConfiguration[sumIndex].getNumberOfAgents();
                    }//end for.

                    //If the current type is the last one and the sum equals to the total agents
                    //complete search.
                    if(type ==  this.agentTypes.length-1){
                        if(this.totalAgents == currentAgentSum){
                            nextFACExist = true;
                        }
                    }else if(this.totalAgents == currentAgentSum){
                        type = type + 2; // The type would be reduced by one in each iteration, which would make this statement as type+1.
                        resetValueFlag = true;//Reset the values for the next run.
                    }else{
                        //Let it continue.
                    }//end if-else.
                    break;
                }//end if.
            }//end for.

            //Break again if next FAC exist.
            if(nextFACExist)
                break;

        }//end for.


        return  nextFACExist;
    }//end method.


    /**
     * This method returns the current configuration.
     * @return current configuration.
     */
    public ConfigurationBean[] next(){
        return  this.currentConfiguration;
    }


    /**
     * This method initializes the current FAC with maximum values from the type it starts.
     * The idea is to start the beginning type-action pairs with maximum values and then set the rest of the values with
     * the remaining agents.
     */
    public void initializeCurrentFAC(){
        //For each type set the values so that it matches the total number of agents for that type.
//        for(int typeCount =0, overallCounter = 0  ; typeCount < this.agentTypes.length; typeCount++,overallCounter++){
        for(int overallCounter = 0  ; overallCounter < this.maxConfiguration.length; overallCounter++){
            this.currentConfiguration[overallCounter] =
                    new ConfigurationBean(this.maxConfiguration[overallCounter].getAgentType(),
                            this.maxConfiguration[overallCounter].getAction(), 0);
        }//end for.s
    }//end method.

    /**
     * This method finds all possible combination of the configuration for the current agent type. The method is recursive in
     * nature, which iterates through all the action of a type given that agent type values. e.g. current configInit is [5,2,-1] and the third
     * location has maximum 7 possible values available , it would check [5,2,0]....[5,2,7] and try to apply the constraints of total sums.
     * @param configInit is the initial array with all -1s for the first time and then current configuration.
     * @param configRef is to compare the generated array with the previous one.
     * @param curConfig is initiated with a 0 and it would be increased in the step by step.
     * @param curMax is the maximum-agent-counts for the type-action pair.
     * @param totalAgents is the total number of agents in the premise.
     * @param configMax is the  maximum agent available for type-action pair from the premise configuration.
     */
    public void getNextFAC(int[] configInit,boolean reset[],int[] configRef,int curConfig,int curMax, int totalAgents, int[] configMax, int currentConfigStartIndex){
        if(!stopRecursive){
            //If the last element in the type.
            if(curConfig == configInit.length - 1){
                //If the resetting the current value is true, then reset the values and set the flag to false.
                if(reset[curConfig] == true){
                    this.currentConfiguration[currentConfigStartIndex+curConfig].setNumberOfAgents(0);
                    reset[curConfig] = false;
                }
                //Try to get all the possible combinations and if it sums up to the total number
                //then add to the list.
                for(int count=this.currentConfiguration[currentConfigStartIndex+curConfig].getNumberOfAgents(); count <= curMax && count <= configMax[curConfig]; count++) {
                    configInit[curConfig] = count;
                    //If the configuration qualifies as correct then add it to the list.
                    if(intArraySum(configInit) == totalAgents && !Arrays.equals(configInit,configRef)) {
                        for(int currentCounter = currentConfigStartIndex, configCounter = 0 ; configCounter < configInit.length
                                ; currentCounter++, configCounter++){
                            this.currentConfiguration[currentCounter].setNumberOfAgents(configInit[configCounter]);
                        }
                        this.stopRecursive = true;
                        break;
                    }//end if.
                }//end for.
                reset[curConfig] = true;
            }else{

                //If the resetting the current value is true, then reset the values and set the flag to false.
                if(reset[curConfig] == true){
                    this.currentConfiguration[currentConfigStartIndex+curConfig].setNumberOfAgents(0);
                    reset[curConfig] = false;
                }

                //Otherwise recursively call the method for finding all possible combination for the next type given the current type.
                for(int count = this.currentConfiguration[currentConfigStartIndex+curConfig].getNumberOfAgents();count <= curMax && count <= configMax[curConfig];count++ ){
                    //Breaking through Recursion.
                    if(stopRecursive)
                        break;
                    configInit[curConfig] = count;
                    getNextFAC(configInit,reset,configRef,curConfig + 1,totalAgents-count,totalAgents,configMax,currentConfigStartIndex);
                }//end for.
                reset[curConfig] = true;
            }//end else.
        }//end if stop recursive.
    }//end method.


    /**
     * This method compares two FAC's current configurations. The current configurations should have equal size and each
     * ConfigurationBean must match others.
     * @param configuration is the configuration to match with the current object's configuration.
     * @return is true/false based on comparison.
     */
    public boolean compareCurrentConfigs(FrameActionConfiguration configuration){

        //If the lengths differ than return false.
        if(configuration.getCurrentConfiguration().length != this.getCurrentConfiguration().length){
            return false;
        }

        //Search and find the match for the configuration bean.
        for(ConfigurationBean configurationBean : configuration.getCurrentConfiguration()){
            boolean isMatch = false;
            for(ConfigurationBean thisConfigBean : this.currentConfiguration){
                if(configurationBean.equals(thisConfigBean)){
                    isMatch = true;
                    break;
                }//end if.
            }//end inner for.
            //If no match found for the configuration than return false.
            if(!isMatch){
                return false;
            }///end if.
        }//end for.

        //All the configuration matches. So, return true.
        return true;
    }//end method.


    //No use.
    @Override
    public String actionName() {
        return null;
    }

    @Override
    public void forEach(Consumer<? super Action> action) {
    }

    @Override
    public Spliterator<Action> spliterator() {
        return null;
    }


//    static ArrayList<ArrayList<String>> test;
//    static ArrayList<String> merge;
//    public  static  void main(String[] args){
//        int a[] = new int[]{1,2,3};
//        int b[] = new int[]{4,5,6};
//        int c[] = new int[a.length + b.length];
//        System.arraycopy(a, 0, c, 0, a.length);
//        System.arraycopy(b, 0, c, a.length, b.length);
//        System.out.println(Arrays.toString(c));
//        test = new ArrayList<ArrayList<String>>();
//        merge = new ArrayList<String>();
//        int types = 2;
//
//        for(int t =1; t <= types; t ++){
//            System.out.println("Type:"+t);
//            ArrayList<String> test1 = new ArrayList<String>();
//            if(t == 1)
//                exploreFAC(new int[]{-1,-1,-1},0,5,7,new int[]{5,7,2},test1);
//            if(t == 2)
//                exploreFAC(new int[]{-1,-1,-1},0,3,7,new int[]{3,7,4},test1);
//            test.add(test1);
//            System.out.println("Type" + t + " count:" + test1.size());
//        }
//
//        mergeArrays("",0);
//
//        System.out.println("Merged Count:" + merge.size());
//        System.out.println("Merged Count:" + merge);
//    }


}

