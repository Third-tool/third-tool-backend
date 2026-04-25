package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.domain.model.AxisAction;
import com.example.thirdtool.LearningFacade.domain.model.CoverageStatus;
import org.springframework.stereotype.Component;

@Component
public class CoverageRecalculator {

    public CoverageStatus recalculate(AxisAction action) {
        CoverageStatus current = action.getCoverageStatus();
        return current == null ? CoverageStatus.NO_MATERIAL : current;
    }
}
