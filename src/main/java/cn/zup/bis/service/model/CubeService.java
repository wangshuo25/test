package cn.zup.bis.service.model;

import cn.zup.bis.dao.model.*;
import cn.zup.bis.entity.*;
import cn.zup.bis.entity.model.Cube;
import cn.zup.bis.entity.model.Dimension;
import cn.zup.bis.entity.model.Measure;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CubeService {
	
	private Logger log = Logger.getLogger(CubeService.class);
	
	@Autowired
	private CubeDao mapper;
	
	@Autowired
	private DimensionDao dimMapper;
	
	@Autowired
	private MeasureDao kpiMapper;
	
	@Autowired
	private CubeColMetaDao metaMapper;
	
	@Autowired
	private DatasetDao dsetMapper;

	public List<Cube> listCube(String keyword){
		return mapper.listCube(keyword);
	}
	
	public Integer getMaxCubeId(){
		return mapper.getMaxCubeId();
	}
	
	public JSONObject getCubeById(Integer cubeId){
		Cube cube = mapper.getCubeById(cubeId);
		JSONObject ret = JSONObject.fromObject(cube);
		//查询字段
		String cfg = dsetMapper.getDatasetCfg(cube.getDsetId());
		JSONObject dset = JSONObject.fromObject(cfg);
		ret.put("cols", dset.get("cols"));
//		ret.put("dynamic", dset.get("dynamic"));
		//查询维度
		ret.put("dims", mapper.getCubeDims(cubeId));
		//查询度量
		ret.put("kpis", mapper.getCubeKpis(cubeId));
		return ret;
	}
	
	public Result insertCube(Cube cube){
		Result ret = new Result();

			System.err.println("开始插入1");
			Integer cubeId = mapper.getMaxCubeId();
			cube.setCubeId(cubeId);
			System.err.println("开始插入2");

			mapper.insertCube(cube);
			System.err.println("开始插入3");

			this.insertDim(cube);
			this.insertDimRela(cube);//cubcolmeta更新的话 都是先删除再 insert
			this.insertKpi(cube);
			this.insertKpiRela(cube);
			ret.setResult(RequestStatus.SUCCESS.getStatus());

		return ret;
	}
	
	public Result deleteCube(Integer cubeId){
		Result ret = new Result();
			//删除表
			mapper.deleteCube(cubeId);
			metaMapper.deleteByCubeId(cubeId);
			//删除指标
			Measure kpi = new Measure();
			kpi.setCubeId(cubeId);
			kpiMapper.deleteKpi(kpi);
			//删除维度
			Dimension dim = new Dimension();
			dim.setCubeId(cubeId);
			dimMapper.deleteDim(dim);
			//删除分组
			dimMapper.deleteGroupByCubeId(cubeId);
			ret.setResult(RequestStatus.SUCCESS.getStatus());

		return ret; 
	}
	
	public Result updateCube(Cube cube){
		Result ret = new Result();

			mapper.updateCube(cube);
			//在编辑立方体时，通过delObj 来描述哪些维度，度量、分组被删除掉了。先第一步删除这些
			List<Map<String, Object>> dels = cube.getDelObj();
			for(int i=0; i<dels.size(); i++){
				Map<String, Object> obj = dels.get(i);
				Object tp = obj.get("type");
				Object id = obj.get("id");
				if(id == null || id.toString().length() == 0){
					continue;
				}
				
				if("dim".equals(tp)){
					Dimension dim = new Dimension();
					dim.setCubeId(cube.getCubeId());
					dim.setDimId((Integer)id);
					dimMapper.deleteDim(dim);
				}else if("kpi".equals(tp)){
					Measure kpi = new Measure();
					kpi.setCubeId(cube.getCubeId());
					kpi.setKpiId((Integer)id);
					kpiMapper.deleteKpi(kpi);
				}else if("group".equals(tp)){
					dimMapper.deleteGroupById((String)id);
				}
			}
			
			//删除关系表数据，再从建
			//处理维度
			this.updateDim(cube);
			this.insertDimRela(cube);
			//处理指标
			this.updateKpi(cube);
			this.insertKpiRela(cube);
			ret.setResult(RequestStatus.SUCCESS.getStatus());

		return ret;
	}
	
	private void updateDim(Cube cube){
		List<String> groupkeys = dimMapper.listGroup(cube.getCubeId());
		List<Dimension> dims = cube.getDims();
		for(int i=0; i<dims.size(); i++){
			Dimension dim = dims.get(i);
			dim.setCubeId(cube.getCubeId());
			dim.setOrd(i);
			String type = dim.getType();
			if(type == null || type.length() == 0){
				dim.setType("frd");
			}
			//判断是否有分组，如果有分组插入分组
			String groupId = dim.getGroupId();
			if(groupId != null && groupId.length() > 0 && !groupkeys.contains(groupId)){
				dimMapper.insertGroup(dim);
				groupkeys.add(groupId);
			}
			Integer targetId = dim.getTargetId();
			if(targetId == null){  //新增维度
				dimMapper.insertDim(dim);
				targetId = dimMapper.getMaxDimId();
			}else{ //修改维度
				String isupdate = dim.getIsupdate();
				if("y".equals(isupdate)){  //只有修改过的维度才更新
					dimMapper.updatedim(dim);
				}
			}
			dim.setDimId(targetId);
		}
	}
	
	private void updateKpi(Cube cube){
		List<Measure> kpis = cube.getKpis();
		for(int i=0; i<kpis.size(); i++){
			Measure kpi = kpis.get(i);
			kpi.setCubeId(cube.getCubeId());
			Integer targetId = kpi.getTargetId();
			if(targetId == null){ //新增
				kpiMapper.insertKpi(kpi);
				targetId = kpiMapper.getMaxKpiId();
			}else{ //修改
				String isupdate = kpi.getIsupdate();
				if("y".equals(isupdate)){
					kpiMapper.updateKpi(kpi);
				}
			}
			kpi.setKpiId(targetId);
		}
	}
	
	private void insertDim(Cube cube){
		List<String> groupkeys = new ArrayList<String>();
		List<Dimension> dims = cube.getDims();
		int dimId = 0;
		for(int i=0; i<dims.size(); i++){
			Dimension dim = dims.get(i);
			dim.setOrd(i);
			String type = dim.getType();
			if(type == null || type.length() == 0){
				dim.setType("frd");
			}
			dim.setCubeId(cube.getCubeId());
			dimMapper.insertDim(dim);
			if(i == 0){
				dimId = dimMapper.getMaxDimId();
			}else{
				dimId++;
			}
		
			//判断是否有分组，如果有分组插入分组
			String groupId = dim.getGroupId();
			if(groupId != null && groupId.length() > 0 && !groupkeys.contains(groupId)){
				dimMapper.insertGroup(dim);
				groupkeys.add(groupId);
			}
			
			dim.setDimId(dimId);//为了插入meata用
		}
	}
	
	private void insertDimRela(Cube cube){
		metaMapper.deleteDimMeta(cube.getCubeId());
		List<Dimension> dims = cube.getDims();
		for(int i=0; i<dims.size(); i++){
			Dimension dim = dims.get(i);
			dim.setColId(dim.getDimId());
			dim.setOrd(i);
			dim.setColType(1);
			metaMapper.insertMeta(dim);
		}
	}
	
	private void insertKpi(Cube cube){
		int kpiId = 0;
		List<Measure> kpis = cube.getKpis();
		for(int i=0; i<kpis.size(); i++){
			Measure kpi = kpis.get(i);
			kpi.setCubeId(cube.getCubeId());
			kpiMapper.insertKpi(kpi);
			if(i == 0){
				kpiId = kpiMapper.getMaxKpiId();
			}else{
				kpiId++;
			}
			kpi.setKpiId(kpiId);
		}
	}
	
	private void insertKpiRela(Cube cube){
		//先删除指标数据
		metaMapper.deleteKpiMeta(cube.getCubeId());
		List<Measure> kpis = cube.getKpis();
		for(int i=0; i<kpis.size(); i++){
			Measure kpi = kpis.get(i);
			kpi.setColId(kpi.getKpiId());
			kpi.setOrd(i);
//			//如果指标不是计算指标，直接拼接，计算指标直接取公式
//			int calcKpi = kpi.getCalcKpi();  //新增度量那创建的计算指标
//			//int calc = kpi.getCalc();  //数据集创建的动态字段
//			if(calcKpi == 0){
//				kpi.setCol(kpi.getAggreCol());
//			}else{
//				kpi.setCol(kpi.getCol());
//			}
			kpi.setCol(kpi.getAggreCol());
			kpi.setColType(2);
			metaMapper.insertMeta(kpi);
		}
	}
	
	public Object treeCube(Integer cubeId){
		Map<String, Object> curGroup = null; //当前分组对象.
		List<Map<String, Object>> ls = mapper.listDs(String.valueOf(cubeId));

		for(int i=0; i<ls.size(); i++){
			Map<String, Object> m = (Map<String, Object>)ls.get(i);
			System.err.println(m);
			m.put("iconCls", "icon-cube");
			//给数据集节点添加维度、指标节点
			List<Map<String, Object>> cubeChild = new ArrayList<Map<String, Object>>();
			m.put("children", cubeChild);

			Map<String, Object> wdnode = new HashMap<String, Object>();
			wdnode.put("id", "wd");
			wdnode.put("text", "维度");

			wdnode.put("iconCls", "icon-dim2");
			List<Map<String, Object>> wdnodeChild = new ArrayList<Map<String, Object>>();
			wdnode.put("children", wdnodeChild);
			cubeChild.add(wdnode);


			Map<String, Object> zbnode = new HashMap<String, Object>();
			zbnode.put("id", "zb");
			zbnode.put("text", "度量");

			zbnode.put("iconCls", "icon-kpigroup");
			List<Map<String, Object>> zbnodeChild = new ArrayList<Map<String, Object>>();
			zbnode.put("children", zbnodeChild);
			cubeChild.add(zbnode);
			
			List<Map<String, Object>> children = mapper.listCubeMeta(cubeId);
			System.err.println(children);


			//设置attributes;
			for(int j=0; j<children.size(); j++){
				Map<String, Object> child = (Map<String, Object>)children.get(j);
				Integer col_type = new Integer(child.get("col_type").toString());
				String grouptype = (String)child.get("grouptype");//找到分组id
				if(grouptype == null || grouptype.length() == 0){
					grouptype = null;
				}
				String groupname = (String)child.get("groupname");//找到分组name
				if(grouptype != null && grouptype.length() > 0){
					if(curGroup == null || !curGroup.get("id").equals(grouptype)){//建立第一个分组 或者该节点不属于之前建立的分组，需要新建
						//添加分组节点
						Map<String, Object> fz = new HashMap<String, Object>();
						fz.put("id", grouptype);
						fz.put("text", groupname);

						fz.put("iconCls", "icon-dim");
						fz.put("children", new ArrayList());
						//给分组添加attributes (把分组的第一个节点信息传递给他,拖拽分组时就当拖拽第一个节点)
						Map<String, Object> attr = new HashMap<String, Object>();
						fz.put("attributes", attr);
						attr.put("col_type", col_type);
						attr.put("col_id", child.get("col_id"));
						attr.put("col_name", child.get("col_name"));
						attr.put("cubeId", child.get("cubeId"));
						attr.put("dsetId", child.get("dsetId"));
						attr.put("dsid", child.get("dsid"));
						attr.put("alias", child.get("alias"));
						attr.put("dim_type", child.get("dim_type"));
//						attr.put("tableName", child.get("tableName") == null ? "" : child.get("tableName"));
//						attr.put("tableColKey", child.get("tableColKey") == null ? "" : child.get("tableColKey"));
//						attr.put("tableColName", child.get("tableColName") == null ? "" : child.get("tableColName"));
//						attr.put("ordcol", child.get("ordcol") == null ? "" : child.get("ordcol"));
//						attr.put("dateformat", child.get("dateformat") == null ? "" : child.get("dateformat"));
						attr.put("tname", child.get("tname"));
//						attr.put("calc", child.get("calc"));
//						if(curGroup == null){
//							attr.put("iscas", "");
//						}else{
//							attr.put("iscas", "y");
//						}
//						attr.put("dimord", child.get("dimord") == null ? "" : child.get("dimord"));
						attr.put("grouptype", grouptype);//Group id
						attr.put("valType", child.get("valType"));
						wdnodeChild.add(fz);
						curGroup = fz;
					}
				}else{
					curGroup = null;
				}
				Map<String, Object> attr = new HashMap<String, Object>();
				child.put("attributes", attr);
				//添加立方体所使用的数据源到Tree
				attr.put("col_type", col_type);
				attr.put("col_id", child.get("col_id"));
				attr.put("col_name", child.get("col_name"));
				attr.put("cubeId", child.get("cubeId"));
				attr.put("dsetId", child.get("dsetId"));
				attr.put("dsid", child.get("dsid"));
				attr.put("alias", child.get("alias"));
//				attr.put("fmt", child.get("fmt") == null ? "" : child.get("fmt"));
				attr.put("aggre", child.get("aggre"));
				attr.put("dim_type", child.get("dim_type"));
//				attr.put("tableName", child.get("tableName") == null ? "" : child.get("tableName"));
//				attr.put("tableColKey", child.get("tableColKey") == null ? "" : child.get("tableColKey"));
//				attr.put("tableColName", child.get("tableColName") == null ? "" : child.get("tableColName"));
//				attr.put("dateformat", child.get("dateformat") == null ? "" : child.get("dateformat"));
				attr.put("tname", child.get("tname"));
//				attr.put("calc", child.get("calc"));
//				if(curGroup == null){
//					attr.put("iscas", "");
//				}else{
//					attr.put("iscas", "y");
//				}
//				attr.put("dimord", child.get("dimord") == null ? "" : child.get("dimord"));
//				attr.put("rate", child.get("rate"));
				attr.put("unit", child.get("unit") == null ? "" : child.get("unit"));
				attr.put("grouptype", grouptype);
//				attr.put("calc_kpi", child.get("calc_kpi"));
				attr.put("valType", child.get("valType"));
//				attr.put("ordcol", child.get("ordcol") == null ? "" : child.get("ordcol"));
				//设置节点图标
				if(col_type == 1){
					if(grouptype == null || grouptype.length() == 0){
						child.put("iconCls", "icon-dim");
					}else{
						child.put("iconCls", "icon-dimlevel");
					}
				}else{
					child.put("iconCls", "icon-kpi");
				}
				if(col_type == 1){
					if(curGroup == null){
						wdnodeChild.add(child);
					}else{//分组
						((List)curGroup.get("children")).add(child);
					}
				}else{
					zbnodeChild.add(child);
				}
			}
		}
		System.err.println(ls);
		return ls;
	}

	public List<Map<String, Object>> listDims(Integer cubeId){
		return mapper.listDims(cubeId);
	}
}
