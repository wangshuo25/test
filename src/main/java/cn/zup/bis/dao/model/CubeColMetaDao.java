package cn.zup.bis.dao.model;

import cn.zup.bis.entity.model.CubeColMeta;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository

public interface CubeColMetaDao {
	
	void insertMeta(CubeColMeta meta);
	
	Integer getMaxRid();

	void deleteKpiMeta(@Param("cubeId") Integer cubeId);
	
	void deleteDimMeta(@Param("cubeId") Integer cubeId);
	
	void deleteByCubeId(@Param("cubeId") Integer cubeId);
}
