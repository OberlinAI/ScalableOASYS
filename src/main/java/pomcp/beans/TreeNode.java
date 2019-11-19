package pomcp.beans;

import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.MutableState;
import burlap.mdp.core.state.State;
import domains.wildfire.FireObservation;
import domains.wildfire.beans.Fire;
import scalability.FrameActionConfiguration;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * The Node class represents a node in the I-POMCP algorithm, which contains 3 parameters
 * 1. Visitation count, 2. The value of the node and 3. The belief state which the node represents.
 * The Node object uses the object of the State, which can later be implemented with any State implementation,
 * and in this case, it would be implemented with the WildFireState.
 */
public class TreeNode implements MutableState,ObjectInstance{

    /**
     * Immediate reward.
     */
    private double  value;

    /**
     * Belief Particles.
     */
    private ArrayList<BeliefParticle> beliefParticles;

    /**
     * Path from the root, which can be considered as hashcode as well.
     * The path is made up of the branch index of each configuration, action and observations.
     * E.g. from root node if the current node can be reached by taking the 2nd configuration branch, 5th Action
     * branch and 0th observation branch, the path should be 2-5-0.
     */
    private String path;


    /**
     * History Element shows the relationship with the parent. In another words the annotated edged between the
     * parent and the child node, which would give all the intermediate nodes and children.
     */
    private ArrayList<HistoryElement> historyElement;



    /**
     * Class name and variable keys for the Node state class.
     */
    public static final String NODE_CLASS_NAME = "NODE";
    private static final String VALUE = "VALUE";
    private static final String BELIEF_PARTICLES = "BELIEF_PARTICLES";
    private static final String PATH = "PATH";
    private static final String HISTORY_ELEMENT = "HISTORY_ELEMENT";
    private static final String BRANCH_COUNT = "BRANCH_COUNT";
    private static final List<Object> VARIABLE_KEYS = Arrays.<Object>asList(VALUE, BELIEF_PARTICLES,PATH,HISTORY_ELEMENT,BRANCH_COUNT);



    /***
     * Default Constructor.
     * Initiates every member with either a zero or a null.
     */
    public TreeNode() {
        this.value = 0.0;
        this.historyElement = new ArrayList<>();
        this.beliefParticles = new ArrayList<>();
        this.path = "";
    }


    /**
     * Create a node from the all the variables available.
     * @param value is the value of this node.
     * @param beliefParticles is the list of belief particles.
     * @param historyElement is the list of history elements of the node.
     * @param path is the path from the root node to the current node.
     */
    public TreeNode(double value, ArrayList<BeliefParticle> beliefParticles,ArrayList<HistoryElement> historyElement,String path) {
        this.value = value;
        this.beliefParticles = beliefParticles;
        this.historyElement = historyElement;
        this.path = path;
    }


    /**
     * This constructor creates a node with the given values of the given object.
     * @param treeNode is the node to be copied.
     * @param includeHistory is a flag if the history elements are needed to be copied in the new node or not.
     */
    public TreeNode(TreeNode treeNode,boolean includeHistory) {
        this.value = treeNode.getValue();
        this.beliefParticles = treeNode.getBeliefParticles();
        this.path = treeNode.getPath();
        this.historyElement = new ArrayList<>();
        if(includeHistory) {
            for (HistoryElement historyElement: treeNode.getHistoryElement()){
                this.historyElement.add(new HistoryElement(historyElement));
            }//end for.
        }//end if.
    }//end constructor.

    //Getter and Setter Methods.
    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public ArrayList<HistoryElement> getHistoryElement() {
        return historyElement;
    }

    public void setHistoryElement(ArrayList<HistoryElement> historyElement) {
        this.historyElement = historyElement;
    }


    public ArrayList<BeliefParticle> getBeliefParticles() {
        return this.beliefParticles;
    }

    public void setBeliefParticles(ArrayList<BeliefParticle> beliefParticles) {
        this.beliefParticles = beliefParticles;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    //Over-ridden methods.
    @Override
    public String className() {
        return TreeNode.NODE_CLASS_NAME;
    }

    @Override
    public String name() {
        return hashCode()+"";
    }

    @Override
    public ObjectInstance copyWithName(String objectName) {
        return null;
    }

    @Override
    public List<Object> variableKeys() {
        return TreeNode.VARIABLE_KEYS;
    }

    @Override
    public Object get(Object variableKey) {
        String key = (String) variableKey;

        if(TreeNode.VALUE.equals(key)){
            return this.getValue();
        }else if(TreeNode.BELIEF_PARTICLES.equals(key)){
            return this.getBeliefParticles();
        }else if(TreeNode.HISTORY_ELEMENT.equals(key)){
            return this.getHistoryElement();
        }else{
            return  null;
        }//end if-else.
    }

    @Override
    public State copy() {
        return new TreeNode(this,true);
    }

    @Override
    public MutableState set(Object variableKey, Object value) {
        String key = (String) variableKey;
        if(TreeNode.VALUE.equals(key)){
            this.setValue((Double)value);
        }else if(TreeNode.BELIEF_PARTICLES.equals(key)){
            this.setBeliefParticles((ArrayList<BeliefParticle>) value);
        }else if(TreeNode.HISTORY_ELEMENT.equals(key)){
            this.setHistoryElement((ArrayList<HistoryElement>)value);
        }else if(TreeNode.PATH.equals(key)){
            this.setPath((String)value);
        }else{
            //DO nothing.
        }//end if-else.
        return  this;
    }

    /**
     * Randomly sample a particle from the list of particles available in the tree node.
     * @return a {@link BeliefParticle} of the node.
     */
     public BeliefParticle sampleParticle(){
        return this.beliefParticles.get((int)Math.abs(Math.random() * this.beliefParticles.size()));
     }//end method.


    /**
     * This method checks if the passed configuration has already been explored in the current tree or not. The
     * measurement criteria is only the current Configuration in the FAC class.
     * @param configuration is the passed configuration to verify.
     * @param isSingleConfigNode is if the I-POMCP tree would just contain one configuration node after a belief node.
     * @return is the index of the config in the history if it has already been explored, -1 otherwise.
     */
    public int configExist(FrameActionConfiguration configuration, boolean isSingleConfigNode){
        //If the I-POMCP tree has a single configuration node and if one history element exist for the tree node,
        //then always return 0, -1 otherwise.
        if(isSingleConfigNode){
            if(this.getHistoryElement() != null && this.getHistoryElement().size() != 0){
                return 0;
            }//end if.
        }else{
            //For each edge from the current tree node, compare if the config already exist.
            for(int history = 0 ; history < this.getHistoryElement().size(); history++){
                //Compare the current configurations in FAC. If the configuration matches with any
                //one in the historyElement than return true.
                if(this.getHistoryElement().get(history).getConfigurationNode().getFac().compareCurrentConfigs(configuration)){
                    return history;
                }//end if.
            }//end for.
        }//end if-else.



        //Configuration not found.
        return  -1;
    }


    /**
     * This method checks if in the current tree node, under the current configuration branch and action branch, the
     * observation exist or not.
     * @param facIndex is the branch index of the configuration.
     * @param actionIndex is the action index of the chosen action.
     * @param fireObservation is the fire observation to find.
     * @return index of the observation node branch which maches the @param - fireObservation, -1 if not found.
     */
    public int obsExist(int facIndex, int actionIndex, State fireObservation){
        //Get Observation nodes, iterate through them and match the observation.
        ArrayList<ObservationNode> observationNodes = this.getHistoryElement().get(facIndex)
                                                                        .getObservationNodes().get(actionIndex);
        for(int obsIndex=0; obsIndex < observationNodes.size(); obsIndex++){
           if(((FireObservation)fireObservation).equals(observationNodes.get(obsIndex).getObservation())){
               return  obsIndex;
           }//end if.
        }//end for.

        return  -1;
    }

    /**
     * This method calculates the total number of configuration sampled from immediate from the root node. It is done
     * by summing up counters of all the FAC nodes.
     * @return counter of all sampled configuration.
     */
    public int countSampledConfigs(){
        int counter = 0;

        //Go through all the branches.
        for(HistoryElement historyElement: this.getHistoryElement()){
            counter += historyElement.getConfigurationNode().getNodeVisit();
        }//end for.
        return  counter;
    }


    /**
     * This method compares the current Node object with that of the passed objects and returns
     * true or false based on the comparison.
     * @param o is the passed object.
     * @return true/false based on the comparison.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TreeNode)) return false;
        TreeNode that = (TreeNode) o;
        return Double.compare(that.getValue(), getValue()) == 0 &&
                Objects.equals(getBeliefParticles(), that.getBeliefParticles()) &&
                Objects.equals(getHistoryElement(), that.getHistoryElement()) &&
                getPath().equals(that.getPath());
    }



    /**
     * Calculate the hash code from the path.
     * @return is the hash value.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.path);
    }//end method.




    @Override
    public String toString() {
        return "BeliefNode{" +
                ":Hashcode=" + hashCode() +
                ":path=" + path +
                ":History Elements=" + historyElement.size() +
                ":belief size=" + beliefParticles.size() +
                '}';
    }
}
