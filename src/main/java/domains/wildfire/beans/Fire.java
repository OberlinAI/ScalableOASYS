/**
 *  {@link domains.wildfire.beans.Fire}  is the class to describe the fire in the premise.
 *  The Fire is described in the form of Fire number, Location and intensity.
 *  This class is being used further to describe WildFire state.
 * @author : Maulik Shah.
 */

package domains.wildfire.beans;

import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.State;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Fire implements ObjectInstance,Serializable{
    /**
     * Fire-Number of the fire.
     */
    private int fireNumber;

    /**
     * Fire-Location : (x,y) co-ordinate of fire described by Location object.
     */
    private Location fireLocation;

    /**
     * Fire-intensity is the intensity of the fire capped by max-fire and min-fire levels.
     */
    private int intensity;


    /**
     * Fire-Type is the type of the fire.
     */
    private int fireType;

    /**
     * Class name and variable keys for the state class.
     */
    public static final String FIRE_CLASS = "FIRE_CLASS";
    private static final String FIRE_NUM = "FIRE_NUM";
    private static final String FIRE_LOC = "FIER_LOC";
    private static final String FIRE_TYPE = "FIER_TYPE";
    private static final List<Object> VARIABLE_KEYS = Arrays.<Object>asList(FIRE_NUM, FIRE_LOC,FIRE_TYPE);

    /**
     * Default constructor - applies -1 to every member.
     */
    public Fire() {
        this.fireNumber = -1;
        this.fireLocation = new Location();
        this.intensity = -1;
        this.fireType = -1;
    }


    /**
     * Primary Constructor to initiate the fire.
     * @param fireNumber is the index of the fire in the premise.
     * @param fireLocation is the location of the fire in the premise.
     * @param intensity is the intensity of the particular fire.
     */
    public Fire(int fireNumber, Location fireLocation, int intensity, int fireType) {
        this.fireNumber = fireNumber;
        this.fireLocation = fireLocation;
        this.intensity = intensity;
        this.fireType = fireType;
    }

    /**
     * This constructor creates a new Fire object from the passed fire object.
     * @param fire is the fire object to be copied into the new one.
     */
    public Fire(Fire fire) {
        this.fireNumber = fire.getFireNumber();
        this.fireLocation = new Location(fire.getFireLocation());
        this.intensity = fire.getIntensity();
        this.fireType = fire.getFireType();
    }

    //Getter and Setter methods.

    public int getFireNumber() {
        return fireNumber;
    }

    public void setFireNumber(int fireNumber) {
        this.fireNumber = fireNumber;
    }

    public Location getFireLocation() {
        return fireLocation;
    }

    public void setFireLocation(Location fireLocation) {
        this.fireLocation = fireLocation;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public int getFireType() {
        return fireType;
    }

    public void setFireType(int fireType) {
        this.fireType = fireType;
    }

    @Override
    /**
     * This method will generate Fire as String.
     * output: e.g. Fire{fireNumber=5,fireLocation=Location{x=1,y=1},intensity=2}.
     * It also uses Location.toString() method intrinsically.
     * @return the Fire string.
     */
    public String toString() {
        return "Fire{" +
                "fireNumber=" + fireNumber +
                ": fireLocation=" + fireLocation +
                ": intensity=" + intensity +
                ": fireType=" + fireType +
                '}';
    }

    @Override
    /**
     * This method overrides the general equals method for the class.
     * It first checks if the object is of the Fire type or not.
     * If Yes, it tries to compare the members and if they equals each other, then return true.
     * return false other wise.
     * This method intrinsically uses the equals method of the Location class.
     * @param o is the any input object that is needed to be compared with other object.
     * @return True or False on the basis of object matching.
     */
    public boolean equals(Object o) {
        //Check if the passed object is the current object.
        if (this == o) return true;
        //Check if the passed object is instance of Fire or not.
        if (!(o instanceof Fire)) return false;
        //Convert the object to Fire object and compare each member.
        Fire fire = (Fire) o;
        return this.fireNumber == fire.fireNumber &&
                this.intensity == fire.intensity &&
                this.fireLocation.equals(fire.fireLocation) &&
                this.fireType == fire.fireType;
    }

    /**
     * This method returns the fire-action name from the current fire object into
     * "X[X-location]Y[Y-location]" eg. X1Y2
     * @return
     */
    public String fireActionName(){
        return "X"+this.getFireLocation().getX()+"Y"+this.getFireLocation().getY();
    }

    @Override
    public String className() {
        return Fire.FIRE_CLASS;
    }

    @Override
    public String name() {
        return this.getFireNumber()+"";
    }

    @Override
    public ObjectInstance copyWithName(String objectName) {
        return null;
    }

    @Override
    public List<Object> variableKeys() {
        return Fire.VARIABLE_KEYS;
    }

    @Override
    public Object get(Object variableKey) {
        String key = (String) variableKey;

        if(Fire.FIRE_NUM.equals(key)){
            return this.getFireNumber();
        }else if(Fire.FIRE_LOC.equals(key)){
            return this.fireLocation;
        }else if(Fire.FIRE_TYPE.equals(key)){
            return this.fireType;
        }else{
            return  null;
        }//end if-else.
    }

    @Override
    public State copy() {
        return new Fire(this);
    }

    @Override
    public int hashCode() {

        return Objects.hash(fireNumber, fireLocation, intensity);
    }
}
