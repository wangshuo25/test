package cn.zup.bis.controller.model;

import cn.zup.bis.entity.model.DataSource;
import cn.zup.bis.service.model.DataSourceService;
import cn.zup.bis.util.BaseController;
import com.ruisi.ext.engine.view.exception.ExtConfigException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping(value = "/rest/bis/DataSourceController")
public class DataSourceController extends BaseController {

	@Resource
	private DataSourceService dsService;

	@RequestMapping(value="/listDataSource")
	public @ResponseBody
    Object list() throws ExtConfigException {
		System.err.println("43242");
		List<DataSource> list = dsService.listDataSource();


		return list;
	}

	@RequestMapping(value="/getDataSource")
	public @ResponseBody
    Object get(String dsid){
		return dsService.getDataSource(dsid);
	}

	@RequestMapping(value="/deleteDataSource")
	public @ResponseBody
    Object delete(String dsid){
		dsService.deleteDataSource(dsid);
		return this.buildSucces();
	}
	@RequestMapping(value="/testDataSource", method = RequestMethod.POST)
	public @ResponseBody
    Object test(DataSource ds)  {
		ds.setUse("jdbc");
		return dsService.testDataSource(ds);
	}

	@RequestMapping(value="/saveDataSource", method = RequestMethod.POST)
	public @ResponseBody
    Object save(DataSource ds){
		dsService.insertDataSource(ds);
		return this.buildSucces();
	}

	@RequestMapping(value="/updateDataSource", method = RequestMethod.POST)
	public @ResponseBody
    Object update(DataSource ds){
		dsService.updateDataSource(ds);
		return this.buildSucces();			
	}
}
