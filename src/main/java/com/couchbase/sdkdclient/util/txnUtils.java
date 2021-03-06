package com.couchbase.sdkdclient.util;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.sdkdclient.protocol.Strings;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class txnUtils {


    public static boolean verifyDocuments(List<String> keys, JsonObject docContent, boolean docExists,String hostname) {
        boolean doc_Exists;
        try{
            Cluster cluster = Cluster.connect(hostname, Strings.ADMIN_USER, Strings.PASSWORD);
            Collection defaultCollection= cluster.bucket("default").defaultCollection();
            for (String key : keys) {
                if (docExists) {
                    JsonObject body = defaultCollection.get(key).contentAs(JsonObject.class);
                    assertEquals(docContent, body);
                } else {
                    doc_Exists = defaultCollection.exists(key).exists();
                    System.out.println("Checking  delete for Key: "+key+ " Does key exist: "+doc_Exists);
                    System.out.println("Content: "+defaultCollection.get(key).toString());
                    assertEquals(docExists, doc_Exists);
                }
            }
            return true;
        }catch(Exception e){
            System.out.println("Exception during verification: "+e);
            return false;
        }
    }
}
