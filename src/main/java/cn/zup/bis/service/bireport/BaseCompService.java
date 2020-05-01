package cn.zup.bis.service.bireport;

import cn.zup.bis.entity.model.DataSource;
import cn.zup.bis.entity.bireport.KpiDto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 组件基类Service
 * @author hq
 *
 */
public abstract class BaseCompService {
	
	protected JSONObject pageBody; //页面配置信息

	public Map<String, String> createTableAlias(JSONObject dset){
		Map<String, String> tableAlias = new HashMap<String, String>();
		tableAlias.put(dset.getString("master"), "a0");
		JSONArray joinTabs = (JSONArray)dset.get("joininfo");
		for(int i=0; joinTabs != null && i<joinTabs.size(); i++){
			JSONObject tab = joinTabs.getJSONObject(i);
			tableAlias.put(tab.getString("ref"), "a" + (i+1));
		}
		return tableAlias;
	}
	
//	public String createDsource(DataSource ds, MVContext mv){
//		DataSourceContext dsource = new DataSourceContext();
//		dsource.putProperty("id", ds.getDsid());
//		String use = dsource.getUse();
//		dsource.putProperty("usetype", use);
//		if(use == null || "jdbc".equalsIgnoreCase(use.toString())){
//			String linktype = ds.getLinkType();
//			dsource.putProperty("linktype", linktype);
//			dsource.putProperty("linkname", ds.getLinkName());
//			dsource.putProperty("linkpwd", ds.getLinkPwd());
//			dsource.putProperty("linkurl", ds.getLinkUrl());
//		}else{
//			dsource.putProperty("jndiname", ds.getJndiName());
//		}
//		//放入MV
//		if(mv.getDsources() == null){
//			mv.setDsources(new HashMap<String, DataSourceContext>());
//		}
//		mv.getDsources().put(dsource.getId(), dsource);
//		return dsource.getId();
//	}

//	public int type2value(String tp){
//		int curDate = 4;;
//		if(tp.equals("year")){
//			curDate = 4;
//		}else if(tp.equals("quarter")){
//			curDate = 3;
//		}else if(tp.equals("month")){
//			curDate = 2;
//		}else if(tp.equals("day")){
//			curDate = 1;
//		}
//		return curDate;
//	}

//	public String loadFieldName(String aggre) {
//		if("sum".equalsIgnoreCase(aggre)){
//			return "合计值";
//		}else if("avg".equalsIgnoreCase(aggre)){
//			return "均值";
//		}else if("max".equalsIgnoreCase(aggre)){
//			return "最大值";
//		}else if("min".equalsIgnoreCase(aggre)){
//			return "最小值";
//		}else if("count".equalsIgnoreCase(aggre)){
//			return "计数";
//		}else if("var".equalsIgnoreCase(aggre)){
//			return "方差";
//		}else if("sd".equalsIgnoreCase(aggre)){
//			return "标准差";
//		}else if("middle".equalsIgnoreCase(aggre)){
//			return "中位数";
//		}else{
//			return "合计";
//		}
//	}

	public String list2String(List<String> rets){
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<rets.size(); i++){
			String ret = rets.get(i);
			sb.append(ret);
			if(i != rets.size() - 1){
				sb.append(",");
			}
		}
		return sb.toString();
	}
	
	/**
	 * 根据指标计算的值筛选，从新设置时间字段的数据区间，主要针对日、月份的数据区间控制
	 */

	
	//输出单位比例
	public String writerUnit(Integer bd){
		if(bd == null){
			return "";
		}else{
			int v = bd.intValue();
			if(v == 1){
				return "";
			}else if(v == 100){
				return "百";
			}else if(v == 1000){
				return "千";
			}else if(v == 10000){
				return "万";
			}else if(v == 1000000){
				return "百万";
			}else if(v == 100000000){
				return "亿";
			}else{
				return "*" + v;
			}
		}
	}

//	public void parserHiddenParam(List<PortalParamDto> params, MVContext mv, Map<String, InputField> mvParams) throws ExtConfigException {
//		if(params != null){
//			for(int i=0; i<params.size(); i++){
//				PortalParamDto param = params.get(i);
//				TextFieldContext target = new TextFieldContextImpl();
//				target.setId(param.getParamid());
//				String defvalue = param.getDefvalue();
//				String type = param.getType();
//				String dtformat = param.getDtformat();
//				if(("dateselect".equals(type) || "monthselect".equals(type) || "yearselect".equals(type) )&& "now".equals(defvalue)){
//					defvalue = new SimpleDateFormat(dtformat).format(new Date());
//				}
//				target.setValue(defvalue);
//				target.setType("hidden");
//				mvParams.put(target.getId(), target);
//				ExtContext.getInstance().putServiceParam(mv.getMvid(), target.getId(), target);
//
//
//				mv.getChildren().add(target);
//				target.setParent(mv);
//			}
//		}
//	}
//
//	/**
//	 * nodetype 表示筛选的类型，分为维度筛选和指标筛选两类，维度筛选和指标筛选对应的SQL位置不一样，维度放where 后， 指标放 having 后
//	 * @param params
//	 * @param nodetype
//	 * @return
//	 */
//	public String dealCubeParams(List<CompParamDto> params, Map<String, String> tableAlias){
//		StringBuffer sb = new StringBuffer("");
//		for(int i=0; params != null && i<params.size(); i++){
//			CompParamDto param = params.get(i);
//
//			String col = param.getCol();
//			String type = param.getType();
//			String val = param.getVal();
//			String val2 = param.getVal2();
//			String valuetype = param.getValuetype();
//			String usetype = param.getUsetype();
//			String linkparam = param.getLinkparam();
//			String linkparam2 = param.getLinkparam2();
//			String tname = param.getTname();
//			col = tableAlias.get(tname)+"." + col;  //加上别名
//
//			if(type.equals("like")){
//				if(val != null){
//					val = "%"+val+"%";
//				}
//				if(val2 != null){
//					val2 = "%"+val2+"%";
//				}
//			}
//			if("string".equals(valuetype)){
//				if(val != null){
//					if("in".equals(type)){  //in需要把数据用逗号分隔的重新生成
//						String[] vls = val.split(",");
//						val = "";
//						for(int j=0; j<vls.length; j++){
//							val = val + "'" + vls[j] + "'";
//							if(j != vls.length - 1){
//								val = val + ",";
//							}
//						}
//					}else{
//						val = "'" + val + "'";
//					}
//				}
//				if(val2 != null){
//					val2 = "'" + val2 + "'";
//				}
//			}
//			if(type.equals("between")){
//				if(usetype.equals("gdz")){
//					sb.append(" and " +  col + " " + type + " " + val + " and " + val2);
//				}else{
//					sb.append("#if([x]"+linkparam+" != '' && [x]"+linkparam2+" != '') ");
//					sb.append(" and "  + col + " " + type + " " + ("string".equals(valuetype)?"'":"") + "[x]"+linkparam +("string".equals(valuetype)?"'":"") + " and " + ("string".equals(valuetype)?"'":"")+ "[x]"+linkparam2 + ("string".equals(valuetype)?"'":"") + " #end");
//				}
//			}else if(type.equals("in")){
//				if(usetype.equals("gdz")){
//					sb.append(" and " + col + " in (" + val + ")");
//				}else{
//					sb.append("#if([x]"+linkparam+" != '') ");
//					sb.append(" and " + col + " in (" + "$extUtils.printVals([x]"+linkparam + ", '"+valuetype+"'))");
//					sb.append("  #end");
//				}
//			}else{
//				if(usetype.equals("gdz")){
//					sb.append(" and " + col + " " + type + " " + val);
//				}else{
//					sb.append("#if([x]"+linkparam+" != '') ");
//					sb.append(" and " + col + " "+type+" " + ("string".equals(valuetype) ? "'"+("like".equals(type)?"%":"")+""+"[x]"+linkparam+""+("like".equals(type)?"%":"")+"'":"[x]"+linkparam) + "");
//					sb.append("  #end");
//				}
//			}
//		}
//		return sb.toString().replaceAll("\\[x\\]", "\\$");
//	}

	/**
	 * 获取字段别名
	 * @param kpi
	 * @param tableAlias
	 * @return
	 */
	public String convertKpiName(KpiDto kpi, Map<String, String> tableAlias){
		String colName = kpi.getCol_name();

		String alias = tableAlias.get(kpi.getTname()) + ".";//指标所在表的别名a0.
		String name = colName.replaceAll("\\((.*?)\\)", "(" + alias+"$1" + ")");//.*？  表示匹配任意字符到下一个符合条件的字符 ,(.*？)设置分组，如果替答换，获取，可以用$1参数代替
		return name;
	}

	/**
	 * 组件联动时，获取 paranName
	 * @param compId
	 * @return
	 */
//	public String findEventParamName(String compId){
//		if(pageBody == null){
//			throw new RuntimeException("pageBody 未初始化...");
//		}
//		String paramName = null;
//		for(int i=1; true; i++){
//			Object tmp = pageBody.get("tr" + i);
//			if(tmp == null){
//				break;
//			}
//			JSONArray trs = (JSONArray)tmp;
//			for(int j=0; j<trs.size(); j++){
//				JSONObject td = trs.getJSONObject(j);
//				Object cldTmp = td.get("children");
//				if(cldTmp != null){
//					JSONArray children = (JSONArray)cldTmp;
//					for(int k=0; k<children.size(); k++){
//						JSONObject comp = children.getJSONObject(k);
//						String type = comp.getString("type");
//						if("chart".equals(type)){
//							PortalChartQuery chart = JSONObject.toJavaObject(comp, PortalChartQuery.class);
//							Map<String, Object> link = chart.getChartJson().getLink();
//							if(link != null){
//								String target = (String)link.get("target");
//								if(target != null && target.indexOf(compId) >= 0){
//									paramName = (String)link.get("paramName");
//									break;
//								}
//							}
//						}else if("table".equals(type)){
//							PortalTableQuery table = JSONObject.toJavaObject(comp, PortalTableQuery.class);
//							Map<String, Object> link =  table.getLink();
//							if(link != null){
//								String target = (String)link.get("target");
//								if(target != null && target.indexOf(compId) >= 0){
//									paramName = (String)link.get("paramName");
//									break;
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		return paramName;
//	}


}
