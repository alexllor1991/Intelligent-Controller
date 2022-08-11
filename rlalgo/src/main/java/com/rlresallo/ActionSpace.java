package com.rlresallo;

import ai.djl.ndarray.NDList;
import ai.djl.util.RandomUtils;
import java.util.ArrayList;
import java.util.Arrays;

/** Available actions that can be taken in an environment. */
public class ActionSpace extends ArrayList<NDList> {

    private static final long serialVersionUID = 8683452581122892189L;

    /**Returns a random action.
     *
     * @return a random action
     */
    public NDList randomAction() {
        //System.out.println(Arrays.toString(get(RandomUtils.nextInt(size())).toArray()));
        return get(RandomUtils.nextInt(size()));
    }
}
