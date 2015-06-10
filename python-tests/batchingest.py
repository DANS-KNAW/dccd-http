#!/usr/bin/python

"""Batch ingest for the DCCD archive

Ingest (upload) data into the DCCD archive
Store the sid's (like dccd:1) of the ingested projects 
in a text file for later use.


Requires:
	docopt
	requests
	
Usage:
	batchingest.py [-d DIR]
	batchingest.py ( -h | --help)
	
Options:
	-h --help         Show this screen
	-d DIR            Input directory [default: data]
	
"""
from docopt import docopt

#
# The upload is done with the RESTfull interface 
# See the dccd-rest project 
# develop01.dans.knaw.nl:/development/git/blessed/service/dccd/dccd-rest.git
#
# Note for curl users
# curl commandline equivalent for a testuser on localhost
# curl -u normaltestuser:testtest -i -F file=@testimport-tridasonly.zip http://localhost:8080/dccd-rest/rest/myproject

import requests
import os
import re

#print "Batch ingest for DCCD";

# Hardcode everything else for now!
""" main """
def main(argv={}):
	#print(argv)
	
	dirName = "geodata"
	if argv['-d']:
		dirName = argv['DIR']

	#restURL = "http://192.168.54.133:8080/dccd-rest/rest/myproject"
	restURL = "http://localhost:8080/dccd-rest/rest/myproject"
	
	userName = "normaltestuser"
	userPasswd = "testtest"
	
	# create or open the file for the sids's of ingested projects
	sidListFilename = "ingested_sid_list.txt"
	sidListFile = open(sidListFilename, "a")
	
	# get all zip file names
	zipFilenames = [f for f in os.listdir(dirName) if re.match(r'.*\.zip', f)]
	numFiles = len(zipFilenames)
	print "Starting upload of %d zip files..."%numFiles
	counter = 0;
	for zipFilename in zipFilenames:
		counter+=1
		zipFilePathname = os.path.join(dirName, zipFilename)
		print "Uploading file (%d of %d): %s"%(counter,numFiles,zipFilePathname)
		# upload
		files = {'file': (zipFilePathname, open(zipFilePathname, 'rb'))}
		r = requests.post(restURL, auth=(userName, userPasswd), files=files)
		if r.status_code != requests.codes.ok : 
			print "Error; ingest stopped!"
			break 
		print "Status: " + str(r.status_code) + "; response: " + r.text
		sidListFile.write(r.text + "\n")
		sidListFile.flush()
		
	sidListFile.close()
	print "Done"


if __name__ == '__main__':
    main(docopt(__doc__))
    