package com.qualityobjects.reports.nativequery;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.sql.JDBCType;
import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.qualityobjects.reports.nativequery.JdbcTemplateSQLWhere.PARAN_NAME_PATTERN;


/**
 * Helper class to work with SQL sentences
 *
 *
 * @author Rob
 */
public class SQL {

	private SQL() {
		super();
	}

	private static final String LIMIT_PARAM = "_limit";
	private static final String OFFSET_PARAM = "_offset";
	private static final String SQL_WHERE = "where";

	/**
	 * We should avoid dot notation (by default) for param names
	 *
	 * @return The field name that will be used as param "key" to replace value
	 */
	public static String normalizeFieldParam(Object... fieldNameParts) {
		String fieldName;
		if (fieldNameParts.length == 1) {
		  fieldName = fieldNameParts[0].toString();
		} else {
		  fieldName = String.join("_", Arrays.stream(fieldNameParts).map(p -> p.toString()).collect(Collectors.toList()));
		}
		return fieldName.replace('.', '_');
	}

	public static JDBCType getJdbcType(Object value) {
		if (value == null) {
			return JDBCType.NULL;
		}
		if (value instanceof Collection || value.getClass().isArray()) {
			return JDBCType.ARRAY;
		}
		if (value instanceof CharSequence) {
			return JDBCType.VARCHAR;
		}
		if (value instanceof Number) {
			return JDBCType.NUMERIC;
		}
		if (value instanceof Boolean) {
			return JDBCType.BOOLEAN;
		}
		if (value instanceof Date || value instanceof LocalDate) {
			return JDBCType.TIMESTAMP;
		}
		if (value instanceof Temporal) {
			return JDBCType.TIMESTAMP_WITH_TIMEZONE;
		}
		return JDBCType.JAVA_OBJECT;
	}

	public static boolean hasWhere(String sql) {
		int whereIdx = sql.toLowerCase().lastIndexOf(SQL_WHERE);
		int fromIdx = sql.toLowerCase().lastIndexOf("from");
		return whereIdx > fromIdx;
	}

	public static String addWhereCondition(String sqlBase, String whereCond) {
		if (!StringUtils.hasLength(whereCond)) {
			return sqlBase;
		}
		boolean hasGroupBy = sqlBase.toLowerCase().contains("group by");
		if (!hasGroupBy) {
			String joinStr = hasWhere(sqlBase) ? "and" : SQL_WHERE;
			return String.join(" ", sqlBase, joinStr, whereCond);
		} else {
			int groupByPos = sqlBase.toLowerCase().indexOf("group by");
			String newBase = sqlBase.substring(0, groupByPos);
			String groupBySentence = sqlBase.substring(groupByPos);
			String joinStr = hasWhere(newBase) ? "and" : SQL_WHERE;
			String finalSQL = String.join(" ", newBase, joinStr, whereCond, groupBySentence);
			return finalSQL.replace("  ", " ");
		}
	}

	public static String addWhereCondition(String sqlBase, JdbcTemplateSQLWhere jtsw) {
		return addWhereCondition(sqlBase, jtsw.getWhereTemplate());
	}

	public static SqlParameterValue paramOf(String paramName, Object value) {
		return new SqlParameterValue(new SqlParameter(paramName, getJdbcType(value).getVendorTypeNumber()), value);
	}

	public static String subqueryTemplate(String sqlBase, JdbcTemplateSQLWhere jtsw) {
		final String TPL = "(%s %s %s)";
		String limitResultsSQL = "LIMIT 1";
		String whereStmt;
		if (!jtsw.isEmpty()) {
			whereStmt = "WHERE " + jtsw.getWhereTemplate();
		} else {
			whereStmt = "";
		}
		return String.format(TPL, sqlBase, whereStmt, limitResultsSQL);
	}

	public static String subqueryBaseSQL(String tableName) {
		return String.format("select 1 as _foo from %s", tableName);
	}

	public static String sqlFilterTemplate(SingleCondition sc) {
		if (sc.getOp() == FilterOperator.IN) {
			return generateInClauseTemplate(sc);
		}
		final String TPL = "(%s %s :%s)";
		String field = sc.getField();
		String operator = sc.getOp().getSqlOp();
		String valueParam = normalizeFieldParam(sc.getField(), sc.getSuffix());
		if (sc.getOp() == FilterOperator.CONTAINS || sc.getOp() == FilterOperator.CONTAINS_IC) {
			final String LIKE_TPL = "(%s %s concat('%%',:%s,'%%') )";
			return String.format(LIKE_TPL, field, operator, valueParam);
		}
		if (sc.getOp() == FilterOperator.IS_NULL) {
			final String ISNULL_TPL = "(%s %s)";
			return String.format(ISNULL_TPL, field, operator);
		}
		return String.format(TPL, field, operator, valueParam);
	}

	private static String generateInClauseTemplate(SingleCondition sc) {
		String field = sc.getField();
		Object value = sc.getValue();
		String valueParamPrefix = normalizeFieldParam(sc.getField(), sc.getSuffix());
		int size;
		if (value.getClass().isArray())  {
		  size = ((Object[])value).length;
		} else {
		  size = Collection.class.cast(value).size();
		}
		StringBuilder sb = new StringBuilder(String.format("(%s %s (", field, sc.getOp().getSqlOp()));
		Iterable<String> paramnames = IntStream.range(0, size).boxed().map(i -> ":" + SQL.normalizeFieldParam(valueParamPrefix, i)).collect(Collectors.toList());
		sb.append(String.join(",", paramnames));
		sb.append("))");
		return sb.toString();
	}

	private static Map<Class<?>, List<String>> entityColumnsCache = new HashMap<>();

	public static List<String> generateColumnList(Class<?> entityClass) {
		List<String> columns = new ArrayList<>();

		ReflectionUtils.doWithFields(entityClass, (Field field) -> {
			String columnName;
			// Inferimos el nombre de la columna convirtiendo el nombre del campo (en
			// camelCase) a
			// snake_case
			columnName = com.qualityobjects.commons.utils.JsonUtils.toSnakeCase(field.getName());
			columns.add(String.format("%s as \"%s\"", columnName, field.getName()));
		});

		return columns;
	}

	public static String generateSelectList(String tableAlias, Class<?> entityClass, String... excludeDbColumns) {
		StringBuilder sb = new StringBuilder();
		List<String> columns = entityColumnsCache.get(entityClass);
		if (columns == null) {
			columns = generateColumnList(entityClass);
			entityColumnsCache.put(entityClass, columns);
		}
		Set<String> columnsToExclude = Arrays.stream(excludeDbColumns).collect(Collectors.toSet());
		for (String col : columns) {
			String colName = col.substring(0, col.indexOf(' '));
			if (!columnsToExclude.contains(colName)) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(tableAlias).append(".").append(col);
			}
		}

		return sb.toString();
	}

	public static String paginationClause(Pageable pagReq, List<SqlParameterValue> currentParams) {
		String sortClauseSql = sortClause(pagReq.getSort());
		String pagSql = String.format(" LIMIT :%s OFFSET :%s", LIMIT_PARAM, OFFSET_PARAM);

		currentParams.add(paramOf(LIMIT_PARAM, pagReq.getPageSize()));
		currentParams.add(paramOf(OFFSET_PARAM, pagReq.getPageSize() * (pagReq.getPageNumber())));

		return sortClauseSql + pagSql;
	}

	public static String sortClause(Sort sort) {
		if (sort == null || Sort.unsorted().equals(sort)) {
			return "";
		}
		Function<Sort.Order, String> mapFunc = s -> String.format("%s %s", s.getProperty(), s.getDirection().name());
		String orderedField = String.join(", ", sort.stream().map(mapFunc).collect(Collectors.toList()));
		return String.format(" order by %s", orderedField);
	}


	/**
	 * Change named params in an sql template, like : c_id_0, name_1, by "?".
	 * @param sql
	 * @return
	 */
	public static String replaceNamedParams(String sql) {
		String sqlSeqParams = String.valueOf(sql);
		Matcher matcher = PARAN_NAME_PATTERN.matcher(sql);

		while (matcher.find()) {
            String paramName = matcher.group(1);
            sqlSeqParams = sqlSeqParams.replaceAll(":"+paramName, "?");
        }

		return sqlSeqParams;
	}


}
