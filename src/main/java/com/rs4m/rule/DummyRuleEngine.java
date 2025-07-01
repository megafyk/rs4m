package com.rs4m.rule;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class DummyRuleEngine implements RuleEngine {
    @Override
    public void loadRules(RuleSource ruleSource) throws RuleEngineException {
        log.info("Loading rules");
    }

    @Override
    public void fireRules(Object fact) throws RuleEngineException {
        log.info("Firing rules with fact: {}", fact);
    }

    @Override
    public <T> T getResult(Class<T> resultType) throws RuleEngineException {
        return null;
    }
}
