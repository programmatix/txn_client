package com.couchbase.sdkdclient.util;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.sdkdclient.protocol.Strings;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class txnUtils {


    public static boolean verifyDocuments(List<String> keys, JsonObject docContent, boolean docExists,String hostname) {
        Cluster cluster = Cluster.connect(hostname, Strings.ADMIN_USER, Strings.PASSWORD);
        Collection defaultCollection= cluster.bucket("default").defaultCollection();
        for (String key : keys) {
            if (docExists) {
                JsonObject body = defaultCollection.get(key).contentAs(JsonObject.class);
                assertEquals(docContent, body);
            } else {
                boolean doc_Exists = defaultCollection.exists(key).exists();
                assertEquals(docExists, doc_Exists);
            }
        }
        return true;
    }
}
