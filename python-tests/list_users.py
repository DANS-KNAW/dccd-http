#!/usr/bin/python
# -*- coding: utf-8 -*-
"""list users in the DCCD archive
	Retrieves the json data on users from the dccd-rest interface 
	
Requires:
	docopt
	requests
	
Usage:
	list_users.py
	list_users.py ( -h | --help)
	
Options:
	-h --help         Show this screen
	
"""
from docopt import docopt

import requests
import os
import re
import json

# Note: each organisation will have an id, and optional city and country properties
def getUsersFromUrl(restURL, userName, userPasswd):
	# could read it from a file previously retrieved
	headers = {'accept': 'application/json'}
	r = requests.get(restURL, auth=(userName, userPasswd), headers=headers)
	if r.status_code != requests.codes.ok : 
		print "Error; could not retrieve data!"
	#print "Status: " + str(r.status_code) + "; response: " + r.text
	# TODO handle errors
	results = r.json()['users']
	return results

def processUser(user):
	#print "user = "
	#print user
	#for k, v in user.iteritems():
	#	print k, v

	# user is assumed to be a dictionary 
	# representing the json response from the RESTfull interface
	
	msg = "id: " + user['id'] + " organisation: " + user['organisation']
	msg += " displayname: " + user['displayname']
	msg += " lastname: " + user['lastname']
	msg += " email: " + user['email']
	print msg

""" main """
def main(argv={}):
	restURL = "http://localhost:8080/dccd-rest/rest/user"
	userName = "normaltestuser"
	userPasswd = "testtest"
	usersContainer = getUsersFromUrl(restURL, userName, userPasswd)
	
	#print usersContainer
	
	users = []
	if type(usersContainer['user']) is list:
		users = usersContainer['user']
	else:
		# fix single result problem
		users.append(usersContainer['user'])
	
	# print properties for each user
	for user in users:
		processUser(user)
			
if __name__ == '__main__':
	main(docopt(__doc__))
