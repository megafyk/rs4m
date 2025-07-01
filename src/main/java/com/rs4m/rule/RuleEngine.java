package com.rs4m.rule;

public interface RuleEngine {
    void loadRules(RuleSource ruleSource) throws RuleEngineException;

    void fireRules(Object fact) throws RuleEngineException;

    <T> T getResult(Class<T> resultType) throws RuleEngineException;
}

