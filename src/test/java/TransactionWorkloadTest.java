import com.couchbase.Constants.Strings;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.grpc.protocol.ResumableTransactionServiceGrpc;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.transactions.TestUtils;
import com.couchbase.transactions.util.DocValidator;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static com.couchbase.grpc.protocol.TxnClient.*;
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
    private static String TXN_SERVER_HOSTNAME = "localhost";
    private static String CLUSTER_HOSTNAME = "172.23.105.65";

    private static int PORT = 8050;
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
        ManagedChannel channel = ManagedChannelBuilder.forAddress(TXN_SERVER_HOSTNAME, PORT).usePlaintext().build();
        stub = ResumableTransactionServiceGrpc.newBlockingStub(channel);

        cluster = Cluster.connect(CLUSTER_HOSTNAME, Strings.ADMIN_USER, Strings.PASSWORD);
        defaultCollection = cluster.bucket("default").defaultCollection();
        conn_info  conn_create_req =
                conn_info.newBuilder()
                        .setHandleHostname(CLUSTER_HOSTNAME)
                        .setHandleBucket("default")
                        .setHandlePort(8091)
                        .setHandleUsername(Strings.ADMIN_USER)
                        .setHandlePassword(Strings.PASSWORD)
                        .setHandleAutofailoverMs(5)
                        .build();
        APIResponse response = stub.createConn(conn_create_req);

    }

    @Test
    void simpleCommit() {
        TransactionsFactoryCreateResponse factory =
            stub.transactionsFactoryCreate(createDefaultTransactionsFactory()
                .build());

        assertTrue(factory.getSuccess());

        TransactionCreateResponse create =
            stub.transactionCreate(TransactionCreateRequest.newBuilder()
                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                .build());

        assertTrue(create.getSuccess());
        String transactionRef = create.getTransactionRef();


        TransactionGenericResponse commit =
            stub.transactionCommit(TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());

        assertTrue(commit.getSuccess());

        assertTrue(stub.transactionsFactoryClose(TransactionsFactoryCloseRequest.newBuilder()
            .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
            .build()).getSuccess());
    }

    @Test
    void simpleInsert() {
        TransactionsFactoryCreateResponse factory =
            stub.transactionsFactoryCreate(createDefaultTransactionsFactory()
                .build());

        assertTrue(factory.getSuccess());

        TransactionCreateResponse create =
            stub.transactionCreate(TransactionCreateRequest.newBuilder()
                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                .build());

        assertTrue(create.getSuccess());
        String transactionRef = create.getTransactionRef();

        String docId = UUID.randomUUID().toString();
        JsonObject content = JsonObject.create().put("hello", "world");

        TransactionGenericResponse insert =
            stub.transactionInsert(TransactionInsertRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .setDocId(docId)
                .setContentJson(content.toString())
                .build());

        assertInsertedDocIsStaged(insert, docId);

        TransactionGenericResponse commit =
            stub.transactionCommit(TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());

        assertTrue(commit.getSuccess());

        assertTrue(stub.transactionsFactoryClose(TransactionsFactoryCloseRequest.newBuilder()
            .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
            .build()).getSuccess());
    }

    @Test
    void failsBeforeCommit() {
        TransactionsFactoryCreateResponse factory =
            stub.transactionsFactoryCreate(createDefaultTransactionsFactory()
                .addHook(Hook.newBuilder()
                    .setHookPoint(HookPoint.BEFORE_ATR_COMMIT)
                    .setHookCondition(HookCondition.ALWAYS)
                    .setHookAction(HookAction.FAIL_NO_ROLLBACK)
                    .build())
                .build());

        assertTrue(factory.getSuccess());

        TransactionCreateResponse create =
            stub.transactionCreate(TransactionCreateRequest.newBuilder()
                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                .build());

        assertTrue(create.getSuccess());
        String transactionRef = create.getTransactionRef();

        String docId = UUID.randomUUID().toString();
        JsonObject content = JsonObject.create().put("hello", "world");

        TransactionGenericResponse insert =
            stub.transactionInsert(TransactionInsertRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .setDocId(docId)
                .setContentJson(content.toString())
                .build());

        assertInsertedDocIsStaged(insert, docId);

        TransactionGenericResponse commit =
            stub.transactionCommit(TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());

        assertFalse(commit.getSuccess());
        assertInsertedDocIsStaged(insert, docId);

        assertTrue(stub.transactionsFactoryClose(TransactionsFactoryCloseRequest.newBuilder()
            .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
            .build()).getSuccess());
    }

    private static TransactionsFactoryCreateRequest.Builder createDefaultTransactionsFactory() {
        return TestUtils.createDefaultTransactionsFactory();
    }

    private void assertInsertedDocIsStaged(TransactionGenericResponse insert, String docId) {
        DocValidator.assertInsertedDocIsStaged(defaultCollection, docId);
    }
}
