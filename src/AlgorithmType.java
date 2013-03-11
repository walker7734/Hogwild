/**
 * Created with IntelliJ IDEA.
 * User: bdwalker
 * Date: 3/10/13
 * Time: 2:17 PM
 * To change this template use File | Settings | File Templates.
 */
public enum AlgorithmType {
    //runs SGD with Hogwild! with no locking and treats non sparse data the same as sparse data
    NORMAL,

    //thread randomly decide whether to update non sparse data, essentially simulating it as being sparse
    RANDOM,

    //locks non-sparse fields so data races won't occur
    LOCK,

    //schedules the updates to non sparse data to prevent data races
    SCHEDULING,

    //replicates the non-sparse data across each thread so there is no chance of a data race
    REPLICATE,


    SINGLE_THREAD,

    ASSIGNMENT
}
