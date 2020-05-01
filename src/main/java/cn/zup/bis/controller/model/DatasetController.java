package cn.zup.bis.controller.model;

import cn.zup.bis.entity.model.Dataset;
import cn.zup.bis.service.model.DataSourceService;
import cn.zup.bis.service.model.DatasetService;
import cn.zup.bis.util.BaseController;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/rest/bis/DatasetController")
public class DatasetController extends BaseController {
	
	@Autowired
	private DatasetService service;
	
	@Autowired
	private DataSourceService dsservice;
	
	@RequestMapping(value="/listDataset")
	public @ResponseBody
    Object list(String keyword){

		List<Dataset> list = service.listDataset(keyword);
		System.err.println(keyword);
		System.err.println(list.size());
		System.err.println(list.get(0).getName());
		return list;
	}
	
	@RequestMapping(value="/listTables")
	public @ResponseBody
    Object listTables(String dsid, String tname) throws Exception{
		return dsservice.listTables(dsid, tname);
	}
	
	@RequestMapping(value="/listTableColumns")//列出单张表的字段
	public @ResponseBody
    Object listTableColumns(String dsid, String tname) throws Exception {
		return service.listTableColumns(dsid, tname);
	}
	
	@RequestMapping(value="/queryDatasetMeta", method = RequestMethod.POST)//列出数据集的字段，包括主表从表
	public @ResponseBody
    Object queryDatasetMeta( String dsid,String cfg) throws Exception {

		JSONObject dset = (JSONObject) JSON.parse(cfg);//cfg“键值对”形式的json字符串，转化为JSONObject对象之后就可以使用其内置的方法，如get，进行各种处理了。
		return service.queryMetaAndIncome(dset, dsid);
	}
	
	@RequestMapping(value="/updateDset", method = RequestMethod.POST)
	public @ResponseBody
    Object updateDset(Dataset dset)  {
		service.updateDset(dset);
		return this.buildSucces();
	}
	
	@RequestMapping(value="/saveDset", method = RequestMethod.POST)
	public @ResponseBody
    Object saveDset(Dataset dset)  {
		System.err.println(dset.getCfg());
		service.insertDset(dset);
		return buildSucces();
	}
	
	@RequestMapping(value="/deleteDset")
	public @ResponseBody
    Object deleteDset(String dsetId)  {
		service.deleteDset(dsetId);
		return buildSucces();
	}
	
	@RequestMapping(value="/getDatasetCfg")
	public @ResponseBody
    Object getDatasetCfg(String dsetId)  {
		String ret = service.getDatasetCfg(dsetId);
        System.err.println(ret);

        return ret;
	}
	
//	@RequestMapping(value="/reloadDset")
//	public @ResponseBody
//    Object reloadDset(String dsetId, String dsid)  {
//		try{
//			service.reloadDset(dsetId, dsid);
//			return super.buildSucces();
//		}catch(Exception ex){
//			ex.printStackTrace();
//			return super.buildError(ex.getMessage());
//		}
//	}
}
