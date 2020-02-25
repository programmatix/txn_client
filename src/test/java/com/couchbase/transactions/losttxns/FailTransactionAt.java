package com.couchbase.transactions.losttxns;

/*
 * Simulate the txn failing at various points, creating a lost txn.
 */
public enum FailTransactionAt {
    BeforeATRPending,

    // Simulate {LOST-STATE-EXPIRED-NOABORT} - leave in PENDING
    BeforeStagingCommand1LeavePending,
    BeforeStagingCommand2LeavePending,
    BeforeStagingCommand3LeavePending,
    BeforeATRCommitLeavePending,

    // Simulate {LOST-STATE-EXPIRED-ABORTED} - leave in ABORTED
    // Update: with {EXP-ROLLBACK] these can no longer happen.  Leaving the logic but these tests won't be run.
    BeforeStagingCommand1LeaveAborted,
    BeforeStagingCommand2LeaveAborted,
    BeforeStagingCommand3LeaveAborted,
    BeforeATRCommitLeaveAborted,

    // Simulate {LOST-STATE-EXPIRED-COMMIT} - leave in COMMITTED
    BeforeCommitOrRollbackCommand1,
    BeforeCommitOrRollbackCommand2,
    BeforeCommitOrRollbackCommand3,
    BeforeATRCompleteOrRolledBack
}
