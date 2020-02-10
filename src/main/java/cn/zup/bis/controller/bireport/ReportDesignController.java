package cn.zup.bis.controller.bireport;

import cn.zup.bis.entity.bireport.OlapInfo;
import cn.zup.bis.service.model.CubeService;
import cn.zup.bis.service.bireport.BisReportService;
import cn.zup.bis.service.bireport.OlapService;
import cn.zup.bis.util.BaseController;
import cn.zup.bis.util.CompPreviewService;
import cn.zup.bis.util.RSBIUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruisi.bi.engine.view.emitter.ContextEmitter;
import com.ruisi.bi.engine.view.emitter.excel.ExcelEmitter;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.ext.engine.view.emitter.pdf.PdfEmitter;
import com.ruisi.ext.engine.view.emitter.text.TextEmitter;
import com.ruisi.ext.engine.view.emitter.word.WordEmitter;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

@Controller
@RequestMapping(value = "/rest/bis/bireport/ReportDesignController")
public class ReportDesignController extends BaseController {
	
	@Autowired
	private CubeService cubeService;
	
	@Autowired
	private OlapService service;
	
	@Autowired
	private BisReportService bisReportService;
	
	@RequestMapping(value="/ReportDesign")
	public String index(Integer pageId, Integer selectDs, ModelMap model){
		if(pageId != null){
			OlapInfo olap = service.getOlap(pageId);
			if(olap != null){
				model.addAttribute("pageInfo", olap.getPageInfo());
				model.addAttribute("pageName", olap.getPageName());
			}
		}else if(selectDs == null){
			model.addAttribute("selectDs", cubeService.getMaxCubeId() - 1);
		}
		return "bis/pages/bireport/ReportDesign";
	}
	
	@RequestMapping(value="/insertChart")
	public String insertChart(String tp, ModelMap model){
		model.addAttribute("tp", tp);
		return "bis/pages/bireport/Panel-insertChart";
	}
	
	@RequestMapping(value="/ReportExport", method = RequestMethod.POST)
	public @ResponseBody
    Object export(String type, String json, String picinfo, HttpServletRequest req, HttpServletResponse res) throws Exception{
		
		ExtContext.getInstance().removeMV(bisReportService.deftMvId);
		JSONObject obj = (JSONObject) JSON.parse(json);
		MVContext mv = bisReportService.json2MV(obj, 0);
		req.setAttribute("picinfo", picinfo);
		CompPreviewService ser = new CompPreviewService(req, res, req.getServletContext());
		ser.setParams(null);
//		ser.initPreview();
		
		String fileName = "file.";
		if("html".equals(type)){
			fileName += "html";
		}else
		if("excel".equals(type)){
			fileName += "xls";
		}else
		if("csv".equals(type)){
			fileName += "csv";
		}else
		if("pdf".equals(type)){
			fileName += "pdf";
		}else 
		if("word".equals(type)){
			fileName += "docx";
		}
		
		res.setContentType("application/x-msdownload");
		String contentDisposition = "attachment; filename=\""+fileName+"\"";
		res.setHeader("Content-Disposition", contentDisposition);
		
		if("html".equals(type)){
			String ret = ser.buildMV(mv, req.getServletContext());
			String html = RSBIUtils.htmlPage(ret, RSBIUtils.getConstant("resPath"), "olap");
			InputStream is = IOUtils.toInputStream(html, "utf-8");
			IOUtils.copy(is, res.getOutputStream());
			is.close();
		}else
		if("excel".equals(type)){
			ContextEmitter emitter = new ExcelEmitter();
			ser.buildMV(mv, emitter, req.getServletContext());
		}else
		if("csv".equals(type)){
			ContextEmitter emitter = new TextEmitter();
			String ret = ser.buildMV(mv, emitter, req.getServletContext());
			InputStream is = IOUtils.toInputStream(ret, "gb2312");
			IOUtils.copy(is, res.getOutputStream());
			is.close();
		}else 
		if("pdf".equals(type)){
			ContextEmitter emitter = new PdfEmitter();
			ser.buildMV(mv, emitter, req.getServletContext());
		}else
		if("word".equals(type)){
			ContextEmitter emitter = new WordEmitter();
			ser.buildMV(mv, emitter, req.getServletContext());
		}
		
		return null;
	}
	
	@RequestMapping(value="/kpidesc")
	public String kpidesc(Integer cubeId, ModelMap model){
		model.addAttribute("ls", service.listKpiDesc(cubeId));
		return "bis/pages/bireport/DataSet-kpidesc";
	}
	
	@RequestMapping(value="/print", method = RequestMethod.POST)
	public Object print(String pageInfo, HttpServletRequest req, HttpServletResponse res) throws Exception{
		ExtContext.getInstance().removeMV(bisReportService.deftMvId);
		JSONObject obj = (JSONObject) JSON.parse(pageInfo);
		MVContext mv = bisReportService.json2MV(obj, 0);
		CompPreviewService ser = new CompPreviewService(req, res, req.getServletContext());
		ser.setParams(null);
//		ser.initPreview();
		
		String ret = ser.buildMV(mv, req.getServletContext());
		req.setAttribute("data", ret);
		
		return "bis/pages/bireport/ReportDesign-print";
	}
}
