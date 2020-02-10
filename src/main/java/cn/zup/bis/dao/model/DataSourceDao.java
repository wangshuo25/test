package cn.zup.bis.dao.model;

import cn.zup.bis.entity.model.DataSource;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface DataSourceDao {

	List<DataSource> listDataSource();
	
	void insertDataSource(DataSource ds);
	
	void updateDataSource(DataSource ds);
	
	void deleteDataSource(@Param("dsid") String dsid);
	
	DataSource getDataSource(@Param("dsid") String dsid);
}
