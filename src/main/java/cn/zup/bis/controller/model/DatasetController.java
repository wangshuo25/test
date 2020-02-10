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
    Object list(){
		List<Dataset> list = service.listDataset();

		return list;
	}
	
	@RequestMapping(value="/listTables")
	public @ResponseBody
    Object listTables(String dsid, String tname) throws Exception{
		return dsservice.listTables(dsid, tname);
	}
	
	@RequestMapping(value="/listTableColumns")
	public @ResponseBody
    Object listTableColumns(String dsid, String tname) throws Exception {
		return service.listTableColumns(dsid, tname);
	}
	
	@RequestMapping(value="/queryDatasetMeta", method = RequestMethod.POST)
	public @ResponseBody
    Object queryDatasetMeta(String cfg, String dsid) throws Exception {
		JSONObject dset = (JSONObject) JSON.parse(cfg);
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
		return ret;
	}
	
	@RequestMapping(value="/reloadDset")
	public @ResponseBody
    Object reloadDset(String dsetId, String dsid)  {
		try{
			service.reloadDset(dsetId, dsid);
			return super.buildSucces();
		}catch(Exception ex){
			ex.printStackTrace();
			return super.buildError(ex.getMessage());
		}
	}
}
