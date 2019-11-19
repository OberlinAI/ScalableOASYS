package posg.model;

import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.mdp.stochasticgames.oo.OOSGDomain;
import scalability.FrameActionConfiguration;

public interface FACRewardFunction extends JointRewardFunction {
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
     public double reward(State s, Action currentAgentAction, FrameActionConfiguration fac, State sp);


    /**
     * This method would give the rewards given  the actions-configuration of the all the agents.
     * The calculations are the same except the negative reward for fighting a fire when empty suppressant is not applied
     * to anyone.
     * @param s is the previous state.
     * @param fac is the frame action configuration of the other agent actions.
     * @param sp is the next state.
     * @return is the sum of the reward, which is only related to the current agent.
     */
    public double reward(State s,  FrameActionConfiguration fac, State sp);

    /**
     * This method would give the rewards given to the each agent, in case of availability of the FAC and
     * Joint Action Class.
     * @param s is the previous state.
     * @param jointAction is the joint action of all the agents.
     * @param fac is the frame action configuration of the other agent actions.
     * @param sp is the next state.
     * @return is an array of reward for each agent.
     */
    public double[] reward(State s, JointAction jointAction, FrameActionConfiguration fac, State sp);



    /**
     * This method would return the maximum possible reward upper bound in the given state using gamma value.
     * The equation can be given as RewardUpperBound = Reward_max / 1 -gamma.
     * @param maxState is the current  state.
     * @param domain domain object to decide the maximum reward.
     * @param gammaValue is the gamma value.
     * @return upper bound.
     * @throws ClassCastException if the state object is not an instance of the specific State object.
     */
     public double getRewardUpperBound(State maxState, OOSGDomain domain,double gammaValue) throws ClassCastException;
}
