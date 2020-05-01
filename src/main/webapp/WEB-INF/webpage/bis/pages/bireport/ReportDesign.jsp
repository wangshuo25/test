<%@ page language="java" contentType="text/html; charset=utf-8"
         pageEncoding="utf-8"%>
<%
    String path = request.getContextPath();
    String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href=" <%=basePath%>">

    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>(OLAP)</title>
<link rel="shortcut icon" type="image/x-icon" href="plug-in/bis/resource/img/rs_favicon.ico">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<link href="plug-in/bis/ext-res/css/bootstrap.min.css" rel="stylesheet">

    <link href="plug-in/bis/resource/css/animate.css" rel="stylesheet">
<link href="plug-in/bis/resource/css/style.css" rel="stylesheet">
<link href="plug-in/bis/resource/css/font-awesome.css?v=4.4.0" rel="stylesheet">
<link href="plug-in/bis/resource/sweetalert/sweetalert.css" rel="stylesheet">
<link href="plug-in/bis/resource/css/bireport.css" rel="stylesheet">
<link href="plug-in/bis/resource/awesome-bootstrap-checkbox/awesome-bootstrap-checkbox.css" rel="stylesheet">
<script type="text/javascript" src="plug-in/bis/ext-res/js/jquery.min.js"></script>
<%--<script type="text/javascript" src="plug-in/bis/ext-res/js/bootstrap.min.js?v=3.3.6"></script>--%>
<link rel="stylesheet" type="text/css" href="plug-in/bis/resource/jquery-easyui-1.4.4/themes/default/easyui.css"/>
<link rel="stylesheet" type="text/css" href="plug-in/bis/resource/jquery-easyui-1.4.4/themes/icon.css"/>
<script type="text/javascript" src="plug-in/bis/resource/jquery-easyui-1.4.4/jquery.easyui.min.js"></script>
<script type="text/javascript" src="plug-in/bis/resource/sweetalert/sweetalert.min.js"></script>
<!--
<script language="javascript" src="plug-in/bis/resource/js/bireport-compress.js?v6"></script>
-->
<script language="javascript" src="plug-in/bis/resource/js/bitable.js"></script>
<script language="javascript" src="plug-in/bis/resource/js/bidrill.js"></script>
<script language="javascript" src="plug-in/bis/resource/js/bichart.js"></script>
<script language="javascript" src="plug-in/bis/resource/js/bireport.js"></script>
<script language="javascript" src="plug-in/bis/resource/js/json.js"></script>
<script type="text/javascript" src="plug-in/bis/ext-res/js/ext-base.js"></script>
<script type="text/javascript" src="plug-in/bis/ext-res/js/echarts.min.js"></script>
</head>

<script language="javascript">


var pageInfo = {"selectDs":'${selectDs}',tab:"1", comps:[{"name":"表格组件","id":1, "type":"table"},{"name":"图表组件","id":2, "type":"chart",chartJson:{type:"line",params:[]},kpiJson:[]}], params:[]};
var isnewpage = true;




var showtit = true;
var curTmpInfo = {"menus":"${menus}"}; //临时对象
// curTmpInfo.isupdate = false; //页面是否已经修改
// curTmpInfo.chartpos = "left";  //too/left 表示图表配置属性的位
$(function(){

	//初始化数据集
	reloadDatasetTree();
	// if(pageInfo.selectDs == ''){
	// 	$("#datasettree").tree("options").dnd = false;
	// }

	// 初始化参数
	// initparam();

	//初始化默认组件
	for(i=0;i<pageInfo.comps.length; i++){
		var t = pageInfo.comps[i];
		 var str = null;

		addComp(t.id, t.name, str, false, t.type, isnewpage ? null : t);
	}

    // //初始化选项卡
	// $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
    //
    //     //  var activeTab = $(e.target).attr("idx");
	// 	//  pageInfo.idx = activeTab;
	// 	// if(pageInfo.idx == "2"){  //图
	// 	// 	$("#datasettree").removeClass("tableTreeCss").addClass("chartTreeCss");
	// 	// }else{  //表
	// 	// 	$("#datasettree").removeClass("chartTreeCss").addClass("tableTreeCss");
	// 	// }
	// });
	// if(pageInfo.idx == "2"){
	// 	$('a[data-toggle="tab"]').last().tab('show');
	// }
	//图标事件
	// $("#chartarea").on("mouseover", "span.charticon", function(){
	// 	$(this).css("opacity", 1);
	// }).on("mouseout", "span.charticon", function(){
	// 	$(this).css("opacity", 0.6);
	// });
	// $("#optarea").on("mouseover", "a.dimDrill", function(){
	// 	$(this).css("opacity", 1);
	// }).on("mouseout", "a.dimDrill", function(){
	// 	$(this).css("opacity", 0.6);
	// });
	// $("#optarea").on("mouseover", "a.dimoptbtn", function(){
	// 	$(this).css("opacity", 1);
	// }).on("mouseout", "a.dimoptbtn", function(){
	// 	$(this).css("opacity", 0.6);
	// });
});

</script>
<style>
/*.panel-body {*/
/*	overflow:hidden;*/
/*}*/
</style>

<body class="gray-bg">


<div class="wrapper wrapper-content">
	<div class="row">
		<div class="col-sm-3">
			<div class="ibox" style="border:none;">
				<div class="ibox-content">
					<button class="" onclick="selectdataset()"><i class="fa fa-refresh"></i> 切换数据</button>
					<p class="text-danger">请拖动维度或者度量</p>
					<div id="datasettreediv"></div>
				</div>
			</div>
		</div>

		<div class="col-sm-9 animated fadeInRight">



							<button type="button" class="fa fa-ban" style="margin-left: 1100px" onclick="cleanData()">清除数据</button>



					<div class="tab-content">
                        <div id="tab-1" class="tab-pane active">
                            <div class="panel-body">
                               <div id="optarea" style="padding:5px; overflow:auto;"></div>
                            </div>
                        </div>


<%--                        <div id="tab-2" class="tab-pane">--%>
<%--                            <div class="panel-body">--%>
<%--								<button class="btn btn-block btn-default" onclick="updateChart()">切换图表类型</button>--%>
<%--                               <div id="chartarea" style="padding:5px;"></div>--%>
<%--                            </div>--%>
<%--                        </div>--%>
                    </div>

		</div>
	</div>
</div>

<div id="pdailog"></div>
<div id="kpioptmenu" class="easyui-menu">
<%--	<div>--%>
<%--    	<span>计算</span>--%>
<%--   	   <div style="width:120px;">--%>
<%--    	<div onclick="kpicompute('sq')">上期值</div>--%>
<%--        <div onclick="kpicompute('tq')">同期值</div>--%>
<%--        <div onclick="kpicompute('zje')">增减额</div>--%>
<%--        <div onclick="kpicompute('hb')">环比(%)</div>--%>
<%--        <div onclick="kpicompute('tb')">同比(%)</div>--%>
<%--        <div class="menu-sep"></div>--%>
<%--        <div onclick="kpicompute('sxpm')">升序排名</div>--%>
<%--        <div onclick="kpicompute('jxpm')">降序排名</div>--%>
<%--        <div onclick="kpicompute('zb')">占比(%)</div>--%>
<%--        <div onclick="kpicompute('ydpj')">移动平均</div>--%>
<%--       </div>--%>
<%--    </div>--%>
<%--  	<div onclick="kpiproperty()">属性...</div>
    <div onclick="crtChartfromTab()">图表...</div>--%>
    <div onclick="kpiFilter('table')">筛选...</div>
    <div onclick="kpiwarning()">预警...</div>
    <div>
    <span>排序</span>
    <div style="width:120px;">
    	<div id="k_kpi_ord1" onclick="kpisort('asc')">升序</div>
        <div id="k_kpi_ord2"  onclick="kpisort('desc')">降序</div>
        <div id="k_kpi_ord3" iconCls="icon-ok" onclick="kpisort('')">默认</div>
    </div>
    </div>
    <div iconCls="icon-remove" onclick="delJsonKpiOrDim('kpi')">删除</div>
</div>
<div id="dimoptmenu" class="easyui-menu">
	<div onclick="dimsort('asc')">升序</div>
    <div onclick="dimsort('desc')">降序</div>
    <div>
    <span>移动</span>
    <div style="width:120px;">
    	<div iconCls="icon-back" onclick="dimmove('left')">左移</div>
        <div iconCls="icon-right" onclick="dimmove('right')">右移</div>
        <div id="m_moveto" onclick="dimexchange()">移至</div>
    </div>
    </div>
    <div iconCls="icon-reload" onclick="changecolrow()">行列互换</div>
 <div iconCls="icon-filter" onclick="filterDims()">筛选...</div>
<%--   <div onclick="getDimTop()" id="m_aggre">取Top...</div>--%>
   <div iconCls="icon-sum" onclick="aggreDim()" id="m_aggre">聚合</div>

   <div onclick="delJsonKpiOrDim('dim')" iconCls="icon-remove">删除</div>
</div>
<%--<div id="chartoptmenu" class="easyui-menu">--%>
<%--   <div onclick="chartsort('asc')">升序</div>--%>
<%--   <div onclick="chartsort('desc')">降序</div>--%>
<%--   <div iconCls="icon-filter" onclick="chartfilterDims()" >筛选...</div>--%>
<%--   <div onclick="setChartKpi()" id="m_set">属性...</div>--%>
<%--   <div onclick="delChartKpiOrDim()" iconCls="icon-remove">清除</div>--%>
<%--</div>--%>
</body>
</html>