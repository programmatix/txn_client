package com.couchbase.sdkdclient.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.gdata.client.docs.DocsService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.acl.AclEntry;
import com.google.gdata.data.acl.AclRole;
import com.google.gdata.data.acl.AclScope;
import com.google.gdata.data.docs.DocumentEntry;
import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.util.ServiceException;


/**
 * Created with IntelliJ IDEA.
 * User: deeptida
 * Date: 11/7/13
 * Time: 3:46 PM
 */
public class SpreadsheetUploader {

    URL documentFeedURL;

    public SpreadsheetUploader() {
    }

    /**
     * Uploads the file and returns the entered document's object i.e. DocumentListEntry.
     * @param filePath
     * @param url
     * @return DocumentListEntry
     * @throws IOException
     * @throws ServiceException
     */
    private DocumentListEntry getDocumentListEntry(String filePath,String url, DocsService service) throws IOException, ServiceException {
        documentFeedURL = new URL(url);
        File documentFile = new File(filePath);
        DocumentEntry newDocument = new DocumentEntry();
        String mimeType = DocumentListEntry.MediaType.fromFileName(documentFile.getName()).getMimeType();
        newDocument.setFile(documentFile, mimeType);
        // Set the title for the new document. For this example we just use the
        // filename of the uploaded file.
        newDocument.setTitle(new PlainTextConstruct(documentFile.getName()));
        // insert the file
        return service.insert(documentFeedURL, newDocument);
    }

    /**
     * Print the details of the uploaded document.
     * @param doc
     */
    public void printDocumentEntry(DocumentListEntry doc) {
        String shortId = doc.getId().substring(doc.getId().lastIndexOf('/') + 1);
        System.out.println(" -- Document(" + shortId + "/" + doc.getTitle().getPlainText()
                + ")");
    }

    /**
     * Print the details of the shared document.
     * @param acl
     */
    public void printAclEntry(AclEntry acl) {
        String shortId = acl.getId().substring(acl.getId().lastIndexOf('/') + 1);
        System.out.println(" -- Document(" + shortId + "/" + acl.getTitle().getPlainText()
                + ")");
    }

    /**
     * Upload a file to google docs and share
     * it with different users in edit mode.
     * @param filePath
     * @param url
     * @param type
     * @param value
     * @param role
     * @throws MalformedURLException
     * @throws IOException
     * @throws ServiceException
     */
    public void uploadShareFile(String filePath, String url, AclScope.Type type,
                                 String value, AclRole role, DocsService service)
            throws MalformedURLException, IOException, ServiceException {
        // Instantiate an AclEntry object to update sharing permissions.
        AclEntry acl = new AclEntry();
        // Set the ACL scope.
        acl.setScope(new AclScope(type, value));
        // Set the ACL role.
        acl.setRole(role);
       // get the uploaded document list entry.
        DocumentListEntry documentListEntry = getDocumentListEntry(filePath, url, service);
        // Print the document entry.
        printDocumentEntry(documentListEntry);
        // Insert the new role into the ACL feed.
        AclEntry aclEntry = service.insert(new URL(documentListEntry.getAclFeedLink().getHref()), acl);
        // Print the ACL feed.
        printAclEntry(aclEntry);
    }
}