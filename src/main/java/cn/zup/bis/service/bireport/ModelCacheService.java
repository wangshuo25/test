package cn.zup.bis.service.bireport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.zup.bis.entity.model.DataSource;
import cn.zup.bis.dao.model.DataSourceDao;
import cn.zup.bis.dao.model.DatasetDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据建模数据源/数据集缓存对象
 * @author hy
 * @date 2017-5-2
 */
@Service
public class ModelCacheService {
	
	/**
	 * 数据源缓存对象
	 */
	private Map<String, DataSource> dsources = new HashMap<String, DataSource>();
	/**
	 * 数据集缓存对象
	 */
	private Map<String, JSONObject> dsets = new HashMap<String, JSONObject>();
	
	@Autowired
	private DataSourceDao dsMapper;
	
	@Autowired
	private DatasetDao dsetMapper;
	
	
	public synchronized DataSource getDsource(String id){
		DataSource ret = dsources.get(id);
		if(ret == null){
			DataSource d = dsMapper.getDataSource(id);
			dsources.put(id, d);
			ret = d;
		}
		return ret;
	}
	
	public synchronized JSONObject getDset(String id){
		JSONObject ret = dsets.get(id);
		if(ret == null){
			String cfg = dsetMapper.getDatasetCfg(id);
			dsets.put(id, JSON.parseObject(cfg));
			ret = dsets.get(id);
		}
		return ret;
	}
	
	public synchronized void removeDsource(String id){
		dsources.remove(id);
	}
	
	public synchronized void removeDset(String id){
		dsets.remove(id);
	}
	
	public synchronized void addDsource(String id, DataSource json){
		dsources.put(id, json);
	}
	
	public synchronized void addDset(String id, JSONObject json){
		dsets.put(id, json);
	}
}
