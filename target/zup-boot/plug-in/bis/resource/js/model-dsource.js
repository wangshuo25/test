if($ == undefined){
    $ = jQuery;
}
function initdsourcetable(){
    if($("#dsourcetable").size() > 0){
        $("#dsourcetable").datagrid("load", {t:Math.random()});
        return;
    }
    var ctx = "<table id=\"dsourcetable\" title=\"数据源管理\"></table>";
    $("#optarea").html(ctx);

    $("#dsourcetable").datagrid({

        url:'rest/bis/DataSourceController/listDataSource',
            singleSelect:true,
            pagination:true,//分页
            fit:true,
            rownumbers: true,
           height:300,
            toolbar:[{
                        text:'新增',
                        iconCls:'icon-add2',
                        handler:function(){
                            newdsource(false);
                        }
                    }],
        columns: [[
            {field:'dsname',title:'数据源名称',width:160,align:'center'},
                    {field:'use',title:'连接方式',width:120,align:'center'},
                    {field:'linkType',title:'数据库类型',width:120,align:'center'},
                    {field:'linkUrl',title:'连接名',width:800,align:'center'},
                    {field:'linkName',title:'用户名',width:150,align:'center'},
                    {field:'action',title:'操作',width:200,align:'center',
                        formatter:function(value,row,index) {

                            return "<button href='javascript:void(0)' onclick='delDsource(&apos;" + index+ "&apos;)'><img src='plug-in/bis/resource/jquery-easyui-1.4.4/themes/icons/no.png' >删除</button><button href='javascript:void(0)' style='margin-left:10px;' onclick='newdsource(&apos;" + true+ " &apos;,&apos;" + index+ "&apos;)'><img src='plug-in/bis/resource/jquery-easyui-1.4.4/themes/icons/pencil.png' >编辑</button>";
                        }
                    }

        ]],

    });



















    //     $("#dsourcetable").datagrid({
    //     singleSelect:true,
    //     pagination:true,//分页
    //     fit:true,
    //     rownumbers: true,
    //     url:'rest/bis/DataSourceController/listDataSource',
    //     toolbar:[{
    //         text:'新增',
    //         iconCls:'icon-add',
    //         handler:function(){
    //             newdsource(false);
    //         }
    //     },{
    //         text:'修改',
    //         iconCls:'icon-edit',
    //         handler:function(){
    //             var row = $("#dsourcetable").datagrid("getChecked");
    //             if(row == null || row.length == 0){
    //                 $.messager.alert("出错了。","您还未勾选数据。", "error");
    //                 return;
    //             }
    //             newdsource(true, row[0].dsid);
    //         }
    //     },{
    //         text:'删除',
    //         iconCls:'icon-cancel',
    //         handler:function(){
    //             var row = $("#dsourcetable").datagrid("getChecked");
    //             if(row == null || row.length == 0){
    //                 $.messager.alert("出错了。","您还未勾选数据。", "error");
    //                 return;
    //             }
    //             delDsource(row[0].dsid);
    //         }
    //     }]
    // });

    // $('#dsourcetable').datagrid({
    //     title:'数据源控制台',
    //     toolbar:[{
    //                 text:'新增',
    //                 iconCls:'icon-add2',
    //                 handler:function(){
    //                     newdsource(false);
    //                 }
    //             }],
    //     pagination:true,//分页
    //     iconCls:'icon-dsource',
    //     fit:true,
    //     rownumbers: true,
    //     width:1400,
    //     height:500,
    //     striped: true,
    //     singleSelect: true,
    //
    //     url:'rest/bis/DataSourceController/listDataSource',
    //
    //     columns:[[
    //         {field:'dsname',title:'数据源名称',width:160,align:'center'},
    //         {field:'use',title:'连接方式',width:120,align:'center'},
    //         {field:'linkType',title:'数据库类型',width:120,align:'center'},
    //         {field:'linkUrl',title:'连接名',width:800,align:'center'},
    //         {field:'linkName',title:'用户名',width:150,align:'center'},
    //         {field:'action',title:'操作',width:200,align:'center',
    //             formatter:function(value,row,index) {
    //
    //                 return "<button href='javascript:void(0)' onclick='delDsource(&apos;" + index+ "&apos;)'><img src='plug-in/bis/resource/jquery-easyui-1.4.4/themes/icons/no.png' >删除</button><button href='javascript:void(0)' style='margin-left:10px;' onclick='newdsource(&apos;" + true+ " &apos;,&apos;" + index+ "&apos;)'><img src='plug-in/bis/resource/jquery-easyui-1.4.4/themes/icons/pencil.png' >编辑</button>";
    //             }
    //         }
    //     ]],
    //     view: detailview,
    //     detailFormatter: function (rowIndex, rowData) {
    //         return '<div  style=""><table class="tableOrderDetial"></table></div> ';
    //     },
    //     onExpandRow: function (index, row) {
    //         var ddv = $(dsourcetable).datagrid('getRowDetail', index).find('table.tableOrderDetial')
    //         ddv.datagrid({
    //             fit: true,
    //             border: false,
    //             url: 'rest/bis/DataSourceController/listDataSource',
    //             striped: true,
    //             rownumbers: true,
    //             singleSelect: true,
    //             columns: [[
    //                 {field:'dsname',title:'数据源名称',width:160,align:'center'},
    //                 {field:'use',title:'连接方式',width:120,align:'center'},
    //             ]],
    //             pagination: true,
    //             pageSize: 10,
    //             pageList: [10, 20, 30],
    //
    //         });
    //     }
    //
    //
    //
    // });

}



function delDsource(a){

    var rows= $("#dsourcetable").datagrid("getRows");
    var dsid=rows[a].dsid;
    if(confirm("是否确认删除？")){
        $.ajax({
            url:'rest/bis/DataSourceController/deleteDataSource',
            data: {dsid:dsid},
            type:'POST',
            dataType:'JSON',
            success:function(){
                $("#dsourcetable").datagrid("reload", {t:Math.random});
            },
            error:function(){
                msginfo("系统出错，请查看后台日志。");
            }
        });
    }
}








function newdsource(isupdate, a){
    var ds;
    if(isupdate){
        var rows= $("#dsourcetable").datagrid("getRows");
        var dsid=rows[a].dsid;
        $.ajax({
            type:'GET',
            url:'rest/bis/DataSourceController/getDataSource',
            dataType:'JSON',
            data:{dsid:dsid},
            async:false,
            success: function(resp){
                ds = resp;
            }
        });
    }
    var ctx = "<div id=\"dsource_tab\" style=\"height:auto; width:auto;\"><div title=\"JDBC\"><form id=\"datasourceform\" name=\"datasourceform\"><input type=\"hidden\" name=\"connstate\" id=\"connstate\"><div class=\"textpanel\"><span class=\"inputtext\">数据源名称：</span><input type=\"text\" id=\"dsname\" name=\"dsname\" class=\"inputform\" style=\"width:400px;\" value=\""+(ds&&ds.use=='jdbc'?ds.dsname:"")+"\"><br/><span class=\"inputtext\">数据源类型：</span><select id=\"linkType\" name=\"linkType\" style=\"width:400px;\" class=\"inputform\"><option value=\"mysql\" "+(ds&&ds.use=='jdbc'&&ds.linkType=='mysql'?"selected":"")+">mysql</option><option value=\"kylin\" "+(ds&&ds.use=='jdbc'&&ds.linkType=='kylin'?"selected":"")+">kylin</option>" +
        "</select><br/><span class=\"inputtext\">连接字符串：</span><input type=\"text\" id=\"linkUrl\" name=\"linkUrl\" class=\"inputform\" style=\"width:400px;\" value=\""+(ds&&ds.use=='jdbc'?ds.linkUrl:"jdbc:mysql://ip/database?useUnicode=true&characterEncoding=UTF8")+"\"><br/><span class=\"inputtext\">连接用户名：</span><input type=\"text\" id=\"linkName\" name=\"linkName\" class=\"inputform\" style=\"width:400px;\" value=\""+(ds&&ds.use=='jdbc'?ds.linkName:"")+"\"> <br/><span class=\"inputtext\">连接密码：</span><input type=\"password\" name=\"linkPwd\" id=\"linkPwd\" style=\"width:400px;\" value=\""+(ds&&ds.use=='jdbc'?ds.linkPwd:"")+"\" class=\"inputform\"></div></form></div></div>"
    $('#pdailog').dialog({
        title: isupdate ? "编辑数据源" : '创建数据源',
        width: 540,
        height: 300,
        closed: false,
        iconCls:"icon-label",
        cache: false,
        content:ctx,
        buttons:[{
            text:"测试连接",
            iconCls:"icon-earth",

            handler:function(){

               {
                    var param = $("#datasourceform").serialize();
                    $.ajax({
                        type: "POST",
                        url: "rest/bis/DataSourceController/testDataSource",
                        dataType:"json",
                        data: param,
                        success: function(resp){
                            if(resp.result == 1){
                                msginfo("测试成功！", "suc");
                                $("#datasourceform #connstate").val("y");
                            }else{
                                msginfo("测试失败！<br/>"+resp.msg);
                            }
                        },
                        error:function(){
                            msginfo("测试失败！");
                        }
                    });
                }
            }
        },{
            text:'确定',
            iconCls:"icon-ok",
            handler:function(){
{
                    if($("#datasourceform #dsname").val() == ''){
                        msginfo("请输入数据源名称！");
                        $("#datasourceform #dsname").focus();
                        return;
                    }
                    if($("#datasourceform #connstate").val() != "y"){
                        msginfo("请先测试连接正常再确定！");
                        return;
                    }
                    if(isupdate == false){
                        var ds = {"linkType":$("#linkType").val(), "linkName":$("#linkName").val(), "linkPwd":$("#linkPwd").val(), "linkUrl":$("#linkUrl").val(),"dsname":$("#datasourceform #dsname").val(),"dsid":newGuid(),"use":"jdbc"};
                        $.ajax({
                            url:'rest/bis/DataSourceController/saveDataSource',
                            data: ds,
                            dataType:'JSON',
                            success:function(){
                                $("#dsourcetable").datagrid("reload", {t:Math.random});
                            },
                            error:function(){
                                msginfo("系统出错，请查看后台日志。");
                            }
                        });
                    }else{
                        var nds = {"linkType":$("#linkType").val(), "linkName":$("#linkName").val(), "linkPwd":$("#linkPwd").val(), "linkUrl":$("#linkUrl").val(),"dsname":$("#datasourceform #dsname").val(),"dsid":dsid,"use":"jdbc"};
                        $.ajax({
                            url:'rest/bis/DataSourceController/updateDataSource',
                            data:nds,
                            dataType:'JSON',
                            success:function(){
                                $("#dsourcetable").datagrid("reload", {t:Math.random});
                            },
                            error:function(){
                                msginfo("系统出错，请查看后台日志。");
                            }
                        });
                    }
                }
                $('#pdailog').dialog('close');
            }
        },{
            text:'取消',
            iconCls:"icon-cancel",
            handler:function(){
                $('#pdailog').dialog('close');
            }
        }]
    });
    $("#pdailog #linkType").change(function(){
        var val = $(this).val();
        if(val == "mysql"){
            $("#pdailog #linkUrl").val("jdbc:mysql://ip/database?useUnicode=true&characterEncoding=UTF8&serverTimezone=GMT%2B8");
        }else if(val == "oracle"){
            $("#pdailog #linkUrl").val("jdbc:oracle:thin:@ip:1521/sid");
        }else if(val == "sqlser"){
            $("#pdailog #linkUrl").val("jdbc:jtds:sqlserver://ip:1433/database");
        }else if(val == "db2"){
            $("#pdailog #linkUrl").val("jdbc:db2://ip:50000/database");
        }else if(val == "postgresql"){
            $("#pdailog #linkUrl").val("jdbc:postgresql://ip:5432/database");
        }else if(val == "hive"){
            $("#pdailog #linkUrl").val("jdbc:hive2://ip:10000/default");
        }else if(val == "kylin"){
            $("#pdailog #linkUrl").val("jdbc:kylin://ip:7070/kylin_project_name");
        }
    });
}