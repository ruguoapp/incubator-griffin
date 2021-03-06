package com.okjike.griffin.dsl

import com.okjike.griffin.dsl.evaluator.Eval
import com.okjike.griffin.dsl.functions.Type.BaseFunction
import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.step.transform.TransformStep

case class JikeDslStep (name: String,
                        rule: String,
                        details: Map[String, Any],
                        cache: Boolean = false
                       ) extends TransformStep {
  override def execute(context: DQContext): Boolean = {
    val sqlContext = context.sqlContext
    try {
      val df = Eval.apply[BaseFunction](preProcess(rule)).apply(sqlContext)
      if (cache) context.dataFrameCache.cacheDataFrame(name, df)
      context.runTimeTableRegister.registerTable(name, df)
      true
    } catch {
      case e: Throwable => {
        error(s"run jike dsl [ $rule ] error: ${e.getMessage}")
        false
      }
    }
  }

  private def preProcess(rule: String) : String = {
    val escapedRule = rule.replaceAll("''", "\"")
    f"import com.okjike.griffin.dsl.functions._\n$escapedRule"
  }
}
