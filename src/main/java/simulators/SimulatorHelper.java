package simulators;

import burlap.behavior.stochasticgames.GameEpisode;
import burlap.mdp.core.Domain;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.stochasticgames.JointAction;
import domains.wildfire.WildfireDomain;
import domains.wildfire.WildfireParameters;
import domains.wildfire.WildfireState;
import domains.wildfire.beans.Agent;
import domains.wildfire.beans.Fire;
import domains.wildfire.beans.Location;

import javax.swing.*;
import java.io.*;
import java.util.List;
import java.util.Map;

public class SimulatorHelper {

    public static String predictionLog;
    public static String configLog;
    public static String beliefLog;
    public static StringBuilder ipomcpTreeLog;
    public static StringBuilder particleLog;

    static {
        predictionLog = "";
        configLog = "";
        beliefLog = "";
        ipomcpTreeLog = new StringBuilder("");
        particleLog = new StringBuilder("");
    }

    /**
     * This method returns suppressant level information of all the agents in a CSV format.
     * @param episode is the current episode object.
     * @param trial is the game trial.
     * @return A CSV string containing all suppressant related information.
     */
    public static String saveSuppressantResultsToFile(GameEpisode episode, int trial) {
        //Maximum stages of the Game
        int maxStages = episode.maxTimeStep();
        //Get number of agents in the game.
        int numAgents =  ((WildfireState)episode.state(0)).getAgentList().size();

        //Suppressant levels Arrays.
        int suppressantLevel[][] = new int[numAgents][maxStages+1];
        int totalSuppressantLevel[] = new int[maxStages+1];
        int suppressatUnitUsed[] = new int[maxStages+1];

        String suppressantHead = "\nTrial "+trial+"\n";
        String suppressantStr = "";
        String totalSupp = "Total Suppressants,";
        String totalUsed = "Suppressant Used,";


        int sumSuppressant = 0;
        int totalUnitUsed = 0;
        for(int i=0; i<=maxStages; i++){
            sumSuppressant=0;
            totalUnitUsed = 0;
            for (int agID=0; agID<numAgents; agID++){
                //suppressants
                suppressantLevel[agID][i] = ((WildfireState)episode.state(i)).getAgentList().get(agID).getAvailability();
                sumSuppressant += suppressantLevel[agID][i];

                //Sum suppressant level.
                if(i != 0 && suppressantLevel[agID][i] < suppressantLevel[agID][i-1]){
                    totalUnitUsed +=  suppressantLevel[agID][i-1] - suppressantLevel[agID][i];
                }
            }
            totalSuppressantLevel[i] = sumSuppressant;
            suppressatUnitUsed[i] = totalUnitUsed;
        }

        for(int agID=0; agID<numAgents; agID++){
            suppressantStr += agID+",";
            for (int i=0; i<=maxStages; i++){
                suppressantStr += Integer.toString(suppressantLevel[agID][i])+",";
            }
            suppressantStr += "\n";
        }

        suppressantStr += totalSupp;
        for (int i=0; i<=maxStages; i++){
            suppressantStr += Integer.toString(totalSuppressantLevel[i])+",";
        }

        suppressantStr += "\n";
        suppressantStr += totalUsed;
        int trialUsedSum = 0;
        for (int i=0; i<=maxStages; i++){
            trialUsedSum += suppressatUnitUsed[i];
            suppressantStr += Integer.toString(suppressatUnitUsed[i])+",";
        }

        suppressantStr += "Total Used:," + trialUsedSum + ",";
        String content = suppressantHead + suppressantStr + "\n" ;

        return content;
    }


    /**
     * This method returns rewards of all the agents in a CSV format.
     * @param episode is the current episode object.
     * @param trial is the game trial.
     * @return A CSV string containing rewards related to each agent.
     */
    public static String saveRewardResultsToFile(GameEpisode episode, int trial) {
        //Maximum stages of the Game
        int maxStages = episode.maxTimeStep();
        //Get number of agents in the game.
        int numAgents =  ((WildfireState)episode.state(0)).getAgentList().size();

        //Define the reward arrays.
        double rewards[][] = new double[numAgents][maxStages+1];
        double totalRewards[] = new double[maxStages+1];

        String rewardHead = "\nTrial "+trial+"\n";
        String rewardStr = "";
        String totals = "Total,";



        double totalAvgReward = 0;
        double sumReward = 0;
        int ag=0;

        //Find the rewards for each stages and sum to get overall rewards.
        for(int i=1; i<=maxStages; i++){
            sumReward=0;
            ag=0;

            for(Double reward : episode.jointReward(i)){
                rewards[ag][i] = reward;
                sumReward+=rewards[ag][i];
                ag++;
            }
            totalRewards[i] = sumReward;
        }

        //Get rewards for each agent to create a structured format.
        for(int agID=0; agID<numAgents; agID++){
            rewardStr += agID+",";
            for (int i=1; i<=maxStages; i++){
                rewardStr += Double.toString(rewards[agID][i])+",";
            }
            rewardStr += "\n";
        }

        rewardStr += totals;
        //Get average reward for each agent.
        for (int i=1; i<=maxStages; i++){
            rewardStr += Double.toString(totalRewards[i])+",";
            totalAvgReward += totalRewards[i];
        }

        totalAvgReward /= maxStages;//Devide the reward by stages.
        String content = rewardHead + rewardStr +  "Total Avg Reward," + totalAvgReward  + "\n";

        return content;
        //writeOutputToFile(content, resultsFile);
    }//end method.


    /**
     * This method returns actions of all the agents in a CSV format.
     * @param episode is the current episode object.
     * @param trial is the game trial.
     * @return A CSV string containing actions related to each agent.
     */
    public static String saveActionResultsToFile(GameEpisode episode, int trial) {
        //Maximum stages of the Game
        int maxStages = episode.maxTimeStep();
        //Get number of agents in the game.
        int numAgents =  ((WildfireState)episode.state(0)).getAgentList().size();


        //Get all the joint actions and create an action multi-dimension array.
        List<JointAction> allJAsList = episode.jointActions;
        String actions[][] = new String[numAgents][maxStages];

        String actionHead = "\nTrial "+trial+"\n";
        String actionStr = "";

        //Populate individual action arrays.
        for(int i=0; i<maxStages; i++){
            for (int agID=0; agID<numAgents; agID++){
                actions[agID][i] = allJAsList.get(i).action(agID).actionName();
            }
        }//end for.

        //Structure arrays into CSV formats.
        for(int agID=0; agID<numAgents; agID++){
            actionStr += agID+",";
            for (int i=0; i<=maxStages; i++){
                if(i!=maxStages){actionStr += actions[agID][i]+",";}
            }
            actionStr += "\n";
        }//end for.

        String content = actionHead + actionStr + "\n";
        return content;
//		writeOutputToFile(content, resultsFile);
    }


    /**
     * This method returns fire information of the domain in a CSV format.
     * @param episode is the current episode object.
     * @param trial is the game trial.
     * @return A CSV string containing fires intensities as well as average intensities.
     */
    public static String saveFireResultsToFile(GameEpisode episode, int trial) {
        //Maximum stages of the Game
        int maxStages = episode.maxTimeStep();
        //Get number of agents in the game.
        int numFires =  ((WildfireState)episode.state(0)).getFireList().size();


        //Create Fire arrays.
        int fireIntensity[][] = new int[numFires][maxStages+1];
        int totalFireIntensity[] = new int[maxStages+1];

        String fireHead = "\nTrial "+trial+"\n";
        String fireStr = "";
        String totals = "Total,";
        int sumFire = 0;

        double totalAvgFire = 0;

        //Iterate through all the fires and get intensities.
        for(int i=0; i<=maxStages; i++){
            //Fires
            sumFire=0;
            for (int f=0; f<numFires; f++){
                fireIntensity[f][i] = ((WildfireState)episode.state(i)).getFireList().get(f).getIntensity();
                sumFire += fireIntensity[f][i];
            }
            totalFireIntensity[i] = sumFire;
        }

        //Convert the fire list into a structure.
        for(int f=0; f<numFires; f++){
            fireStr += f+",";
            for (int i=0; i<=maxStages; i++){
                fireStr += Integer.toString(fireIntensity[f][i])+",";
            }
            fireStr += "\n";
        }

        fireStr += totals;
        for (int i=0; i<=maxStages; i++){
            fireStr += Integer.toString(totalFireIntensity[i])+",";
            totalAvgFire += totalFireIntensity[i];//Sum the total intensity.
        }


        totalAvgFire /= maxStages; //Average the intensity
        String content = fireHead + fireStr + "Avg Intensity," + totalAvgFire + "\n" ;
        return content;
    }



    static String saveAllResultsToFile(GameEpisode episode,int trial) {
        //Maximum stages of the Game
        int maxStages = episode.maxTimeStep();
        //Get number of agents in the game.
        int numAgents =  ((WildfireState)episode.state(0)).getAgentList().size();
        //Get number of agents in the game.
        int numFires =  ((WildfireState)episode.state(0)).getFireList().size();

        //Arrays for actions, rewards, fires, and suppressants.
        List<JointAction> allJAsList = episode.jointActions;
        String actions[][] = new String[numAgents][maxStages];
        double rewards[][] = new double[numAgents][maxStages+1];
        double totalRewards[] = new double[maxStages+1];
        int suppressantLevel[][] = new int[numAgents][maxStages+1];
        int totalSuppressantLevel[] = new int[maxStages+1];
        int fireIntensity[][] = new int[numFires][maxStages+1];
        int totalFireIntensity[] = new int[maxStages+1];

        String trialHead = "\nTrial " + trial + "\n";
        String fireHead = "\nFire Intensities\n";
        String fireStr = "";
        String rewardHead = "\nJoint Rewards\n";
        String rewardStr = "";
        String actionHead = "\nJoint Actions\n";
        String actionStr = "";
        String suppressantHead = "\nSuppressant Level\n";
        String suppressantStr = "";
        String totals = "Total,";

        double sumReward = 0;
        int sumSuppressant = 0;
        int sumFire = 0;
        int ag=0;


        for(int i=0; i<=maxStages; i++){
            sumSuppressant=0;
            for (int agID=0; agID<numAgents; agID++){
                if(i!=maxStages){
                    actions[agID][i] =  allJAsList.get(i).action(agID).actionName();
                }

                //suppressants
                suppressantLevel[agID][i] = ((WildfireState)episode.state(i)).getAgentList().get(agID).getAvailability();
                sumSuppressant += suppressantLevel[agID][i];
            }
            totalSuppressantLevel[i] = sumSuppressant;

            //Rewards
            sumReward=0;
            ag=0;
            if(i!=maxStages){
                for(Double reward : episode.jointReward(i+1)){
                    rewards[ag][i] = reward;
                    sumReward+=rewards[ag][i];
                    ag++;
                }
                totalRewards[i] = sumReward;
            }
            //Fires
            sumFire=0;
            for (int f=0; f<numFires; f++){
                fireIntensity[f][i] = ((WildfireState)episode.state(i)).getFireList().get(f).getIntensity();
                sumFire += fireIntensity[f][i];
            }
            totalFireIntensity[i] = sumFire;
        }

        for(int f=0; f<numFires; f++){
            fireStr += f+",";
            for (int i=0; i<=maxStages; i++){
                fireStr += Integer.toString(fireIntensity[f][i])+",";
            }
            fireStr += "\n";
        }

        for(int agID=0; agID<numAgents; agID++){
            rewardStr += agID+",";
            actionStr += agID+",";
            suppressantStr += agID+",";
            for (int i=0; i<=maxStages; i++){
                if(i!=maxStages){rewardStr += Double.toString(rewards[agID][i])+",";}
                if(i!=maxStages){actionStr += actions[agID][i]+",";}
                suppressantStr += Integer.toString(suppressantLevel[agID][i])+",";
            }
            rewardStr += "\n";
            actionStr += "\n";
            suppressantStr += "\n";
        }

        fireStr += totals;
        rewardStr += totals;
        suppressantStr += totals;
        for (int i=0; i<=maxStages; i++){
            fireStr += Integer.toString(totalFireIntensity[i])+",";
            if(i!=maxStages){rewardStr += Double.toString(totalRewards[i])+",";}
            suppressantStr += Integer.toString(totalSuppressantLevel[i])+",";
        }

        String content = trialHead + fireHead + fireStr + "\n" +
                suppressantHead + suppressantStr + "\n" +
                actionHead + actionStr + "\n" +
                rewardHead + rewardStr + "\n" ;


        return content;
    }


    /**
     * This method would save the information for state => action-config vlaues separated on the
     * bases of location.
     * @param episode is the current trial game episode.
     * @param domain is the wildfire domain.
     * @param trial is the trial number.
     * @return CSV String content of the analysis.
     */
    static String analyticsWithLocation(GameEpisode episode, Domain domain, int trial) {

        //Wildfire Domain
        WildfireDomain wildfireDomain = (WildfireDomain)domain;
        //Maximum stages of the Game
        int maxStages = episode.maxTimeStep();
        //Get number of agents in the game.
        int numAgents =  ((WildfireState)episode.state(0)).getAgentList().size();

        //Suppressant levels Arrays.
        int suppressantLevel[][] = new int[numAgents][maxStages+1];
        int suppressatUnitUsed[] = new int[maxStages+1];


        String content = "\nTrial " + trial + "\n";

        //Iterate through each state and gather information.
        for(int stage = 0; stage <= maxStages ; stage++){
            String stageContent = "";

            //Wildfire state.
            WildfireState wildfireState = (WildfireState) episode.state(stage);

            //Start with putting labels in the CSV.
            if(stage == 0){
                stageContent += "STATE:,";

                //Add Fire Labels.
                for(int fireIndex = 0; fireIndex < wildfireState.getFireList().size(); fireIndex++){
                    stageContent +=  wildfireState.getFireList().get(fireIndex).getFireNumber() + ",";
                }//end for.

                //Add suppressant Labels.
                stageContent += "Suppressants:,";
                for(Map.Entry<String,Agent> agentEntry : wildfireDomain.sampleAgentPerGroup.entrySet()){
                    stageContent += " Group - Location - Type,";
                    for(int supp= 0 ; supp < WildfireParameters.MAX_SUPPRESSANT_STATES ; supp++){
                        stageContent +=  supp + ",";
                    }//end for.
                }//end for.

                stageContent += "Total Suppressants Used,";
                //Add suppressant Labels.
                stageContent += "ACTIONS:,";
                for(Map.Entry<String,Agent> agentEntry : wildfireDomain.sampleAgentPerGroup.entrySet()){
                    stageContent += " Group - Location - Type,";
                    for(ActionType actionType : wildfireDomain.actionsPerGroup.get(agentEntry.getKey())){
                        stageContent +=  actionType.associatedAction("Useless argument").actionName() + ",";
                    }//end for.
                }//end for.

                stageContent += "\n";
            }//end if labels.


            stageContent += stage + ",";//For the state column,put the value of stage.
            //Add Fire Intensities.
            for(int fireIndex = 0; fireIndex < wildfireState.getFireList().size(); fireIndex++){
                stageContent +=  wildfireState.getFireList().get(fireIndex).getIntensity() + ",";
            }//end for.

            stageContent += ",";

            //Suppressant Levels sums.
            //Add for each values.
            for(Map.Entry<String,Agent> agentEntry : wildfireDomain.sampleAgentPerGroup.entrySet()){
                Agent sampleAgent = agentEntry.getValue();
                //Add group, location, power type value.
                stageContent +=  sampleAgent.getAgentGroup() + "-" + sampleAgent.getAgentLocation()
                                        + "-" + sampleAgent.getPowerType() + ",";


                for(int supp= 0 ; supp < WildfireParameters.MAX_SUPPRESSANT_STATES ; supp++){
                    int supValue = 0;
                    //Increase the number of agents value for matching agents with the same
                    //suppressant levels.
                    for(Agent agent : wildfireState.getAgentList()){
                        if(agent.getAgentLocation().equals(sampleAgent.getAgentLocation())
                                && agent.getPowerType() == sampleAgent.getPowerType()
                                && agent.getAvailability() == supp){
                            supValue++;
                        }//end if.
                    }//end for.

                    stageContent += supValue + ",";
                }//end for.
            }//end for.


            //Find the total number of suppressant units used.
            int totalUnitUsed = 0;
            for (int agID=0; agID<numAgents ; agID++){
                //suppressants
                suppressantLevel[agID][stage] = ((WildfireState)episode.state(stage)).getAgentList().get(agID).getAvailability();
                //Sum suppressant level.
                if(stage!= 0 && suppressantLevel[agID][stage] < suppressantLevel[agID][stage-1]){
                    totalUnitUsed +=  suppressantLevel[agID][stage-1] - suppressantLevel[agID][stage];
                }//end if.
            }//end for.
            stageContent+= totalUnitUsed + ",";


            stageContent += ","; //Action values.

            if(stage != maxStages){
                //Action information sum.Add for each values.
                for(Map.Entry<String,Agent> agentEntry : wildfireDomain.sampleAgentPerGroup.entrySet()){
                    Agent sampleAgent = agentEntry.getValue();
                    //Add group, location, power type value.
                    stageContent +=  sampleAgent.getAgentGroup() + "-" + sampleAgent.getAgentLocation()
                            + "-" + sampleAgent.getPowerType() + ",";
                    //Iterate through all the action types.
                    for(ActionType actionType : wildfireDomain.actionsPerGroup.get(agentEntry.getKey())){
                        int actionValue = 0;
                        String action = actionType.associatedAction("Useless argument").actionName();
                        //Joint action.
                        JointAction ja = episode.jointActions.get(stage);
                        //Increase the number of agents value for matching agents with the same
                        //action.
                        for(int agentIndex = 0; agentIndex < wildfireState.getAgentList().size() ; agentIndex++){
                            Agent agent = wildfireState.getAgentList().get(agentIndex);
                            if(agent.getAgentLocation().equals(sampleAgent.getAgentLocation())
                                    && agent.getPowerType() == sampleAgent.getPowerType()
                                    &&  action.equals(ja.action(agentIndex).actionName())){
                                actionValue++;
                            }//end if.
                        }//end for.

                        stageContent += actionValue + ",";
                    }//end for.
                }//end for.
            }//end if.
            stageContent += "\n";
            content += stageContent;
        }//end for.

        return content;
    }

    public static void writeOutputToFile(String content, String filename){
        boolean writeToFile = true;

        if(writeToFile){
            try {
                File file = new File(filename);
                System.out.println("Write Output Parent:" + file.getParentFile());
                file.getParentFile().mkdirs();

                // if file doesnt exists, then create it
                if (!file.exists()) {
                    file.createNewFile();
                }
                //Write the content to file.
                FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(content);
                bw.close();
                fw.close();

//				System.out.println("Done writing serialized output to file..");
            } catch (IOException e) {
                e.printStackTrace();
            }//end if-else.
        }//end if.
    }//end method.

    /**
     * This method is used for logging the IPOMCP prediction logs.
     * @param log is the log for each stage and action.
     */
    public static void addPrediction(String log){
        SimulatorHelper.predictionLog += log;
    }

    /**
     * This method is used for logging the IPOMCP configuration logs.
     * @param log is the log of IPOMCP layer-0 configurations and actual state.
     */
    public static void addConfigInfo(String log){
        SimulatorHelper.configLog += log;
    }

    /**
     * This method is used for logging the IPOMCP belief particles.
     * @param log is the log of IPOMCP belief particles.  
     */
    public static void addBeliefInfo(String log){
        SimulatorHelper.beliefLog += log;
    }


    /**
     * This method is used for logging the IPOMCP Tree.
     * @param log is the log of IPOMCP tree.
     */
    public static void addTreeInfo(String log){
        SimulatorHelper.ipomcpTreeLog.append(log);
    }

    /**
     * This method is used for logging the IPOMCP particles.
     * @param log is the log of IPOMCP tree particles at the beginning of the stage.
     */
    public static void addParticleLog(String log){
        SimulatorHelper.particleLog.append(log);
    }
}
