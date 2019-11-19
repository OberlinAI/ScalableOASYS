/**
 * This interface is an extension to all the methods in the FullJointModel to use the FrameActionConfiguration.
 * @author: Maulik Shah.
 */
package posg.model;

import burlap.mdp.core.Domain;
import burlap.mdp.core.StateTransitionProb;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.model.FullJointModel;

import pomcp.beans.TreeNode;
import scalability.FrameActionConfiguration;

import java.util.List;

public interface FullJointIPOMCPModel extends FullJointModel {

    /**
     * This method converts the JointAction object into a FrameActionConfiguration.
     * JointAction object has an array list of actions and the index of this array list is the agent number, while
     * the FrameActionConfiguration object would contain an array of the (Action-Agent Type) pairs and their counts.
     * @param ja is the JointAction object for the domain.
     * @param wf is the current domain object.
     * @return FrameActionConfiguration with required conversation.
     */
    FrameActionConfiguration convertJAtoFAC(JointAction ja, Domain wf);


    /**
     * Replacement of the actionHelper method in the previous version.
     * Sample the next output state by generating the transition probability randomly and use the FrameActionConfiguration
     * instead of the Joint Action.
     * @param s is the current state.
     * @param fac is the FrameActionConfiguration.
     * @return next sampled state.
     */
    State sample(State s, FrameActionConfiguration fac);


    /**
     * This method returns the state transition probabilities for the next state given the current state and the
     * FAC. The method first calculates the Fire Transition and then the self-agent transitions.
     * @param s is the current state.
     * @param fac is the Frame-Action-Configuration happened.
     * @return probabilities for the next state.
     */
    List<StateTransitionProb> stateTransitions(State s, FrameActionConfiguration fac);


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
    State sample(State state, TreeNode node, int branchNumber, int actionNumber);


    /**
     * Sample the next internal state for the agent given the current state and action.
     * @param currentState is the current internal state.
     * @param action is the action performed by the agent.
     * @return sampled next Internal state after the transition.
     */
    int sampleInternalTransition(int currentState, Action action);

}
