package com.qualityobjects.reports.nativequery.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import com.qualityobjects.commons.exception.DataReadRuntimeException;
import com.qualityobjects.commons.exception.QORuntimeException;
import com.qualityobjects.reports.nativequery.Condition;
import com.qualityobjects.reports.nativequery.JdbcTemplateSQLWhere;
import com.qualityobjects.reports.nativequery.NativeQuery;
import com.qualityobjects.reports.nativequery.SQL;
import com.qualityobjects.reports.nativequery.mapper.CustomBeanRowMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterDisposer;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Repository
public class NativeQueryRepositoryImpl implements NativeQueryRepository {

	private static final Logger LOG = LoggerFactory.getLogger(NativeQueryRepositoryImpl.class);

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	DataSource dataSource;

	@Override
	public <T> Page<T> findAll(Condition where, Pageable pagRequest, Class<T> dtoClass) {
		NativeQuery nq = dtoClass.getAnnotation(NativeQuery.class);
		JdbcTemplateSQLWhere jtsw = where != null ? where.toJdbcTemplateSQLWhere() : JdbcTemplateSQLWhere.empty();

		CustomPreparedStatementCreatorFactory pscCount = pscfOfCount(nq.value(), jtsw, nq.forceWhere());
		int total = jdbcTemplate.<Integer>query(pscCount.newPreparedStatementCreator(), rs -> {
				rs.next();
				return rs.getInt(1);
		});
		List<T> list;
		if (total > 0) {
			CustomPreparedStatementCreatorFactory psc = pscfOf(nq.value(), jtsw, pagRequest, nq.forceWhere());
			RowMapper<T> srm = CustomBeanRowMapper.of(dtoClass);
			list = jdbcTemplate.query(psc.newPreparedStatementCreator(), srm);
		} else {
			list = List.of();
		}
		
		return new PageImpl<>(list, pagRequest, total);
	}

	@Override
	public <T> List<T> findAll(Condition where, Class<T> dtoClass) {
		return findAll(where, Sort.unsorted(), dtoClass);
	}

	@Override
	public <T> List<T> findAll(Condition where, Sort sort, Class<T> dtoClass) {

		NativeQuery nq = dtoClass.getAnnotation(NativeQuery.class);
		JdbcTemplateSQLWhere jtsw = where != null ? where.toJdbcTemplateSQLWhere() : JdbcTemplateSQLWhere.empty();
		CustomPreparedStatementCreatorFactory psc = pscfOf(nq.value(), jtsw, sort, nq.forceWhere());
		RowMapper<T> srm = CustomBeanRowMapper.of(dtoClass);

		return jdbcTemplate.query(psc.newPreparedStatementCreator(), srm);
	}

	@Override
	public <T> Stream<T> findAllAsStream(Condition where, Class<T> dtoClass) {
		return findAllAsStream(where, Sort.unsorted(), dtoClass);
	}

	@Override
	public <T> Stream<T> findAllAsStream(Condition where, Sort sort, Class<T> dtoClass) {
		NativeQuery nq = dtoClass.getAnnotation(NativeQuery.class);
		JdbcTemplateSQLWhere jtsw = where != null ? where.toJdbcTemplateSQLWhere() : JdbcTemplateSQLWhere.empty();
		CustomPreparedStatementCreatorFactory psc = pscfOf(nq.value(), jtsw, sort, nq.forceWhere());

		return this.execute(psc.newPreparedStatementCreator(),
				preparedStatement -> {
					final ResultSet rs = preparedStatement.executeQuery();

					AbstractSpliterator<T> iterator = new ResultSetIterator<>(rs, dtoClass);
					return StreamSupport.stream(iterator, false).onClose(new ResultSetCloser(rs));
				});
		
	}

	private <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) {
		Connection con = DataSourceUtils.getConnection(dataSource);
		PreparedStatement ps = null;
		try {
			ps = psc.createPreparedStatement(con);
			return action.doInPreparedStatement(ps);
		}
		catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			if (psc instanceof ParameterDisposer) {
				((ParameterDisposer) psc).cleanupParameters();
			}
			JdbcUtils.closeStatement(ps);
			DataSourceUtils.releaseConnection(con, dataSource);
			throw new QORuntimeException("Error en SQL: " + ex);
		}

	}

	@SuppressWarnings("unchecked")
	private CustomPreparedStatementCreatorFactory pscfOf(String sqlBase, JdbcTemplateSQLWhere jtsw, Sort sort, boolean forceWhereClause) {
		String sql = SQL.addWhereCondition(sqlBase, jtsw.getWhereTemplate(), forceWhereClause);
		sql += SQL.sortClause(sort);
		List<? extends SqlParameter> params = new ArrayList<>(jtsw.getParams());
		return CustomPreparedStatementCreatorFactory.of(sql, (List<SqlParameter>) params);
	}

	@SuppressWarnings("unchecked")
	private CustomPreparedStatementCreatorFactory pscfOf(String sqlBase, JdbcTemplateSQLWhere jtsw, Pageable pagReq, boolean forceWhereClause) {
		String sql = SQL.addWhereCondition(sqlBase, jtsw.getWhereTemplate(), forceWhereClause);
		List<? extends SqlParameter> params = new ArrayList<>(jtsw.getParams());
		sql += SQL.paginationClause(pagReq, (List<SqlParameterValue>) params);
		return CustomPreparedStatementCreatorFactory.of(sql, (List<SqlParameter>) params);
	}
	
	@SuppressWarnings("unchecked")
	private CustomPreparedStatementCreatorFactory pscfOfCount(String sqlBase, JdbcTemplateSQLWhere jtsw, boolean forceWhereClause) {
		final String sqlCountTemplate = "select count(1) as total from (%s) virtual_table"; 
		List<? extends SqlParameter> params = new ArrayList<>(jtsw.getParams());
		String sql = String.format(sqlCountTemplate, SQL.addWhereCondition(sqlBase, jtsw.getWhereTemplate(), forceWhereClause)); 
		return CustomPreparedStatementCreatorFactory.of(sql, (List<SqlParameter>) params);
	}
	
	@AllArgsConstructor
	private class ResultSetCloser implements Runnable {
		private final ResultSet rs;

		@Override
		public void run() {
			try {
				if (!rs.isClosed()) {
					JdbcUtils.closeResultSet(rs);
				}
				if (!rs.getStatement().isClosed()) {
					JdbcUtils.closeStatement(rs.getStatement());					
				}
				DataSourceUtils.releaseConnection(rs.getStatement().getConnection(), dataSource);
			} catch (SQLException e) {
				LOG.error("Error (ignored) closing resultset and statement:{} " , e.getMessage());
			}
		}
	}

	private class ResultSetIterator<T> extends AbstractSpliterator<T> {
		private final ResultSet rs;
		private final RowMapper<T> srm;

		public ResultSetIterator(ResultSet rs, Class<T> rowBeanClass) {
			super(Long.MAX_VALUE, Spliterator.IMMUTABLE);
			this.rs = rs;
			srm = CustomBeanRowMapper.of(rowBeanClass);
		}

		@Override
		public boolean tryAdvance(Consumer<? super T> action) {
			try {
				if (!rs.next()) {
					return false;
				}
				action.accept(srm.mapRow(rs, rs.getRow()));
				return true;
			} catch (SQLException ex) {
				LOG.error("Error reading ResulSet: {}", ex.toString());
				throw new DataReadRuntimeException(); 
			}
		}
	}

	
	private static class CustomPreparedStatementCreatorFactory extends PreparedStatementCreatorFactory {
		@Getter
		final List<? extends SqlParameter> params;
		
		private CustomPreparedStatementCreatorFactory(String sql, List<SqlParameter> params) {
			super(sql, params);
			this.params = params;
		}
		
		@SuppressWarnings("unchecked")
		public PreparedStatementCreator newPreparedStatementCreator() {
			return super.newPreparedStatementCreator((List<SqlParameterValue>)this.params);
		}
		
		public static CustomPreparedStatementCreatorFactory of(String sql, List<SqlParameter> params) {
			String nativeSqlPreparedStmt = SQL.replaceNamedParams(sql);
			return new CustomPreparedStatementCreatorFactory(nativeSqlPreparedStmt, params);
		}
	}
}
