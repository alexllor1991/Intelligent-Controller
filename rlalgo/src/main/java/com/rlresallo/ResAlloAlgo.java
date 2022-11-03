package com.rlresallo;

import com.rlresallo.ActionSpace;
import com.rlresallo.LruReplayBuffer;
import com.rlresallo.ReplayBuffer;
import com.rlresallo.agents.RlAgent;
import com.rlresallo.Environment;
import com.rlresallo.ODLBrain;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.util.RandomUtils;

//import org.sdnhub.odl.tutorial.learningswitch.impl.TutorialL2Forwarding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import java.awt.*;
//import java.awt.event.WindowAdapter;
//import java.awt.event.WindowEvent;
//import java.awt.image.BufferedImage;
import java.util.*;
import com.opencsv.CSVWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.util.NavigableSet;

import java.time.LocalDateTime;

import static com.rlresallo.TrainResAlloAlgo.OBSERVE;
//import static com.rlresallo.TrainResAlloAlgo.NUMBER_CLUSTER;
//import static com.rlresallo.TrainResAlloAlgo.NUMBER_NODES;

public class ResAlloAlgo implements Environment {
    //private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ResAlloAlgo.class);
    
    private static int envState;
    public static final int ENV_START = 1;
    public static final int ENV_OVER = 2;

    public static final int[] DO_NOTHING_1 = {1,0,0,0,0};
    public static final int[] NODE1_1 = {0,1,0,0,0};
    public static final int[] NODE2_1 = {0,0,1,0,0};
    public static final int[] NODE3_1 = {0,0,0,1,0};
    public static final int[] NODE4_1 = {0,0,0,0,1};

    public static final int[] DO_NOTHING_2 = {1,0,0,0,0,0,0,0,0};
    public static final int[] NODE1_2 = {0,1,0,0,0,0,0,0,0};
    public static final int[] NODE2_2 = {0,0,1,0,0,0,0,0,0};
    public static final int[] NODE3_2 = {0,0,0,1,0,0,0,0,0};
    public static final int[] NODE4_2 = {0,0,0,0,1,0,0,0,0};
    public static final int[] NODE5_2 = {0,0,0,0,0,1,0,0,0};
    public static final int[] NODE6_2 = {0,0,0,0,0,0,1,0,0};
    public static final int[] NODE7_2 = {0,0,0,0,0,0,0,1,0};
    public static final int[] NODE8_2 = {0,0,0,0,0,0,0,0,1};

    public static final int[] DO_NOTHING_3 = {1,0,0,0,0,0,0,0,0,0,0,0,0};
    public static final int[] NODE1_3 = {0,1,0,0,0,0,0,0,0,0,0,0,0};
    public static final int[] NODE2_3 = {0,0,1,0,0,0,0,0,0,0,0,0,0};
    public static final int[] NODE3_3 = {0,0,0,1,0,0,0,0,0,0,0,0,0};
    public static final int[] NODE4_3 = {0,0,0,0,1,0,0,0,0,0,0,0,0};
    public static final int[] NODE5_3 = {0,0,0,0,0,1,0,0,0,0,0,0,0};
    public static final int[] NODE6_3 = {0,0,0,0,0,0,1,0,0,0,0,0,0};
    public static final int[] NODE7_3 = {0,0,0,0,0,0,0,1,0,0,0,0,0};
    public static final int[] NODE8_3 = {0,0,0,0,0,0,0,0,1,0,0,0,0};
    public static final int[] NODE9_3 = {0,0,0,0,0,0,0,0,0,1,0,0,0};
    public static final int[] NODE10_3 = {0,0,0,0,0,0,0,0,0,0,1,0,0};
    public static final int[] NODE11_3 = {0,0,0,0,0,0,0,0,0,0,0,1,0};
    public static final int[] NODE12_3 = {0,0,0,0,0,0,0,0,0,0,0,0,1};
    //private boolean withGraphics;

    private final NDManager manager;
    private final ReplayBuffer replayBuffer;
    private NDList currentObservation;
    private NDList lastAction;
    private ActionSpace actionSpace;
    private NDArray status;
    private NDArray mask;
    private String currentVNFName;
    private String serviceName;
    private NDArray vnfRequest;
    private int clusters;
    private int nodes;

    private static final String CSV_FILE_DATASET = "src/main/resources/model/Dataset.csv";
    private static final String CSV_FILE_ENV = "src/main/resources/model/Env_results.csv";
    private static File file_dataset = null;
    private static File file_env = null;
    private static FileWriter outputDataset = null;
    private static FileWriter outputEnv = null;
    private static CSVWriter writerDataset = null;
    private static CSVWriter writerEnv = null;

    /**
     * Constructs a {@link ResourceAllocationEnvironment} with a basic {@link LruReplayBUffer}
     * 
     * @param manager          the manager for creating the environment
     * @param batchSize        the number of steps to train on per batch
     * @param replayBufferSize the number of steps to hold in the buffer
     */
    public ResAlloAlgo(NDManager manager, int batchSize, int replayBufferSize, int noClusters, int noNodes) {
        this(manager, new LruReplayBuffer(batchSize, replayBufferSize));
        //this.withGraphics = withGraphics;
        clusters = noClusters;
        nodes = noNodes;
        
        actionSpace = new ActionSpace();
        lastAction = new NDList();

        if (clusters == 1) {
            actionSpace.add(new NDList(manager.create(DO_NOTHING_1)));
            actionSpace.add(new NDList(manager.create(NODE1_1)));
            actionSpace.add(new NDList(manager.create(NODE2_1)));
            actionSpace.add(new NDList(manager.create(NODE3_1)));
            actionSpace.add(new NDList(manager.create(NODE4_1)));
        }
        if (clusters == 2) {
            actionSpace.add(new NDList(manager.create(DO_NOTHING_2)));
            actionSpace.add(new NDList(manager.create(NODE1_2)));
            actionSpace.add(new NDList(manager.create(NODE2_2)));
            actionSpace.add(new NDList(manager.create(NODE3_2)));
            actionSpace.add(new NDList(manager.create(NODE4_2)));
            actionSpace.add(new NDList(manager.create(NODE5_2)));
            actionSpace.add(new NDList(manager.create(NODE6_2)));
            actionSpace.add(new NDList(manager.create(NODE7_2)));
            actionSpace.add(new NDList(manager.create(NODE8_2)));
        }
        if (clusters == 3) {
            actionSpace.add(new NDList(manager.create(DO_NOTHING_3)));
            actionSpace.add(new NDList(manager.create(NODE1_3)));
            actionSpace.add(new NDList(manager.create(NODE2_3)));
            actionSpace.add(new NDList(manager.create(NODE3_3)));
            actionSpace.add(new NDList(manager.create(NODE4_3))); //controller node cluster 1
            actionSpace.add(new NDList(manager.create(NODE5_3)));
            actionSpace.add(new NDList(manager.create(NODE6_3)));
            actionSpace.add(new NDList(manager.create(NODE7_3)));
            actionSpace.add(new NDList(manager.create(NODE8_3))); //controller node cluster 2
            actionSpace.add(new NDList(manager.create(NODE9_3)));
            actionSpace.add(new NDList(manager.create(NODE10_3)));
            actionSpace.add(new NDList(manager.create(NODE11_3)));
            actionSpace.add(new NDList(manager.create(NODE12_3))); //controller node cluster 3
        }

        file_dataset = new File(CSV_FILE_DATASET);
        file_env = new File(CSV_FILE_ENV);

        String[] headerDataset = {"Timestamp",
                                  "Preobservation",
                                  "Postobservation",
                                  "Action",
                                  "Reward"};

        String[] headerEnv = {"Timestamp",
                              "Env_step",
                              "Reward",
                              "Reward_prepenalization",
                              "Resource_cost_term",
                              "Life_term",
                              "Deployment_term",
                              "Epsilon"};
 
        try {
            outputDataset = new FileWriter(file_dataset);
            outputEnv = new FileWriter(file_env);
            writerDataset = new CSVWriter(outputDataset);
            writerEnv = new CSVWriter(outputEnv);
            writerDataset.writeNext(headerDataset);
            writerEnv.writeNext(headerEnv);
            writerDataset.flush();
            writerEnv.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
        for(int k = 1; k <= noClusters; k++) {
            for(int n = 1; n <= noNodes; n++) {
                NODE[0] = k;
                NODE[1] = n;
                actionSpace.add(new NDList(manager.create(NODE))); // Action to place VNF in node n of cluster k
            }
        }
        */

        //status = ODLBrain.getEdgeNodesStatus(noClusters, noNodes);
        //System.out.println(status);
        //currentVNFName = ODLBrain.getCurrentVNF();
        //System.out.println(currentVNFName);
        //vnfRequest = ODLBrain.getVNFRequest();
        //System.out.println(vnfRequest);
        //currentObservation = createObservation(status, vnfRequest);
        //System.out.println("------------Initial observation------------");
        //System.out.println(Arrays.toString(currentObservation.toArray()));
        
        setEnvState(ENV_START);
    }

    /**
     * Constructs a {@link ResAlloAlgo}.
     * 
     * @param manager      the manager for creating the environmet
     * @param replayBuffer the replay buffer for storing data
     */
    
    public ResAlloAlgo(NDManager manager, ReplayBuffer replayBuffer) {
        this.manager = manager;
        this.replayBuffer = replayBuffer;
    }
    
    public static int envStep = 0;
    public static int trainStep = 0;
    private static boolean currentTerminal = false;
    private static boolean rejectedLastVNF = false;
    private static boolean assumedVNFRejected = false;
    private static float currentReward = 0.0f;
    private static float averageRewardEpoch = 0.0f;
    private static float totalRewardEpoch = 0.0f;
    private static double epsilon = 0.0d;
    private String trainState = "observe";
    private static NDList extCurrentObservation;

    /**
     * {@inheritDoc}
     */
    @Override
    public Step[] runEnvironment(RlAgent agent, boolean training) {
        Step[] batchSteps = new Step[0];
        reset();

        // run the environment

        status = ODLBrain.getEdgeNodesStatus(clusters, nodes);
        //System.out.println(status);
        currentVNFName = ODLBrain.getCurrentVNF();
        serviceName = ODLBrain.getServiceName();
        vnfRequest = ODLBrain.getVNFRequest();
        //System.out.println(vnfRequest);
        currentObservation = createObservation(status, vnfRequest);
        mask = ODLBrain.getNodesMask(clusters, nodes);
        ResAlloAlgo.setExtCurrentObservation(currentObservation);
        System.out.println();
        System.out.println("------------Current observation before step environment------------");
        System.out.println(Arrays.toString(currentObservation.toArray()));

        System.out.println("------------Choosing action-----------");
        NDList action = agent.chooseAction(this, training, nodes, clusters, mask);

        if (!lastAction.isEmpty()) {
            if ((lastAction.singletonOrThrow().contentEquals(action.singletonOrThrow()) && currentReward == 0.0f && rejectedLastVNF) || assumedVNFRejected) {
                NavigableSet<String> keys = ODLBrain.usingNodes.keySet();
                Iterator<String> iteratorKey = keys.iterator();
                List<Integer> indexNodesActionSpace = new ArrayList<Integer>();
                boolean availableUsedMultipleNodes = false;

                while (iteratorKey.hasNext()) {
                    String master = iteratorKey.next();
                    List<String> usedNodes = ODLBrain.usingNodes.get(master);

                    int cluster = Integer.parseInt(master.substring(master.lastIndexOf("e") + 1));
                    int initialIndex = (cluster - 1) * nodes;
        
                    for (int i = 0; i < usedNodes.size(); i++) {
                        String node = usedNodes.get(i);
                        int index = 0;
        
                        if (!node.equals("kubernetes-control-plane")) {
                            index = initialIndex + Integer.parseInt(node.substring(node.lastIndexOf("r") + 1));
                        } else {
                            index = initialIndex;
                        }
                        
                        float capacity = currentObservation.singletonOrThrow().getFloat(index);
                        if (capacity < 0.875f) {
                            availableUsedMultipleNodes = true;
                            int nodeActionSpace = 0;
                            if (!node.equals("kubernetes-control-plane")) {
                                nodeActionSpace = initialIndex + Integer.parseInt(node.substring(node.lastIndexOf("r") + 1));
                            } else {
                                nodeActionSpace = initialIndex + 4;
                            } 
                            indexNodesActionSpace.add(nodeActionSpace);
                        }
                    }    
                }

                if (!availableUsedMultipleNodes) {
                    System.out.println("------------Random action------------");
                    action = getActionSpace().randomAction();
                    while (true) {
                        if (action.singletonOrThrow().getInt(4) == 1 || action.singletonOrThrow().getInt(8) == 1 || action.singletonOrThrow().getInt(12) == 1) {
                            action = getActionSpace().randomAction();
                        } else {
                            break;
                        }
                    }
                    rejectedLastVNF = false;
                } else {
                    int size = indexNodesActionSpace.size();
                    int indexAction = indexNodesActionSpace.get(RandomUtils.nextInt(size));
                    action = actionSpace.get(indexAction);
                }
                assumedVNFRejected = false;
            }
        }

        step(action, training);
        if (training) {
            batchSteps = this.getBatch();
        }
        if (envStep % 5000 == 0) {
            this.closeStep();
        }
        if (envStep <= OBSERVE) {
            trainState = "observe";
        } else {
            trainState = "explore";
        }
        envStep++;

        return batchSteps;
    }

    /** 
    * {@inheritDoc}
    * action = {0,0} : do nothing
    * action = {k,n} : place vnf in node n of clsuter k
    */
    @Override
    public void step(NDList action, boolean training) {
        if (action.singletonOrThrow().getInt(0) == 0) { //Action different to do_nothing
            int cluster = 1;
            int node = 0;
            int indexes = clusters * nodes;
            int j = 1;
            for (int i = 1; i < indexes + 1; ++i) {
                if (j > nodes) {
                    cluster++;
                    j = 1;
                }
                if (action.singletonOrThrow().getInt(i) == 1) {
                    node = j;
                    break;
                }
                j++;
            }
            //int cluster = action.singletonOrThrow().getInt(0);
            //int node = action.singletonOrThrow().getInt(1);
            if (currentVNFName.length() > 1) {
                System.out.println("------------Placement decision------------");
                System.out.println("VNF: " + currentVNFName.substring(currentVNFName.indexOf("vnf-")));
                System.out.println("Cluster: " + Integer.toString(cluster));
                System.out.println("Node: " + Integer.toString(node));

                float cpuRequestedVNF = vnfRequest.getFloat(0) * ODLBrain.MAX_CPU_NODES;
                if (vnfRequest.getFloat(1) > 0) {
                    ODLBrain.notifyVNFDeployment(cluster, node, currentVNFName, cpuRequestedVNF, "Job", serviceName);
                } else {
                    ODLBrain.notifyVNFDeployment(cluster, node, currentVNFName, cpuRequestedVNF, "Deployment", serviceName);
                }

                int count = 0;
                while (currentVNFName.length() > 1) {
                    //System.out.println("------Waiting VNF deployment------");
                    if (ODLBrain.currentVNFDeployed() || ODLBrain.currentVNFRejected() || count == 60) {
                        if (count == 60) { //Assume current VNF has failed
                            assumedVNFRejected = true;

                            ODLBrain.setFailedVNFs();

                            int vnfServScheduled = 0;
                            Set<String> keysVNfs = ODLBrain.serviceRequestedtoDeploy.get(serviceName).keySet();
                            Iterator<String> iteratorKeys = keysVNfs.iterator();
                            while(iteratorKeys.hasNext()) {
                                String key = iteratorKeys.next();
                                List<String> infoVNf = ODLBrain.serviceRequestedtoDeploy.get(serviceName).get(key);
                                if (infoVNf.get(5).equals("deployed")) {
                                    vnfServScheduled++;
                                }
                                String keyConcat = infoVNf.get(2).concat(key);
                                if (ODLBrain.vnfRequestedtoDeploy.containsKey(keyConcat)) {
                                    ODLBrain.vnfRequestedtoDeploy.remove(keyConcat);

                                    ODLBrain.setDiscardedVNFs();
                                    System.out.println("Removing key: " + keyConcat + " from vnfRequestedtoDeploy");
                                }
                            }
                            
                            if (!ODLBrain.servicesState.containsKey(serviceName)) {
                                ODLBrain.servicesState.put(serviceName, "Rejected");
                                int delta = keysVNfs.size() - vnfServScheduled;
                                ODLBrain.setRejectedVNFs(delta);
                                ODLBrain.setRejectedService();
                            }

                            ODLBrain.updateResults();
                            //int failedVNFs = ODLBrain.getFailedVNFs();
                            //ODLBrain.multiFailedVNFs.set(failedVNFs + 1);
                            //ODLBrain.multiFailedVNFs.getAndIncrement();
                            //int vnfServScheduled = 0;
                            //Set<String> keysVNfs = ODLBrain.serviceRequestedtoDeploy.get(serviceName).keySet();
                            //Iterator<String> iteratorKeys = keysVNfs.iterator();
                            //while(iteratorKeys.hasNext()) {
                            //    String key = iteratorKeys.next();
                            //    List<String> infoVNf = ODLBrain.serviceRequestedtoDeploy.get(serviceName).get(key);
                            //    if (infoVNf.get(5).equals("deployed")) {
                            //        vnfServScheduled++;
                            //    }
                            //    String keyConcat = infoVNf.get(2).concat(key);
                            //    if (ODLBrain.vnfRequestedtoDeploy.containsKey(keyConcat)) {
                            //        ODLBrain.vnfRequestedtoDeploy.remove(keyConcat);
                            //    }
                            //}
                            //int rejectedServices = ODLBrain.getRejectedServices();
                            //ODLBrain.multiRejectedService.set(rejectedServices + 1);
                            //ODLBrain.multiRejectedService.getAndIncrement();
                            //int delta = keysVNfs.size() - vnfServScheduled;
                            //int rejectedVNFs = ODLBrain.getRejectedVNFs();
                            //ODLBrain.multiRejectedVNFs.set(rejectedVNFs + delta);
                            //ODLBrain.multiRejectedVNFs.getAndAdd(delta);
                            //ResAlloAlgo.setCurrentReward(0f);
                        }
                        count = 0;
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                        count++;
                    } catch  (InterruptedException e){
                        e.printStackTrace();
                    }   
                }

                if (ODLBrain.currentVNFDeployed()) {
                    float cpuDeployedNode = 0f;
                    if (node == nodes) {
                        cpuDeployedNode = ODLBrain.nodesCPUPerMaster.get("kubernetes-control-plane" + Integer.toString(cluster)).get("kubernetes-control-plane");
                    } else {
                        cpuDeployedNode = ODLBrain.nodesCPUPerMaster.get("kubernetes-control-plane" + Integer.toString(cluster)).get("kubernetes-worker" + Integer.toString(node));
                    }
                    float cpuDeployedVNF = vnfRequest.getFloat(0) * ODLBrain.MAX_CPU_NODES;
                    float newCpuDeployedNode = cpuDeployedNode + cpuDeployedVNF;
                    if (node == nodes) {
                        ODLBrain.nodesCPUPerMaster.get("kubernetes-control-plane" + Integer.toString(cluster)).put("kubernetes-control-plane", newCpuDeployedNode);
                    } else {
                        ODLBrain.nodesCPUPerMaster.get("kubernetes-control-plane" + Integer.toString(cluster)).put("kubernetes-worker" + Integer.toString(node), newCpuDeployedNode);
                    }
                }
    
                if (ODLBrain.currentVNFRejected()) {
                    rejectedLastVNF = true;
                }
                ODLBrain.isCurrentVNFDeployed.set(false);
                ODLBrain.isCurrentVNFRejected.set(false);
            }

            if (currentVNFName.length() < 1) {
                ResAlloAlgo.setCurrentReward(0f);
            }
        }

        if (action.singletonOrThrow().getInt(0) == 1 && currentVNFName.length() < 1) {
            ResAlloAlgo.setCurrentReward(1f);
        } 

        NDList preObservation = currentObservation;

        status = ODLBrain.getEdgeNodesStatus(clusters, nodes);
        //System.out.println(status);
        //currentVNFName = ODLBrain.getCurrentVNF();
        //System.out.println(currentVNFName);
        //vnfRequest = ODLBrain.getVNFRequest();
        //System.out.println(vnfRequest);
        currentObservation = createObservation(status, vnfRequest);
        System.out.println("------------Current observation after step environment------------");
        System.out.println(Arrays.toString(currentObservation.toArray()));

        lastAction = action;

        ResAlloAlgoStep step = new ResAlloAlgoStep(manager.newSubManager(), preObservation, currentObservation, action, currentReward, currentTerminal);
        if (training) {
            System.out.println("------------Storing transition in experience replay buffer-----------");
            replayBuffer.addStep(step);

            LocalDateTime timestamp = LocalDateTime.now();
            String[] dataset = {timestamp.toString(),
                                Arrays.toString(preObservation.singletonOrThrow().toArray()),
                                Arrays.toString(currentObservation.singletonOrThrow().toArray()),
                                Arrays.toString(action.singletonOrThrow().toArray()),
                                Float.toString(step.getReward().getFloat())};

            float rewardPre_penal = 0.0f;
            if (step.getReward().getFloat() != 0.0f) {
                rewardPre_penal = ODLBrain.getRewardPrepenal();
            }

            String[] env_result = {timestamp.toString(),
                                   Integer.toString(envStep),
                                   Float.toString(step.getReward().getFloat()),
                                   Float.toString(rewardPre_penal),
                                   Float.toString(ODLBrain.getResourceCost()),
                                   Float.toString(ODLBrain.getLifetimeTerm()),
                                   Float.toString(ODLBrain.getDeployedEventTerm()),
                                   Double.toString(getEpsilon())};

            try {
                writerDataset.writeNext(dataset);
                writerEnv.writeNext(env_result);
                writerDataset.flush();
                writerEnv.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            float rewardPre_penal = 0.0f;
            if (step.getReward().getFloat() != 0.0f) {
                rewardPre_penal = ODLBrain.getRewardPrepenal();
            }

            LocalDateTime timestamp = LocalDateTime.now();

            String[] env_result = {timestamp.toString(),
                                   Integer.toString(envStep),
                                   Float.toString(step.getReward().getFloat()),
                                   Float.toString(rewardPre_penal),
                                   Float.toString(ODLBrain.getResourceCost()),
                                   Float.toString(ODLBrain.getLifetimeTerm()),
                                   Float.toString(ODLBrain.getDeployedEventTerm()),
                                   Double.toString(getEpsilon())};

            try {
                writerEnv.writeNext(env_result);
                writerEnv.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        logger.info("ENV_STEP " + envStep +
                " / " + "TRAIN_STEP " + trainStep + 
                " / " + getTrainState() +
                " / " + "ACTION " + (Arrays.toString(action.singletonOrThrow().toArray())) +
                " / " + "REWARD " + step.getReward().getFloat() +
                " / " + "EPSILON " + getEpsilon());
        System.out.println();
        if (envState == ENV_OVER) {
            restartEnv();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NDList getObservation() {
        return currentObservation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionSpace getActionSpace() {
        return this.actionSpace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Step[] getBatch() {
        return replayBuffer.getBatch();
    }

    /**
     * Close the steps in replayBuffer which are not pointed to.
     */
    public void closeStep() {
        replayBuffer.closeStep();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        manager.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        currentReward = 0.0f;
        currentTerminal = false;
    }

    public NDList createObservation(NDArray status, NDArray vnfRequest) {
        return new NDList(NDArrays.concat(new NDList(status, vnfRequest)));
    }

    static final class ResAlloAlgoStep implements Environment.Step {
        private final NDManager manager;
        private final NDList preObservation;
        private final NDList postObservation;
        private final NDList action;
        private final float reward;
        private final boolean terminal;

        private ResAlloAlgoStep(NDManager manager, NDList preObservation, NDList postObservation, NDList action, float reward, boolean terminal) {
            this.manager = manager;
            this.preObservation = preObservation;
            this.postObservation = postObservation;
            this.action = action;
            this.reward = reward;
            this.terminal = terminal;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDList getPreObservation(NDManager manager) {
            preObservation.attach(manager);
            return preObservation;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDList getPreObservation() {
            return preObservation;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDList getPostObservation(NDManager manager) {
            postObservation.attach(manager);
            return postObservation;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDList getPostObservation() {
            return postObservation;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDManager getManager() {
            return this.manager;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDList getAction() {
            return action;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDArray getReward() {
            return manager.create(reward);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isTerminal() {
            return terminal;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            this.manager.close();
        }
    }

    /**
     * Restart environment
     */
    private void restartEnv() {
        setEnvState(ENV_START);
    }

    public static void setEnvState(int envState) {
        ResAlloAlgo.envState = envState;
    }

    public String getTrainState() {
        return this.trainState;
    }

    public static void setCurrentTerminal(boolean currentTerminal) {
        ResAlloAlgo.currentTerminal = currentTerminal;
    }

    public static void setCurrentReward(float currentReward) {
        ResAlloAlgo.currentReward = currentReward;
    }

    public static void setEpochReward(float averageReward) {
        ResAlloAlgo.averageRewardEpoch = averageReward;
    }

    public static void setEpochCumReward(float cumReward) {
        ResAlloAlgo.totalRewardEpoch = cumReward;
    }

    public static void setEpsilon(double epsilon) {
        ResAlloAlgo.epsilon = epsilon;
    }

    public static float getEpochReward() {
        return ResAlloAlgo.averageRewardEpoch;
    }

    public static float getEpochCumReward() {
        return ResAlloAlgo.totalRewardEpoch;
    }

    public static double getEpsilon() {
        return ResAlloAlgo.epsilon;
    }

    public static NDList getExtCurrentObservation() {
        return ResAlloAlgo.extCurrentObservation;
    }

    public static void setExtCurrentObservation(NDList observation) {
        ResAlloAlgo.extCurrentObservation = observation;
    }
}
