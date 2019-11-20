# ScalableOASYS
Scalable Decision-Theoretic Planning in Open and Typed Multiagent Systems (AAAI 2020)

## Installation and Setup

This project required both [JDK 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and [Maven](https://maven.apache.org/) to be installed. Clone this repository and navigate to the containing folder `ScalableOASYS` and enter the command `mvn package` to build the project. At any point, `mvn clean` can be used to reset the Maven build files.

At this point, the two scripts `run_NestedVI.sh` and `run_IPOMCP.sh` can be used to execute the NestedVI model and IMPOMCP model respectively. Each script provides a collection of default arguments to the Java builds that can be modified within the script file. To have a better understanding of the simulation proceeding and to log the simulation details, the following script can be useful:
```
./<run_NestedVI|run_IPOMCP>.sh | tee -a <LOG FILE NAME>
```

## Runtime

The experiments to run can be specified within the first few lines of each script file.

+ The `CONF_ARRAY` parameter specifies the setup to use for each experiment.
+ The `EXP_ARRAY` parameter can be modified to keep track of the current experiment number.
+ The `CUR_GROUP_ARRAY` parameter specifies the range of agent numbers in each of the locations of the setup
+ The `NEIGHBOUR_GROUP_ARRAY` parameter specifies the range of agent numbers of neighbors in each of the locations of the setup (this should duplicate `CUR_GROUP_ARRAY`)
+ The `MAXSTAGES` parameter describes the number of steps per simulation.
+ The `MAXTRIALS` parameter describes the number of total simulation trials to run.
+ The `SETSOFTRIAL` parameter describes the number of threads to use to run the trials in parallel
+ `ITERATIVE_TRIALS` = `MAXTRIALS` / `SETSOFTRIAL`. = number of trials per thread
+ `SAMPLING_ERROR_BOUND` and `ALPHA` are parameters of the sampling performed by the IPOMCP model.
+ `PARTICLE_COUNT` and 	`BANDIT_CONSTANT` are parameters of the IPOMCP model.
+ `TIME_BOUND` is another parameter of the IPOMCP model, and is the most important in terms of performance, as it sets a depth limit for the prediction search.

Further modifications can be made to all experiments within the shell script loop. This section also contains parameters to change the fire spreading, and the starting supplies of agent suppressant.


## Setups

The experimental setups can be found in ```<repository_path>/ScalableOASYS/src/main/java/domains/wildfire/WildfireParameters.java``` class. The experimental configurations are represented as a comma-separated string; the first experiment string looks as ```XY-3-3,A-20-0-1-1,A-20-2-1-1,F-0-0-2,F-1-1-1,F-2-0-2,F-2-2-2,P-1-0.1,MFS-5,MSS-3```. Each configuration string can be read as follows:

+ ```XY-<x>-<y>``` defines the length of X and Y axis of the experimental premise.
+ ```A-<agents>-<x>-<y>-<agent type>``` defines the number of agents at ```<x>-<y>``` location of the ```<agent type>``` type. In the AAAI-paper experiments, the type `1` agents are ground fire fighters, where as type `2` agents are helicopters.
+  ```F-<x>-<y>-<fire type>``` defines the fire location ```<x>-<y>``` and the fire type. The constants related to the fire type are defined at the beginning of the class.
+ ```P-<agent type>-<agent extinguishing power>``` defines the extinguishing power of each agent type. In the AAAI-paper experiments, the ground firefighters have extinguishing power of 0.1, where as the helicopters have it of 0.2.
+ ```MFS-<maximum fire states>``` defines the maximum number of fire states of each fire. Here, the ```0``` fire intensity indicates an extinguished fire, while  ```<maximum fire state> - 1``` indicates a burnt-out location.
+ ```MSS-<maximum agent states>``` defines the agent's suppressant level. Here a ```0``` indicates an empty suppressant level, in which the agents' actions become ineffective.
