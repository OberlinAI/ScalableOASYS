package common;

import burlap.behavior.policy.Policy;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.world.World;
import posg.model.FACRewardFunction;




/**
 * An agent class to contain pre-calculated policies for agents in a partially observable domain.
 */
public class PlannedPOSGAgent implements SGAgent {


    /**
     * Agent Group.
     */
    protected String agentName;


    /**
     * Agent type.
     */
    protected SGAgentType agentType;

    /**
     * Policy of the agent.
     * The policy object used here is {@link OtherAgentsNMDPPolicy}, which contains major information about the
     * domain and the current agent.
     */
    Policy agentPolicy;


    /**
     * Reward Function.
     */
    protected FACRewardFunction rewardFunction;


    /**
     * Constructor for populating all the information for an agent.
     * @param agentName is the name or agent number of the agent.
     * @param agentType is the agent-type.
     * @param agentPolicy is the learned policy for the domain.
     * @param rewardFunction is the reward function for an agent.
     */
    public PlannedPOSGAgent(String agentName, SGAgentType agentType, Policy agentPolicy,
                            FACRewardFunction rewardFunction) {
        this.agentName = agentName;
        this.agentType = agentType;
        this.agentPolicy = agentPolicy;
        this.rewardFunction = rewardFunction;
    }

    @Override
    public String agentName() {
        return this.agentName;
    }

    @Override
    public SGAgentType agentType() {
        return this.agentType;
    }

    @Override
    public void gameStarting(World w, int agentNum) {
        //Do Nothing.
    }

    /**
     * Get the state to action mapping from the policy.
     * @param s is the current state of the premise.
     * @return is the action to perform in this state from the given policy.
     */
    @Override
    public Action action(State s) {
        return  this.agentPolicy.action(s);
    }

    @Override
    public void observeOutcome(State s, JointAction jointAction, double[] jointReward, State sprime, boolean isTerminal) {
        //Nothing to do.
    }

    @Override
    public void gameTerminated() {
        //Do nothing.
    }
}
