package cn.zup.bis.service.bireport;

import cn.zup.bis.entity.bireport.DimDto;
import cn.zup.bis.entity.bireport.KpiDto;
import cn.zup.bis.entity.bireport.ParamDto;
import cn.zup.bis.entity.bireport.TableQueryDto;
import com.mysql.jdbc.Driver;
import com.ruisi.ext.engine.view.context.cross.CrossReportContextImpl;
import com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContext;
import cn.zup.bis.util.RSBIUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.util.IdCreater;
import com.ruisi.ext.engine.view.context.Element;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.ext.engine.view.context.MVContextImpl;
import com.ruisi.ext.engine.view.context.form.InputField;
import com.ruisi.ispire.dc.grid.GridFilter;
import com.ruisi.ispire.dc.grid.GridProcContext;
import com.ruisi.ext.engine.view.context.form.InputField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Scope("prototype")
public class TableService extends BaseCompService {
	
	public final static String deftMvId = "mv.tmp.table";
	
	private Map<String, InputField> mvParams = new HashMap<String, InputField>(); //mv的参数
	
	private StringBuffer scripts = new StringBuffer(); //用来构造js 脚本的字符串对象
	
	/***
	 * 当指标有计算指标时，需要计算上期、同期等值，在显示数据时需要对偏移的数据进行过滤，
	 */
	private List<com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext> filters = new ArrayList<com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext>();
	


	@Autowired
	private ModelCacheService cacheService;
	
	public @PostConstruct void init() {
		
	}  
	
	public @PreDestroy void destory() {
		mvParams.clear();
		filters.clear();
	}
	
	public MVContext json2MV(TableQueryDto table) throws ParseException, IOException{
		//创建MV
		MVContext mv = new MVContextImpl();
		mv.setChildren(new ArrayList<Element>());
		String formId = ExtConstants.formIdPrefix + IdCreater.create();
		mv.setFormId(formId);
		mv.setMvid(deftMvId);
		
		//创建corssReport
		//添加kpiOther
		DimDto kpiOther = new DimDto();
		kpiOther.setType("kpiOther");
		table.getCols().add(kpiOther);
		com.ruisi.ext.engine.view.context.cross.CrossReportContext cr = json2Table(table);
		//移除kpiOther
		table.getCols().remove(table.getCols().size() - 1);
		//设置ID
		String id = ExtConstants.reportIdPrefix + IdCreater.create();
		cr.setId(id);
		cr.setOut("olap");
		cr.setShowData(true);
		//cr.setExportName(title);
	
		mv.getChildren().add(cr);
		cr.setParent(mv);
		
		Map<String, com.ruisi.ext.engine.view.context.cross.CrossReportContext> crs = new HashMap<String, com.ruisi.ext.engine.view.context.cross.CrossReportContext>();
		crs.put(cr.getId(), cr);
		mv.setCrossReports(crs);
		
		//创建datacenter
		String sql = this.createSql(table, 0);
		com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContext dc = this.createDataCenter(sql, table);
		cr.setRefDataCetner(dc.getId());
		if(mv.getGridDataCenters() == null){
			mv.setGridDataCenters(new HashMap<String, com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContext>());
		}
		mv.getGridDataCenters().put(dc.getId(), dc);
		
		//判断是否需要创建数据源
		String dsid = super.createDsource(cacheService.getDsource(table.getDsid()), mv);
		dc.getConf().setRefDsource(dsid);
		
		String scripts = this.scripts.toString();
		if(scripts != null && scripts.length() > 0){
			mv.setScripts(scripts);
		}
		return mv;
	}
	
	public com.ruisi.ext.engine.view.context.cross.CrossReportContext json2Table(TableQueryDto table) throws ParseException{
		com.ruisi.ext.engine.view.context.cross.CrossReportContext ctx = new CrossReportContextImpl();
		
		com.ruisi.ext.engine.view.context.cross.CrossCols cols = new com.ruisi.ext.engine.view.context.cross.CrossCols();
		cols.setCols(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
		ctx.setCrossCols(cols);
		
		com.ruisi.ext.engine.view.context.cross.CrossRows rows = new com.ruisi.ext.engine.view.context.cross.CrossRows();
		rows.setRows(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
		ctx.setCrossRows(rows);
		
		ctx.setLabel(table.getCompId());  //给组件设置label
		boolean uselink = false;
		Map<String, Object> link = table.getLink();
		if(link != null && !link.isEmpty()){
			com.ruisi.ext.engine.view.context.cross.RowLinkContext rlink = new com.ruisi.ext.engine.view.context.cross.RowLinkContext();
			rlink.setParamName((String)link.get("paramName"));
			String url = (String)link.get("url");  //url 优先于联动组件
			if(url != null && url.length() >0){
				rlink.setUrl(url);
			}else{
				String target = (String)link.get("target");
				String type = (String)link.get("type");
				rlink.setTarget(target.split(","));
				rlink.setType(type.split(","));
			}
			ctx.getCrossRows().setLink(rlink);
			uselink = true;
		}
		
		//表格钻取维度
		List<com.ruisi.ext.engine.view.context.cross.RowDimContext> drill = this.getDrillDim(table);
		if(drill != null && drill.size() > 0){
			ctx.setDims(drill);
			uselink = true;
		}

		loopJsonField(table.getCols(), cols.getCols(), table.getKpiJson(), "col", uselink);
		loopJsonField(table.getRows(), rows.getRows(), table.getKpiJson(), "row", uselink);
		
		//如果没有维度，添加none维度
		if(cols.getCols().size() == 0){
			com.ruisi.ext.engine.view.context.cross.CrossField cf = new com.ruisi.ext.engine.view.context.cross.CrossField();
			cf.setType("none");
			cf.setDesc("合计");
			cols.getCols().add(cf);
		}
		if(rows.getRows().size() == 0){
			com.ruisi.ext.engine.view.context.cross.CrossField cf = new com.ruisi.ext.engine.view.context.cross.CrossField();
			cf.setType("none");
			cf.setDesc("合计");
			rows.getRows().add(cf);
		}
		
		return ctx;
	}
	
	/**
	 * 生成表格SQL
	 * @param sqlVO
	 * @param tinfo
	 * @param params
	 * @param release  判断当前是否为发布状态, 0 表示不是发布，1表示发布到多维分析，2表示发布到仪表盘
	 * @return
	 * @throws ParseException
	 */
	public String createSql(TableQueryDto table, int release) throws ParseException{
		JSONObject dset = cacheService.getDset(table.getDsetId());
		Map<String, String> tableAlias = createTableAlias(dset);
		//判断是否需要计算上期、同期值
		int jstype = table.getKpiComputeType();
		StringBuffer sql = new StringBuffer();
		sql.append("select ");
		List<DimDto> dims = table.getDims();
		for(int i=0; i<dims.size(); i++){
			DimDto dim = dims.get(i);
			String key = dim.getTableColKey();
			String txt = dim.getTableColName();
			String tname = dim.getTableName();
			if(key != null && txt != null && key.length() >0 && txt.length() >0){
				sql.append( tableAlias.get(tname) + "."+key+", " + tableAlias.get(tname) + "." + txt + ",");
			}else{
				if(dim.getCalc() == 0){
					sql.append( tableAlias.get(dim.getTname()) + "." + dim.getColname()+" "+dim.getAlias()+", ");
				}else{
					sql.append( dim.getColname()+" "+dim.getAlias()+", ");
				}
			}
		}
		
		List<KpiDto> kpis = table.getKpiJson();
		if(kpis.size() == 0){
			sql.append(" 0 kpi_value ");
		}else{
			for(int i=0; i<kpis.size(); i++){
				KpiDto kpi = kpis.get(i);
				if(kpi.getCalc() != null && kpi.getCalc() == 1){  //表达式，直接取表达式
					sql.append(kpi.getCol_name() + " ");
				}else{  //获取字段别名
					String name = super.convertKpiName(kpi, tableAlias);
					sql.append( name + " ");
				}
				sql.append(kpi.getAlias());
				if(i != kpis.size() - 1){
					sql.append(",");
				}
			}
		}
		JSONArray joinTabs = (JSONArray)dset.get("joininfo");
		String master = dset.getString("master");
		sql.append(" from " + master + " a0");
		
		for(int i=0; joinTabs!=null&&i<joinTabs.size(); i++){  //通过主表关联
			JSONObject tab = joinTabs.getJSONObject(i);
			String ref = tab.getString("ref");
			String refKey = tab.getString("refKey");
			String jtype = (String)tab.get("jtype");
			if("left".equals(jtype) || "right".equals(jtype)){
				sql.append(" " + jtype);
			}
			sql.append(" join " + ref+ " " + tableAlias.get(ref));
			sql.append(" on a0."+tab.getString("col")+"="+tableAlias.get(ref)+"."+refKey);
			sql.append(" ");
		}
		sql.append(" where 1=1 ");
		

		
		for(int i=0; i<dims.size(); i++){
			DimDto dim = dims.get(i);
			
			//处理日期限制
			if(dim.getType().equals("day")){
				if(dim.getDay() != null){
					String start = dim.getDay().getStartDay();
					String end = dim.getDay().getEndDay();
					if(jstype != 0){ //有计算指标，从写数据区间
						String[] q = resetBetween(start, end, dim.getType(), dim.getDateformat(), jstype);
						start = q[0];
						end = q[1];
						com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext filter = new com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext();
						filter.setColumn(dim.getAlias());
						filter.setFilterType(GridFilter.between);
						filter.setValue(dim.getDay().getStartDay());
						filter.setValue2(dim.getDay().getEndDay());
						filter.setDateFormat(dim.getDateformat());
						this.filters.add(filter);
					}
					sql.append(" and " + dim.getColname() + " between '"+start+"' and '" + end + "'");
				}else
				if(dim.getVals() != null && dim.getVals().length() > 0){
					String ret = dim.getVals();
					if(jstype != 0){
						ret = resetVals(ret, dim.getType(), dim.getDateformat(), jstype);
						com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext filter = new com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext();
						filter.setColumn(dim.getAlias());
						filter.setFilterType(GridFilter.in);
						filter.setValue(dim.getVals());
						this.filters.add(filter);
					}
					ret = RSBIUtils.dealStringParam(ret);
					sql.append(" and " + dim.getColname()+ " in ("+ret+")");
				}
			}
			//处理月份
			else if(dim.getType().equals("month")){
				if(dim.getMonth() != null){
					//如果有计算指标，需要重写数据区间
					String start = dim.getMonth().getStartMonth();
					String end = dim.getMonth().getEndMonth();
					if(jstype != 0){
						String[] q = resetBetween(start, end, dim.getType(), dim.getDateformat(), jstype);
						start = q[0];
						end = q[1];
						com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext filter = new com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext();
						filter.setColumn(dim.getAlias());
						filter.setFilterType(GridFilter.between);
						filter.setValue(dim.getMonth().getStartMonth());
						filter.setValue2(dim.getMonth().getEndMonth());
						filter.setDateFormat(dim.getDateformat());
						this.filters.add(filter);
					}
					sql.append(" and " + dim.getColname()+ " between '"+start+"' and '" + end + "'");
				}else
				if(dim.getVals() != null && dim.getVals().length() > 0){
					//如果有计算指标，需要重写数据值列表
					String ret = dim.getVals();
					if(jstype != 0){
						ret = resetVals(ret, dim.getType(), dim.getDateformat(), jstype);
						com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext filter = new com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext();
						filter.setColumn(dim.getAlias());
						filter.setFilterType(GridFilter.in);
						filter.setValue(dim.getVals());
						this.filters.add(filter);
					}
					ret = RSBIUtils.dealStringParam(ret);
					sql.append(" and " + dim.getColname() + " in ("+ret+")");
				}
			} else {
				//限制维度筛选
				if(dim.getVals() != null && dim.getVals().length() > 0){
					String vls = null;
					if( jstype != 0){  //有计算指标，需要从写时间值
						vls = resetVals(dim.getVals(), dim.getType(), dim.getDateformat(), jstype);
						com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext filter = new com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext();
						filter.setColumn(dim.getAlias());
						filter.setFilterType(GridFilter.in);
						filter.setValue(dim.getVals());
						this.filters.add(filter);
					}else{
						vls = dim.getVals();
					}
					//处理字符串
					if("string".equalsIgnoreCase(dim.getValType())){
						vls = RSBIUtils.dealStringParam(vls);
					}
					sql.append(" and " + (dim.getCalc() == 1 ? dim.getColname(): tableAlias.get(dim.getTname()) + "." + dim.getColname()) + " in ("+vls+")");
				}
			}
		}
		
		//限制参数的查询条件
		for(int i=0; table.getParams() != null && i<table.getParams().size(); i++){
			ParamDto param = table.getParams().get(i);
			String tp = param.getType();
			String colname = param.getColname();
			String alias = param.getAlias();
			String valType = param.getValType();
			String dateformat = param.getDateformat();
			String tname = param.getTname();
			//只有参数和组件都来源于同一个表，才能进行参数拼装
			if((tp.equals("day") || tp.equals("month"))){
				if(release == 0 && param.getSt() != null && param.getSt().length() > 0 ){
					String ostart = param.getSt();
					String oend = param.getEnd();
					String start = ostart;
					String end = oend;
					if(jstype != 0){
						String[] q = resetBetween(start, end, tp, param.getDateformat(), jstype);
						start = q[0];
						end = q[1];
					}
					sql.append(" and " + colname + " between '"+ start  + "' and '" + end + "'");
				}else if(release == 1){
					sql.append(" #if($s_"+alias+" != '' && $e_"+alias+" != '') and " + colname + " between $myUtils.resetBetween($s_"+alias+", $e_"+alias+", '"+tp+"', '"+dateformat+"', "+jstype+") #end");
				}else if(release == 2){
					sql.append(" #if($"+alias+" != '') and "+tableAlias.get(tname) + "." + colname+" = $"+alias+" #end");
				}
				//生成filter
				if(jstype != 0){
					com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext filter = new com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext();
					filter.setColumn(param.getAlias());
					filter.setFilterType(GridFilter.between);
					filter.setDateFormat(param.getDateformat());
					if(release == 0 && param.getSt() != null && param.getSt().length() > 0 ){
						String ostart = param.getSt();
						String oend = param.getEnd();
						filter.setValue(ostart);
						filter.setValue2(oend);
					}else if(release == 1){
						filter.setValue("${s_"+alias+"}");
						filter.setValue2("${e_"+alias+"}");
					}else if(release == 2){
						filter.setValue("${"+alias+"}");
					}
					this.filters.add(filter);
				}
			}else{
				if(release == 0 && param.getVals() != null && param.getVals().length() > 0){
					//字符串特殊处理
					String  vls = param.getVals();
					if(jstype != 0 && ("year".equals(tp) || "quarter".equals(tp))){
						vls = resetVals(vls, tp, param.getDateformat(), jstype);
					}
					if("string".equalsIgnoreCase(valType)){
						vls = RSBIUtils.dealStringParam(vls);
					}
					sql.append(" and " + (param.getCalc() == 0 ?(tableAlias.get(tname) + "."):"") +colname + " in ("+vls+")");
				}else if(release == 1 || release == 2){
					sql.append(" #if($"+alias+" != '') and " + (param.getCalc() == 0 ?(tableAlias.get(tname) + "."):"") +colname + " in ($extUtils.printVals($myUtils.resetVals($"+alias+",'"+tp+"','"+dateformat+"', "+jstype+"), '"+valType+"')) #end");
				}
				//生成filter
				if(jstype != 0){
					com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext filter = new com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext();
					filter.setColumn(param.getAlias());
					filter.setFilterType(GridFilter.in);
					if(release == 0 && param.getVals() != null && param.getVals().length() > 0){
						filter.setValue(param.getVals());
					}else if(release == 1 || release == 2){
						filter.setValue("${"+colname+"}");
					}
					this.filters.add(filter);
				}
			}
		}
		
		//处理事件接受的参数限制条件
		Map<String, Object> linkAccept = table.getLinkAccept();
		if(linkAccept != null && linkAccept.size() > 0){
			String col = (String)linkAccept.get("col");
			String valtype = (String)linkAccept.get("valType");
			String ncol = "$" + col;
			if("string".equalsIgnoreCase(valtype)){
				ncol = "'" + ncol + "'";
			}
			sql.append(" and  " + col + " = " + ncol);
		}
		
		if(dims.size() > 0){
			sql.append(" group by ");
			for(int i=0; i<dims.size(); i++){
				DimDto dim = dims.get(i);
				String key = dim.getTableColKey();
				String txt = dim.getTableColName();
				String tname = dim.getTableName();
				if(key != null && txt != null && key.length() >0 && txt.length() >0){
					sql.append(tableAlias.get(tname) + "."+key+", "+ tableAlias.get(tname) + "." + txt);
				}else{
					if(dim.getCalc() == 1){
						sql.append(dim.getColname());
					}else{
						sql.append(tableAlias.get(dim.getTname()) + "."+dim.getColname());
					}
				}
				if(i != dims.size() - 1){
					sql.append(",");
				}
			}
		}
		//处理指标过滤
		StringBuffer filter = new StringBuffer("");
		for(KpiDto kpi : table.getKpiJson()){
			if(kpi.getFilter() != null){
				filter.append(" and "+kpi.getCol_name()+" ");
				String tp = kpi.getFilter().getFilterType();
				filter.append(tp);
				filter.append(" ");
				double val1 = kpi.getFilter().getVal1();
				if(kpi.getFmt() != null && kpi.getFmt().endsWith("%")){
					val1 = val1 / 100;
				}
				filter.append(val1 * (kpi.getRate() == null ? 1 : kpi.getRate()));
				if("between".equals(tp)){
					double val2 = kpi.getFilter().getVal2();
					if(kpi.getFmt() != null && kpi.getFmt().endsWith("%")){
						val2 = val2 / 100;
					}
					filter.append(" and " + val2 * (kpi.getRate() == null ? 1 : kpi.getRate()));
				}
			}
		}
		if(filter.length() > 0){
			sql.append(" having 1=1 " + filter);
		}
		if(dims.size() > 0){
			StringBuffer order = new StringBuffer();
			order.append(" order by ");
			//先按col排序
			for(int i=0; i<table.getCols().size(); i++){
				DimDto dim = dims.get(i);
				if(dim.getDimord() != null && dim.getDimord().length() > 0){
					order.append(dim.getTableColKey() != null && dim.getTableColKey().length() > 0 ? dim.getTableColKey() : dim.getColname());
					order.append(" ");
					order.append(dim.getDimord());
					order.append(",");
				}
			}
			//判断是否按指标排序
			for(int i=0; i<kpis.size(); i++){
				KpiDto kpi = kpis.get(i);
				if(kpi.getSort() != null && kpi.getSort().length() > 0){
					order.append(kpi.getAlias() + " " + kpi.getSort());
					order.append(",");
				}
			}
			
			//再按row排序
			for(int i=0; i<table.getRows().size(); i++){
				DimDto dim = table.getRows().get(i);
				if(dim.getDimord() != null && dim.getDimord().length() > 0){
					order.append(dim.getTableColKey() != null && dim.getTableColKey().length() > 0 ? dim.getTableColKey() : dim.getColname());
					order.append(" ");
					order.append(dim.getDimord());
					order.append(",");
				}
			}
			
			
			if(order.length() <= 11 ){  //判断是否拼接了 order by 字段
				return sql.toString().replaceAll("@", "'");
			}else{
				//返回前先去除最后的逗号
				return (sql + order.toString().substring(0, order.length() - 1)).replaceAll("@", "'");
			}
		}else{
			return sql.toString().replaceAll("@", "'");
		}
	}
	
	/**
	 * 创建表格datacenter
	 * @param sql
	 * @return
	 * @throws IOException
	 */
	public com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContext createDataCenter(String sql, TableQueryDto dto) throws IOException{
		GridDataCenterContext ctx = new com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContextImpl();
		com.ruisi.ext.engine.view.context.dc.grid.GridSetConfContext conf = new com.ruisi.ext.engine.view.context.dc.grid.GridSetConfContext();
		ctx.setConf(conf);
		ctx.setId("DC-" + IdCreater.create());
//		String name = TemplateManager.getInstance().createTemplate(sql);
		String name = "mv1";
		ctx.getConf().setTemplateName(name);
		
		//判断指标计算
		for(KpiDto kpi : dto.getKpiJson()){
			if(kpi.getCompute() != null && kpi.getCompute().length() > 0){
				if("zb".equals(kpi.getCompute())){
					GridProcContext proc = this.createAccount(dto, kpi);
					ctx.getProcess().add(proc);
				}else if("sxpm".equals(kpi.getCompute()) || "jxpm".equals(kpi.getCompute())){
					GridProcContext proc = this.createSort(dto, kpi);
					ctx.getProcess().add(proc);
				}else if("ydpj".equals(kpi.getCompute())){
					GridProcContext proc = this.createMoveAvg(dto, kpi);
					ctx.getProcess().add(proc);
				}else{
					String[] jss = kpi.getCompute().split(",");
					for(String js : jss){
						GridProcContext proc = this.createShift(dto, kpi, js);
						ctx.getProcess().add(proc);
					}
				}
			}
		}
		
		//判断是否有时间偏移的计算
		for(com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext filter : this.filters){
			ctx.getProcess().add(filter);
		}
		
		return ctx;
	}
	
	/**
	 * 创建指标排名process
	 * @param sqlVO
	 * @param kpi
	 * @return
	 */
	private GridProcContext createSort(TableQueryDto sqlVO, KpiDto kpi){
		com.ruisi.ext.engine.view.context.dc.grid.GridSortContext proc = new com.ruisi.ext.engine.view.context.dc.grid.GridSortContext();
		proc.setAppendOrder(true);
		proc.setChangeOldOrder(false);
		
		//创建排序的分组维
		StringBuffer sb = new StringBuffer("");
		StringBuffer orderSb = new StringBuffer("");
		for(int i=0; i<sqlVO.getCols().size(); i++){
			DimDto dim = sqlVO.getCols().get(i);
			//设置 col 维度 为分组维
			sb.append(dim.getAlias());
			sb.append(",");
			orderSb.append(dim.getDimord());
			orderSb.append(",");
		}
		sb.append(kpi.getAlias());
		orderSb.append("sxpm".equals(kpi.getCompute())?"asc":"desc");
		proc.setColumn(sb.toString().split(","));
		proc.setType(orderSb.toString().split(","));
		return proc;
	}
	
	/**
	 * 创建时间偏移process,时间偏移用来计算同比、环比、上期、同期等值
	 * @param sqlVO
	 * @param kpi
	 * @return
	 */
	private GridProcContext createShift(TableQueryDto sqlVO, KpiDto kpi, String compute){
		//查询最小时间维度
		int minDate = 4;
		DimDto minDim = null;
		for(DimDto dim : sqlVO.getDims()){
			String tp = dim.getType();
			if("frd".equalsIgnoreCase(tp)){
				continue;
			}
			int curDate = type2value(tp);
			if(curDate <= minDate){
				minDate = curDate;
				minDim = dim;
			}
		}
		com.ruisi.ext.engine.view.context.dc.grid.GridShiftContext proc = new com.ruisi.ext.engine.view.context.dc.grid.GridShiftContext();
		proc.setDateType(minDim.getType());
		proc.setDateFormat(minDim.getDateformat());
		proc.setDateColumn(minDim.getAlias());
		proc.setComputeType(compute);
		proc.setKpiColumn(new String[]{kpi.getAlias()});
		//设置过滤维度
		StringBuffer sb = new StringBuffer("");
		for(DimDto dim : sqlVO.getDims()){
			String tp = dim.getType();
			if("year".equals(tp) || "quarter".equals(tp) || "month".equals(tp) || "day".equals(tp)){
				continue;
			}
			sb.append(dim.getAlias());
			sb.append(",");
		}
		if(sb.length() > 0){
			String str = sb.substring(0, sb.length() - 1);
			proc.setKeyColumns(str.split(","));
		}
		return proc;
	}
	
	private GridProcContext createMoveAvg(TableQueryDto sqlVO, KpiDto kpi){
		com.ruisi.ext.engine.view.context.dc.grid.ComputeMoveAvgContext ctx = new com.ruisi.ext.engine.view.context.dc.grid.ComputeMoveAvgContext();
		ctx.setAlias(kpi.getAlias()+"_ydpj");
		ctx.setColumn(kpi.getAlias());
		ctx.setStep(3);
		return ctx;
	}
	
	/**
	 * 创建占比计算process
	 */
	private GridProcContext createAccount(TableQueryDto sqlVO, KpiDto kpi){
		com.ruisi.ext.engine.view.context.dc.grid.GridAccountContext proc = new com.ruisi.ext.engine.view.context.dc.grid.GridAccountContext();
		proc.setColumn(kpi.getAlias());
		//创建计算的分组维
		StringBuffer sb = new StringBuffer("");
		for(int i=0; i<sqlVO.getCols().size(); i++){
			DimDto dim = sqlVO.getCols().get(i);
			sb.append(dim.getColname());
			sb.append(",");
			
		}
		if(sb.length() > 0){
			String str = sb.substring(0, sb.length() - 1);
			proc.setGroupDim(str.split(","));
		}
		return proc;
	}
	
	private void loopJsonField(List<DimDto> arrays, List<com.ruisi.ext.engine.view.context.cross.CrossField> ls, List<KpiDto> kpis, String pos, boolean uselink) throws ParseException{
		List<com.ruisi.ext.engine.view.context.cross.CrossField> tmp = ls;
		for(int i=0; i<arrays.size(); i++){
			DimDto obj = arrays.get(i);
			String type = obj.getType();
			String issum = obj.getIssum();
			
			if(type.equals("kpiOther")){
				
				List<com.ruisi.ext.engine.view.context.cross.CrossField> newCf = new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>();
				if(tmp.size() == 0){
					for(KpiDto kpi : kpis){
						com.ruisi.ext.engine.view.context.cross.CrossField cf = new com.ruisi.ext.engine.view.context.cross.CrossField();
						cf.setType(type);
						cf.setAggregation(kpi.getAggre());
						cf.setAlias(kpi.getAlias());
						cf.setFormatPattern(kpi.getFmt());
						cf.setOrder("y".equals(kpi.getOrder()));
						cf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
						//用 id来表示指标ID，用在OLAP中,对指标进行操作
						cf.setId(String.valueOf(kpi.getKpi_id()));
						if(kpi.getRate() != null){
							cf.setKpiRate(new BigDecimal(kpi.getRate()));
						}
						String ru = this.writerUnit(kpi.getRate()) +kpi.getUnit();
						if(ru != null && ru.length() > 0){
							cf.setDesc(kpi.getKpi_name() + "("  + ru + ")");  //指标名称+ 单位
						}else{
							cf.setDesc(kpi.getKpi_name());  //指标名称
						}
						
						//当回调函数和指标预警同时起作用时， 指标预警起作用
						//处理回调函数
						cf.setJsFunc(kpi.getFuncname());
						String code = kpi.getCode();
						if(code != null && code.length() > 0){
							try {
								code = URLDecoder.decode(code, "UTF-8");
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							this.scripts.append("function "+cf.getJsFunc()+"(value,col,row,data){"+code+"}");
						}
						
						//处理指标预警
						Map<String, Object> warn = kpi.getWarning();
						if(warn != null && !warn.isEmpty()){
							String name = createWarning(warn, kpi.getFmt(), scripts);
							cf.setJsFunc(name);
						}
						tmp.add(cf);
						newCf.add(cf);
						
						//判断指标是否需要进行计算
						if(kpi.getCompute() != null && kpi.getCompute().length() > 0){
							String[] jss = kpi.getCompute().split(",");  //可能有多个计算，用逗号分隔
							for(String js : jss){
								com.ruisi.ext.engine.view.context.cross.CrossField compute = this.kpiCompute(js, kpi);
								tmp.add(compute);
								newCf.add(compute);
							}
						}
					}
				}else{
					for(com.ruisi.ext.engine.view.context.cross.CrossField tp : tmp){
						for(KpiDto kpi : kpis){
							com.ruisi.ext.engine.view.context.cross.CrossField cf = new com.ruisi.ext.engine.view.context.cross.CrossField();
							cf.setType(type);
							cf.setAggregation(kpi.getAggre());
							cf.setAlias(kpi.getAlias());
							cf.setFormatPattern(kpi.getFmt());
							cf.setOrder("y".equals(kpi.getOrder()));
							cf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
							//用 size来表示指标ID，用在OLAP中
							cf.setId(String.valueOf(kpi.getKpi_id()));
							if(kpi.getRate() != null){
								cf.setKpiRate(new BigDecimal(kpi.getRate()));
							}
							String ru = this.writerUnit(kpi.getRate()) +kpi.getUnit();
							if(ru != null && ru.length() > 0){
								cf.setDesc(kpi.getKpi_name() + "("  + ru + ")");  //指标名称+ 单位
							}else{
								cf.setDesc(kpi.getKpi_name());  //指标名称
							}
							//当回调函数和指标预警同时起作用时， 指标预警起作用
							//处理回调函数
							cf.setJsFunc(kpi.getFuncname());
							String code = kpi.getCode();
							if(code != null && code.length() > 0){
								try {
									code = URLDecoder.decode(code, "UTF-8");
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								this.scripts.append("function "+cf.getJsFunc()+"(value,col,row,data){"+code+"}");
							}
							//处理指标预警
							Map<String, Object> warn = kpi.getWarning();
							if(warn != null && !warn.isEmpty()){
								String name = createWarning(warn, kpi.getFmt(), scripts);
								cf.setJsFunc(name);
							}
							tp.getSubs().add(cf);
							newCf.add(cf);
							
							//判断指标是否需要进行计算
							if(kpi.getCompute() != null && kpi.getCompute().length() > 0){
								String[] jss = kpi.getCompute().split(",");  //可能有多个计算，用逗号分隔
								for(String js : jss){
									com.ruisi.ext.engine.view.context.cross.CrossField compute = this.kpiCompute(js, kpi);
									tp.getSubs().add(compute);
									newCf.add(compute);
								}
							}
						}
					}
				}
				tmp = newCf;
				
				
			}else if("day".equals(type)){
				List<com.ruisi.ext.engine.view.context.cross.CrossField> newCf = new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>();
				
				if(tmp.size() == 0){
					com.ruisi.ext.engine.view.context.cross.CrossField cf = new com.ruisi.ext.engine.view.context.cross.CrossField();
					/**
					cf.setType(type);
					if(sqlVO.getDayColumn() != null && sqlVO.getDayColumn().getStartDay() != null && sqlVO.getDayColumn().getStartDay().length() > 0){
						cf.setStart(sqlVO.getDayColumn().getEndDay());
						cf.setSize(sqlVO.getDayColumn().getBetweenDay() + 1);
					}else{
						int size = 365;
						cf.setStart("${s.defDay}");
						cf.setSize(size);
					}
					**/
					cf.setCasParent(true);
					
					Integer top = obj.getTop();
					cf.setTop(top);
					String topType = obj.getTopType();
					cf.setTopType(topType);
					cf.setId(String.valueOf(obj.getId()));
					cf.setType("frd");
					cf.setDateType("day");
					cf.setDateTypeFmt(obj.getDateformat());
					cf.setUselink(uselink);
					cf.setValue(obj.getVals());
					cf.setMulti(true);
					cf.setShowWeek(false);
					cf.setDesc(obj.getDimdesc());
					String alias = obj.getAlias();
					cf.setAlias(alias);
					//cf.setAggreDim("true".equalsIgnoreCase((String)obj.get("issum")));
					cf.setAliasDesc(alias);
					cf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
					tmp.add(cf);
					newCf.add(cf);
					
					//添加合计项
					if("y".equals(issum)){
						com.ruisi.ext.engine.view.context.cross.CrossField sumcf = new com.ruisi.ext.engine.view.context.cross.CrossField();
						sumcf.setType("none");
						String aggre = obj.getAggre();
						if(aggre != null && aggre.length() > 0 && !"auto".equals(aggre)){
							sumcf.setDimAggre(aggre);
						}
						sumcf.setDesc(loadFieldName(sumcf.getDimAggre()));
						sumcf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
						tmp.add(sumcf);
						newCf.add(sumcf);
					}
					
				}else{
					for(com.ruisi.ext.engine.view.context.cross.CrossField tp : tmp){
						//如果上级是合计，下级不包含维度了
						if(tp.getType().equals("none")){
							continue;
						}
						com.ruisi.ext.engine.view.context.cross.CrossField cf = new com.ruisi.ext.engine.view.context.cross.CrossField();
						/**
						cf.setType(type);
						if(sqlVO.getDayColumn() != null && sqlVO.getDayColumn().getStartDay() != null && sqlVO.getDayColumn().getStartDay().length() > 0){
							cf.setStart(sqlVO.getDayColumn().getEndDay());
							cf.setSize(sqlVO.getDayColumn().getBetweenDay() + 1);
						}else{
							int size = 365;
							cf.setStart("${s.defDay}");
							cf.setSize(size);
						}**/
						cf.setCasParent(true);
						cf.setTop(obj.getTop());
						cf.setTopType(obj.getTopType());
						cf.setId(String.valueOf(obj.getId()));
						cf.setType("frd");
						cf.setDateType("day");
						cf.setDateTypeFmt(obj.getDateformat());
						cf.setUselink(uselink);
						cf.setValue(obj.getVals());
						cf.setMulti(true);
						cf.setShowWeek(false);
						cf.setDesc(obj.getDimdesc());
						String alias = obj.getAlias();
						cf.setAlias(alias);
						cf.setAliasDesc(alias);
						cf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
						cf.setParent(tp);
						
						tp.getSubs().add(cf);
						newCf.add(cf);
						
						//添加合计项
						if("y".equals(issum)){
							com.ruisi.ext.engine.view.context.cross.CrossField sumcf = new com.ruisi.ext.engine.view.context.cross.CrossField();
							sumcf.setType("none");
							String aggre = obj.getAggre();
							if(aggre != null && aggre.length() > 0 && !"auto".equals(aggre)){
								sumcf.setDimAggre(aggre);
							}
							sumcf.setDesc(loadFieldName(sumcf.getDimAggre()));
							sumcf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
							tp.getSubs().add(sumcf);
							newCf.add(sumcf);
						}
					}
				}
				tmp = newCf;
				
			}else if("month".equals(type)){
				List<com.ruisi.ext.engine.view.context.cross.CrossField> newCf = new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>();
				if(tmp.size() == 0){
					com.ruisi.ext.engine.view.context.cross.CrossField cf = new com.ruisi.ext.engine.view.context.cross.CrossField();
					/**
					cf.setType(type);
					if(sqlVO.getMonthColumn()!= null && sqlVO.getMonthColumn().getStartMonth() != null && sqlVO.getMonthColumn().getStartMonth().length() > 0){
						cf.setStart(sqlVO.getMonthColumn().getEndMonth());
						cf.setSize(sqlVO.getMonthColumn().getBetweenMonth() + 1);
					}else{
						int size = 12;
						cf.setStart("${s.defMonth}");
						cf.setSize(size);
					}
					**/
					cf.setCasParent(true);
					cf.setTop(obj.getTop());
					cf.setTopType(obj.getTopType());
					cf.setId(String.valueOf(obj.getId()));
					cf.setType("frd");
					cf.setDateType("month");
					cf.setDateTypeFmt(obj.getDateformat());
					cf.setUselink(uselink);
					cf.setValue(obj.getVals());
					cf.setMulti(true);
					cf.setDesc(obj.getDimdesc());
					String alias = obj.getAlias();
					cf.setAlias(alias);
					cf.setAliasDesc(alias);
					cf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
					tmp.add(cf);
					newCf.add(cf);
					
					//添加合计项
					if("y".equals(issum)){
						com.ruisi.ext.engine.view.context.cross.CrossField sumcf = new com.ruisi.ext.engine.view.context.cross.CrossField();
						sumcf.setType("none");
						String aggre = obj.getAggre();
						if(aggre != null && aggre.length() > 0 && !"auto".equals(aggre)){
							sumcf.setDimAggre(aggre);
						}
						sumcf.setDesc(loadFieldName(sumcf.getDimAggre()));
						sumcf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
						tmp.add(sumcf);
						newCf.add(sumcf);
					}
					
				}else{
					for(com.ruisi.ext.engine.view.context.cross.CrossField tp : tmp){
						//如果上级是合计，下级不包含维度了
						if(tp.getType().equals("none")){
							continue;
						}
						com.ruisi.ext.engine.view.context.cross.CrossField cf = new com.ruisi.ext.engine.view.context.cross.CrossField();
						/**
						cf.setType(type);
						if(sqlVO.getMonthColumn()!= null && sqlVO.getMonthColumn().getStartMonth() != null && sqlVO.getMonthColumn().getStartMonth().length() > 0){
							cf.setStart(sqlVO.getMonthColumn().getEndMonth());
							cf.setSize(sqlVO.getMonthColumn().getBetweenMonth() + 1);
						}else{
							int size = 12;
							cf.setStart("${s.defMonth}");
							cf.setSize(size);
						}
						**/
						cf.setCasParent(true);
						cf.setTop(obj.getTop());
						cf.setTopType(obj.getTopType());
						cf.setId(String.valueOf(obj.getId()));
						cf.setType("frd");
						cf.setDateType("month");
						cf.setDateTypeFmt(obj.getDateformat());
						cf.setUselink(uselink);
						cf.setValue(obj.getVals());
						cf.setMulti(true);
						cf.setDesc(obj.getDimdesc());
						String alias = obj.getAlias();
						cf.setAlias(alias);
						cf.setAliasDesc(alias);
						cf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
						cf.setParent(tp);
						
						tp.getSubs().add(cf);
						newCf.add(cf);
						
						//添加合计项
						if("y".equals(issum)){
							com.ruisi.ext.engine.view.context.cross.CrossField sumcf = new com.ruisi.ext.engine.view.context.cross.CrossField();
							sumcf.setType("none");
							String aggre = obj.getAggre();
							if(aggre != null && aggre.length() > 0 && !"auto".equals(aggre)){
								sumcf.setDimAggre(aggre);
							}
							sumcf.setDesc(loadFieldName(sumcf.getDimAggre()));
							sumcf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
							tp.getSubs().add(sumcf);
							newCf.add(sumcf);
						}
					}
				}
				tmp = newCf;
			}else{
				List<com.ruisi.ext.engine.view.context.cross.CrossField> newCf = new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>();
				if(tmp.size() == 0){
					com.ruisi.ext.engine.view.context.cross.CrossField cf = new com.ruisi.ext.engine.view.context.cross.CrossField();
					cf.setType("frd"); //统一为frd
					cf.setId(String.valueOf(obj.getId()));
					cf.setDesc(obj.getDimdesc());
					String alias = obj.getAlias();
					String tableColKey = obj.getTableColKey();
					String tableColName = obj.getTableColName();
					if(tableColKey == null || tableColKey.length() == 0 || tableColName == null || tableColName.length() == 0){
						cf.setAlias(alias);
						cf.setAliasDesc(alias);
					}else{
						cf.setAlias(tableColKey);
						cf.setAliasDesc(tableColName);
					}
					cf.setCasParent(true);
					cf.setTop(obj.getTop());
					cf.setTopType(obj.getTopType());
					cf.setUselink(uselink);
					cf.setValue(obj.getVals());
					cf.setMulti(true);
					cf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
					tmp.add(cf);
					newCf.add(cf);
					
					//添加合计项
					if("y".equals(issum)){
						com.ruisi.ext.engine.view.context.cross.CrossField sumcf = new com.ruisi.ext.engine.view.context.cross.CrossField();
						sumcf.setType("none");
						String aggre = obj.getAggre();
						if(aggre != null && aggre.length() > 0 && !"auto".equals(aggre)){
							sumcf.setDimAggre(aggre);
						}
						sumcf.setDesc(loadFieldName(sumcf.getDimAggre()));
						sumcf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
						tmp.add(sumcf);
						newCf.add(sumcf);
						
						//如果是col,需要给合计添加指标
						/**
						if(pos.equals("col")){
							List<KpiInfo> kpis = sqlVO.getKpis();
							for(KpiInfo kpi : kpis){
								CrossField kpicf = new CrossField();
								kpicf.setType(type);
								kpicf.setDesc(kpi.getKpiName());
								kpicf.setAggregation(kpi.getAggre());
								kpicf.setAlias(kpi.getAlias());
								kpicf.setFormatPattern(kpi.getFmt());
								kpicf.setSubs(new ArrayList<CrossField>());
								//用 size来表示指标ID，用在OLAP中
								kpicf.setId(kpi.getId().toString());
								if(kpi.getRate() != null){
									kpicf.setKpiRate(new BigDecimal(kpi.getRate()));
								}
								sumcf.getSubs().add(kpicf);
								kpicf.setParent(sumcf);
							}
						}
						**/
					}
					
				}else{
					for(com.ruisi.ext.engine.view.context.cross.CrossField tp : tmp){
						//如果上级是合计，下级不包含维度了, 但需要包含指标
						if(tp.getType().equals("none")){
							
							//如果是col,需要给合计添加指标
							if(pos.equals("col")){
								for(KpiDto kpi : kpis){
									com.ruisi.ext.engine.view.context.cross.CrossField kpicf = new com.ruisi.ext.engine.view.context.cross.CrossField();
									kpicf.setType("kpiOther");
									kpicf.setDesc(kpi.getKpi_name());
									kpicf.setAggregation(kpi.getAggre());
									kpicf.setAlias(kpi.getAlias());
									kpicf.setFormatPattern(kpi.getFmt());
									kpicf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
									kpicf.setId(String.valueOf(kpi.getKpi_id()));
									if(kpi.getRate() != null){
										kpicf.setKpiRate(new BigDecimal(kpi.getRate()));
									}
									tp.getSubs().add(kpicf);
									kpicf.setParent(tp);
								}
							}
							
							continue;
						}
						com.ruisi.ext.engine.view.context.cross.CrossField cf = new com.ruisi.ext.engine.view.context.cross.CrossField();
						cf.setType("frd"); //统一为frd
						cf.setId(String.valueOf(obj.getId()));
						cf.setDesc(obj.getDimdesc());
						String alias = obj.getAlias();
						String tableColKey = obj.getTableColKey();
						String tableColName = obj.getTableColName();
						if(tableColKey == null || tableColKey.length() == 0 || tableColName == null || tableColName.length() == 0){
							cf.setAlias(alias);
							cf.setAliasDesc(alias);
						}else{
							cf.setAlias(tableColKey);
							cf.setAliasDesc(tableColName);
						}
						cf.setCasParent(true);
						
						cf.setTop(obj.getTop());
						cf.setTopType(obj.getTopType());
						cf.setUselink(uselink);
						cf.setValue(obj.getVals());
						cf.setMulti(true);
						cf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
						cf.setParent(tp);
						tp.getSubs().add(cf);
						newCf.add(cf);
						
						//添加合计项
						if("y".equals(issum)){
							com.ruisi.ext.engine.view.context.cross.CrossField sumcf = new com.ruisi.ext.engine.view.context.cross.CrossField();
							sumcf.setType("none");
							String aggre = obj.getAggre();
							if(aggre != null && aggre.length() > 0 && !"auto".equals(aggre)){
								sumcf.setDimAggre(aggre);
							}
							sumcf.setDesc(loadFieldName(sumcf.getDimAggre()));
							sumcf.setSubs(new ArrayList<com.ruisi.ext.engine.view.context.cross.CrossField>());
							tp.getSubs().add(sumcf);
							newCf.add(sumcf);
						}
					}
				}
				tmp = newCf;
			}
			
		}
	}	
	
	public List<com.ruisi.ext.engine.view.context.cross.RowDimContext> getDrillDim(TableQueryDto table){
		List<Map<String, Object>> drillDim = table.getDrillDim();
		if(drillDim == null || drillDim.isEmpty() || table.getCols().size() != 1){  //只有一个row维度才能启用钻取
			return null;
		}
		List<com.ruisi.ext.engine.view.context.cross.RowDimContext> ret = new ArrayList<com.ruisi.ext.engine.view.context.cross.RowDimContext>();
		for(int i=0; i<drillDim.size(); i++){
			Map<String, Object> obj = (Map<String, Object>)drillDim.get(i);
			com.ruisi.ext.engine.view.context.cross.RowDimContext dim = new com.ruisi.ext.engine.view.context.cross.RowDimContext();
			dim.setCode((String)obj.get("code"));
			dim.setName((String)obj.get("name"));
			dim.setCodeDesc(dim.getCode());
			dim.setType("frd");
			ret.add(dim);
		}
		return ret;
	}
	
	public String createWarning(Map<String, Object> warn, String kpiFmt, StringBuffer scripts ){
		String funcName = "warn"+ IdCreater.create();
		scripts.append("function " +funcName+"(val, a, b, c, d){");
		//先输出值
		scripts.append("if(d == 'html'){out.print('<span class=\"kpiValue\">');}");
		scripts.append("if(val == null){out.print('-')}else{out.print(val, '"+kpiFmt+"');}");
		scripts.append("if(d != 'html'){"); //只在html模式下起作用
		scripts.append(" return;");
		scripts.append("}");
		scripts.append("if(val "+warn.get("logic1")+" "+warn.get("val1")+"){");
		scripts.append("out.print(\"<span class='"+warn.get("pic1")+"'></span>\")");
		scripts.append("}else if(val "+(warn.get("logic1").equals(">=")?"<":"<=")+" "+warn.get("val1")+" && val "+warn.get("logic2")+" "+warn.get("val2")+"){");
		scripts.append("out.print(\"<span class='"+warn.get("pic2")+"'></span>\")");
		scripts.append("}else{");
		scripts.append("out.print(\"<span class='"+warn.get("pic3")+"'></span>\")");
		scripts.append("}");
		scripts.append("if(d == 'html'){out.print('</span>');}");
		scripts.append("}");
		return funcName; 
	}
	
	private com.ruisi.ext.engine.view.context.cross.CrossField kpiCompute(String compute, KpiDto kpi){
		com.ruisi.ext.engine.view.context.cross.CrossField cf = new com.ruisi.ext.engine.view.context.cross.CrossField();
		if("zb".equals(compute)){
			cf.setDesc("占比");
			cf.setAggregation("avg");
			cf.setAlias(kpi.getAlias() + "_zb");
			cf.setFormatPattern("0.00%");
		}else if("sxpm".equals(compute) || "jxpm".equals(compute)){
			cf.setDesc(("sxpm".equals(compute) ? "升序":"降序") + "排名");
			cf.setAggregation("avg");
			cf.setAlias(kpi.getAlias() + "_order");
			cf.setFormatPattern("#,###");
			cf.setStyleClass("pms");
			cf.setStyleToLine(true);
		}else if("ydpj".equals(compute)){
			cf.setDesc("移动平均");
			cf.setAggregation(kpi.getAggre());
			cf.setAlias(kpi.getAlias() + "_ydpj");
			cf.setFormatPattern(kpi.getFmt());
			if(kpi.getRate() != null){
				cf.setKpiRate(new BigDecimal(kpi.getRate()));
			}
		}else if("sq".equals(compute)){
			cf.setDesc("上期值");
			cf.setAggregation(kpi.getAggre());
			cf.setAlias(kpi.getAlias()+"_sq");
			cf.setFormatPattern(kpi.getFmt());
			if(kpi.getRate() != null){
				cf.setKpiRate(new BigDecimal(kpi.getRate()));
			}
		}else if("tq".equals(compute)){
			cf.setDesc("同期值");
			cf.setAggregation(kpi.getAggre());
			cf.setAlias(kpi.getAlias()+"_tq");
			cf.setFormatPattern(kpi.getFmt());
			if(kpi.getRate() != null){
				cf.setKpiRate(new BigDecimal(kpi.getRate()));
			}
		}else if("zje".equals(compute)){
			cf.setDesc("增减额");
			cf.setAggregation(kpi.getAggre());
			cf.setAlias(kpi.getAlias() + "_zje");
			cf.setFormatPattern(kpi.getFmt());
			if(kpi.getRate() != null){
				cf.setKpiRate(new BigDecimal(kpi.getRate()));
			}
			cf.setFinanceFmt(true);
		}else if("hb".equals(compute)){
			cf.setDesc("环比");
			cf.setAggregation("avg");
			cf.setAlias(kpi.getAlias() + "_hb");
			cf.setFormatPattern("0.00%");
			cf.setFinanceFmt(true);
		}else if("tb".equals(compute)){
			cf.setDesc("同比");
			cf.setAggregation("avg");
			cf.setAlias(kpi.getAlias()+"_tb");
			cf.setFormatPattern("0.00%");
			cf.setFinanceFmt(true);
		}
		cf.setType("kpiOther");
		cf.setId("ext_" + kpi.getKpi_id()+"_"+compute); //表示当前指标是由基本指标衍生而来，比如昨日、累计、同比、环比、排名、占比等内容。
		return cf;
	}

	public Map<String, InputField> getMvParams() {
		return mvParams;
	}

	public StringBuffer getScripts() {
		return scripts;
	}


	public String creattable(String sql, TableQueryDto tableJson) throws SQLException {

		StringBuilder res=new StringBuilder();
		StringBuilder sql1=new StringBuilder();//维度表头
		StringBuilder sql4=new StringBuilder();//指标表头
		StringBuilder sql2=new StringBuilder();//维度
		StringBuilder sql3=new StringBuilder();//指标



//<div class="mv_main mv_main2" id="mv.tmp.table">
// <div class="crossReport">
//	<table class="d_table2">
//	<tbody>
//		<tr>
//	       <td class="blank" valign="bottom">
//			<div class="rowDimsList">
//				<table class="grid5" cellpadding="0" cellspacing="0">
//			        <tbody>
//				       <tr>
//
//	                   </tr>
//	                </tbody>
//				</table>
//			</div>
//			</td>
//
//		   <td>
//	      <div id="d_colDims" class="droppable" style="width: 155px;">
//	          <div class="colDimsList">
//	            <div style="margin:3px;color:#999999;font-size:13px;">列标签区域</div>
//	          </div>
//              <table class="grid5" cellpadding="0" cellspacing="0">
//                <tbody>
//                <tr class="scrollColThead">
//                    <th class="null" colspan="1">
//	                   <span class="colkpi">
//	                   <span class="kpiname" title="合计">合计
//	                   </span>
//	                   </span>
//	                  </th>
//	               </tr>

//	或者	<th class="null" colspan="1"><span class="colkpi"><span class="kpiname" title="sort">sort</span><a class="dimoptbtn set" href="javascript:;" onclick="setKpiInfo(this,'259');" style="opacity: 0.6;"> &nbsp; </a></span></th>



//	             </tbody>
//	           </table>
//           </div>
//	     </td>
//		</tr>

//        <tr>
//		  <td valign="top">
//            <div id="d_rowDims" class="droppable" style="height: 587px; border: none;">
//			<table class="grid5" cellpadding="0" cellspacing="0" style="margin-top: 0px;">
//				<tbody>


//				</tbody>
//		    </table>
//			</div>
//          </td>
//
//
//       <td valign="top">
//		   <div id="d_kpi" class="droppable" style="width: 221px; height: 577px;">
//            <table class="grid5" cellpadding="0" cellspacing="0">
//                  <tbody>
//		            <tr><td align="right" class="kpiData1 grid5-td"><span class="kpiValue"><div style="height:70px;margin-top:10px;"><a href="javascript:linkdetail({&quot;logo&quot;:&quot;http://39.98.239.6:8080/file/20190601/img_486529c5564c46a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg&quot;,&quot;name&quot;:&quot;元气森林&quot;});">12.0</a></div></span></td><td align="right" class="kpiData1 grid5-td"><span class="kpiValue"><div style="height:70px;margin-top:10px;"><a href="javascript:linkdetail({&quot;logo&quot;:&quot;http://39.98.239.6:8080/file/20190601/img_486529c5564c46a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg&quot;,&quot;name&quot;:&quot;元气森林&quot;});">-</a></div></span></td></tr>
//		            </tbody>
//		       </table>
//          </div>
//	     </td>








//	</tbody>
//    </table>
// </div>
//</div>




		/**
		 * 执行sql语句
		 */

		DriverManager.registerDriver(new Driver());

		Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/bi-demo?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai", "root", "123456");

		Statement stat = conn.createStatement();

		ResultSet rs =  stat.executeQuery(sql);
		ResultSetMetaData rsmd = rs.getMetaData();

		int lie=rsmd.getColumnCount();
		/**
		 * 获取表头（维度）
		 */
		for(int i=0;i<tableJson.getDims().size();i++) {
			String name=rsmd.getColumnName(i+1);
			sql1.append("<th><span>"+name+"<a href=\"javascript:;\" onclick=\"setRdimInfo(this, &quot;"+tableJson.getDims().get(i).getId()+"&quot;, &quot;name&quot;)\" class=\"dimoptbtn set\" style=\"opacity: 0.6;\"> &nbsp; </a></span></th>");

		}
		/**
		 * 获取表头（指标）
		 */
		if(tableJson.getKpiJson().size()==0){
			sql4.append("<th class=\"null\" colspan=\"1\">\n" +
					"<span class=\"colkpi\">\n" +
					"<span class=\"kpiname\" title=\"\">暂无度量!\n" +
					"</span>\n" +
					"</span>\n" +
					"</th>");
		}else {
			for(int i=0;i<tableJson.getKpiJson().size();i++) {
				String name=rsmd.getColumnName(i+tableJson.getDims().size()+1);
				sql4.append("<th class=\"null\" colspan=\"1\"><span class=\"colkpi\"><span class=\"kpiname\" title=\"" +name+"\">"+name+"</span><a class=\"dimoptbtn set\" href=\"javascript:;\" onclick=\"setKpiInfo(this,'"+tableJson.getKpiJson().get(i).getKpi_id()+"');\" style=\"opacity: 0.6;\"> &nbsp; </a></span></th>");
//	或者	<th class="null" colspan="1"><span class="colkpi"><span class="kpiname" title="sort">sort</span><a class="dimoptbtn set" href="javascript:;" onclick="setKpiInfo(this,'259');" style="opacity: 0.6;"> &nbsp; </a></span></th>


			}
		}


		int hangshu=0;
		/**
		 * 获取dim 和 kpi
		 */
		while(rs.next()) {
			hangshu++;
			sql2.append("<tr class=\"tr-row1\">");
			for (int i = 0; i <tableJson.getDims().size(); i++) {
				String dim = rs.getString(i+1);
				sql2.append("<th align=\"left\" valign=\"top\" rowspan=\"1\" class=\"grid5-td\"><span class=\"s_rowDim\" title=\""+dim+"\"><a class=\"dimgoup\" onclick=\"goupDim(1, this, 'row','26', true)\" style=\"opacity:0.5\">  </a>"+dim+"</span></th>");

			}
			sql2.append("</tr>");

			sql3.append("<tr>");
			for (int j = 0; j <tableJson.getKpiJson().size(); j++) {
				String kpi = rs.getString(j+tableJson.getDims().size()+1);
				System.err.println(kpi);
				sql3.append("<td align=\"right\" class=\"kpiData1 grid5-td\"><span class=\"kpiValue\">"+kpi+"</a></span></td>");

			}
			sql3.append("</tr>");
		}
//		            <tr><td align="right" class="kpiData1 grid5-td"><span class="kpiValue"><div style="height:70px;margin-top:10px;"><a href="javascript:linkdetail({&quot;logo&quot;:&quot;http://39.98.239.6:8080/file/20190601/img_486529c5564c46a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg&quot;,&quot;name&quot;:&quot;元气森林&quot;});">12.0</a></div></span></td>
//		                 <td align="right" class="kpiData1 grid5-td"><span class="kpiValue"><div style="height:70px;margin-top:10px;"><a href="javascript:linkdetail({&quot;logo&quot;:&quot;http://39.98.239.6:8080/file/20190601/img_486529c5564c46a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg&quot;,&quot;name&quot;:&quot;元气森林&quot;});">-</a></div></span></td></tr>

//<tr class="tr-row1"><th align="left" valign="top" rowspan="1" class="grid5-td"><span class="s_rowDim" title="元气森林"><a class="dimgoup" onclick="goupDim(1, this, 'row','26', true)" style="opacity:0.5">  </a>元气森林</span></th>
//	                <th align="left" valign="top" rowspan="1" class="grid5-td"><span class="s_rowDim" style="height:80px;" title="http://39.98.239.6:8080/file/20190601/img_486529c5564c46a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg"><a class="dimDrill" onclick="drillDim(1, this, 'row', 'http://39.98.239.6:8080/file/20190601/img_486529c5564c46a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg','http://39.98.239.6:8080/file/20190601/img_486529c5564c46a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg', '27')" style="opacity:0.5">  </a>http://39.98.2<br>39.6:8080/file<br>/20190601/img_<br>486529c5564c46<br>a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg</span></th></tr>
		/**
		 * 维度的表头
		 */
		res.append("<div class=\"mv_main mv_main2\" id=\"mv.tmp.table\">\n" +
				" <div class=\"crossReport\">\n" +
				"\t<table class=\"d_table2\">\n" +
				"\t<tbody>\n" +
				"\t\t<tr>\n" +
				"\t       <td class=\"blank\" valign=\"bottom\">\n" +
				"\t\t\t<div class=\"rowDimsList\">\n" +
				"\t\t\t\t<table class=\"grid5\" cellpadding=\"0\" cellspacing=\"0\">\n" +
				"\t\t\t        <tbody>\n" +
				"\t\t\t\t       <tr>");
		res.append(sql1);

		/**
		* 指标的表头
		*/
		res.append("\t                   </tr>\n" +
				"\t                </tbody>\n" +
				"\t\t\t\t</table>\n" +
				"\t\t\t</div>\n" +
				"\t\t\t</td>\n" +
				"\n" +
				"\t\t <td>\n" +
				"\t      <div id=\"d_colDims\" class=\"droppable\" style=\"width: 155px;\">\n" +
				"\t          <div class=\"colDimsList\">\n" +
				"\t            <div style=\"margin:3px;color:#999999;font-size:13px;\">列标签区域</div>\n" +
				"\t          </div>\n" +
				"              <table class=\"grid5\" cellpadding=\"0\" cellspacing=\"0\">\n" +
				"                <tbody><tr class=\"scrollColThead\">");

        res.append(sql4);

		res.append("\t             </tbody>\n" +
				"\t           </table>\n" +
				"           </div>\n" +
				"\t     </td>\n" +
				"\t\t</tr>");


		/**
		 * Dim
		 */
		res.append("        <tr>\n" +
				"\t\t  <td valign=\"top\">\n" +
				"            <div id=\"d_rowDims\" class=\"droppable\" style=\"height: 587px; border: none;\">\n" +
				"\t\t\t<table class=\"grid5\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top: 0px;\">\n" +
				"\t\t\t\t<tbody>");
		res.append(sql2);
		res.append("\t\t\t\t</tbody>\n" +
				"\t\t    </table>\n" +
				"\t\t\t</div>\n" +
				"          </td>");


		/**
		 * kpi
		 */
        res.append("<td valign=\"top\">\n" +
				"\t\t   <div id=\"d_kpi\" class=\"droppable\" style=\"width: 221px; height: 577px;\">\n" +
				"            <table class=\"grid5\" cellpadding=\"0\" cellspacing=\"0\">\n" +
				"                  <tbody>");
        res.append(sql3);

        res.append("\t\t            </tbody>\n" +
				"\t\t       </table>\n" +
				"          </div>\n" +
				"\t     </td>");





		res.append("\t</tbody>\n" +
				"    </table>\n" +
				" </div>\n" +
				"</div>");


		while(rs.next()) {








//			hangshu++;
//			sql1.append("<tr class=\"tr-row1" );
//			sql2.append("<tr>");
//			for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
//
//    			String mingzi = rs.getString(i+1);
//				sql1.append("<th align=\"left\" valign=\"top\" rowspan=\"" + hangshu + "\" class=\"grid5-td\" ><span class=\"s_rowDim\" title=\"" + mingzi +
//						"\">\n" + "\t\t<a class=\"dimDrill\" onclick=\"drillDim(1, this, 'row', '" + mingzi + "','" + mingzi + "', '26')\" style=\"opacity:0.5\"></a>" + mingzi + "</span></th>");
//
//
//
//				sql2.append("<td align='right' class='kpiData" + hangshu + "grid5-td'><span class=\"kpiValue\"><a href='javascript:linkdetail({\"name\":\"" + mingzi + "\"});'>-</a></span></td>");
//			}
//			sql1.append("</tr>");
//			sql2.append("</tr>");


		}



		System.out.println(tableJson.getCols().size());
		System.out.println(tableJson.getRows().size());
		System.out.println(tableJson.getDims().size());
		System.out.println(tableJson.getKpiJson().size());












		return res.toString();
	}


}
