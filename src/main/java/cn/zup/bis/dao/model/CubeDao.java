package cn.zup.bis.dao.model;

import cn.zup.bis.entity.model.Cube;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
@Repository

public interface CubeDao {

	List<Cube> listCube(@Param("keyword") String keyword);
	
	Integer getMaxCubeId();
	
	void insertCube(Cube cube);
	
	void updateCube(Cube cube);
	
	void deleteCube(@Param("cubeId") Integer cubeId);
	
	Cube getCubeById(@Param("cubeId") Integer cubeId);
	
	List<Map<String, Object>> getCubeDims(@Param("cubeId") Integer cubeId);
	
	List<Map<String, Object>> getCubeKpis(@Param("cubeId") Integer cubeId);
	
	List<Map<String, Object>> listCubeMeta(@Param("cubeId") Integer cubeId);
	
	List<Map<String, Object>> listDs(@Param("selectDsIds") String selectDsIds);

	List<Map<String, Object>> listDims(@Param("cubeId") Integer cubeId);
}
