/**
 *  {@link domains.wildfire.beans.Location}  is the made up of X and Y co-ordinates, which can be an indicator of a location of any object.
 *  This class is being used further to describe Fire and Agent locations.
 * @author : Maulik Shah.
 */


package domains.wildfire.beans;


import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.State;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class Location implements ObjectInstance,Serializable {
    /**
     * X- co-ordinate of the location.
     */
    private int x;

    /**
     * Y- co-ordinate of the location.
     */
    private int y;

    /**
     * Class name and variable keys for the state class.
     */
    public static final String LOC_CLASS = "LOC_CLASS";
    private static final String X = "X";
    private static final String Y = "Y";
    private static final List<Object> VARIABLE_KEYS = Arrays.<Object>asList(X,Y);

    /**
     * Default Constructor. Setting all the location to -1,-1.
     */
    public Location() {
        this.x = -1;
        this.y = -1;
    }

    /**
     * Create a new location with desired parameters.
     * @param x is the x- co-ordinate.
     * @param y is the y- co-ordinate.
     */
    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * This constructor creates a new location object from the given one.
     * @param location is the location object to be copied.
     */
    public Location(Location location) {
        this.x = location.x;
        this.y = location.y;
    }

    //Getter and Setter methods.

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }


    @Override
    /**
     * This method will convert Location object into string. output e.g. Location{x=1,y=1}.
     * @return the Location string.
     */
    public String toString() {
        return "Location{" +
                "x=" + x +
                ": y=" + y +
                '}';
    }

    @Override
    /**
     * This method overrides the general equals method for the class.
     * It first checks if the object is of the Location type or not.
     * If Yes, it tries to compare the members and if they equals each other, then return true.
     * return false other wise.
     * @param o is the any input object that is needed to be compared with other object.
     * @return True or False on the basis of object matching.
     */
    public boolean equals(Object o) {
        //Check if the passed object is the current object.
        if (this == o) return true;
        //If o is not instance of the Location. Return False.
        if (!(o instanceof Location)) return false;
        //If the object is of location. Compare and return the final result.
        Location location = (Location) o;
        return x == location.x &&
                y == location.y;
    }

    @Override
    public String className() {
        return Location.LOC_CLASS;
    }

    @Override
    public String name() {
        return this.getX()+"-"+this.getY();
    }

    @Override
    public ObjectInstance copyWithName(String objectName) {
        return null;
    }

    @Override
    public List<Object> variableKeys() {
        return Location.VARIABLE_KEYS;
    }

    @Override
    public Object get(Object variableKey) {
        String key = (String) variableKey;

        if(Location.X.equals(key)){
            return this.getX();
        }else if(Location.Y.equals(key)){
            return this.getY();
        }else{
            return  null;
        }//end if-else.
    }

    @Override
    public State copy() {
        return null;
    }

    @Override
    public int hashCode() {

        return Objects.hash(x, y);
    }
}
