#!/usr/bin/python

# delete all project listed in the ingested_sid_list file
# and write the sid in a text file

import requests

#restURL = "http://192.168.54.133:8080/dccd-rest/rest/myproject"
restURL = "http://localhost:8080/dccd-rest/rest/myproject"

userName = "normaltestuser"
userPasswd = "testtest"

ingestedSidListFilename = "ingested_sid_list.txt"
deletedSidListFilename = "deleted_sid_list.txt"

sidListFile = open(ingestedSidListFilename)
# read all into a list in memory, maybe not nice if the list is huge
sidLines = sidListFile.readlines()
sidListFile.close()

sidListFile = open(deletedSidListFilename, "a")

for sidLine in sidLines :
	sid = sidLine.strip()
	print "Deleting sid: " + sid
	r = requests.delete(restURL+ "/" + sid, auth=(userName, userPasswd))
	if r.status_code != requests.codes.ok : 
		print "Error; delete stopped!"
		break
	sidListFile.write(sid + "\n")
	sidListFile.flush()
	
sidListFile.close()

print "Done"