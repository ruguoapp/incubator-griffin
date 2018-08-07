package com.okjike.griffin.dsl

import com.holdenkarau.spark.testing.DataFrameSuiteBase
import com.okjike.griffin.dsl.evaluator.Eval
import com.okjike.griffin.dsl.functions.Type.BaseFunction
import org.apache.griffin.measure.configuration.enums.{DslType, JikeDslType}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.scalatest.{FlatSpec, Matchers}

class JikeDslTest extends FlatSpec with Matchers with DataFrameSuiteBase {

  "count diff_ratio" should "be able get diff" in {
    val schema = StructType(Array(
      StructField("dt", StringType)
    ))
    val rows = Seq(Row("2018-08-06", "2018-08-05", "2018-08-06"))
    val rowRdd = sqlContext.sparkContext.parallelize(rows)
    sqlContext.createDataFrame(rowRdd, schema).createOrReplaceTempView("dates")

    val df = Eval.apply[BaseFunction]("import com.okjike.griffin.dsl.functions._\nCount.diff_ratio(\"dates\", \"difference\")")
      .apply(sqlContext)
    df.show(10)
  }

  "jike dsl" should "be parsed successfully" in {
    val p = "^(?i)jike-?sql$"
    val m = "jike-sql" match {
      case JikeDslType.idPattern() => true
      case _ => false
    }
    println(m)
    DslType("jike-sql") should === (JikeDslType)
    DslType("jike-dsl") should === (JikeDslType)
  }
}
