package com.codeflow.model;

import java.util.HashMap;
import java.util.Map;

public class ExecutionContext {

    public Map<String, Object> variables = new HashMap<>();
    public int currentStep = 0;

}
