package com.csvplatform.analysis.services.rules;

import com.csvplatform.common.model.Issue;

import java.util.List;

public interface Rule {
    List<Issue> check(Dataset dataset);
}
