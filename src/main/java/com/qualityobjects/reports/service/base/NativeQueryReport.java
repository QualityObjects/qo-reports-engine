package com.qualityobjects.reports.service.base;

import com.qualityobjects.commons.exception.QOException;
import com.qualityobjects.commons.exception.QORuntimeException;
import com.qualityobjects.reports.nativequery.Condition;
import com.qualityobjects.reports.nativequery.repo.NativeQueryRepository;
import com.qualityobjects.springboot.dto.PageData;
import com.qualityobjects.springboot.dto.PageParams;

import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.MultiValueMap;

/**
 * Interface que deben implementar todos los reports con salida CSV
 */
public interface NativeQueryReport<T> {

	public NativeQueryRepository getRepository();
	public Sort getDefaultSort();
	
	public Condition createCondition(MultiValueMap<String, String> filterParams);

	@SuppressWarnings("unchecked")
	public default Class<T> getDomainType() {
		Class<?>[] klasses = GenericTypeResolver.resolveTypeArguments(getClass(), NativeQueryReport.class);

		if (klasses != null && klasses.length > 0) {
			return (Class<T>)klasses[0];
		} else {
			throw new QORuntimeException("No se pudo resolver el tipo Generic de la clase: " + getClass());
		}
	} 

	/**
	 * Method to process data after the row is retrieved from DB.
	 * @param row
	 */
	public default T postProcessData(T row) throws QOException {
		return row;
	}
	
    public default PageData<T> getPage(Pageable pageable, Condition where, PageParams params) throws QOException {
        Page<T> page = getRepository().findAll(where, pageable, getDomainType());

        for (T ars : page) {
            postProcessData(ars);
        }
        return PageData.of(page,params);
    }


}
