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
    <title>数据建模工具</title>
    <link rel="shortcut icon" type="image/x-icon" href="plug-in/bis/bis/resource/img/rs_favicon.ico">
    <link href="plug-in/bis/ext-res/css/bootstrap.min.css" rel="stylesheet">
    <link href="plug-in/bis/resource/css/style.css" rel="stylesheet">
    <script type="text/javascript" src="plug-in/bis/ext-res/js/jquery.min.js"></script>
    <script type="text/javascript" src="plug-in/bis/ext-res/My97DatePicker/WdatePicker.js"></script>
    <script language="javascript" src="plug-in/bis/resource/js/json.js"></script>
    <script language="javascript" src="plug-in/bis/ext-res/js/ext-base.js"></script>
    <link rel="stylesheet" type="text/css" href="plug-in/bis/resource/jquery-easyui-1.4.4/themes/default/easyui.css">
    <link rel="stylesheet" type="text/css" href="plug-in/bis/resource/jquery-easyui-1.4.4/themes/icon.css">
    <script type="text/javascript" src="plug-in/bis/resource/jquery-easyui-1.4.4/jquery.easyui.min.js"></script>
    <script type="text/javascript" src="plug-in/bis/resource/jquery-easyui-1.4.4/locale/easyui-lang-zh_CN.js"></script>

    <script type="text/javascript" src="plug-in/bis/resource/jquery-easyui-1.4.4/datagrid-detailview.js" charset="utf-8"></script>


    <script language="javascript" src="plug-in/bis/resource/js/model.js"></script>
    <script language="javascript" src="plug-in/bis/resource/js/model-dsource.js"></script>
    <script language="javascript" src="plug-in/bis/resource/js/model-dset.js"></script>
    <script language="javascript" src="plug-in/bis/resource/js/model-cube.js"></script>
</head>

<script language="javascript">

	$(function(){
		initModelTree();
	});

</script>
<style>
	.msginfo{
		width:85%;
		height:90%;
		padding-left:40px;
		line-height:20px;
	}
	.msgerr{
		background-image:url(../resource/img/icon-error.gif);
		background-position:left center;
		background-repeat:no-repeat;
	}
	.msgsuc{
		background-image:url(../resource/img/icon-suc.gif);
		background-position:left center;
		background-repeat:no-repeat;
	}
</style>


<body id="metalayout" class="easyui-layout">



<div data-options="region:'west',split:true,title:'数据建模'"  style="width:210px;">
	<ul id="modeltree"></ul>
</div>

<div data-options="region:'center',title:''" id="optarea">
</div>

<table id="tb">
</table>


<div id="pdailog"></div>

</body>
</html>