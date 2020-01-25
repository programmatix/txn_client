import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.security.Policy;
import java.util.concurrent.ExecutionException;

public class main {
    public static void main(String args[]) throws IOException, ParseException, ExecutionException {
        setup(args);


    }

    public static void setup(String[] params) throws IOException, ParseException, ExecutionException {
        Configuration  configParams  =  new Configuration(params[0]);
        configParams.readandStoreParams();
        configParams.printConfiguration();

        ClusterInstaller cb = new ClusterInstaller(configParams);
        cb.installCluster();
        System.out.println("Completed cluster installation");
        System.exit(-1);
    }
}
