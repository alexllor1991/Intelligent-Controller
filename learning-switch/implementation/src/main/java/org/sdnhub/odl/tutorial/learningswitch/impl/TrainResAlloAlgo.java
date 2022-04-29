package org.sdnhub.odl.tutorial.learningswitch.impl;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
//import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
//import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.initializer.NormalInitializer;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.LinearTracker;
import ai.djl.training.tracker.Tracker;
import com.rlresallo.agents.EpsilonGreedy;
import com.rlresallo.agents.QAgent;
import com.rlresallo.agents.RlAgent;
import com.rlresallo.Environment;
//import com.rlresallo.Arguments;
import org.sdnhub.odl.tutorial.learningswitch.impl.ResAlloAlgo;
//import org.apache.commons.cli.ParseException;
//import org.sdnhub.odl.tutorial.learningswitch.impl.TutorialL2Forwarding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
//import java.io.LineNumberReader;
//import java.nio.charset.MalformedInputException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class TrainResAlloAlgo {
    private static final Logger logger = LoggerFactory.getLogger(TrainResAlloAlgo.class);

    public static final int OBSERVE = 1000; // steps to observe before training
    public static final int EXPLORE = 3000000; // frames over which to anneal epsilon
    public static final int SAVE_EVERY_STEPS = 100000; // same model every x steps
    public static final int REPLAY_BUFFER_SIZE = 50000; // number of previous transitions to remember
    public static final float REWARD_DISCOUNT = 0.9f; //decay rate of past observations
    public static final float INITIAL_EPSILON = 0.01f;
    public static final float FINAL_EPSILON = 0.0001f;
    public static final String PARAMS_PREFIX = "dqn-trained";
    public static final String MODEL_PATH = "src/main/resources/model";
    public static int NUMBER_INPUTS; // number of inputs in the neural network
    public static int NUMBER_OUTPUTS; // number of outputs in the neural network
    public static int HIDDEN_NEURONS; // number of neurons in the hidden layer

    static Environment.Step[] batchSteps;

    public TrainResAlloAlgo(int noClusters, int noNodes, int batchSize, boolean preTrained, boolean testing) {
        NUMBER_INPUTS = 2 * noClusters * noNodes + 5;
        NUMBER_OUTPUTS =  noClusters * noNodes + 1;
        HIDDEN_NEURONS = (NUMBER_INPUTS + NUMBER_OUTPUTS) / 2;

        /*
        Model model = createOrLoadModel(preTrained);
        if (testing) {
            test(model);
        } else {
            train(batchSize, preTrained, testing, model, noClusters, noNodes);
        }
        */
    }

    /*
    public static void main(String[] args) throws ParseException, IOException, MalformedModelException {
        Arguments arguments = Arguments.parseArgs(args);
        Model model = createOrLoadModel(arguments);
        if (arguments.isTesting()) {
            test(model);
        } else {
            train(arguments, model, noClusters, noNodes);
        }
    }
    */

    public Model createOrLoadModel(boolean preTrained) throws IOException, MalformedModelException {
        Model model = Model.newInstance("QNetwork"); // Creates an empty model instance.
        model.setBlock(getBlock()); // Sets the block (It is a composable function that forms a NN) for the Model for training and inference.
        if (preTrained) {
            model.load(Paths.get(MODEL_PATH), PARAMS_PREFIX); // Loads the model from the modelPath and the given name.
        }
        
        return model;
    }

    public void train(int miniBatch, boolean preTrained, boolean testing, Model model, int noClusters, int noNodes) {
        //boolean withGraphics = arguments.withGraphics();
        boolean training = !testing;
        int batchSize = miniBatch; // size of the mini batch

        ResAlloAlgo env = new ResAlloAlgo(NDManager.newBaseManager(), batchSize, REPLAY_BUFFER_SIZE, noClusters, noNodes);

        DefaultTrainingConfig config = setupTrainingConfig();
        try (Trainer trainer = model.newTrainer(config)) {
            trainer.initialize(new Shape(1, NUMBER_INPUTS)); // Initialize the model to be trained. 
            trainer.notifyListeners(listener -> listener.onTrainingBegin(trainer)); // Listens to the beginning of training.

            RlAgent agent = new QAgent(trainer, REWARD_DISCOUNT);
            // Tracker represents a hyper-parameter that changes gradually through the training process.
            Tracker exploreRate = new LinearTracker.Builder()  // A tracker that is updated by a constant factor.
                                        .setBaseValue(INITIAL_EPSILON) // Sets the initial value after no steps.
                                        .optSlope(-(INITIAL_EPSILON - FINAL_EPSILON) / EXPLORE) // Sets the value of the linear slope.
                                        .optMinValue(FINAL_EPSILON) // Sets the minimum value for a negative slope.
                                        .build(); // Builds a LinearTracker block.
            agent = new EpsilonGreedy(agent, exploreRate);

            int numOfThreads = 2;
            List<Callable<Object>> callables = new ArrayList<>(numOfThreads); 
            callables.add(new GeneratorCallable(env, agent, training));
            if(training) {
                callables.add(new TrainerCallable(model, agent));
            }
            ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);
            try {
                try {
                    List<Future<Object>> futures = new ArrayList<>();
                    for (Callable<Object> callable : callables) {
                        futures.add(executorService.submit(callable));
                    }
                    for (Future<Object> future : futures) {
                        future.get();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("", e);
                }
            } finally {
                executorService.shutdown();
            }
        }
    }

    public void test(Model model) {
        ResAlloAlgo env = new ResAlloAlgo(NDManager.newBaseManager(), 1, 1, 1, 1);
        DefaultTrainingConfig config = setupTrainingConfig();
        try (Trainer trainer = model.newTrainer(config)) {
            RlAgent agent = new QAgent(trainer, REWARD_DISCOUNT);
            while (true) {
                env.runEnvironment(agent, false);
            }
        }
    }

    public static class TrainerCallable implements Callable<Object> {
        private final RlAgent agent;
        private final Model model;

        public TrainerCallable(Model model, RlAgent agent) {
            this.model = model;
            this.agent = agent;
        }

        @Override
        public Object call() throws Exception {
            while (ResAlloAlgo.trainStep < EXPLORE) {
                Thread.sleep(0);
                if (ResAlloAlgo.envStep > OBSERVE) {
                    this.agent.trainBatch(batchSteps);
                    ResAlloAlgo.trainStep++;
                    if (ResAlloAlgo.trainStep > 0 && ResAlloAlgo.trainStep % SAVE_EVERY_STEPS == 0) {
                        model.save(Paths.get(MODEL_PATH), "dqn-" + ResAlloAlgo.trainStep);
                    }
                }
            }
            return null;
        }
    }

    private static class GeneratorCallable implements Callable<Object> {
        private final ResAlloAlgo env;
        private final RlAgent agent;
        private final boolean training;

        public GeneratorCallable(ResAlloAlgo env, RlAgent agent, boolean training) {
            this.env = env;
            this.agent = agent;
            this.training = training;
        }

        @Override
        public Object call() {
            while (ResAlloAlgo.trainStep < EXPLORE) {
                batchSteps = env.runEnvironment(agent, training);
            }
            return null;
        }
    }

    public static SequentialBlock getBlock() {
        
        /**
         * A Block whose children form a chain of blocks with each child block feeding its output to the next. 
         * The output of the last child is returned as the output of the SequentialBlock.
         */ 
        return new SequentialBlock() 
                .add(Linear
                        .builder()
                        .setUnits(HIDDEN_NEURONS).build()) // Number of neurons in the hidden layer can be defined as the mean of the input and output layer.
                .add(Activation::relu) // ReLU is defined by: y=max(0,x).

                .add(Linear
                        .builder() // A Linear block applies a linear transformation Y=XWT+b.
                        .setUnits(NUMBER_OUTPUTS).build()); // Sets the number of output channels.
    }

    /**
     * An interface that is responsible for holding the configuration required by Trainer.
     * A Trainer requires an Initializer to initialize the parameters of the model, 
     * an Optimizer to compute gradients and update the parameters according to a Loss function. 
     * It also needs to know the Evaluators that need to be computed during training.
     */
    public static DefaultTrainingConfig setupTrainingConfig() {
        return new DefaultTrainingConfig(Loss.l2Loss()) // Creates an instance of DefaultTrainingConfig with the given Loss. Calculates L2Loss between label and prediction, a.k.a. MSE(Mean Square Error).
                .optOptimizer(Adam.builder().optLearningRateTracker(Tracker.fixed(1e-6f)).build()) // Adam is a generalization of the AdaGrad Optimizer. Sets the learningRateTracker for this optimizer.
                .addEvaluator(new Accuracy())  // Adds an Evaluator that needs to be computed during training. Accuracy is an Evaluator that computes the accuracy score. It is defined as accuracy(y,y^)=1/n∑n−1i=01(yi^==yi).
                .optInitializer(new NormalInitializer()) // NormalInitializer initializes weights with random values sampled from a normal distribution with a mean of zero and standard deviation of sigma. Default standard deviation is 0.01.
                .addTrainingListeners(TrainingListener.Defaults.basic()); // A basic TrainingListener set with minimal recommended functionality.
    }
}
