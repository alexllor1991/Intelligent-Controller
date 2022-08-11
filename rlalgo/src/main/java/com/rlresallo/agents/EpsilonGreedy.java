package com.rlresallo.agents;

import com.rlresallo.Environment;
import ai.djl.ndarray.NDList;
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
    private final Tracker exploreRate; //Represents a hyper-parameter that changes gradually through the training process.
    private int counter; //Total number of steps/updates.

    /**
     * Constructs an {@link ai.djl.modality.rl.agent.EpsilonGreedy}.
     * 
     * @param baseAgent   the agent to use for exploitation and to train
     * @param exploreRate the probability of takinf a random action 
     */
    public EpsilonGreedy(RlAgent baseAgent, Tracker exploreRate) {
        this.baseAgent = baseAgent;
        this.exploreRate = exploreRate;
    }

    private static final Logger logger = LoggerFactory.getLogger(EpsilonGreedy.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public NDList chooseAction(Environment env, boolean training, int nodes, int clusters) {
        if (training && RandomUtils.random() < exploreRate.getNewValue(counter++)) {
            logger.info("**********RANDOM ACTION************");
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

            if (!(action.singletonOrThrow().getInt(4) == 1) || !(action.singletonOrThrow().getInt(8) == 1) || !(action.singletonOrThrow().getInt(12) == 1)) {
                return action;
            } else {
                while (action.singletonOrThrow().getInt(4) == 1 || action.singletonOrThrow().getInt(8) == 1 || action.singletonOrThrow().getInt(12) == 1) {
                    action = env.getActionSpace().randomAction();
                    System.out.println(Arrays.toString(action.toArray()));
                }
                return action;
            }

            //return env.getActionSpace().randomAction();
        } else return baseAgent.chooseAction(env, training, nodes, clusters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trainBatch(Environment.Step[] batchSteps) {
        baseAgent.trainBatch(batchSteps);
    }
}
