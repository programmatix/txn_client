#!/usr/bin/env python

import os
import os.path
import tinys3
import argparse
import gzip
from datetime import datetime

S3_BUCKET = "sdk-testresults.couchbase.com"
S3_SECRET = "PRnYRzepuMBHNTJ5MRRsfL6kxCoJ/VYSb5XQMvZ9"
S3_ACCESS = "AKIAJIAHNTVSCR4PWVAQ"
S3_DIR = "sdkd"


def _upload_data(file):
    conn = tinys3.Connection(S3_ACCESS, S3_SECRET, tls=True)
    f = open(file, 'rb')
    conn.upload(file, f, S3_BUCKET)
    return "http://{0}.s3.amazonaws.com/{1}".format(S3_BUCKET, file)


def s3_upload(fname, dstname = None):
    data = open(fname, "r").read()
    if not dstname:
        dstname = os.path.basename(fname)

    return _upload_data(data, dstname)

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("-f", "--file", help = "Filename to upload",
                    required = True)


    opts = ap.parse_args()
    today = datetime.utcnow().timetuple()
    output = ""

    for i in today:
        output += "{0}".format(i)

    to_upload = opts.file

    with open('out.txt', 'rb') as f_in, gzip.open('{0}.gz'.format(output), 'wb') as f_out:
        f_out.writelines(f_in)
        f_out.close()
        f_in.close()

    print "Uploading", to_upload
    url = _upload_data('{0}.gz'.format(output))
    print "Uploaded to", url