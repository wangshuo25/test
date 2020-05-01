package cn.zup.bis.service.model;

import cn.zup.bis.dao.model.DataSourceDao;
import cn.zup.bis.entity.model.DataSource;
import cn.zup.bis.entity.RequestStatus;
import cn.zup.bis.entity.Result;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*
@Service告诉spring创建一个实现类的实例 spring容器要为他创建对象\


 */
@Service("DataSourceService")
public class DataSourceService {
	/**
	public static final String mysql = "com.mysql.jdbc.Driver";
	public static final String oracle = "oracle.jdbc.driver.OracleDriver";
	public static final String sqlserver = "net.sourceforge.jtds.jdbc.Driver";
	public static final String db2 = "com.ibm.db2.jcc.DB2Driver";
	public static final String psql = "org.postgresql.Driver";
	public static final String hive = "org.apache.hive.jdbc.HiveDriver";
	public static final String kylin = "org.apache.kylin.jdbc.Driver";
	**/
	/**
	public static final String showTables_mysql = "show tables";
	public static final String showTables_oracle = "select table_name from tabs";
	public static final String showTables_sqlser = "select name from sysobjects where xtype='U' order by name";
	public static final String showTables_db2 = "select name from sysibm.systables where type='T' and creator='$0'";
	public static final String showTables_psql = "select tablename from pg_tables where tableowner='$0'";
	public static final String showTables_hive = "show tables";
	public static final String showTables_kylin = "show tables";
	**/

	@Resource
	private DataSourceDao mapper;

	
	public List<DataSource> listDataSource(){
		return mapper.listDataSource();
	}
	
	public void insertDataSource(DataSource ds){
		mapper.insertDataSource(ds);
	}
	
	public void updateDataSource(DataSource ds){
		mapper.updateDataSource(ds);
//		//清除缓存
//		this.cacheService.removeDsource(ds.getDsid());
	}
	
	public void deleteDataSource(String dsid){
		mapper.deleteDataSource(dsid);
//		//清除缓存
//		this.cacheService.removeDsource(dsid);
	}
	
	public DataSource getDataSource(String dsid){
		return mapper.getDataSource(dsid);
	}
	

	
	public Connection getJDBC(DataSource ds) throws Exception{
		try {
			Connection conn = null;
		Class.forName("org.apache.kylin.jdbc.Driver").newInstance();
			conn= DriverManager.getConnection(ds.getLinkUrl(), ds.getLinkName(), ds.getLinkPwd());
			return conn;
		} catch (Exception e) {
			throw e;
		}
	}



	public Result testDataSource(DataSource ds) {
		Result ret = new Result();
		String clazz ="";
		if("mysql".equals(ds.getLinkType())) {
			clazz="com.mysql.cj.jdbc.Driver";

		}else if("kylin".equals(ds.getLinkType())){
			clazz="org.apache.kylin.jdbc.Driver";
		}
		Connection conn = null;

		try {
			Class.forName(clazz).newInstance();
			System.err.println(clazz);


			conn= DriverManager.getConnection(ds.getLinkUrl(), ds.getLinkName(),  ds.getLinkPwd());
			if(conn != null){
				ret.setResult(RequestStatus.SUCCESS.getStatus());
			}else{
				ret.setResult(RequestStatus.FAIL_FIELD.getStatus());
			}
		} catch (Exception e) {

			ret.setResult(RequestStatus.FAIL_FIELD.getStatus());
			ret.setMsg(e.getMessage());
		}finally{
			if(conn != null){
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();//打印错误信息在控制台
				}
			}
		}
		return ret;
	}

	public List<Map<String, Object>> listTables(String dsid, String searchTname) throws Exception{
		DataSource ds = mapper.getDataSource(dsid);
		final List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		Connection conn = null;
		try {
			 if(ds.getUse().equals("jdbc")){
				conn = this.getJDBC(ds);
			}
			String schem = null;

			String catalog = null;
			if("mysql".equals(ds.getLinkType())) {
				catalog = conn.getCatalog();//catalog为数据源
			}


//(searchTname!=null&&searchTname.length() > 0) ?("%"+searchTname+"%"):"%"
			ResultSet tbs = conn.getMetaData().getTables(catalog, schem,searchTname , new String[]{"TABLE"});//（数据库名，默认，表名，默认）
			while(tbs.next()){
				Map<String, Object> m = new HashMap<String, Object>();
				String tname = tbs.getString("TABLE_NAME");
				m.put("id", tname);
				m.put("text", tname);
				m.put("iconCls", "icon-table");
				ret.add(m);
			}
			tbs.close();

			/**
			String qsql = null;
			if("mysql".equals(ds.getLinkType())){
				qsql = showTables_mysql;
			}else if("oracle".equals(ds.getLinkType())){
				qsql = showTables_oracle;
			}else if("sqlserver".equals(ds.getLinkType())){
				qsql = showTables_sqlser;
			}else if("db2".equals(ds.getLinkType())){
				qsql = ConstantsEngine.replace(showTables_db2, ds.getLinkName());
			}else if("postgresql".equals(ds.getLinkType())){
				qsql = ConstantsEngine.replace(showTables_psql, ds.getLinkName());
			}else if("hive".equals(ds.getLinkType())){
				qsql = showTables_hive;
			}else if("kylin".equals(ds.getLinkType())){
				qsql = showTables_kylin;
			}
			ResultSet tbs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});


			while(tbs.next()){
				for(int i=0; i<tbs.getMetaData().getColumnCount(); i++){
					String col = tbs.getMetaData().getColumnLabel(i + 1);
					System.out.println( col + " === " + tbs.getString(col));
				}
			}
			tbs.close();
			PreparedStatement ps = conn.prepareStatement(qsql);
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				Map<String, Object> m = new HashMap<String, Object>();
				copyData(rs, m);
				ret.add(m);
			}
			rs.close();
			ps.close();
			**/
		}catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("sql 执行报错.");
		}finally{
			if(conn != null){
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}
}
