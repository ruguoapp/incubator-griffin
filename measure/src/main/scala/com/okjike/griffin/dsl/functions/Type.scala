package com.okjike.griffin.dsl.functions

import org.apache.spark.sql.{DataFrame, SQLContext}

object Type {
  type BaseFunction = Function[SQLContext, DataFrame]
}

