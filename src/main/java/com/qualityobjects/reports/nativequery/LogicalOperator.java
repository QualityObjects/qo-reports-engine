package com.qualityobjects.reports.nativequery;

public enum LogicalOperator {
	AND, OR;
	
	public String operator() {
		return name().toLowerCase();
	}
}
