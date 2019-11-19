package posg.environment;


import burlap.mdp.auxiliary.StateGenerator;
import burlap.mdp.auxiliary.common.ConstantStateGenerator;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.environment.EnvironmentOutcome;
import burlap.mdp.singleagent.environment.extensions.EnvironmentObserver;
import burlap.mdp.singleagent.environment.extensions.EnvironmentServerInterface;
import burlap.mdp.singleagent.environment.extensions.StateSettableEnvironment;


import java.util.LinkedList;
import java.util.List;

import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.model.JointModel;
import posg.POOOSGDomain;


public class SimulatedSGEnvironment implements StateSettableEnvironment, EnvironmentServerInterface {

    protected JointModel model;


    /**
     * The state generator used to generate new states when the environment is reset with {@link #resetEnvironment()};
     */
    protected StateGenerator stateGenerator;

    /**
     * The current state of the environment
     */
    protected State curState;

    /**
     * The last reward generated from this environment.
     */
    protected double lastReward = 0.;

    protected boolean terminated = false;

    /**
     * A flag indicating whether the environment will respond to actions from a terminal state. If false,
     * then once a the environment transitions to a terminal state, any action attempted by the {@link #executeAction(Action)}
     * method will result in no change in state and to enable action again, the Environment state will have to be
     * manually changed with the {@link #resetEnvironment()} method or the {@link #setCurStateTo(State)} method.
     * If this value is true, then actions will be carried out according to the domain's transition dynamics.
     */
    protected boolean allowActionFromTerminalStates = false;


    /**
     * The {@link EnvironmentObserver} objects that will be notified of {@link burlap.mdp.singleagent.environment.Environment}
     * events.
     */
    protected List<EnvironmentObserver> observers = new LinkedList<EnvironmentObserver>();



    public SimulatedSGEnvironment(POOOSGDomain domain){
        if(domain.getJointActionModel() == null){
            throw new RuntimeException("SimulatedSGEnvironment requires a Domain with a model, but the input domain does not have one.");
        }
        this.model = domain.getJointActionModel();
    }

    public SimulatedSGEnvironment(POOOSGDomain domain, State initialState) {

        this.stateGenerator = new ConstantStateGenerator(initialState);
        this.curState = initialState;
        if(domain.getJointActionModel() == null){
            throw new RuntimeException("SimulatedSGEnvironment requires a Domain with a model, but the input domain does not have one.");
        }
        this.model = domain.getJointActionModel();
    }

    public SimulatedSGEnvironment(POOOSGDomain domain, StateGenerator stateGenerator) {
        this.stateGenerator = stateGenerator;
        this.curState = stateGenerator.generateState();
        if(domain.getJointActionModel() == null){
            throw new RuntimeException("SimulatedSGEnvironment requires a Domain with a model, but the input domain does not have one.");
        }
        this.model = domain.getJointActionModel();
    }

    public SimulatedSGEnvironment(JointModel model){
        this.model = model;
    }

    public SimulatedSGEnvironment(JointModel model, State initialState) {

        this.stateGenerator = new ConstantStateGenerator(initialState);
        this.curState = initialState;
        this.model = model;
    }

    public SimulatedSGEnvironment(JointModel model, StateGenerator stateGenerator) {
        this.stateGenerator = stateGenerator;
        this.curState = stateGenerator.generateState();
        this.model = model;
    }



    public StateGenerator getStateGenerator() {
        return stateGenerator;
    }

    public void setStateGenerator(StateGenerator stateGenerator) {
        this.stateGenerator = stateGenerator;
    }

    @Override
    public void addObservers(EnvironmentObserver... observers) {
        for(EnvironmentObserver o : observers){
            this.observers.add(o);
        }
    }

    @Override
    public void clearAllObservers() {
        this.observers.clear();
    }

    @Override
    public void removeObservers(EnvironmentObserver... observers) {
        for(EnvironmentObserver o : observers){
            this.observers.remove(o);
        }
    }

    @Override
    public List<EnvironmentObserver> observers() {
        return this.observers;
    }

    /**
     * Sets whether the environment will respond to actions from a terminal state. If false,
     * then once a the environment transitions to a terminal state, any action attempted by the {@link #executeAction(Action)}
     * method will result in no change in state and to enable action again, the Environment state will have to be
     * manually changed with the {@link #resetEnvironment()} method or the {@link #setCurStateTo(State)} method.
     * If this value is true, then actions will be carried out according to the domain's transition dynamics.
     * @param allowActionFromTerminalStates if false, then actions are not allowed from terminal states; if true, then they are allowed.
     */
    public void setAllowActionFromTerminalStates(boolean allowActionFromTerminalStates){
        this.allowActionFromTerminalStates = true;
    }

    @Override
    public void setCurStateTo(State s) {
        if(this.stateGenerator == null){
            this.stateGenerator = new ConstantStateGenerator(s);
        }
        this.curState = s;
    }

    @Override
    public State currentObservation() {
        return this.curState.copy();
    }

    @Override
    public EnvironmentOutcome executeAction(Action a) {
        return null;
    }


    /**
     * Current implementation of the execute action to fit the criteria.
     * @param jointAction is the joint action.
     * @return updated state.
     */
    public State executeAction(JointAction jointAction) {

//        for(EnvironmentObserver observer : this.observers){
//            observer.observeEnvironmentActionInitiation(this.currentObservation(), jointAction);
//        }

        State output;
        if(this.allowActionFromTerminalStates || !this.isInTerminalState()) {
            output = model.sample(this.curState, jointAction);
        }
        else{
            output = this.curState.copy();
        }

        return output;
    }

    @Override
    public double lastReward() {
        return this.lastReward;
    }

    @Override
    public boolean isInTerminalState() {
        return this.terminated;
    }

    @Override
    public void resetEnvironment() {
        this.lastReward = 0.;
        this.terminated = false;
        this.curState = stateGenerator.generateState();
        for(EnvironmentObserver observer : this.observers){
            observer.observeEnvironmentReset(this);
        }
    }
}