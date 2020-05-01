package cn.zup.bis.controller.bireport;

import cn.zup.bis.service.model.CubeService;
import cn.zup.bis.util.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/rest/bis/bireport/ReportDesignController")
public class ReportDesignController extends BaseController {
	
	@Autowired
	private CubeService cubeService;

	@RequestMapping(value="/ReportDesign")
	public String index( ModelMap model){

		model.addAttribute("selectDs", cubeService.getMaxCubeId() - 1);

		return "bis/pages/bireport/ReportDesign";
	}
	
//	@RequestMapping(value="/insertChart")
//	public String insertChart(String tp, ModelMap model){
//		model.addAttribute("tp", tp);
//		return "bis/pages/bireport/Panel-insertChart";
//	}
	
//	@RequestMapping(value="/ReportExport", method = RequestMethod.POST)
//	public @ResponseBody
//    Object export(String type, String json, String picinfo, HttpServletRequest req, HttpServletResponse res) throws Exception{
//
//		ExtContext.getInstance().removeMV(bisReportService.deftMvId);
//		JSONObject obj = (JSONObject) JSON.parse(json);
//		MVContext mv = bisReportService.json2MV(obj, 0);
//		req.setAttribute("picinfo", picinfo);
//		CompPreviewService ser = new CompPreviewService(req, res, req.getServletContext());
//		ser.setParams(null);
////		ser.initPreview();
//
//		String fileName = "file.";
//		if("html".equals(type)){
//			fileName += "html";
//		}else
//		if("excel".equals(type)){
//			fileName += "xls";
//		}else
//		if("csv".equals(type)){
//			fileName += "csv";
//		}else
//		if("pdf".equals(type)){
//			fileName += "pdf";
//		}else
//		if("word".equals(type)){
//			fileName += "docx";
//		}
//
//		res.setContentType("application/x-msdownload");
//		String contentDisposition = "attachment; filename=\""+fileName+"\"";
//		res.setHeader("Content-Disposition", contentDisposition);
//
//		if("html".equals(type)){
//			String ret = ser.buildMV(mv, req.getServletContext());
//			String html = RSBIUtils.htmlPage(ret, RSBIUtils.getConstant("resPath"), "olap");
//			InputStream is = IOUtils.toInputStream(html, "utf-8");
//			IOUtils.copy(is, res.getOutputStream());
//			is.close();
//		}else
//		if("excel".equals(type)){
//			ContextEmitter emitter = new ExcelEmitter();
//			ser.buildMV(mv, emitter, req.getServletContext());
//		}else
//		if("csv".equals(type)){
//			ContextEmitter emitter = new TextEmitter();
//			String ret = ser.buildMV(mv, emitter, req.getServletContext());
//			InputStream is = IOUtils.toInputStream(ret, "gb2312");
//			IOUtils.copy(is, res.getOutputStream());
//			is.close();
//		}else
//		if("pdf".equals(type)){
//			ContextEmitter emitter = new PdfEmitter();
//			ser.buildMV(mv, emitter, req.getServletContext());
//		}else
//		if("word".equals(type)){
//			ContextEmitter emitter = new WordEmitter();
//			ser.buildMV(mv, emitter, req.getServletContext());
//		}
//
//		return null;
//	}
//
//	@RequestMapping(value="/kpidesc")
//	public String kpidesc(Integer cubeId, ModelMap model){
//		model.addAttribute("ls", service.listKpiDesc(cubeId));
//		return "bis/pages/bireport/DataSet-kpidesc";
//	}
//
//	@RequestMapping(value="/print", method = RequestMethod.POST)
//	public Object print(String pageInfo, HttpServletRequest req, HttpServletResponse res) throws Exception{
//		ExtContext.getInstance().removeMV(bisReportService.deftMvId);
//		JSONObject obj = (JSONObject) JSON.parse(pageInfo);
//		MVContext mv = bisReportService.json2MV(obj, 0);
//		CompPreviewService ser = new CompPreviewService(req, res, req.getServletContext());
//		ser.setParams(null);
////		ser.initPreview();
//
//		String ret = ser.buildMV(mv, req.getServletContext());
//		req.setAttribute("data", ret);
//
//		return "bis/pages/bireport/ReportDesign-print";
//	}
}
