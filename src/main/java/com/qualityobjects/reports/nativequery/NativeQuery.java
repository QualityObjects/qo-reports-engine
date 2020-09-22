package com.qualityobjects.reports.nativequery;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NativeQuery {

	String value();

}
