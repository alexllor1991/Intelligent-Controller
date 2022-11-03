package com.rlresallo.agents;

import com.rlresallo.Environment;
import com.rlresallo.ResAlloAlgo;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDArray;
import ai.djl.training.tracker.Tracker;
import ai.djl.util.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

/**
 * {@link ai.djl.modality.rl.agent.EpsilonGreedy} is a simple exploration/excitation agent.
 * 
 * <p>It is used to help other agents to explore their environments during training phases by picking
 * random actions sometimes.
 * 
 * <p>It will only explore paths through the known environment when a model based agent is used. 
 * Although, it is important to sometimes explore new path as well. Thus, this agent makes a tradeoff
 * that takes random paths a fixed percentage of the time during the training stage.
 */

public class EpsilonGreedy implements RlAgent {

    private final RlAgent baseAgent;
    //private final Tracker exploreRate; //Represents a hyper-parameter that changes gradually through the training process.
    private int counter; //Total number of steps/updates.
    private float initial_epsilon;
    private float final_epsilon;
    private double decay_epsilon;

    /**
     * Constructs an {@link ai.djl.modality.rl.agent.EpsilonGreedy}.
     * 
     * @param baseAgent   the agent to use for exploitation and to train
     * @param exploreRate the probability of takinf a random action 
     */
    public EpsilonGreedy(RlAgent baseAgent, float initial_epsilon, float final_epsilon, double decay_epsilon) { //Tracker exploreRate
        this.baseAgent = baseAgent;
        this.initial_epsilon = initial_epsilon;
        this.final_epsilon = final_epsilon;
        this.decay_epsilon = decay_epsilon;
        //this.exploreRate = exploreRate;
    }

    private static final Logger logger = LoggerFactory.getLogger(EpsilonGreedy.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public NDList chooseAction(Environment env, boolean training, int nodes, int clusters, NDArray mask) {
        //float epsilon = exploreRate.getNewValue(counter++);
        double epsilon = (double)final_epsilon + (double)(initial_epsilon - final_epsilon) * Math.exp((decay_epsilon*counter++)); 
        ResAlloAlgo.setEpsilon(epsilon);
        if (training && RandomUtils.random() < epsilon) {
            logger.info("******RANDOM ACTION******");
            logger.info("Epsilon: " + Double.toString(epsilon));
            NDList action = env.getActionSpace().randomAction();
            System.out.println(Arrays.toString(action.toArray()));

            // if (!(action.singletonOrThrow().getInt(4) == 1)) {
            //     return action;
            // } else {
            //     while (action.singletonOrThrow().getInt(4) == 1) {
            //         action = env.getActionSpace().randomAction();
            //         System.out.println(Arrays.toString(action.toArray()));
            //     }
            //     return action;
            // }

            // if (!(action.singletonOrThrow().getInt(4) == 1) || !(action.singletonOrThrow().getInt(8) == 1)) {
            //     return action;
            // } else {
            //     while (action.singletonOrThrow().getInt(4) == 1 || action.singletonOrThrow().getInt(8) == 1) {
            //         action = env.getActionSpace().randomAction();
            //         System.out.println(Arrays.toString(action.toArray()));
            //     }
            //     return action;
            // }
            boolean nodeOK = checkNode(env, action, nodes, clusters);

            if (action.singletonOrThrow().getInt(4) != 1 && action.singletonOrThrow().getInt(8) != 1 && action.singletonOrThrow().getInt(12) != 1 && nodeOK) {
                return action;
            } else {
                while (action.singletonOrThrow().getInt(4) == 1 || action.singletonOrThrow().getInt(8) == 1 || action.singletonOrThrow().getInt(12) == 1 || !nodeOK) {
                    action = env.getActionSpace().randomAction();
                    System.out.println(Arrays.toString(action.toArray()));
                    nodeOK = checkNode(env, action, nodes, clusters);
                }
                return action;
            }

            //return env.getActionSpace().randomAction();
        } else return baseAgent.chooseAction(env, training, nodes, clusters, mask);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trainBatch(Environment.Step[] batchSteps) {
        baseAgent.trainBatch(batchSteps);
    }

    public boolean checkNode(Environment env, NDList action, int nodes, int clusters) {
        boolean nodeOK = false;
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

            int iniIndex = (cluster - 1) * nodes;
			int indexNode = iniIndex + node;
			float capacity = env.getObservation().singletonOrThrow().getFloat(indexNode);
			if (capacity < 0.875f) {//0.875f
				nodeOK = true;
                return nodeOK;
			} else {
                return nodeOK;
            }
        } else {
            nodeOK = true;
            return nodeOK;
        }
    }
}
