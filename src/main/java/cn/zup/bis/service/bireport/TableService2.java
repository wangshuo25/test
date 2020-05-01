package cn.zup.bis.service.bireport;

import cn.zup.bis.dao.model.DataSourceDao;
import cn.zup.bis.dao.model.DatasetDao;
import cn.zup.bis.entity.bireport.DimDto;
import cn.zup.bis.entity.bireport.KpiDto;
import cn.zup.bis.entity.bireport.ParamDto;
import cn.zup.bis.entity.bireport.TableQueryDto;
import cn.zup.bis.entity.model.DataSource;
import com.alibaba.fastjson.JSON;
import com.mysql.jdbc.Driver;

import cn.zup.bis.util.RSBIUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

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

/**
 * @Author:Sure
 * @Date:2020/4/29 9:15
 */
@Service

public class TableService2  extends BaseCompService{

    public final static String deftMvId = "mv.tmp.table";
    private static final String KYLIN_DRIVER = "org.apache.kylin.jdbc.Driver";

    @Autowired
    private DatasetDao dsetMapper;

    @Autowired
    private DataSourceDao mapper;


    public String createSql(TableQueryDto table, int release) {

        JSONObject dset = JSON.parseObject(dsetMapper.getDatasetCfg(table.getDsetId()));
        Map<String, String> tableAlias = createTableAlias(dset);//code_area -> a1   test_dm_logs -> a0
        StringBuffer sql = new StringBuffer();
        sql.append("select ");
        List<DimDto> dims = table.getDims();
        for(int i=0; i<dims.size(); i++){
            DimDto dim = dims.get(i);
           sql.append( tableAlias.get(dim.getTname())+"."+dim.getColname()+" "+dim.getAlias()+", ");
        }

        List<KpiDto> kpis = table.getKpiJson();
        if(kpis.size() == 0){
            sql.append(" 0 kpi_value ");
        }else{
            for(int i=0; i<kpis.size(); i++){
                KpiDto kpi = kpis.get(i);

                String name = super.convertKpiName(kpi, tableAlias);
                sql.append( name + " ");

                sql.append(kpi.getAlias());
                if(i != kpis.size() - 1){
                    sql.append(",");
                }
            }
        }
        JSONArray joinTabs = (JSONArray)dset.get("joininfo");
        String master = dset.getString("master");
        sql.append(" from " + master + " a0");//主表都是a0

        for(int i=0; joinTabs!=null&&i<joinTabs.size(); i++){  //通过主表关联
            JSONObject tab = joinTabs.getJSONObject(i);
            String ref = tab.getString("ref");//从表
            String refKey = tab.getString("refKey");//从表关联字段
            String jtype = (String)tab.get("jtype");//连接方式
            if("left".equals(jtype) || "right".equals(jtype)){
                sql.append(" " + jtype);
            }
            sql.append(" join " + ref+ " " + tableAlias.get(ref));//join code_area a1
            sql.append(" on a0."+tab.getString("col")+"="+tableAlias.get(ref)+"."+refKey);// on a0.did=a1.id
            sql.append(" ");
        }
        sql.append(" where 1=1 ");

// and a1.COUNTY in ('丰台区')
        for(int i=0; i<dims.size(); i++){
            DimDto dim = dims.get(i);
                //限制维度筛选
                if(dim.getVals() != null && dim.getVals().length() > 0){
                    String vls = null;
                        vls = dim.getVals();
                    //字符串     丰台区->'丰台区'
                    if("string".equalsIgnoreCase(dim.getValType())){
                        vls = RSBIUtils.dealStringParam(vls);
                    }
                    sql.append(" and " + (tableAlias.get(dim.getTname()) + "." + dim.getColname()) + " in ("+vls+")");
                }
        }





        if(dims.size() > 0){
            sql.append(" group by ");
            for(int i=0; i<dims.size(); i++){
                DimDto dim = dims.get(i);
                    sql.append(tableAlias.get(dim.getTname()) + "."+dim.getColname());


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
                    order.append(dim.getColname());
                    order.append(" ");
                    order.append(dim.getDimord());
                    order.append(",");
                }
            }
            //再按row排序
            for(int i=0; i<table.getRows().size(); i++){
                DimDto dim = table.getRows().get(i);
                if(dim.getDimord() != null && dim.getDimord().length() > 0){
                    order.append(dim.getColname());
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




            if(order.length() <= 11 ){  //判断是否拼接了 order by 字段
                return sql.toString();
            }else{
                //返回前先去除最后的逗号
                return (sql + order.toString().substring(0, order.length() - 1));
            }
        }else{
            return sql.toString();
        }
    }






    public String creattable(String sql, TableQueryDto tableJson) throws SQLException, ClassNotFoundException {

        StringBuilder res=new StringBuilder();
        StringBuilder sql1=new StringBuilder();//维度行表头 row
        StringBuilder sql11=new StringBuilder();//维度列表头col

        StringBuilder sql4=new StringBuilder();//指标表头
        StringBuilder sql2=new StringBuilder();//row维度
        StringBuilder sql22=new StringBuilder();//col维度
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
//	         <div id="d_colDims" class="droppable" style="width: 155px;">
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
        Class.forName(KYLIN_DRIVER);
        String url=tableJson.getDsid();
        DataSource ds=mapper.getDataSource(url);
        Connection conn = DriverManager.getConnection(ds.getLinkUrl(),ds.getLinkName(), ds.getLinkPwd());

        Statement stat = conn.createStatement();

        ResultSet rs =  stat.executeQuery(sql);
        ResultSetMetaData rsmd = rs.getMetaData();
        int lie=rsmd.getColumnCount();
        /**
         * 获取列表头（维度）
         */
        for(int i=0;i<tableJson.getRows().size();i++) {

            String name=tableJson.getRows().get(i).getDimdesc();
            System.out.println(name);
            sql1.append("<th><span>"+name+"<a href=\"javascript:;\" onclick=\"setRdimInfo(this, &quot;"+tableJson.getRows().get(i).getId()+"&quot;, &quot;name&quot;)\" class=\"fa fa-cog\" style=\"padding-left:5px;\">操作 </a></span></th>");

        }
/**
 * 获取行表头（维度）
 */
        for(int i=0;i<tableJson.getCols().size();i++) {

                String name=tableJson.getCols().get(i).getDimdesc();
                System.err.println(name);
                sql11.append("<span>"+name+"<a href=\"javascript:;\" onclick=\"setCdimInfo(this, &quot;"+tableJson.getCols().get(i).getId()+"&quot;, &quot;name&quot;)\" class=\"fa fa-cog\" style=\"padding-left:5px;\">操作 </a></span></th>");
        }


//<tr class="scrollColThead"><th class="null" colspan="1"><div class="coldim"><a class="dimDrill" onclick="drillDim(1, this, 'col', '元气森林', '元气森林', '16')" style="opacity:0.5">  </a><span class="s_colDim" title="元气森林">元气森林</span></div></th></tr>


        List<String> r = new ArrayList<>();//保存dim行
        int hangshu=0;
        /**
         * 获取dim
         */
        if (tableJson.getRows().size()==0){//若是没有列维度，就要显示拖动区域
            sql2.append("<span class=\"s_rowDim\" title=\"列维拖动区域\n" +
                    "列维拖动区域\">列维拖动区域</span>");

        }
        if (tableJson.getCols().size()!=0||tableJson.getRows().size()!=0){

            while(rs.next()) {
                hangshu++;
                //dim列
                sql2.append("<tr>");
                for (int i = 0; i <tableJson.getRows().size(); i++) {
                    String dim = rs.getString(tableJson.getRows().get(i).getColname());
                    if(i==tableJson.getRows().size()-1){
                        sql2.append("<th align=\"left\" valign=\"top\" rowspan=\"1\" ><span class=\"s_rowDim\" title=\""+dim+"\"><a class=\"fa fa-plus-square\" onclick=\"drillDim(1, this, 'row', '"+dim+"','"+dim+"', '"+tableJson.getRows().get(i).getId()+"')\" style=\"opacity:0.5\">  </a>"+dim+"</span></th>\n");

                    }else {
                        sql2.append("<th align=\"left\" valign=\"top\" rowspan=\"1\"><span class=\"s_rowDim\" title=\""+dim+"\"><a class=\"fa fa-minus-square\" onclick=\"goupDim(1, this, 'row','"+tableJson.getRows().get(i).getId()+"', true)\" style=\"opacity:0.5\">  </a>"+dim+"</span></th>");

                    }



                }
                sql2.append("</tr>");

                //dim行先存到r中

                for (int i = 0; i <tableJson.getCols().size(); i++) {
                    String dim1 = rs.getString(tableJson.getCols().get(i).getColname());
                    r.add(dim1);
                    //sql22.append("<th class=\"null\" colspan=\"1\"><div class=\"coldim\"><a class=\"dimDrill\" onclick=\"drillDim(1, this, 'col', '"+dim1+"', '"+dim1+", '16')\" style=\"opacity:0.5\">  </a><span class=\"s_colDim\" title="+dim1+">"+dim1+"</span></div></th>");
                }



            }
            //dim行  hangshu*tableJson.getCols().size()表示r中一共多少元素，i=i+tableJson.getCols().size()表示取出同一个dim的
            for(int i=0;i<tableJson.getCols().size();i++){
                sql22.append("<tr>");
                for(int j=i;j<hangshu*tableJson.getCols().size();j=j+tableJson.getCols().size()){
                    String dim1=r.get(j);
                    int a=tableJson.getKpiJson().size();//动态拓展长度
                    sql22.append("<th class=\"null\" colspan=\""+a+"\"><div style=\"width:110px;\"><span class=\"s_colDim\" title="+dim1+">"+dim1+"</span></div></th>");
                }
                sql22.append("</tr>");
            }


        }
        rs =  stat.executeQuery(sql);
        //rs.beforeFirst();//回到首行
        int a=0;
        while (rs.next()){
            /**
             * 获取kpi
             */
            a++;
            sql3.append("<tr>");
            if(tableJson.getKpiJson().size()==0&&tableJson.getCols().size()!=0){//没有指标，但是有行维和列为
                for (int i=0;i<hangshu;i++)
                    sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">-</span></td>");
            }else if (tableJson.getKpiJson().size()!=0&&tableJson.getCols().size()==0){//只有列维，且有指标
                for (int j = 0; j <tableJson.getKpiJson().size(); j++) {
                    String kpi = rs.getString(j+tableJson.getDims().size()+1);

                    if(tableJson.getKpiJson().get(j).getWarning()!=null){//有预警信息
                        Map map=tableJson.getKpiJson().get(j).getWarning();
                        String pictype=(String)map.get("pictype");
                        String logic1= (String) map.get("logic1");
                        String val1= (String) map.get("val1");

                        String logic2= (String) map.get("logic2");
                        String val2= (String) map.get("val2");

                        String pic1= (String) map.get("pic1");
                        String pic2= (String) map.get("pic2");
                        String pic3= (String) map.get("pic3");

                        if(logic1.equals(">=")){
                            if( Float.parseFloat(kpi)>= Float.parseFloat(val1)){
                                sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic1+"></span>"+"</a></span></td>");
                            }else if (logic2.equals(">=")){
                                if( Float.parseFloat(kpi)< Float.parseFloat(val1)&&Float.parseFloat(kpi)>= Float.parseFloat(val2)){
                                    sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic2+"></span>"+"</a></span></td>");
                                }else {
                                    sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic3+"></span>"+"</a></span></td>");
                                }
                            }else if (logic2.equals(">")){
                                if( Float.parseFloat(kpi)< Float.parseFloat(val1)&&Float.parseFloat(kpi)> Float.parseFloat(val2)){
                                    sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic2+"></span>"+"</a></span></td>");
                                }else {
                                    sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic3+"></span>"+"</a></span></td>");
                                }
                            }

                        } else if(logic1.equals(">")){
                            if( Float.parseFloat(kpi)> Float.parseFloat(val1)){
                                sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic1+"></span>"+"</a></span></td>");
                            }else if (logic2.equals(">=")){
                                if( Float.parseFloat(kpi)<= Float.parseFloat(val1)&&Float.parseFloat(kpi)>= Float.parseFloat(val2)){
                                    sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic2+"></span>"+"</a></span></td>");
                                }else {
                                    sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic3+"></span>"+"</a></span></td>");
                                }

                            }else if (logic2.equals(">")){
                                if( Float.parseFloat(kpi)<= Float.parseFloat(val1)&&Float.parseFloat(kpi)> Float.parseFloat(val2)){
                                    sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic2+"></span>"+"</a></span></td>");
                                }else {
                                    sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic3+"></span>"+"</a></span></td>");
                                }
                            }
                        }


                    }else {
                        sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"</a></span></td>");
                    }

                }
            } else {//行维列维指标都有的报表
                for(int i=1;i<=hangshu*tableJson.getKpiJson().size();i++){
                    if(i==(a-1)*tableJson.getKpiJson().size()+1){
                        for (int j = 0; j <tableJson.getKpiJson().size(); j++) {
                            if(tableJson.getKpiJson().get(j).getWarning()!=null){//有预警信息

                                String kpi = rs.getString(j+tableJson.getDims().size()+1);
                                Map map=tableJson.getKpiJson().get(j).getWarning();
                                String pictype=(String)map.get("pictype");
                                String logic1= (String) map.get("logic1");
                                String val1= (String) map.get("val1");

                                String logic2= (String) map.get("logic2");
                                String val2= (String) map.get("val2");

                                String pic1= (String) map.get("pic1");
                                String pic2= (String) map.get("pic2");
                                String pic3= (String) map.get("pic3");

                                    if(logic1.equals(">=")){
                                        if( Float.parseFloat(kpi)>= Float.parseFloat(val1)){
                                            sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic1+"></span>"+"</a></span></td>");
                                        }else if (logic2.equals(">=")){
                                            if( Float.parseFloat(kpi)< Float.parseFloat(val1)&&Float.parseFloat(kpi)>= Float.parseFloat(val2)){
                                                sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic2+"></span>"+"</a></span></td>");
                                            }else {
                                                sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic3+"></span>"+"</a></span></td>");
                                            }
                                        }else if (logic2.equals(">")){
                                            if( Float.parseFloat(kpi)< Float.parseFloat(val1)&&Float.parseFloat(kpi)> Float.parseFloat(val2)){
                                                sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic2+"></span>"+"</a></span></td>");
                                            }else {
                                                sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic3+"></span>"+"</a></span></td>");
                                            }
                                        }

                                    } else if(logic1.equals(">")){
                                        if( Float.parseFloat(kpi)> Float.parseFloat(val1)){
                                            sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic1+"></span>"+"</a></span></td>");
                                        }else if (logic2.equals(">=")){
                                            if( Float.parseFloat(kpi)<= Float.parseFloat(val1)&&Float.parseFloat(kpi)>= Float.parseFloat(val2)){
                                                sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic2+"></span>"+"</a></span></td>");
                                            }else {
                                                sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic3+"></span>"+"</a></span></td>");
                                            }

                                        }else if (logic2.equals(">")){
                                            if( Float.parseFloat(kpi)<= Float.parseFloat(val1)&&Float.parseFloat(kpi)> Float.parseFloat(val2)){
                                                sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic2+"></span>"+"</a></span></td>");
                                            }else {
                                                sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"<span class="+pic3+"></span>"+"</a></span></td>");
                                            }
                                        }
                                    }


                            }else {
                                String kpi = rs.getString(j+tableJson.getDims().size()+1);

                                sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">"+kpi+"</a></span></td>");
                            }

                        }
                        i=(a-1)*tableJson.getKpiJson().size()+1;
                    }else {
                        sql3.append("<td align=\"right\" class=\"grid5-td\"><span class=\"kpiValue\">-</span></td>");//没有值，用-补上
                    }

                }

//				for (int j = 0; j <tableJson.getKpiJson().size(); j++) {
//					String kpi = rs.getString(j+tableJson.getDims().size()+1);
//					for(int i=1;i<=hangshu;i++){
//						if (i<=a*tableJson.getKpiJson().size()&&i>=(a-1)*tableJson.getKpiJson().size()+1){
//							sql3.append("<td align=\"right\" class=\"kpiData1 grid5-td\"><span class=\"kpiValue\">"+kpi+"</a></span></td>");
//
//						}else {
//							sql3.append("<td align=\"right\" class=\"kpiData1 grid5-td\"><span class=\"kpiValue\">-</span></td>");
//						}
//					}
//
//				}
            }
            sql3.append("</tr>");
        }



        /**
         * 获取表头（指标）
         */
        if(tableJson.getKpiJson().size()==0){
            sql4.append("<th class=\"null\" colspan=\"1\">\n" +
                    "<span >\n" +
                    "<span >暂无度量!\n" +
                    "</span>\n" +
                    "</span>\n" +
                    "</th>");
        }else if(tableJson.getCols().size()==0){
            sql4.append("<tr>");
            for (int j=0;j<tableJson.getKpiJson().size();j++){
                String name=rsmd.getColumnName(j+tableJson.getDims().size()+1);
                sql4.append("<th style=\"width:110px;\" colspan=\"1\"><span><span  title=\"" +name+"\">"+name+"</span><a class=\"fa fa-bar-chart\" href=\"javascript:;\" onclick=\"setKpiInfo(this,'"+tableJson.getKpiJson().get(j).getKpi_id()+"');\" style=\"padding-left:10px\"> </a></span></th>");

            }
            sql4.append("</tr>");
        } else {
            sql4.append("<tr>");
            for(int i=0;i<hangshu;i++) {
                for (int j=0;j<tableJson.getKpiJson().size();j++){
                    String name=rsmd.getColumnName(j+tableJson.getDims().size()+1);
                    sql4.append("<th style=\"width:110px;\" colspan=\"1\"><span class=\"colkpi\"><span title=\"" +name+"\">"+name+"</span><a class=\"fa fa-bar-chart\" href=\"javascript:;\" onclick=\"setKpiInfo(this,'"+tableJson.getKpiJson().get(j).getKpi_id()+"');\" style=\"padding-left:10px\"> </a></span></th>");

                }
//	或者	<th class="null" colspan="1"><span class="colkpi"><span class="kpiname" title="sort">sort</span><a class="dimoptbtn set" href="javascript:;" onclick="setKpiInfo(this,'259');" style="opacity: 0.6;"> &nbsp; </a></span></th>


            }

            sql4.append("</tr>");
        }
//		            <tr><td align="right" class="kpiData1 grid5-td"><span class="kpiValue"><div style="height:70px;margin-top:10px;"><a href="javascript:linkdetail({&quot;logo&quot;:&quot;http://39.98.239.6:8080/file/20190601/img_486529c5564c46a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg&quot;,&quot;name&quot;:&quot;元气森林&quot;});">12.0</a></div></span></td>
//		                 <td align="right" class="kpiData1 grid5-td"><span class="kpiValue"><div style="height:70px;margin-top:10px;"><a href="javascript:linkdetail({&quot;logo&quot;:&quot;http://39.98.239.6:8080/file/20190601/img_486529c5564c46a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg&quot;,&quot;name&quot;:&quot;元气森林&quot;});">-</a></div></span></td></tr>

//<tr class="tr-row1"><th align="left" valign="top" rowspan="1" class="grid5-td"><span class="s_rowDim" title="元气森林"><a class="dimgoup" onclick="goupDim(1, this, 'row','26', true)" style="opacity:0.5">  </a>元气森林</span></th>
//	                <th align="left" valign="top" rowspan="1" class="grid5-td"><span class="s_rowDim" style="height:80px;" title="http://39.98.239.6:8080/file/20190601/img_486529c5564c46a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg"><a class="dimDrill" onclick="drillDim(1, this, 'row', 'http://39.98.239.6:8080/file/20190601/img_486529c5564c46a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg','http://39.98.239.6:8080/file/20190601/img_486529c5564c46a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg', '27')" style="opacity:0.5">  </a>http://39.98.2<br>39.6:8080/file<br>/20190601/img_<br>486529c5564c46<br>a79a4baba9a6def7c7_webwxgetmsgimg (6).jpg</span></th></tr>
        /**
         * 维度的col表头
         */
        res.append("<div id=\"mv.tmp.table\">\n" +
                " <div>\n" +
                "\t<table class=\"\">\n" +
                "\t<tbody>\n" +
                "\t\t<tr>\n" +
                "\t       <td class=\"blank\" valign=\"bottom\">\n" +
                "\t\t\t<div class=\"rowDimsList\">\n" +
                "\t\t\t\t<table class=\"grid5\" cellpadding=\"0\" cellspacing=\"0\">\n" +
                "\t\t\t        <tbody>\n" +
                "\t\t\t\t       <tr>");
        res.append(sql1);

        /**
         * 维度的row表头
         */
        res.append("\t                   </tr>\n" +
                "\t                </tbody>\n" +
                "\t\t\t\t</table>\n" +
                "\t\t\t</div>\n" +
                "\t\t\t</td>\n" +
                "\n" +
                "\t\t <td>\n" +
                "\t      <div id=\"d_colDims\" class=\"droppable\" style=\"width: 155px;\">\n" +
                "\t          <div class=\"colDimsList\">\n");
        res.append(sql11);
        res.append(				"\t          </div>\n" +
                "              <table class=\"grid5\" cellpadding=\"0\" cellspacing=\"0\">\n" +
                "                <tbody>");

        /**
         * 行维度值
         */

        res.append(sql22);
        /**
         * kpi头
         */
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
                "\t\t   <div id=\"d_kpi\" class=\"droppable\" style=\"width: 3000px; height: 577px;\">\n" +
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


//
//		System.out.println(tableJson.getCols().size());
//		System.out.println(tableJson.getRows().size());
//		System.out.println(tableJson.getDims().size());
//		System.out.println(tableJson.getKpiJson().size());
//

        return res.toString();
    }

}
