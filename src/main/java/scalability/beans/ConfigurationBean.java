/**
 * This class would create a building block for a FrameActionConfiguration.
 * @author:Maulik Shah
 */
package scalability.beans;

import burlap.mdp.core.action.Action;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class represents a frame-action-configuration in a state with anonymity settings. The configuration is made up of
 * agent's type, action and the number of agents falls into such criteria.
 */
public class ConfigurationBean implements Serializable{
    /**
     * Type of the agent. In terms of Wildfire : power of the agent.
     */
    int agentType;
    /**
     * Action being performed by the agents of type agentType in the context.
     */
    Action action;
    /**
     * Number of agents doing such actions having the type as agentType.
     */
    int numberOfAgents;


    /**
     * This constructor just instantiates the object.
     */
    public ConfigurationBean() {
        //Do-nothing.
    }

    /**
     * This constructor initiates the object with the given value.
     * @param agentType is the type of the agents.
     * @param action is the action performed by the agents.
     * @param numberOfAgents is the number of agents performing the @param action of being @param agentType type.
     */
    public ConfigurationBean(int agentType, Action action, int numberOfAgents) {
        this.agentType = agentType;
        this.action = action;
        this.numberOfAgents = numberOfAgents;
    }



    /**
     * This constructor initiates the object with the given value.
     * @param  configurationBean is the configuration bean to copy.
     */
    public ConfigurationBean(ConfigurationBean configurationBean) {
        this.agentType = configurationBean.getAgentType();
        this.action = configurationBean.getAction();
        this.numberOfAgents = configurationBean.getNumberOfAgents();
    }

    //Getter and Setter Methods.
    public int getAgentType() {
        return agentType;
    }

    public void setAgentType(int agentType) {
        this.agentType = agentType;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public int getNumberOfAgents() {
        return numberOfAgents;
    }

    public void setNumberOfAgents(int numberOfAgents) {
        this.numberOfAgents = numberOfAgents;
    }


    //Generated Equal method.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigurationBean)) return false;
        ConfigurationBean that = (ConfigurationBean) o;
        return this.agentType == that.agentType &&
                this.numberOfAgents == that.numberOfAgents &&
                this.action.actionName().equals(that.getAction().actionName());
    }

    @Override
    public String toString() {
        return "Config:" +
                 action.actionName() + "-" +
                 agentType + "-" +
                 numberOfAgents;
    }
}//end class.