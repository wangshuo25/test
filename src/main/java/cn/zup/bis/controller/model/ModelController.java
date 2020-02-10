package cn.zup.bis.controller.model;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(value = "/rest/bis/ModelController")
public class ModelController {

	@RequestMapping(value="")
	public ModelAndView index(HttpServletRequest request){
		return new ModelAndView("bis/pages/model/ModelIndex");
	}

}
