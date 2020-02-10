package cn.zup.bis.dao.bireport;

import cn.zup.bis.entity.bireport.OlapInfo;
import org.jeecgframework.minidao.annotation.Arguments;
import org.jeecgframework.minidao.annotation.MiniDao;

import java.util.List;
import java.util.Map;

@MiniDao
public interface OlapDao {
	@Arguments({"cubeId"})
	List<Map<String, Object>> listDims(Integer cubeId);

	@Arguments({"pageId"})
    OlapInfo getOlap(Integer pageId);

	@Arguments({"keyword"})
	List<OlapInfo> listreport(String keyword);


	@Arguments({"pageId"})
	void deleteOlap(Integer pageId);

	void insertOlap(OlapInfo olap);

	void renameOlap(OlapInfo olap);

	void updateOlap(OlapInfo olap);

	Integer maxOlapId();

	@Arguments({"pageName"})
	Integer olapExist(String pageName);
	@Arguments({"cubeId"})
	List<Map<String, Object>> listKpiDesc(Integer cubeId);
}
