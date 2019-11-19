package pomcp.beans;

import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.MutableState;
import burlap.mdp.core.state.State;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * The I-POMCP Tree is an solution exploration tree for an agent in a Partially observable multi-agent structure and
 * which is an extension to the POMCP which deals in a single agent domain. The I-POMCP tree includes the frame-action
 * configuration of all the other agents in the domain as an extra exploration step from the POMCP.
 */
public class IPOMCPTree implements MutableState,ObjectInstance{

    /**
     * The current hash-map saves all the node that exist in the tree, with a unique hash code.
     * The BeleifNode contains information about the parent and children of a node, by referencing to the
     * hashcode of the hash map.
     */
    HashMap<Integer,TreeNode> beliefTree;


    /**
     * Class name and variable keys for the Node state class.
     */
    public static final String TREE_CLASS_NAME = "TREE";
    private static final String BELIEF_TREE = "BELIEF_TREE";
    private static final List<Object> VARIABLE_KEYS = Arrays.<Object>asList(BELIEF_TREE);


    //Constructors.
    public IPOMCPTree() {
        this.beliefTree = new HashMap<Integer,TreeNode>();
    }

    public IPOMCPTree(IPOMCPTree ipomcpTree) {
        this.beliefTree = ipomcpTree.getBeliefTree();
    }


    //Getter and Setter Method.

    public HashMap<Integer, TreeNode> getBeliefTree() {
        return beliefTree;
    }

    public void setBeliefTree(HashMap<Integer, TreeNode> beliefTree) {
        this.beliefTree = beliefTree;
    }

    @Override
    public String className() {
        return TREE_CLASS_NAME;
    }

    @Override
    public String name() {
        return TREE_CLASS_NAME;
    }

    @Override
    public ObjectInstance copyWithName(String objectName) {
        return null;
    }

    @Override
    public MutableState set(Object variableKey, Object value) {
        String key = (String) variableKey;
        if(IPOMCPTree.BELIEF_TREE.equals(key)){
            this.setBeliefTree((HashMap<Integer, TreeNode>) value);
        }else{
            //DO nothing.
        }//end if-else.
        return  this;
    }

    @Override
    public List<Object> variableKeys() {
        return IPOMCPTree.VARIABLE_KEYS;
    }

    @Override
    public Object get(Object variableKey) {
        String key = (String) variableKey;

        if(IPOMCPTree.BELIEF_TREE.equals(key)){
            return this.getBeliefTree();
        }else{
            return  null;
        }//end if-else.
    }

    @Override
    public State copy() {
        return new IPOMCPTree(this);
    }


    /**
     * This method adds or updates the current node in the list using the hashcode of that node.
     * @param node is the node to update.
     */
    public void setNode(TreeNode node){
        this.getBeliefTree().put(node.hashCode(),node);
    }


    /**
     * This method adds or updates the current node in the list using the hashcode of that node.
     * @param hashcode is the hash code for the node.
     */
    public TreeNode getNode(Integer hashcode){
        return this.beliefTree.get(hashcode);
    }

    /**
     * Clears the tree to conserve memory.
     */
    public void clear() {
        this.beliefTree.clear();
    }

}
