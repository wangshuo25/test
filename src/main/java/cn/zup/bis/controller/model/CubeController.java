package cn.zup.bis.controller.model;

import cn.zup.bis.entity.model.Cube;
import cn.zup.bis.entity.Result;
import cn.zup.bis.service.model.CubeService;
import cn.zup.bis.util.BaseController;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/rest/bis/CubeController")
public class CubeController extends BaseController {
	
	@Autowired
	private CubeService service;
	
	@RequestMapping(value="/listCube")
	public @ResponseBody
    Object list(String key){
		return service.listCube(key);
	}
//
//	@RequestMapping(value="/pageCube")
//	public @ResponseBody
//    Object page(String key, PageParam page){
//		PageHelper.startPage(page.getPage(), page.getRows());
//		List<Cube> ls = service.listCube(key);
//		PageInfo<Cube> pageInfo=new PageInfo<Cube>(ls);
//		return super.buildSucces(pageInfo);//记得看看一下
//	}

	@RequestMapping(value="/saveCube", method = RequestMethod.POST)
	public @ResponseBody
    Object save(@RequestBody Cube cube){

		Result ret = service.insertCube(cube);
		return ret;
	}
	
	@RequestMapping(value="/updateCube", method = RequestMethod.POST)
	public @ResponseBody
    Object update(@RequestBody Cube cube){
		Result ret = service.updateCube(cube);
		return ret;
	}
	
	@RequestMapping(value="/delCube")
	public @ResponseBody
    Object delete(Integer cubeId){
		Result ret = service.deleteCube(cubeId);
		return ret;
	}
	
	@RequestMapping(value="/getCube")
	public @ResponseBody
    Object get(Integer cubeId){
		return service.getCubeById(cubeId).toString();
	}
	
	@RequestMapping(value="/treeCube")
	public @ResponseBody
    Object tree(Integer cubeId){
		return service.treeCube(cubeId);
	}
}
