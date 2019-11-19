package common;


import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.world.World;
import pomcp.IPOMCP;


/**
 * An IPOMCP agent, which has an IPOMCP behavior.
 */
public class IPOMCPAgent implements SGAgent {
    /**
     * Agent Group.
     */
    protected String agentName;

    /**
     * IPOMCP Solver object.
     */
    IPOMCP ipomcp;


    /**
     * Constructor for populating all the information for an agent.
     * @param agentName is the name or agent number of the agent.
     * @param ipomcp is the IPOMCP solver object.
     */
    public IPOMCPAgent(String agentName, IPOMCP ipomcp) {
        this.agentName = agentName;
        this.ipomcp = ipomcp;
    }

    @Override
    public String agentName() {
        return this.agentName;
    }

    @Override
    public SGAgentType agentType() {
        return null;
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
        try{
            return  ipomcp.searchTree(s);
        }catch (Exception e){
            System.out.println("IPOMCP algorithm has some errors. ERROR:" + e.getMessage());
        }
        return  null;
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
