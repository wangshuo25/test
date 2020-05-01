package cn.zup.bis.dao.model;

import cn.zup.bis.entity.model.Dataset;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository

public interface DatasetDao {

	List<Dataset> listDataset(@Param("keyword") String keyword);
	
	void updateDset(Dataset ds);
	
	void insertDset(Dataset ds);
	
	void deleteDset(@Param("dsetId") String dsetId);
	
	String getDatasetCfg(@Param("dsetId") String dsetId);
	
	void updateDsetCfg(Dataset ds);
}
