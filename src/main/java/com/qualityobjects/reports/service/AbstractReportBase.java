package com.qualityobjects.reports.service;

import com.qualityobjects.reports.nativequery.repo.NativeQueryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public abstract class AbstractReportBase<T> implements NativeQueryReport<T> {

    @Autowired
    private NativeQueryRepository repository;

    @Override
    public NativeQueryRepository getRepository() {
        return repository;
    }

}
