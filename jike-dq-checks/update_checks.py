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
  segment["as.baseline"] = True
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

def get_all_existing_measures(endpoint):
  api_uri = endpoint + measure_path
  response = requests.get(api_uri).json()
  return { measure["name"] : measure for measure in response}

def get_all_existing_jobs(endpoint):
  api_uri = endpoint + job_path
  response = requests.get(api_uri).json()
  return { job["job.name"] : job for job in response}

def get_existing_measure_by_name(measure_name, existing_measures):
  if measure_name in existing_measures:
    return existing_measures[measure_name]
  return None

def get_existing_job_by_name(job_name, existing_jobs):
  if job_name in existing_jobs:
    return existing_jobs[job_name]
  return None

def create_measure(endpoint, measure):
  '''Create a griffin-measure and return the measure.'''
  api_uri = endpoint + measure_path
  r = requests.post(api_uri, json=measure)
  if r.status_code == 409:
    return -1
  elif r.status_code >= 300:
    measure_fail_to_create(measure, r)
    return None
  else:
    return r.json()

def update_measure(endpoint, measure):
  '''Update a griffin-measure and return the measure.'''
  api_uri = endpoint + measure_path
  r = requests.put(api_uri, json=measure)
  if r.status_code >= 300:
    measure_fail_to_create(measure, r)
    return None
  else:
    return r.json()

def create_job(endpoint, job):
  '''Create a griffin-job and return the job.'''
  api_uri = endpoint + job_path
  r = requests.post(api_uri, json=job)
  if r.status_code >= 300:
    job_fail_to_create(job, r)
  return r.json()

def update_job(endpoint, id, job):
  '''Update a griffin-job and return the job.'''
  api_uri = endpoint + job_path + "/" + str(id)
  requests.delete(api_uri)
  return create_job(endpoint, job)

existing_jobs = get_all_existing_jobs(griffin_endpoint)
existing_measures = get_all_existing_measures(griffin_endpoint)

def is_duplicate(entity, existing):
  if existing == None:
    return False
  if isinstance(entity, dict):
    for key in entity:
      if key not in existing:
        return False
      if not is_duplicate(entity[key], existing[key]):
        return False
    return True
  elif isinstance(entity, list) and isinstance(existing, list):
    if len(entity) != len(existing):
      return False
    for i in range(len(entity)):
      if not is_duplicate(entity[i], existing[i]):
        return False
    return True
  else:
    return entity == existing

with open("all_checks.json", 'r') as f:
  checks = json.load(f)
  for check in checks["checks"]:
    print("============ Updating %s =============" % check["name"]) 

    create_measure_request = construct_measure_request(check)
    measure_name = create_measure_request["name"]
    existing_measure = get_existing_measure_by_name(measure_name, existing_measures)
    new_measure = None
    message = ""
    if existing_measure == None:
      new_measure = create_measure(griffin_endpoint, create_measure_request)
      message = "Created measure [%s]" % measure_name
    elif not is_duplicate(create_measure_request, existing_measure):
      create_measure_request["id"] = existing_measure["id"]
      new_measure = update_measure(griffin_endpoint, create_measure_request)
      message = "Updated measure [%s]" % measure_name
    else:
      new_measure = existing_measure
      message = "Measure [%s] already exists." % measure_name
    if new_measure == None:
      continue
    existing_measures[measure_name] = new_measure
    print("  %s" % message)

    create_job_request = construct_job_request(check, new_measure["id"])
    job_name = create_job_request["job.name"]
    existing_job = get_existing_job_by_name(job_name, existing_jobs)
    new_job = None
    if existing_job == None:
      new_job = create_job(griffin_endpoint, create_job_request)
      message = "Created job [%s]" % job_name
    elif not is_duplicate(create_job_request, existing_job):
      new_job = update_job(griffin_endpoint, existing_job["id"], create_job_request)
      message = "Updated job [%s]" % job_name
    else:
      new_job = existing_job
      message = "Job [%s] already exists." % job_name
    if new_job == None:
      continue
    existing_jobs[job_name] = new_job
    print("  %s" % message)