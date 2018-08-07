package org.apache.griffin.measure.step.builder

import com.okjike.griffin.dsl.JikeDslStep
import org.apache.griffin.measure.configuration.dqdefinition.RuleParam
import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.step.DQStep

case class JikeDslDQStepBuilder() extends RuleParamStepBuilder {
  def buildSteps(context: DQContext, ruleParam: RuleParam): Seq[DQStep] = {
    val name = getStepName(ruleParam.getName)
    val transformStep = JikeDslStep(
      name, ruleParam.getRule, ruleParam.getDetails, ruleParam.getCache)
    transformStep +: buildDirectWriteSteps(ruleParam)
  }
}
