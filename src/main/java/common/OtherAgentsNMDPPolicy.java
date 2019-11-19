package common;


import burlap.behavior.policy.Policy;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.state.State;
import domains.wildfire.WildfireDomain;
import posg.POOOSGDomain;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * This class represents the learned NMDP policy for an agent group. The object of this class would represent a particular
 * agent group and would give the best action or a random action from the set of best actions.
 * @author Maulik
 */
public class OtherAgentsNMDPPolicy implements Policy {
    /**
     * The domain object for the policy.
     */
    private POOOSGDomain domain;
    /**
     * Agent Group Name for the domain.
     */
    private String agentName;
    /**
     * Type of the Policy using enum.
     */
    public enum PolicyType{Stochastic, Deterministic}
    private PolicyType policyType;

    /**
     * The list of actions that any agent in the agent group can do.
     */
    private Action[] agentActions;
    /**
     * Note: Use when using the stochastic policy.
     * The mapping of the state to action probabilities. Here, the probability index refers the
     * index of the action in the agentActions object.
     */
     private double[][] stateActionProbMap;

    /**
     * Note: Use when using the deterministic policy.
     * The mapping of the state to best actions. (There can be multiple actions for a state. One can be chosen randomly
     * in such case.)
     */
    private Action[][] stateBestActionMaps;



    /**
     * The default constructor can only be accessed, when reading policy from the file.
     */
    public OtherAgentsNMDPPolicy(POOOSGDomain domain,String agentName,PolicyType policyType){
        this.policyType = policyType;
        this.domain = domain;
        this.agentName = agentName;
    }

    /**
     * Constructor for the copying the object.
     * @param otherAgentsNMDPPolicy is the object to copy.
     */
    public OtherAgentsNMDPPolicy(OtherAgentsNMDPPolicy otherAgentsNMDPPolicy) {
        this.domain = otherAgentsNMDPPolicy.getDomain();
        this.agentName = otherAgentsNMDPPolicy.getAgentName();
        this.agentActions =  otherAgentsNMDPPolicy.getAgentActions();
        this.stateActionProbMap = otherAgentsNMDPPolicy.getStateActionProbMap();
        this.stateBestActionMaps = otherAgentsNMDPPolicy.getStateBestActionMaps();
        this.policyType = otherAgentsNMDPPolicy.getPolicyType();
    }


    /**
     * This method is the only way to initialize the policy object, using the policy file.
     * @param policyFileName is the name of the policy file.
     * @param numberOfStates is the number of states in the domain.
     * @return is the object of the current class with all the state-action pair setup.
     */
    public void readPolicy(String policyFileName, int numberOfStates){
        //Convert the object into the WildfireDomain object.
        WildfireDomain wildfireDomain = (WildfireDomain)this.domain;


        //Read the file.
        try{
            //Scan the policy file. Match the actions with those in the domain and add it to the action objects,
            //if everything goes correct. Throw exception otherwise.
            Scanner policyScan = new Scanner(new File(policyFileName));

            //Get the action Names from the policy file.
            if(policyScan.hasNext()){
                String actionStringLine = policyScan.nextLine();
                String[] actionStrings = actionStringLine.split(",");

                //Get the list of the agent actions in the domain and try to match it with that of the policy.
                List<ActionType> domainActions = wildfireDomain.actionsPerGroup.get(this.agentName);
                this.agentActions = new Action[domainActions.size()];
                int actionCounter = 0;
                for(String actionString : actionStrings){
                    //Flag to find difference between the domain actions and the policy.
                    boolean actionExist = false;
                    //Verify if the action type in the policy file exist in the domain actions or not.
                    for(ActionType domainAction: domainActions){
                        if(domainAction.typeName().equals(actionString)){
                            //Add the action to the file.
                            this.agentActions[actionCounter++] = domainAction.associatedAction("Useless argument");
                            actionExist = true;
                            break;
                        }
                    }//end for.
                    //Exit the process, if the policy and the actions does not matches.
                    if(!actionExist){
                        throw new Exception("Policy Actions and Domain Actions mismatch.");
                    }
                }//end for - actionString.
            }//end if -reading actions.


            //Initialize the policy array with number of states.
            this.stateBestActionMaps = new Action[numberOfStates][];
            //Now read action probability for each state.
            while (policyScan.hasNext()){
                String actionProbString = policyScan.nextLine();
                //The policy file contains a blank line in the end generally.
                if(!actionProbString.contains(":")){
                    break;
                }
                String[] actionProbArray = actionProbString.split(":");
                if(this.getPolicyType() == PolicyType.Deterministic){
                    //Get the state.
                    int policyStateIndex = Integer.parseInt(actionProbArray[0].trim());
                    //Get all the actions and add the actions with more than 0 probability.
                    ArrayList<Action> bestActions = new ArrayList<>();
                    String[] actionProbs = actionProbArray[2].split(",");
                    for(int action = 0 ; action < actionProbs.length;action++){
                        //The last element in this string array might contain just "".
                        //Add action to the array.
                        if( (actionProbs[action] != null) &&
                                (!actionProbs[action].trim().equals("")) &&
                                (Double.parseDouble(actionProbs[action].trim()) > 0)){
                            bestActions.add(this.getAgentActions()[action]);
                        }
                    }//end for.

                    //Convert it to array.
                    Action[] bestActionArray = new Action[bestActions.size()];
                    for(int index = 0; index < bestActions.size(); index++){
                        bestActionArray[index] = bestActions.get(index);
                    }

                    this.stateBestActionMaps[policyStateIndex] = bestActionArray;
                }else {
                    //TODO: Implementation for the Stochastic policy. LATER.
                }//end if-else.
            }//end while.
        }catch (FileNotFoundException f){
            f.printStackTrace();
            System.err.println("Policy Loading Error:" + f.getMessage());
            System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
            System.err.println("Policy Loading Error:" + e.getMessage());
            System.exit(0);
        }
    }//end method.





    //Getter and Setter Methods.
    public POOOSGDomain getDomain() {
        return domain;
    }

    public void setDomain(POOOSGDomain domain) {
        this.domain = domain;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentGroupName) {
        this.agentName = agentGroupName;
    }

    public Action[] getAgentActions() {
        return agentActions;
    }

    public void setAgentActions(Action[] agentActions) {
        this.agentActions = agentActions;
    }


    /**
     * This method would return all the actions of the agent in {@link Action} list format.
     * @return is the list of actions.
     */
    public List<Action> agentActions() {
        List<Action> actions = new ArrayList<>();

        for(Action action : this.agentActions){
            actions.add(action);
        }//end for.
        return actions;
    }//end method.


    public double[][] getStateActionProbMap() {
        return stateActionProbMap;
    }

    public void setStateActionProbMap(double[][] stateActionProbMap) {
        this.stateActionProbMap = stateActionProbMap;
    }

    public Action[][] getStateBestActionMaps() {
        return stateBestActionMaps;
    }

    public void setStateBestActionMaps(Action[][] stateBestActionMaps) {
        this.stateBestActionMaps = stateBestActionMaps;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType(PolicyType policyType) {
        this.policyType = policyType;
    }

    /**
     * This implementation of the action-policy gives best action for the current state. If there are more
     * than one action for any deterministic policy, the actions would be chosen at random.
     * @param s is the state value.
     * @return is the best action.
     */
    @Override
    public Action action(State s) {
        Action bestAction = null;
        if(this.getPolicyType() == PolicyType.Deterministic){
            Action[] bestActionList = this.getStateBestActionMaps()[s.hashCode()];
            if(bestActionList.length > 1){
                //Randomly choose action in case of more than one actions available.
                bestAction = (Action)bestActionList[(int) (Math.random() * bestActionList.length)];
            }else{
                bestAction = (Action)bestActionList[0];
            }
        }else {
            //TODO: Sampling for stochastic policy.
        }
        return bestAction;
    }


    //Not Used
    @Override
    public double actionProb(State s, Action a) {
        return 0;
    }

    //Not used.
    @Override
    public boolean definedFor(State s) {
        return false;
    }
}
