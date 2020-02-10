package cn.zup.bis.dao.model;

import cn.zup.bis.entity.model.Dimension;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
@Repository

public interface DimensionDao {
	
	void insertDim(Dimension dim);
	
	void updatedim(Dimension dim);
	
	void deleteDim(Dimension dim);
	
	void insertGroup(Dimension dim);
	
	Integer getMaxDimId();
	
	void deleteGroupById(@Param("groupId") String groupId);
		
	void deleteGroupByCubeId(@Param("cubeId") Integer cubeId);
	
	List<String> listGroup(@Param("cubeId") Integer cubeId);
	
	Dimension getDimInfo(@Param("dimId") Integer dimId, @Param("cubeId") Integer cubeId);
	
	void updateColType(Map<String, Object> dim);
}
