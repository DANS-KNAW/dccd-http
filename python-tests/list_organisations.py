#!/usr/bin/python
# -*- coding: utf-8 -*-
"""list organisations in the DCCD archive
	Retrieves the json data on organisations from the dccd-rest interface 
	
Requires:
	docopt
	requests
	
Usage:
	list_organisations.py
	list_organisations.py ( -h | --help)
	
Options:
	-h --help         Show this screen
	
"""
from docopt import docopt

import requests
import os
import re
import json

# Note: each organisation will have an id, and optional city and country properties
def getOrganisationsFromUrl(restURL):
	# could read it from a file previously retrieved
	headers = {'accept': 'application/json'}
	r = requests.get(restURL, headers=headers)
	if r.status_code != requests.codes.ok : 
		print "Error; could not retrieve data!"
	#print "Status: " + str(r.status_code) + "; response: " + r.text
	# TODO handle errors
	results = r.json()['organisations']
	return results

def processOrganisation(organisation):
	#print "organisation = "
	#print organisation
	#for k, v in organisation.iteritems():
	#	print k, v

	# organisation is assumed to be a dictionary 
	# representing the json response from the RESTfull interface
	
	msg = "id: " + organisation['id']
	if 'city' in organisation:
		msg += " city: " + organisation['city']
	if 'country' in organisation:
		msg += " country: " + organisation['country']
		
	print msg

""" main """
def main(argv={}):
	restURL = "http://localhost:8080/dccd-rest/rest/organisation"
	organisationsContainer = getOrganisationsFromUrl(restURL)
	
	#print organisationsContainer
	
	organisations = []
	if type(organisationsContainer['organisation']) is list:
		organisations = organisationsContainer['organisation']
	else:
		# fix single result problem
		organisations.append(organisationsContainer['organisation'])
	
	# print properties for each organisation
	for organisation in organisations:
		processOrganisation(organisation)
			
if __name__ == '__main__':
	main(docopt(__doc__))
