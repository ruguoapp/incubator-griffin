package org.apache.griffin.measure.step.builder.udf

import org.apache.spark.sql.{Row, SQLContext}

object JikeDataQualityCheckUDFs {

  def register(sqlContext: SQLContext): Unit = {
    registerUDF(sqlContext)
    registerUDAF(sqlContext)
  }

  /** Register all UDF. */
  private def registerUDF(sqlContext: SQLContext): Unit = {
    sqlContext.udf.register("rowHasNullColumns", rowHasNullColumns _)
    sqlContext.udf.register("rowIsAllNull", rowIsAllNull _)
  }

  /**
    * Determine if a row has null columns.
    *
    * Usage:
    *
    * // Counts of rows having null columns.
    * select count(*) from user_action where rowHasNullColumns(Struct(*))
    *
    * // Counts of rows having null user_id or item_type
    * select count(*) from user_action where rowHasNullColumns(Struct(user_id, item_type))
    */
  private def rowHasNullColumns(row: Row): Boolean = {
    row.anyNull
  }

  /**
    * Determine if a row doesn't have non-null columns.
    *
    * Usage:
    *
    * // Counts of rows having all null columns.
    * select count(*) from user_action where rowIsAllNull(Struct(*))
    *
    * // Counts of rows having all null columns.
    * select count(*) from user_action where rowIsAllNull(Struct(user_id, item_type))
    */
  private def rowIsAllNull(row: Row): Boolean = {
    (0 until row.size).map(row.isNullAt).reduce((x, y) => x && y)
  }

  /** Register all UDAF. */
  private def registerUDAF(sQLContext: SQLContext): Unit = {}

}
