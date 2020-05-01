package cn.zup.bis.controller.model;

import cn.zup.bis.entity.model.DataSource;
import cn.zup.bis.service.model.DataSourceService;
import cn.zup.bis.util.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/*
@responseBody注解的作用是将controller的方法返回的对象通过适当的转换器转换为指定的格式之后，写入到response对象的body区，通常用来返回JSON数据或者是XML数据。
@RestController等同于@Controller + @ResponseBody。可以删掉@ResponseBody
@RequestMapping注解是用来映射请求的，即指明处理器可以处理哪些URL请求，该注解既可以用在类上，也可以用在方法上。
@Resource 依赖注入

*/



@RestController
@RequestMapping(value = "/rest/bis/DataSourceController")
public class DataSourceController extends BaseController {

	@Resource
	private DataSourceService dsService;

	@RequestMapping(value="/listDataSource")
	public @ResponseBody
    Object list() {
		List<DataSource> list = dsService.listDataSource();
		return list;
	}

	@RequestMapping(value="/getDataSource")
	public @ResponseBody
    Object get(String dsid){
		return dsService.getDataSource(dsid);
	}

	@RequestMapping(value="/deleteDataSource")
	public
    Object delete(String dsid){
		dsService.deleteDataSource(dsid);
		return this.buildSucces();
	}
	@RequestMapping(value="/testDataSource")
	public
    Object test(DataSource ds)  {
		ds.setUse("jdbc");
		return dsService.testDataSource(ds);
	}

	@RequestMapping(value="/saveDataSource")
	public
    Object save(DataSource ds){
		dsService.insertDataSource(ds);
		return this.buildSucces();
	}

	@RequestMapping(value="/updateDataSource")
	public @ResponseBody
    Object update(DataSource ds){
		dsService.updateDataSource(ds);
		return this.buildSucces();			
	}
}
