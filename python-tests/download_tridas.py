#!/usr/bin/python
# -*- coding: utf-8 -*-
"""Download TRiDaS (XML) from DCCD projects archive


Requires:
    docopt
    requests
    progress.py

Usage:
    download_tridas.py --from URL -U username [-P password] [-o OUT_DIR] (-f ID_FILE | <id>...)
    download_tridas.py ( -h | --help)

Options:
    --from URL        URL for the project level [default: http://localhost:8080/dccd-rest/rest/project]
    -U username       Username
    -P password       Password; prompted for when not given as option
    -f ID_FILE        File with the id's (one for each line)
    -o OUT_DIR        Output directory; where files are saved [default: .]
    -h --help         Show this screen

"""
import docopt
import requests
import getpass
from requests.auth import HTTPBasicAuth

import progress


def download(restURL, username, password, sid, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/" + sid + "/tridas", auth=HTTPBasicAuth(username, password))
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s" % (r.status_code, sid)

    # The TRiDaS XML won't be huge, just save it
    path = output_dir + "/" + sid.replace(':', '_') + "_tridas.xml"
    with open(path, "wb") as code:
        code.write(r.content)

        #print "saved as: " + path


if __name__ == '__main__':
    try:
        # Parse arguments, use file docstring as a parameter definition
        arguments = docopt.docopt(__doc__)

        # restURL = "http://localhost:1405/dccd-rest/rest/project"
        restURL = "http://localhost:8080/dccd-rest/rest/project"
        if arguments["--from"]:
            restURL = arguments["--from"]

        if arguments["-U"]:
            username = arguments["-U"]  #"normaltestuser"

        if arguments["-P"]:
            password = arguments["-P"]  #"testtest"
        else:
            password = getpass.getpass()

        output_dir = '.'
        if arguments["-o"]:
            output_dir = arguments["-o"]

        #sids = ["dccd:527", "dccd:849"]
        sids = []
        if arguments["-f"]:
            sid_file = arguments["-f"]
            print "Reading id's from: " + sid_file
            sids = [line.strip() for line in open(sid_file)]
        elif arguments["<id>"]:
            sids = arguments["<id>"]

        print "Downloading from: " + restURL
        print "Saving in: " + output_dir

        # actual downloading
        num_sids = len(sids)
        sid_cnt = 0
        #print "id's: " + ", ".join(sids)
        print "Start downloading %s files..." % num_sids
        for sid in sids:
            download(restURL, username, password, sid, output_dir)
            sid_cnt += 1
            progress.show_progress(sid_cnt, num_sids)

        print "Done"

    # invalid options
    except docopt.DocoptExit as e:
        print e.message
