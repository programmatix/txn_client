/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

/**
 * Class to read JUnit XML results.
 */
public class XUnitReader {
  final File path;
  final Document doc;
  boolean verbose = false;

  public XUnitReader(File path) throws Exception {
    this.path = path;
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    doc = db.parse(path);
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public void readResult(PrintStream ps) throws Exception {
    // Print the name and the result?

    NodeList suites = doc.getElementsByTagName("testsuite");
    for (int i = 0; i < suites.getLength(); i++) {
      Element curSuite = (Element)suites.item(i);
      String suiteName = curSuite.getAttribute("name");
      int nErrors = Integer.parseInt(curSuite.getAttribute("errors"));
      int nFails = Integer.parseInt(curSuite.getAttribute("failures"));
      NodeList cases = curSuite.getElementsByTagName("testcase");

      for (int j = 0; j < cases.getLength(); j++) {
        Element curCase = (Element)cases.item(j);
        String caseName = curCase.getAttribute("name");
        String className = curCase.getAttribute("classname");
        ps.printf("%s/%s.%s: ", suiteName, caseName, className);
        if (nErrors > 0 || nFails > 0) {
          ps.printf("BAD: E=%d, F=%d", nErrors, nFails);
        } else {
          ps.print("OK");
        }
        if (verbose) {
          Element output = (Element)curCase.getElementsByTagName("system-err").item(0);
          ps.printf("%nOUTPUT: %s", output.getTextContent());
        }
        ps.println();
        ps.flush();
      }
    }
  }

  public static void main(String[] args) throws Exception {
    List<String> ll = Arrays.asList(args);
    boolean verbose = ll.contains("-v");
    for (String s : ll) {
      if (s.startsWith("-")) {
        continue;
      }
      XUnitReader xr = new XUnitReader(new File(s));
      xr.setVerbose(verbose);
      xr.readResult(System.out);
      if (verbose) {
        System.out.println("====================");
        System.out.println();
      }
    }
  }
}
