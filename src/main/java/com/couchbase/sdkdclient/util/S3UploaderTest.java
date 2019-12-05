package com.couchbase.sdkdclient.util;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import com.couchbase.sdkdclient.batch.HistoryOptions;
import com.couchbase.sdkdclient.options.FileOption;
import com.couchbase.sdkdclient.options.OptBuilder;
import com.couchbase.sdkdclient.options.OptionParser;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.options.OptionUtils;
import com.couchbase.sdkdclient.options.RawOption;
import com.couchbase.sdkdclient.options.StringOption;

public class S3UploaderTest {
	
	private static S3Uploader s3u;
	private static S3Options s3Options = new S3Options();
	
	static class S3Options extends HistoryOptions {
		  private FileOption optS3Creds = OptBuilder.startExistingFile("s3auth")
		          .help("S3 credentials")
		          .defl("S3Creds_tmp")
		          .shortAlias("A")
		          .build();

		  private StringOption optS3Bucket = OptBuilder.startString("s3-bucket")
		          .help("Bucket name for S3 uploads")
		          .defl("sdk-testresults.couchbase.com")
		          .build();
		  
		  private StringOption optsdkdLogs = OptBuilder.startString("sdkd-logs")
		          .help("Sdkd logs")
		          .shortAlias("F")
		          .build();
		  
		  
		  public File getS3Credentials() {
			  return optS3Creds.getValue();
		  }
		  
		  public String getSdkdLogs() {
			  return optsdkdLogs.getValue();
		  }

		  public String getS3Bucket() {
			  return optS3Bucket.getValue();
		  }
		  
		
          @Override
	      public OptionTree getOptionTree() {
			  OptionTree tree = super.getOptionTree();
			  for (RawOption opt : OptionTree.extract(this, S3Options.class)) {
			      tree.addOption(opt);
			  }
			  return tree;
	      }
		  
	}
	

	public static void main(String[] args) throws Exception {
        
        	OptionParser parser = new OptionParser(args);
        	parser.setPrintAndExitOnError(true);
        	OptionUtils.scanInlcudes(parser);
        	parser.addTarget(s3Options.getOptionTree());
        	parser.apply();
        
        	OptionUtils.sealTree(s3Options.getOptionTree());
        	File S3Creds = s3Options.getS3Credentials();
        	String S3Bucket = s3Options.getS3Bucket();
        	String SdkdLogs = s3Options.getSdkdLogs();
        
       		if (SdkdLogs == null){
            		throw new Exception("Required to input upload log file path! -F <log_file>");
        	}
        
        	File SdkdLogs_file = new File(SdkdLogs);
        	URL url;
        
        	System.out.println("=========== Connecting S3 Bucket ==========");
        	s3u = new S3Uploader(S3Bucket,S3Creds);
        	s3u.connect();

        	final AtomicReference<URL> boxedUrl = new AtomicReference<URL>();
        	boxedUrl.set(s3u.uploadFile(null,SdkdLogs_file,SdkdLogs, "txt"));
        
        	url = boxedUrl.get();
        	if (url == null) {
           		 url = URI.create("http://upload.error/no/file").toURL();
        	} else {
            		System.out.format("Uploaded Successfully => \n%s\n", url.toString());
        	}
   	 }	
}
