#!/usr/bin/env python

import json
import requests

griffin_endpoint = "http://ec2-52-80-231-9.cn-north-1.compute.amazonaws.com.cn:8080"
measure_path = "/api/v1/measures"
job_path = "/api/v1/jobs"

def construct_data_source(check):
  '''Construct a data source'''
  ret = []
  source = {}
  source["name"] = check["name"] + "_source"
  source["connectors"] = []
  for table in check["tables"]:
    source["connectors"].append({
      "name":source["name"] + "_" + table,
      "type":"HIVE",
      "version":"1.2",
      "data.unit":"1day",
      "data.time.zone":"UTC(WET,GMT)",
      "config":{
        "database":"default",
        "table.name":table
      }
    })
  ret.append(source)
  return ret

def construct_rules(check):
  '''Construct rules'''
  ret = {}
  ret["rules"] = []
  for rule in check["rules"]:
    new_rule = {}
    new_rule["dsl.type"] = rule["dsl"]
    new_rule["dq.type"] = "PROFILING"
    new_rule["name"] = rule["name"]
    new_rule["rule"] = rule["rule"]
    if "export" in rule and rule["export"] == "true":
      new_rule["metric"] = {}
    ret["rules"].append(new_rule)
  return ret

def construct_measure_request(check):
  '''Construct griffin-measure json requests based on check.'''
  ret = {}
  ret["name"] = check["name"] + "_measure"
  ret["measure.type"] = "griffin"
  ret["dq.type"] = "ACCURACY"
  ret["process.type"] = "BATCH"
  ret["owner"] = "test"
  ret["description"] = check["description"]
  ret["data.sources"] = construct_data_source(check)
  ret["evaluate.rule"] = construct_rules(check)
  return ret

def construct_job_request(check, measure_id):
  '''Construct griffin-job json requests based on check.'''
  ret = {}
  ret["measure.id"] = measure_id
  ret["job.name"] = check["name"] + "_job"
  ret["job.type"] = "batch"
  ret["cron.expression"] = check["cron.expression"]
  ret["cron.time.zone"] = check["cron.time.zone"]
  ret["data.segments"] = []
  segment = {}
  segment["as.baseline"] = "true"
  segment["data.connector.name"] = construct_data_source(check)[0]["connectors"][0]["name"]
  segment["segment.range"] = {
    "begin": check["offset"],
    "length": "1d"
  }
  ret["data.segments"].append(segment)
  return ret

def measure_fail_to_create(measure, r):
  print("Measure %s fails to be created. Status code %d. Error message is %s." % (measure["name"], r.status_code, r.json()))

def job_fail_to_create(job, r):
  print("Job %s fails to be created. Status code %d. Error message is %s." % (job["job.name"], r.status_code, r.json()))

def create_measure(endpoint, measure):
  '''Create a griffin-measure and return the measure id.'''
  api_uri = endpoint + measure_path
  r = requests.post(api_uri, json=measure)
  if r.status_code == 409:
    return -1
  elif r.status_code >= 300:
    measure_fail_to_create(measure, r)
    return -1
  else:
    return r.json()["id"]

def create_job(endpoint, job):
  '''Create a griffin-job and return if the job is created.'''
  api_uri = endpoint + job_path
  r = requests.post(api_uri, json=job)
  # TODO: handle already exist error
  if r.status_code >= 300:
    job_fail_to_create(job, r)
  return r.status_code == 200 or r.status_code == 201

with open("all_checks.json", 'r') as f:
  checks = json.load(f)
  for check in checks["checks"]:
    create_measure_request = construct_measure_request(check)
    measure_id = create_measure(griffin_endpoint, create_measure_request)
    if measure_id < 0:
      continue
    create_job_request = construct_job_request(check, measure_id)
    create_job(griffin_endpoint, create_job_request)