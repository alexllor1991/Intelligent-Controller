package com.rlresallo.agents;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.types.Shape;
import ai.djl.ndarray.NDManager;
import com.rlresallo.ResAlloAlgo;
import com.rlresallo.TrainResAlloAlgo;
import com.rlresallo.ActionSpace;
import com.rlresallo.Environment;
import com.rlresallo.Environment.Step;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.training.GradientCollector;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.listener.TrainingListener.BatchData;
import ai.djl.training.TrainingResult;
import ai.djl.translate.Batchifier;
import ai.djl.metric.Metrics;
import ai.djl.nn.ParameterList;
import ai.djl.nn.Parameter;
import ai.djl.training.ParameterStore;
import ai.djl.training.ParameterServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import ai.djl.util.Pair;
//import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link RlAgent} implementing Q or Deep-Q Learning.
 * 
 * <p>Deep-Q Learning estimates the total reward that will be given until the environment ends in a
 * particular state after taking an specifi action. It is trained by ensuring that the prediction before
 * taking the action match what would be predicted after taking the action.
 */
public class QAgent implements RlAgent {
    
    private Trainer trainer;  //Trainer provides an easy, and manageable interface for training. The trainer for the model to learn. 
    private float rewardDiscount; //Value to apply to rewards from future states. 
    //private int epoch;
    private ParameterStore parameterStore;
    private Trainer targetTrainer;
    /**
     * Constructs a {@link ai.djl.modality.rl.agent.QAgent} 
     */
    public QAgent(Trainer trainer, float rewardDiscount, Model model, DefaultTrainingConfig config, int batchSize, int Inputs) {
        this.trainer = trainer;
        this.rewardDiscount = rewardDiscount;
        //epoch = 0;
        targetTrainer = model.newTrainer(config);
        targetTrainer.initialize(new Shape(batchSize, Inputs));

        syncNets();
    }

    /**
    public QAgent(Trainer trainer, float rewardDiscount) {
        this.trainer = trainer;
        this.rewardDiscount = rewardDiscount;
        //epoch = 0;
    }
    */

    private static final Logger logger = LoggerFactory.getLogger(QAgent.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public NDList chooseAction(Environment env, boolean training, int nodes, int clusters, NDArray mask) {
        int secondBestAction = 0;
        boolean availableResourcesComp = false;
        //int thirdBestAction = 0;
        int indexes = clusters * nodes;
        ActionSpace actionSpace = env.getActionSpace();
        NDArray actionReward = trainer.evaluate(env.getObservation()).singletonOrThrow(); //Evaluates function of the model once on the given input and return the predict function.
        actionReward = actionReward.mul(mask);
        logger.info(Arrays.toString(actionReward.toFloatArray()));
        int bestAction = Math.toIntExact(actionReward.argMax().getLong());
        //System.out.println(bestAction);
        //System.out.println(env.getObservation().singletonOrThrow().getFloat(1));
        //System.out.println(env.getObservation().singletonOrThrow().getFloat(2));
        //System.out.println(env.getObservation().singletonOrThrow().getFloat(3));
        if (clusters == 1 && (env.getObservation().singletonOrThrow().getFloat(1) < 0.9f || env.getObservation().singletonOrThrow().getFloat(2) < 0.9f || env.getObservation().singletonOrThrow().getFloat(3) < 0.9f)) {
            availableResourcesComp = true;
        }
        if (clusters == 2 && (env.getObservation().singletonOrThrow().getFloat(1) < 0.9f || env.getObservation().singletonOrThrow().getFloat(2) < 0.9f || env.getObservation().singletonOrThrow().getFloat(3) < 0.9f || env.getObservation().singletonOrThrow().getFloat(5) < 0.9f || env.getObservation().singletonOrThrow().getFloat(6) < 0.9f || env.getObservation().singletonOrThrow().getFloat(7) < 0.9f)) {
            availableResourcesComp = true;
        }
        if (clusters == 3 && (env.getObservation().singletonOrThrow().getFloat(1) < 0.9f || env.getObservation().singletonOrThrow().getFloat(2) < 0.9f || env.getObservation().singletonOrThrow().getFloat(3) < 0.9f || env.getObservation().singletonOrThrow().getFloat(5) < 0.9f || env.getObservation().singletonOrThrow().getFloat(6) < 0.9f || env.getObservation().singletonOrThrow().getFloat(7) < 0.9f) || env.getObservation().singletonOrThrow().getFloat(9) < 0.9f || env.getObservation().singletonOrThrow().getFloat(10) < 0.9f || env.getObservation().singletonOrThrow().getFloat(11) < 0.9f) {
            availableResourcesComp = true;
        }
        if (actionSpace.get(bestAction).singletonOrThrow().getInt(nodes) == 1 && availableResourcesComp == true) {
            NDArray actionRewardIndexesSorted = actionReward.argSort();
            //System.out.println(actionRewardIndexesSorted);
            System.out.println(Arrays.toString(actionRewardIndexesSorted.toArray()));
            //System.out.println(actionRewardIndexesSorted.toLongArray()[3]);
            //System.out.println(actionRewardIndexesSorted.toLongArray()[2]);
            if (clusters == 1) {
                secondBestAction = Math.toIntExact(actionRewardIndexesSorted.toLongArray()[3]);
                if (secondBestAction == 0) {
                    secondBestAction = Math.toIntExact(actionRewardIndexesSorted.toLongArray()[2]);
                }
            }
            if (clusters == 2) {
                secondBestAction = actionRewardIndexesSorted.getInt(7);
            }
            if (clusters == 3) {
                secondBestAction = actionRewardIndexesSorted.getInt(11);
            }
            if (secondBestAction % 4 == 0) {
                if (clusters == 2) {
                    secondBestAction = actionRewardIndexesSorted.getInt(6);
                }
                if (clusters == 3) {
                    secondBestAction = actionRewardIndexesSorted.getInt(10);
                } 
            }
            bestAction = secondBestAction;
        }
        return actionSpace.get(bestAction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trainBatch(Step[] batchSteps) {
        //epoch++;

        if (ResAlloAlgo.trainStep > 0 && ResAlloAlgo.trainStep % TrainResAlloAlgo.SAVE_EVERY_STEPS == 0) {
            syncNets();
        }

        long time = System.nanoTime();
        trainer.addMetric("start_training", time);

        BatchData batchData = new BatchData(null, new ConcurrentHashMap<>(), new ConcurrentHashMap<>()); //Create a TrainingListener.BatchData.

        NDManager temporaryManager = NDManager.newBaseManager(); //Creates a temporary manager for attaching NDArray to reduce the Device(CPU/GPU) memory usage.

        NDList preObservationBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> preObservationBatch.addAll(step.getPreObservation(temporaryManager)));
        NDList preInput = new NDList(NDArrays.concat(preObservationBatch, 0));

        NDList postObservationBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> postObservationBatch.addAll(step.getPostObservation(temporaryManager)));
        NDList postInput = new NDList(NDArrays.concat(postObservationBatch, 0));

        NDList actionBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> actionBatch.addAll(step.getAction()));
        NDList actionInput = new NDList(NDArrays.stack(actionBatch, 0));

        NDList rewardBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> rewardBatch.addAll(new NDList(step.getReward())));
        NDList rewardInput = new NDList(NDArrays.stack(rewardBatch, 0));

        float ave_reward = rewardInput.singletonOrThrow().mean().getFloat();
        ResAlloAlgo.setEpochReward(ave_reward);

        //GradientCollector provides a mechanism to collect gradients during training.
        try (GradientCollector collector = trainer.newGradientCollector()) { 
            NDList QReward = trainer.forward(preInput);
            //NDList targetQReward = trainer.forward(postInput);
            NDList targetQReward = targetTrainer.forward(postInput);

            NDList Q = new NDList(QReward.singletonOrThrow().mul(actionInput.singletonOrThrow()).sum(new int[]{1}));

            NDArray[] targetQValue = new NDArray[batchSteps.length];
            for (int i = 0; i < batchSteps.length; i++) {
                if (batchSteps[i].isTerminal()) {
                    targetQValue[i] = batchSteps[i].getReward();
                } else {
                    targetQValue[i] = targetQReward.singletonOrThrow().get(i).max().mul(rewardDiscount).add(rewardInput.singletonOrThrow().get(i));
                }
            }
            NDList targetQBatch = new NDList();
            Arrays.stream(targetQValue).forEach(value -> targetQBatch.addAll(new NDList(value)));
            NDList targetQ = new NDList(NDArrays.stack(targetQBatch, 0));

            time = System.nanoTime();
            NDArray lossValue = trainer.getLoss().evaluate(targetQ, Q); //Gets the training loss function of the trainer
            collector.backward(lossValue); // Calculate the gradient
            trainer.addMetric("backward", time);
            time = System.nanoTime();
            batchData.getLabels().put(targetQ.singletonOrThrow().getDevice(), targetQ);
            batchData.getPredictions().put(Q.singletonOrThrow().getDevice(), Q);
            trainer.addMetric("training-metrics", time);

            //trainer.step(); //Updates the model parameters
        }
        trainer.notifyListeners(listener -> listener.onTrainingBatch(trainer, batchData));
        
        for (Step step : batchSteps) {
            step.getPreObservation().attach(step.getManager());
            step.getPostObservation().attach(step.getManager());
        }
        temporaryManager.close();  //close the temporary manager
    }

    /**
	 * Method used during the training phase to synchronize the model's weights 
     * of the target network with through the ones of the online network.
	 */
    protected final void syncNets() {
        for (Pair<String, Parameter> params : trainer.getModel().getBlock().getParameters()) {
            //targetTrainer.getModel().getBlock().getParameters().get(params.getKey()).getArray().set(params.getValue().getArray().duplicate());
            params.getValue().getArray().duplicate().copyTo(targetTrainer.getModel().getBlock().getParameters().get(params.getKey()).getArray());
        }
    }

}
