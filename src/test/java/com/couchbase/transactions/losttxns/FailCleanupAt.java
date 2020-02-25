package com.couchbase.transactions.losttxns;

/*
 * Simulate failures at the various stages the cleanup process goes through.
 */
public enum FailCleanupAt {
    DoNotFailCleanup,

    // Removing as this wasn't actually being tested.  It's done in handleCleanupRequest and the tests check from
    // cleanupATREntry onwards
    //    BeforeAtrGet,

    // Before cleaning up a doc, we get it.  This tests failing on that get.  We don't test on Command2 and 3 because
    // it's not worth the test explosion.
    BeforeCleaningOnGetCommand1,

    // Test if the actual cleanup of a doc fails, e.g. when removing its txn links or whatever is needed
    BeforeCleaningCommand1,
    BeforeCleaningCommand2,
    BeforeCleaningCommand3,

    BeforeATRRemove,

    // [RETRY-ERR-AMBIG]  Same as above, but what if they error e.g. with DurabilityAmbigous, but actually succeed?
//    BeforeCleaningCommand1ButActuallySucceed,
//    BeforeCleaningCommand2ButActuallySucceed,
//    BeforeCleaningCommand3ButActuallySucceed,
//    BeforeATRRemoveButActuallySucceed
}
