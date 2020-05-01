package cn.zup.bis.service.model;

import cn.zup.bis.entity.model.Dimension;
import cn.zup.bis.dao.model.DimensionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DimensionService {

	@Autowired
	private DimensionDao mapper;
	
	public Dimension getDimInfo(Integer dimId, Integer cubeId){
		return mapper.getDimInfo(dimId, cubeId);
	}
}
