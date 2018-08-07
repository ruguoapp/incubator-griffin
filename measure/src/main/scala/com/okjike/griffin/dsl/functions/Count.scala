package com.okjike.griffin.dsl.functions

import com.google.common.collect.ImmutableList
import com.okjike.griffin.dsl.functions.Type.BaseFunction
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{DoubleType, StructField, StructType}

object Count {

  def diff_ratio(table: String, name: String = "diff_ratio", offset_of_now: Int = 1, offset_to_now: Int = 1): BaseFunction = {
    sqlContext => {
      val today = sqlContext.sql(getCountQuery(table, offset_of_now)).collect().head.getLong(0)
      val yesterday = sqlContext.sql(getCountQuery(table, offset_to_now + offset_of_now)).collect().head.getLong(0)
      val diff_ratio = 1.0 * (today - yesterday) / yesterday
      sqlContext.createDataFrame(
        ImmutableList.of(Row(diff_ratio)),
        StructType(Array(StructField(name, DoubleType))))
    }
  }

  private[Count] def getCountQuery(table: String, offset: Int = 1, alias: String = "c"): String = {
    f"""select count(*) as c from $table where dt=date_sub(current_date(), $offset)"""
  }
}
