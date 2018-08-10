package com.okjike.griffin.dsl

import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    val today = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now())
    val yesterday = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now().minusDays(1))
    val rows = Seq(Row(today), Row(yesterday), Row(today))
    val rowRdd = sqlContext.sparkContext.parallelize(rows)
    sqlContext.createDataFrame(rowRdd, schema).createOrReplaceTempView("dates")

    val df = Eval.apply[BaseFunction]("import com.okjike.griffin.dsl.functions._\nCount.diff_ratio(\"dates\", \"difference\", 0)")
      .apply(sqlContext)
    df.collect().head.getDouble(0) should === (1.0)
  }

  "count match_ratio" should "succeed" in {
    val schema = StructType(Array(
      StructField("id", StringType)
    ))
    val left = Seq(Row("1"), Row("2"), Row("3"))
    val leftRdd = sqlContext.sparkContext.parallelize(left)
    sqlContext.createDataFrame(leftRdd, schema).createOrReplaceTempView("left")

    val right = Seq(Row("1"), Row("2"))
    val rightRdd = sqlContext.sparkContext.parallelize(right)
    sqlContext.createDataFrame(rightRdd, schema).createOrReplaceTempView("right")

    val df = Eval.apply[BaseFunction]("import com.okjike.griffin.dsl.functions._\nCount.match_ratio(\"left\", \"right\", Array((\"left.id\", \"right.id\")))")
      .apply(sqlContext)
    df.collect().head.getDouble(0) should === (2.0 / 3)
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
