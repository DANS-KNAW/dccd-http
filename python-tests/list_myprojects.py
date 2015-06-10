#!/usr/bin/python

"""List my projects the DCCD archive


Requires:
	docopt
	requests
	
Usage:
	list_myprojects.py
	list_myprojects.py ( -h | --help)
	
Options:
	-h --help         Show this screen
	
"""
from docopt import docopt

import requests
import os
import re

def processProject(project):
	#print "project = "
	#print project
	#for k, v in project.iteritems():
	#	print k, v

	# project is assumed to be a dictionary 
	# representing the json response from the RESTfull interface
	msg = "Sid: " + project['sid'] + " Title: " + project['title'] + " state: " + project['state']
	if 'location' in project:
		location = project['location']
		msg += " Location: " + location['lat'] + " " + location['lng']
	print msg

def getProjects(restURL, userName, userPasswd, offset, limit):
	# ask for json
	headers = {'accept': 'application/json'}
	params = {'offset': offset, 'limit': limit}
	r = requests.get(restURL, auth=(userName, userPasswd), headers=headers, params=params)
	if r.status_code != requests.codes.ok : 
		print "Error; could not retrieve data!"
	#print "Status: " + str(r.status_code) + "; response: " + r.text
	projects = r.json()['projects']
	#print "projects = "
	#print projects
	return projects
	

# Hardcode everything else for now!
""" main """
def main(argv={}):
	#print(argv)
	
	#restURL = "http://192.168.54.133:8080/dccd-rest/rest/myproject"
	restURL = "http://localhost:8080/dccd-rest/rest/myproject"
	
	userName = "normaltestuser"
	userPasswd = "testtest"
	
	offset = 0
	limit = 100
	
	projects = getProjects(restURL, userName, userPasswd, offset, limit)
	total = int(projects['@total'])
	print "Total number of projects: " + projects['@total']
	projectItems = projects['project']
	if type(projectItems) is list:
		# more then one
		for project in projectItems:
			processProject(project)
	else:
		# only one
		processProject(projectItems)
		
	# if there is more to be retrieved try to retrieve it
	while offset+limit < total:
		offset = offset + limit
		print "offset= %d, limit= %d"%(offset,limit)
		projects = getProjects(restURL, userName, userPasswd, offset, limit)
		projectItems = projects['project']
		if type(projectItems) is list:
			# more then one
			for project in projectItems:
				processProject(project)
		else:
			# only one
			processProject(projectItems)

	print "Done"

if __name__ == '__main__':
	main(docopt(__doc__))

