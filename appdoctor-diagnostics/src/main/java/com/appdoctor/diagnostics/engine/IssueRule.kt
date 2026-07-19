package com.appdoctor.diagnostics.engine

/**
 * Independent deterministic diagnostics rule.
 */
public interface IssueRule {
    public fun evaluate(context: RuleContext): RuleMatch?
}
