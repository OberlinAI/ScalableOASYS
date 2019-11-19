package pomcp.beans;

import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.State;
import scalability.FrameActionConfiguration;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This class is a node after sampling a FAC from the tree node. The node contains the visitation count from the
 * parent.
 */
public class FACNode implements ObjectInstance,Serializable {

    /**
     * Sampled FAC from the {@link TreeNode}.
     */
    FrameActionConfiguration fac;
    /**
     * Number of visits for the current node.
     */
    int nodeVisit;


    /**
     * Class name and variable keys for the Node state class.
     */
    public static final String CLASS_NAME = "FAC_NODE";
    private static final String FAC = "FAC";
    private static final String VISITS = "VISITS";
    private static final List<Object> VARIABLE_KEYS = Arrays.<Object>asList(VISITS,FAC);

    /**
     * Default Constructor.
     */
    public FACNode() {
        this.fac = null;
        this.nodeVisit = 0;
    }

    /**
     * Copy the content from the FAC node.
     * @param fac is the {@link FrameActionConfiguration} of the node.
     * @param nodeVisit is the visit count.
     */
    public FACNode(FrameActionConfiguration fac, int nodeVisit) {
        this.fac = fac;
        this.nodeVisit = nodeVisit;
    }

    /**
     * Copy an agent node to the current one.
     * @param facNode is an FACNode.
     */
    public FACNode(FACNode facNode) {
        this.fac = facNode.getFac();
        this.nodeVisit = facNode.getNodeVisit();
    }


    //Getter and Setter methods.

    public FrameActionConfiguration getFac() {
        return fac;
    }

    public void setFac(FrameActionConfiguration fac) {
        this.fac = fac;
    }

    public int getNodeVisit() {
        return nodeVisit;
    }

    public void setNodeVisit(int nodeVisit) {
        this.nodeVisit = nodeVisit;
    }

    /**
     * This method increases the current visitation count by 1.
     */
    public void incrementVisit() {
        this.nodeVisit = this.nodeVisit + 1;
    }

    @Override
    public String className() {
        return FACNode.CLASS_NAME;
    }

    @Override
    public String name() {
        return this.hashCode()+"";
    }

    @Override
    public ObjectInstance copyWithName(String objectName) {
        return null;
    }

    @Override
    public List<Object> variableKeys() {
        return FACNode.VARIABLE_KEYS;
    }

    @Override
    public Object get(Object variableKey) {
        String key = (String) variableKey;

        if(FACNode.FAC.equals(key)){
            return this.getFac();
        }else if(FACNode.VISITS.equals(key)){
            return this.getNodeVisit();
        }else{
            return  null;
        }//end if-else.
    }

    @Override
    public State copy() {
        return new FACNode(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FACNode)) return false;
        FACNode facNode = (FACNode) o;
        return getNodeVisit() == facNode.getNodeVisit() &&
                Objects.equals(getFac(), facNode.getFac());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getFac(), getNodeVisit());
    }

    @Override
    public String toString() {
        return "Configuration Node{" +
                "Config=" + fac.printCurrentConfig() +
                ":nodeVisit=" + nodeVisit +
                '}';
    }
}
