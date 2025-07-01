package com.rs4m.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("defaultRuleEngineManager")
public class DefaultRuleEngineManager implements RuleEngineManager {

    private final DummyRuleEngine dummy = DummyRuleEngine.builder().build();

    public DefaultRuleEngineManager() throws RuleEngineException {
        dummy.loadRules(null); // Assuming null is acceptable for the rule source
    }

    @Override
    public RuleEngine getEngine(String engineName) {
        return this.dummy;
    }
}
