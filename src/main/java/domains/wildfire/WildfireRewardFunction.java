package domains.wildfire;


import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.SimpleAction;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.oo.OOSGDomain;
import domains.wildfire.beans.Agent;
import domains.wildfire.beans.Fire;
import posg.model.FACRewardFunction;
import scalability.FrameActionConfiguration;

public class WildfireRewardFunction implements FACRewardFunction {
    /**
     * Returns the reward received by each agent specified in the joint action.
     * @param s that state in which the joint action was taken.
     * @param ja the joint action taken.
     * @param sp the resulting state from taking the joint action
     * @return reward array. Here, the index of the array is the agent number.
     */
    @Override
    public double[] reward(State s, JointAction ja, State sp) {
        double[] rewards = new double[ja.size()];

        // count the number of locations not on fire
        WildfireState wsCurrent = (WildfireState) sp;
        WildfireState wsPrevious = (WildfireState) s;

        int sharedReward = 0;
//        Add Burnout Penalty for each burned out fire.
        for (int fireIndex = 0 ; fireIndex < wsCurrent.getFireList().size() ; fireIndex++) {
            Fire previousFire = wsPrevious.getFireList().get(fireIndex);
            Fire fire = wsCurrent.getFireList().get(fireIndex);

            //Penalty only once for going to burnout.
            if(fire.getIntensity() == WildfireParameters.MAX_FIRE_STATES-1 &&
                    previousFire.getIntensity() != WildfireParameters.MAX_FIRE_STATES-1){
                sharedReward += WildfireParameters.FIRE_BURNOUT_PENALTY;
            }

            //Reward once for extinguishing.
            if (fire.getIntensity() == 0  && previousFire.getIntensity() != 0){
                if(fire.getFireType() == WildfireParameters.BIG_FIRE){
                    sharedReward += WildfireParameters.NO_FIRE_REWARD_BIG;
                }else if(fire.getFireType() == WildfireParameters.VERY_BIG_FIRE){
                    sharedReward += WildfireParameters.NO_FIRE_REWARD_VB;
                }else{
                    sharedReward += WildfireParameters.NO_FIRE_REWARD_SMALL;
                }
            }
        }//end for.

        int available = 0;

        // Refer to the previous state to find the availability of the agents.

        for (int actionIndex = 0 ; actionIndex < ja.size();actionIndex++) {
            //Convert into simple action.
            SimpleAction simpleAction = (SimpleAction)ja.action(actionIndex);

            int reward = sharedReward;

            //Check if the action was not NO operation action.
            if (!WildfireParameters.NOOP_ACTION.equals(simpleAction.actionName())) {


                //Find an agent's availability from the state.
                //Here the loop is used in case any agent is out of sequence.
                if (wsPrevious.getSelfAgent().getAgentNumber() != WildfireParameters.MASTER_STATE_AGENT_NUM
                                            && actionIndex == wsPrevious.getSelfAgent().getAgentNumber()) {
                    //Get Availability.
                    available = wsPrevious.getSelfAgent().getAvailability();
                } else {
                    for (Agent agent : wsPrevious.getAgentList()) {
                        if (actionIndex == agent.getAgentNumber()) {
                            available = agent.getAvailability();
                            break;
                        }
                    }
                }//end if-else.

                // did the agent try to act when it is unavailable?
                if (available == 0) {
                    reward += WildfireParameters.NO_FIRE_PENALTY_CURRENT;
                } else {
                    for (Fire fire: wsPrevious.getFireList()) {
                        //If the fire is nill or in the burned out state.
                        //and agent is still fighting that fire then add penalty.
                        if ((0 == fire.getIntensity()
                                || WildfireParameters.MAX_FIRE_STATES - 1 == fire.getIntensity())
                                && simpleAction.actionName().equals(fire.fireActionName())) {
                            reward += WildfireParameters.NO_FIRE_PENALTY_CURRENT;
                        }//end if.
                    }//end for.
                }//end if-else.
            }//end if no-op.

//				System.out.println(agent + " - Rew: "+ reward);
            rewards[actionIndex] = reward;
        }//end for.

        return rewards;
    }//end method.

    /**
     * This method would give the rewards given the current agent action and the action-configuration of the other agents.
     * The calculations are the same except the negative reward for fighting a fire when empty suppressant is only applied to the
     * current agent.
     * @param s is the previous state.
     * @param currentAgentAction is the current agent action.
     * @param fac is the frame action configuration of the other agent actions.
     * @param sp is the next state.
     * @return is the sum of the reward, which is only related to the current agent.
     */
    @Override
    public double reward(State s, Action currentAgentAction, FrameActionConfiguration fac, State sp) {
        double rewards = 0;

        // count the number of locations not on fire
        // from the resulting state.
        WildfireAnonymousState resultingState = (WildfireAnonymousState) sp;
        WildfireAnonymousState previousState = (WildfireAnonymousState) s;


        //Check if any fire is at level 0, then increase the reward by 1 for each agent.
        for (int fireIndex = 0 ; fireIndex < resultingState.getFireList().length ; fireIndex++) {
            Fire previousFire = previousState.getFireList()[fireIndex];
            Fire fire = resultingState.getFireList()[fireIndex];

            if(fire.getIntensity() == WildfireParameters.MAX_FIRE_STATES-1
                    && previousFire.getIntensity() != WildfireParameters.MAX_FIRE_STATES -1){
                rewards += WildfireParameters.FIRE_BURNOUT_PENALTY ;
            }

            if (fire.getIntensity() == 0 && previousFire.getIntensity() != 0){
                //No fire reward is for each agent including the current agent.
                if(fire.getFireType() == WildfireParameters.BIG_FIRE){
                    rewards +=  WildfireParameters.NO_FIRE_REWARD_BIG;
                }else if(fire.getFireType() == WildfireParameters.VERY_BIG_FIRE){
                    rewards +=  WildfireParameters.NO_FIRE_REWARD_VB;
                }else{
                    rewards +=  WildfireParameters.NO_FIRE_REWARD_SMALL;
                }
            }//end if.
        }//end for.


        //If the current agent's action is not NO-OP and it's suppressant level is empty
        //then add the penalty to the agent. The state referred is the previous state.
        if(!(currentAgentAction.actionName().equals(WildfireParameters.NOOP_ACTION))){
            if(previousState.getSelfAgent().getAvailability() == 0){
                rewards += WildfireParameters.NO_FIRE_PENALTY_CURRENT;
            }//end if.
        }//end if.


        //Also, if the intensity is 0 , or burn-out and the current agent is still fighting the fire ,
        for (Fire fire: previousState.getFireList()) {
            if (fire.getIntensity() == 0 || fire.getIntensity() == WildfireParameters.MAX_FIRE_STATES-1){
                if(previousState.getSelfAgent().getAvailability() != 0 &&
                        currentAgentAction.actionName().equals(fire.fireActionName())){
                    rewards += WildfireParameters.NO_FIRE_PENALTY_CURRENT;
                }
            }//end if.
        }//end for.

        return rewards;
    }//end method.


    /**
     * Same as previous method, except you don't have current agent's action, so just passing
     * NOOP for that parameter.
     * @param s is the previous state.
     * @param fac is the frame action configuration of the other agent actions.
     * @param sp is the next state.
     * @return overall reward.
     */
    @Override
    public double reward(State s,FrameActionConfiguration fac, State sp) {
        return reward(s, new SimpleAction(WildfireParameters.NOOP_ACTION),fac,sp);
    }


    /**
     * This method is gets reward for each agent given the Joint Action. The FAC would be also
     * helpful reward calculation if used.
     * @param s is the previous state.
     * @param jointAction is the joint action of all the agents.
     * @param fac is the frame action configuration of the other agent actions.
     * @param sp is the next state.
     * @return
     */
    @Override
    public double[] reward(State s, JointAction jointAction, FrameActionConfiguration fac, State sp) {
        //Calculate reward for each agent.
        return reward(s,jointAction,sp);
    }

    /**
     * This method calculates the maximum reward can be achieved from the given anonymous state.
     * The calculation is very simple,the best possible scenario where all the fires are extinguished and
     * no penalty.
     * So, Max Reward = (Reward for a fire extinguished) * (No. of fires)
     * @param maxState is the master anonymous state in consideration.
     * @param domain is the wildfire domain object to decide the max reward.
     * @return is the maximum reward.
     */
    public double getMaxReward(State maxState,OOSGDomain domain) throws ClassCastException{
        double maxReward = 0;//Max Reward

        WildfireDomain wildfireDomain = (WildfireDomain)domain;

         //The state must be anonymous state.
         if(! (maxState instanceof WildfireAnonymousState)){
             throw new ClassCastException("State mismatch.");
         }//end if.

        WildfireAnonymousState wildfireAnonymousState = (WildfireAnonymousState)maxState;
        //Get the maximum shared reward.
        for(Fire fire: wildfireAnonymousState.getFireList()){
            if(fire.getFireType() == WildfireParameters.BIG_FIRE){
                maxReward +=  WildfireParameters.NO_FIRE_REWARD_BIG ;
            }else if(fire.getFireType() == WildfireParameters.VERY_BIG_FIRE){
                maxReward +=  WildfireParameters.NO_FIRE_REWARD_VB ;
            }else{
                maxReward +=  WildfireParameters.NO_FIRE_REWARD_SMALL ;
            }
        }//end for.


        return maxReward;
    }//end method.


    /**
     * This method calculates the upper bound for the reward function given the current master state and
     * the gamma value.
     * The upper bound equation is UB = R_max / (1 - gamma).
     * In case gamma is 1, then return R_max.
     * @param maxState is the current anonymous state.
     * @param domain is the domain object.
     * @param gammaValue is the gamma value.
     * @return upper bound.
     * @throws ClassCastException if the state object is not an instance of the Wildfire Anonymous class.
     */
    @Override
    public double getRewardUpperBound(State maxState, OOSGDomain domain,double gammaValue) throws ClassCastException{
        if (gammaValue != 0){
            return getMaxReward(maxState,domain)/(1-gammaValue);
        }else{
            return getMaxReward(maxState,domain);
        }
    }//end method.
}//end class.
