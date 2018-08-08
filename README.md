# Apache Griffin for Jike

Apache Griffin is a model driven data quality solution for modern data systems. It provides a
standard process to define data quality measures, execute, report, as well as an unified dashboard
across multiple data systems.

This repo is a fork for Jike. It includes some jike specific configurations and UDF/DSL.

## TL;DR

魔改 [all_checks.json](https://github.com/ruguoapp/incubator-griffin/blob/jike/jike-dq-checks/all_checks.json),
运行 [update_checks.py](https://github.com/ruguoapp/incubator-griffin/blob/jike/jike-dq-checks/update_checks.py).
祈祷没问题.

## Concepts

Griffin has two types of entities:
* **Measure** describes how to computes a metric.
* **Job** schedules when to compute a metric.

For example, we define a measure called daily_user_action_count_diff, which means the 
difference ratio of total user actions count between today and yesterday. Defining a measure won't
start any computation. To calculate the ratio every day, we need to also create a job.

## Architecture

Griffin has 2.1 main components
* Service (1 pt)
* Measure (1 pt)
* UI      (0.1 pt)

### Service

Griffin service provides some simple REST APIs. Users can use these APIs to manage measurements and
jobs. Measurements and jobs data are stored in a PG database right now.

### Measure

Measure is where the main logic lives. Griffin service calls livy to trigger jobs based on the
specified cron expressions. Livy parses the request payload, downloads the "measure" jar to actually
execute the job.

### UI

[UI](http://ec2-52-80-231-9.cn-north-1.compute.amazonaws.com.cn:8080/) provides a simple interface
to create measurements and jobs. It is far from complete. Using our own scripts is highly recommended.

## Build

We use Docker to run a Griffin service. You can find the docker file
[here](https://github.com/ruguoapp/incubator-griffin/blob/jike/Dockerfile). The simplest way to
build is running

```
./start.sh
```

## Run

After we build a new griffin docker image, simply log in to the griffin cluster and then run the
"run-griffin.sh" script at the home directory. To easily check the log, I'm using tmux. You may need
to run

```

tmux attach -t griffin-docker
./run-griffin.sh
# detach by using `Ctrl-b d`

```

## Current Measures and Jobs

Current measures and jobs are defined [here](https://github.com/ruguoapp/incubator-griffin/blob/jike/jike-dq-checks/all_checks.json). To update
the data after modifying this file, simply run 

```
cd jike-dq-checks
./update_checks.py
```

### Check Schema

```
    {
      "name": "daily_user_action_count_diff",                                            # Name. Don't Conflict.
      "description": "Daily user action count compaired with yesterday.",                # Description.
      "tables": ["user_action"],                                                         # All used tables.
      "rules": [                                                                         # Define you logic here.
        {
            "dsl":"spark-sql",                                                           # This rule is a spark sql.
            "name": "all_null_stats",                                                    # Create a view of results using this name.
            "rule": "select dt, case when rowIsAllNull(struct(*)) then 1 else 0 end as isAllNull from user_action where dt=date_sub(current_date(), 1)"
        },
        ...
        {
            "dsl":"spark-sql",
            "name": "all_null_ratio",
            "rule": "select (all_null_count.c / total_count.c) as all_null_ratio from all_null_count inner join total_count on all_null_count.dt = total_count.dt",
            "export": "true"                                                             # export this as part of results.
        }
      ],
      "cron.expression": "0 0 10 1/1 * ? *",                                             # When to run this
      "cron.time.zone": "GMT+8:00",                                                      # Time zone
      "offset": "-1d"                                                                    # -1d means runing on yesterday's data.
    },
```

**Don't worry, update_checks will ONLY upsert new/changed checks.**

**Note**: this python script requires *requests* library.

## *DSL*s and *UDF*s

To make writing measures easier, we pre-define some UDFs and DSLs. DSLs are actually not
DSLs. Currently, it is only evaluated by using Scala *eval*. It can potentially damage our
data, but it is not a big problem since Griffin is only available internally.

### UDF

* ```rowHasNullColumns```: determine if a row has columns with null values.
* ```rowIsAllNull```: determine is all columns are now in one column.

### DSL

* ```Count.diff_ratio(table_name, result_name)```: compute the difference ratio
of row counts between today and yesterday for ```table_name```.

### How to Contribute

UDFs are defined in [JikeDataQualityCheckUDFs](https://github.com/ruguoapp/incubator-griffin/blob/jike/measure/src/main/scala/org/apache/griffin/measure/step/builder/udf/JikeDataQualityCheckUDFs.scala)

DSLs are defined in [com.okjike.griffin.dsl](https://github.com/ruguoapp/incubator-griffin/tree/jike/measure/src/main/scala/com/okjike/griffin/dsl)

## Alerts

Currently alerts are done using [Zeppelin](http://zepl.ruguoapp.com:8890/#/notebook/2DK6315ER).
Simply adding your test configuration in paragraph '数据质量测试项配置'. To make our lives better, emails
are sent only if any test fails.