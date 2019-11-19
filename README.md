# ScalableOASYS
Scalable Decision-Theoretic Planning in Open and Typed Multiagent Systems (AAAI 2020)

## Installation and Setup

This project required both [JDK 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and [Maven](https://maven.apache.org/) to be installed. Clone this repository and navigate to the containing folder `ScalableOASYS` and enter the command `mvn package` to build the project. At any point, `mvn clean` can be used to reset the Maven build files. 

At this point, the two scripts `run_NestedVI.sh` and `run_IPOMCP.sh` can be used to execute the NestedVI model and IMPOMCP model respectively. Each script provides a collection of default arguments to the Java builds that can be modified within the script file. 

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
