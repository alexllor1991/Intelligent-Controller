package org.sdnhub.odl.tutorial.learningswitch.impl;

import com.rlresallo.ActionSpace;
import com.rlresallo.LruReplayBuffer;
import com.rlresallo.ReplayBuffer;
import com.rlresallo.agents.RlAgent;
import com.rlresallo.Environment;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;

import org.sdnhub.odl.tutorial.learningswitch.impl.TutorialL2Forwarding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import java.awt.*;
//import java.awt.event.WindowAdapter;
//import java.awt.event.WindowEvent;
//import java.awt.image.BufferedImage;
import java.util.*;
import static org.sdnhub.odl.tutorial.learningswitch.impl.TrainResAlloAlgo.OBSERVE;
//import static com.rlresallo.TrainResAlloAlgo.NUMBER_CLUSTER;
//import static com.rlresallo.TrainResAlloAlgo.NUMBER_NODES;

public class ResAlloAlgo implements Environment {
    //private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ResAlloAlgo.class);
    
    private static int envState;
    public static final int ENV_START = 1;
    public static final int ENV_OVER = 2;
    public static final int[] DO_NOTHING = {0,0};
    //public static final int[] MIGRATE = {0, 0};
    //public static final int[] PLACE_VNF = {0, 0};
    public static int[] NODE;
    //private boolean withGraphics;

    private final NDManager manager;
    private final ReplayBuffer replayBuffer;
    private NDList currentObservation;
    private ActionSpace actionSpace;
    private NDArray status;
    private String currentVNFName;
    private NDArray vnfRequest;

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
        
        actionSpace = new ActionSpace();
        actionSpace.add(new NDList(manager.create(DO_NOTHING)));
        for(int k = 1; k <= noClusters; k++) {
            for(int n = 1; n <= noNodes; n++) {
                NODE[0] = k;
                NODE[1] = n;
                actionSpace.add(new NDList(manager.create(NODE))); // Action to place VNF in node n of cluster k
            }
        }

        status = TutorialL2Forwarding.getEdgeNodesStatus(noClusters, noNodes);
        currentVNFName = TutorialL2Forwarding.getCurrentVNF();
        vnfRequest = TutorialL2Forwarding.getVNFRequest();
        currentObservation = createObservation(status, vnfRequest);
        
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
    private static float currentReward = 0.0f;
    private String trainState = "observe";

    /**
     * {@inheritDoc}
     */
    @Override
    public Step[] runEnvironment(RlAgent agent, boolean training) {
        Step[] batchSteps = new Step[0];
        reset();

        // run the environment
        NDList action = agent.chooseAction(this, training);
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
        if (action.singletonOrThrow().getInt(0) != 0 && action.singletonOrThrow().getInt(1) != 0) {
            int[] placeVNF = action.singletonOrThrow().toIntArray();
            int cluster = placeVNF[0];
            int node = placeVNF[1];
            if (currentVNFName.length() > 1) {
                TutorialL2Forwarding.notifyVNFDeployment(cluster, node, currentVNFName);
            }
            while (true) {
                if (TutorialL2Forwarding.isCurrentVNFDeployed) {
                    break;
                }
            }
            TutorialL2Forwarding.isCurrentVNFDeployed = false;
        }

        NDList preObservation = currentObservation;
        currentObservation = createObservation(status, vnfRequest);

        ResAlloAlgoStep step = new ResAlloAlgoStep(manager.newSubManager(), preObservation, currentObservation, action, currentReward, currentTerminal);
        if (training) {
            replayBuffer.addStep(step);
        }
        logger.info("ENV_STEP " + envStep +
                " / " + "TRAIN_STEP " + trainStep + 
                " / " + getTrainState() +
                " / " + "ACTION " + (Arrays.toString(action.singletonOrThrow().toArray())) +
                " / " + "REWARD " + step.getReward().getFloat());
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
        return new NDList(status, vnfRequest);
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
}
