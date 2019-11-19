package domains.wildfire;

/**
 * The {@link WildfireSpreadModel} class encapsulates the wildfire spread
 * parameters and probabilities, based on:
 * Boychuk, D. et al. 2009. A stochastic forest fire growth model.
 * Environment and Ecological Statistics. 16(2): 133-151.
 * 
 * @author Adam Eck
 * @author Maulik Shah - Scalable OASYS.
 */
public class WildfireSpreadModel {

    /** The size of a cell in meters. */
    public final double cellSize;
    
    /** The direction of the wind. */
    /* Note: 0 = North, PI = SOUTH, 1/2 PI = EAST, 3/2 PI = WEST */
    public final double windDirection;
    
    /** The ignition probability to the north of a burning cell. */
    public final double northIgnitionProb;
    
    /** The ignition probability to the east of a burning cell. */
    public final double eastIgnitionProb;
    
    /** The ignition probability to the south of a burning cell. */
    public final double southIgnitionProb;
    
    /** The ignition probability to the west of a burning cell. */
    public final double westIgnitionProb;
    
    /** The burnout probability of a cell. */
    public final double burnoutProb;
    
    /** The base spread rate \lambda^b. */
    public static final double BASE_SPREAD_RATE = 3.0;
    
    /** The maximum spread rate \lambda^m. */
    public static final double MAX_SPREAD_RATE = 67.0;
    
    /**
     * Constructs a new {@link WildfireSpreadModel}, which calculates the ignition probability of spreading fire to
     * some other locations.
     * @param cellSize The size of a cell in meters.
     * @param windDirection The direction of the wind ( 0 = North, PI = SOUTH, 1/2 PI = EAST, 3/2 PI = WEST).
     */
    public WildfireSpreadModel(double cellSize, double windDirection) {
        // save the cell size and wind direction
        this.cellSize = cellSize;
        this.windDirection = windDirection;
        
        // calculate the ignition probabilities
        //TODO: Put equations.
        northIgnitionProb =  (BASE_SPREAD_RATE) / 
                (cellSize * (1 - Math.cos(0 - windDirection) * (1 - BASE_SPREAD_RATE / MAX_SPREAD_RATE)));
        eastIgnitionProb = (BASE_SPREAD_RATE) 
                / (cellSize * (1 - Math.cos(0.5 * Math.PI - windDirection) * (1 - BASE_SPREAD_RATE / MAX_SPREAD_RATE)));
        southIgnitionProb = (BASE_SPREAD_RATE) 
                / (cellSize * (1 - Math.cos(Math.PI - windDirection) * (1 - BASE_SPREAD_RATE / MAX_SPREAD_RATE)));
        westIgnitionProb = (BASE_SPREAD_RATE) 
                / (cellSize * (1 - Math.cos(1.5 * Math.PI - windDirection) * (1 - BASE_SPREAD_RATE / MAX_SPREAD_RATE)));
        
        // calculate the burnout probability
        burnoutProb = (0.167 * MAX_SPREAD_RATE / cellSize);
    }//end constructor.
}//end class.
