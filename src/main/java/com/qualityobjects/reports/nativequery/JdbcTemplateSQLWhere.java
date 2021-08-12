package com.qualityobjects.reports.nativequery;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.SqlParameterValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor(staticName = "of")
public class JdbcTemplateSQLWhere {

	private final String whereTemplate;
	private final List<SqlParameterValue> params;

	public boolean isEmpty() {
		return whereTemplate == null;
	}

	public static JdbcTemplateSQLWhere empty() {
		return of(null, List.of());
	}

	public static JdbcTemplateSQLWhere of(String where) {
		return of(where, List.of());
	}
	
	public static final Pattern PARAN_NAME_PATTERN = Pattern.compile("[^:]:([a-z0-9_$]+)");

	public static List<String> extractOrderedParamNames(String sql) {
		List<String> paramNames = new ArrayList<>();
		Matcher matcher = PARAN_NAME_PATTERN.matcher(sql);
		while (matcher.find()) {
            String paramName = matcher.group(1);
            paramNames.add(paramName);
        }
		
		return paramNames;
	}
	
	public static JdbcTemplateSQLWhere merge(LogicalOperator op, List<JdbcTemplateSQLWhere> others) {
	    String sep = String.format(" %s ", op.operator());
	    List<String> allSqlConds = others.stream().map(JdbcTemplateSQLWhere::getWhereTemplate).collect(Collectors.toList());
	    String sqlTemplate = String.format("( %s )", String.join(sep, allSqlConds));
	    List<SqlParameterValue> allparams = others.stream().flatMap(jtsw -> jtsw.getParams().stream()).collect(Collectors.toList());
	    List<String> paramNames = extractOrderedParamNames(sqlTemplate);
	    
	    List<SqlParameterValue> orderedParams = paramNames.stream().map(paramName -> {
	    	Optional<SqlParameterValue> param = allparams.parallelStream().filter(p -> paramName.equals(p.getName())).findAny();
	    	return param.get();
	    }).collect(Collectors.toList()); 

	    return JdbcTemplateSQLWhere.of(sqlTemplate, orderedParams);
	}
}
