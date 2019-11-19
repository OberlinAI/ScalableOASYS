#/bin/bash

declare -a CONF_ARRAY
CONF_ARRAY=("1")

declare -a EXPT_ARRAY
EXPT_ARRAY=("1")
NUMEXPTS=${#CONF_ARRAY[@]}


declare -a CUR_GROUP_ARRAY
CUR_GROUP_ARRAY=("0-19" "20-39")
NUMCUR=${#CUR_GROUP_ARRAY[@]}

declare -a NEIGHBOUR_GROUP_ARRAY
NEIGHBOUR_GROUP_ARRAY=("0-19" "20-39")
NUMNEIGHBOUR=${#CUR_GROUP_ARRAY[@]}



for ((i=0;i<$NUMEXPTS;i++)); do

CONF=${CONF_ARRAY[i]}
EXPT=${EXPT_ARRAY[i]}

GAMMA="0.9"
MAXLEVEL="0"
MAXDELTA="0.1"
MAXHORIZONS="10"
EPSILON="0.01"
MAXSTAGES="15"
MAXTRIALS="100"
ITERATIVE_TRIALS="10"
SETSOFTRIAL="10"
SAMPLING_ERROR_BOUND="0"
ALPHA="0"
PARTICLE_COUNT="1000"
BANDIT_CONSTANT="250"
TIME_BOUND="10000"
IS_FIRE_RANDOM="false"
IS_SUPP_RANDOM="true"
IS_AVGQ_ANODE="false"
QVALUE_ERROR_MARGIN="0.01"
IS_OPEN_SUPP="false"

#export MAVEN_OPTS=-Xmx8192m
#export MAVEN_OPTS=-Xmx16384m
export MAVEN_OPTS=-Xmx32768m


clear
echo "Running Experiment $EXPT, Config $CONF"

echo "Running NestedVI to build policy."
echo "With params: Config - $CONF Gamma - $GAMMA Max Level - $MAXLEVEL Max Delta - $MAXDELTA Max Horizon - $MAXHORIZONS Epsione- $EPSILON Experiment - $EXPT Q-Value Error $QVALUE_ERROR_MARGIN"
NESTEDVI_SCRIPT="echo \"RUN:\""

  for ((j=0;j<$NUMCUR;j++)); do
  CUR_GROUP=${CUR_GROUP_ARRAY[j]}
  NEIGHBOUR_GROUP=${NEIGHBOUR_GROUP_ARRAY[j]}

     NESTEDVI_SCRIPT="$NESTEDVI_SCRIPT & mvn exec:java -Dexec.mainClass=\"nestedMDPSolver.NestedVI\" -Dexec.args=\"$CONF $GAMMA $MAXLEVEL $MAXDELTA $MAXHORIZONS $EPSILON $EXPT $CUR_GROUP $NEIGHBOUR_GROUP $QVALUE_ERROR_MARGIN\""

  done
echo $NESTEDVI_SCRIPT
eval $NESTEDVI_SCRIPT



echo "Running NestedVI Simulator"
echo "With params: Config- $CONF Stages - $MAXSTAGES Trials - $MAXTRIALS Max Horizons - $MAXHORIZONS Experiment - $EXPT"
mvn exec:java -Dexec.mainClass="simulators.NestedVIBaselineSimulator" -Dexec.args="$CONF $MAXSTAGES $MAXTRIALS $MAXHORIZONS $EXPT $IS_FIRE_RANDOM $IS_SUPP_RANDOM $IS_OPEN_SUPP" | tee -a NESTEDVISIM_EXP"$EXPT"_CONFIG"$CONF"_MAXHORIZON"$MAXHORIZONS".log

echo "Running NOOP Simulator"
echo "With params: Config- $CONF Max Stages- $MAXSTAGES Max Trials - $MAXTRIALS Experiment - $EXPT"
mvn exec:java -Dexec.mainClass="simulators.NOOPBaselineSimulator" -Dexec.args="$CONF $MAXSTAGES $MAXTRIALS $EXPT $IS_FIRE_RANDOM $IS_SUPP_RANDOM $IS_OPEN_SUPP" | tee -a NOOP_EXP"$EXPT"_CONFIG"$CONF".log

echo "Running Heuristic Simulator"
echo "With params: Config- $CONF Max Stages- $MAXSTAGES Max Trials - $MAXTRIALS Experiment - $EXPT"
mvn exec:java -Dexec.mainClass="simulators.HeuristicBaselineSimulator" -Dexec.args="$CONF $MAXSTAGES $MAXTRIALS $EXPT $IS_FIRE_RANDOM $IS_SUPP_RANDOM $IS_OPEN_SUPP" | tee -a HEURISTIC_EXP"$EXPT"_CONFIG"$CONF".log

done

