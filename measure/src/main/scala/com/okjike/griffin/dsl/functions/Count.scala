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

  def match_ratio(src_table: String, dst_table: String, cols: Array[(String, String)],
                  name: String = "match_ratio",
                  src_conditions: String = "", dst_conditions: String = ""): BaseFunction = {
    sqlContext => {
      var join_count_query = getJoinCountQuery(src_table, dst_table, cols)
      var conditions = if (src_conditions == null) "" else src_conditions
      if (dst_conditions != null && !dst_conditions.isEmpty) {
        conditions = if (conditions.isEmpty) dst_conditions else conditions + " and " + dst_conditions
      }
      if (!conditions.isEmpty) {
        join_count_query = join_count_query + " where " + conditions
      }
      val match_count = sqlContext.sql(join_count_query).collect().head.getLong(0)

      var count_query = f"select count(*) from $src_table"
      if (src_conditions != null && !src_conditions.isEmpty) {
        count_query = count_query + " where " + src_conditions
      }
      val total_count = sqlContext.sql(count_query).collect().head.getLong(0)
      val match_ratio = 1.0 * match_count / total_count
      sqlContext.createDataFrame(
        ImmutableList.of(Row(match_ratio)),
        StructType(Array(StructField(name, DoubleType))))
    }
  }

  private[Count] def getJoinCountQuery(left: String, right: String, on: Array[(String, String)]): String = {
    val onClauses = on.map(tuple2 => f"""${tuple2._1} = ${tuple2._2}""").mkString(" and ")
    f"""select count(*) from $left inner join $right on $onClauses"""
  }

  private[Count] def getCountQuery(table: String, offset: Int = 1, alias: String = "c"): String = {
    f"""select count(*) as c from $table where dt=date_sub(current_date(), $offset)"""
  }
}
