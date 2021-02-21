package com.qualityobjects.reports.service;

import com.qualityobjects.reports.nativequery.repo.NativeQueryRepository;
import com.qualityobjects.reports.service.base.NativeQueryReport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class to be extended by report Components
 */
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public abstract class AbstractReportBase<T> implements NativeQueryReport<T> {

    @Autowired
    private NativeQueryRepository repository;

    @Override
    public NativeQueryRepository getRepository() {
        return repository;
    }

}
