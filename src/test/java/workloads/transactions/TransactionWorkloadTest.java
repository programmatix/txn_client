package workloads.transactions;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.grpc.protocol.ResumableTransactionServiceGrpc;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.sdkdclient.protocol.Strings;
import com.couchbase.sdkdclient.stester.STester;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * This is a prototype attempt to add an additional method of running tests in batches.
 * <p>
 * Instead of using BRun, this will allow running them via JUnit.  This should allow tests to run from the CLI and from
 * IDEs and provides a lot of useful functionality, like rerunning just failed tests.
 */
// TODO: STester can configure a cluster and start the servers for us.  For now, do that by hand.
// TODO: Share STester between test files
// TODO: Allow STester to be configurable from config files
// TODO: Get these tests running from CLI

public class TransactionWorkloadTest {
    private static String HOSTNAME = "localhost";
    private static int PORT = 8050;
    private static STester stester = null;
    private static ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub = null;
    private static Cluster cluster;
    private static Collection defaultCollection;

    @BeforeAll
    static void beforeAll() {

        //        HarnessBuilder hb = new HarnessBuilder();
        //        RunContextBuilder rcb = new RunContextBuilder(Configured.create(hb));
        //        stester = new STester(Configured.create(hb));
        //        stester.configure(Configured.create(rcb));
        //        stester.run();

        // GRPC is used to connect to the server(s)
        ManagedChannel channel = ManagedChannelBuilder.forAddress(HOSTNAME, PORT).usePlaintext().build();
        stub = ResumableTransactionServiceGrpc.newBlockingStub(channel);

        cluster = Cluster.connect(HOSTNAME, Strings.ADMIN_USER, Strings.PASSWORD);
        defaultCollection = cluster.bucket("default").defaultCollection();
    }

    @Test
    void simpleCommit() {
        TxnClient.TransactionsFactoryCreateResponse factory =
            stub.transactionsFactoryCreate(createDefaultTransactionsFactory()
                .build());

        assertTrue(factory.getSuccess());

        TxnClient.TransactionCreateResponse create =
            stub.transactionCreate(TxnClient.TransactionCreateRequest.newBuilder()
                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                .build());

        assertTrue(create.getSuccess());
        String transactionRef = create.getTransactionRef();


        TxnClient.TransactionGenericResponse commit =
            stub.transactionCommit(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());

        assertTrue(commit.getSuccess());

        assertTrue(stub.transactionsFactoryClose(TxnClient.TransactionsFactoryCloseRequest.newBuilder()
            .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
            .build()).getSuccess());
    }

    @Test
    void simpleInsert() {
        TxnClient.TransactionsFactoryCreateResponse factory =
            stub.transactionsFactoryCreate(createDefaultTransactionsFactory()
                .build());

        assertTrue(factory.getSuccess());

        TxnClient.TransactionCreateResponse create =
            stub.transactionCreate(TxnClient.TransactionCreateRequest.newBuilder()
                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                .build());

        assertTrue(create.getSuccess());
        String transactionRef = create.getTransactionRef();

        String docId = UUID.randomUUID().toString();
        JsonObject content = JsonObject.create().put("hello", "world");

        TxnClient.TransactionGenericResponse insert =
            stub.transactionInsert(TxnClient.TransactionInsertRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .setDocId(docId)
                .setContentJson(content.toString())
                .build());

        assertInsertedDocIsStaged(insert, docId);

        TxnClient.TransactionGenericResponse commit =
            stub.transactionCommit(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());

        assertTrue(commit.getSuccess());

        assertTrue(stub.transactionsFactoryClose(TxnClient.TransactionsFactoryCloseRequest.newBuilder()
            .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
            .build()).getSuccess());
    }

    @Test
    void failsBeforeCommit() {
        TxnClient.TransactionsFactoryCreateResponse factory =
            stub.transactionsFactoryCreate(createDefaultTransactionsFactory()
                .addHook(TxnClient.Hook.BEFORE_ATR_COMMIT)
                .addHookCondition(TxnClient.HookCondition.ALWAYS)
                .addHookErrorToRaise(TxnClient.HookErrorToRaise.FAIL_NO_ROLLBACK)
                .build());

        assertTrue(factory.getSuccess());

        TxnClient.TransactionCreateResponse create =
            stub.transactionCreate(TxnClient.TransactionCreateRequest.newBuilder()
                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                .build());

        assertTrue(create.getSuccess());
        String transactionRef = create.getTransactionRef();

        String docId = UUID.randomUUID().toString();
        JsonObject content = JsonObject.create().put("hello", "world");

        TxnClient.TransactionGenericResponse insert =
            stub.transactionInsert(TxnClient.TransactionInsertRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .setDocId(docId)
                .setContentJson(content.toString())
                .build());

        assertInsertedDocIsStaged(insert, docId);

        TxnClient.TransactionGenericResponse commit =
            stub.transactionCommit(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());

        assertFalse(commit.getSuccess());
        assertInsertedDocIsStaged(insert, docId);

        assertTrue(stub.transactionsFactoryClose(TxnClient.TransactionsFactoryCloseRequest.newBuilder()
            .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
            .build()).getSuccess());
    }

    private static TxnClient.TransactionsFactoryCreateRequest.Builder createDefaultTransactionsFactory() {
        return TxnClient.TransactionsFactoryCreateRequest.newBuilder()

            // Disable these threads so can run multiple Transactions (and hence hooks)
            .setCleanupClientAttempts(false)
            .setCleanupLostAttempts(false)

            // This is default durability for txns library
            .setDurability(TxnClient.Durability.MAJORITY)

            // Plenty of time for manual debugging
            .setExpirationSeconds(120);
    }

    private void assertInsertedDocIsStaged(TxnClient.TransactionGenericResponse insert, String docId) {
        assertTrue(insert.getSuccess());
        GetResult get = defaultCollection.get(docId);
        // TXNJ-125: inserted doc will be there, but should be empty
        assertEquals(0, get.contentAsObject().size());
    }
}
