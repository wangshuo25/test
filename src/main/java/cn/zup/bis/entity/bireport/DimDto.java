package cn.zup.bis.entity.bireport;

import cn.zup.bis.entity.common.BaseEntity;

public class DimDto {

	private Integer id;
	private String type;
	private String colname; //码表在事实表中对应的字段名
	private String alias; //别名
	private String vals; //码表的限制维
	private String valDesc; //码表限制维的名称

	public String getVals() {
		return vals;
	}

	public void setVals(String vals) {
		this.vals = vals;
	}

	public String getValDesc() {
		return valDesc;
	}

	public void setValDesc(String valDesc) {
		this.valDesc = valDesc;
	}

	//	private String issum; //y,n两值
	private String tname; //维度所在表name
//	private Integer calc;  //是否计算列
//	private String tableName; //维度码表表名
//	private String tableColKey; //码表表KEY字段
//	private String tableColName; //码表表name字段
//
	private String dimord; //维度排序方式
	private String ordcol; //维度排序字段
	private String dimdesc; //维度名称
	private String valType; //维度value 字段的类型，用在拼接sql中，判断是否增加单引号
	
//	private String dimpos; //维度所在位置，行维度还是列维度
//	private String pos; //col还是row, 用在图形中表示钻取维度的来源
//	private String dateformat; //如果是时间维度，设置时间类型
//	private String grouptype;
//	private String iscas;
//	private Integer top;
//	private String topType;
//	private String aggre;
//	private Integer filtertype;
//	private String startmt;
//	private String endmt;
//	private String startdt;
//	private String enddt;
//	private Integer cubeId;
//	private String xdispName;
//	private String tickInterval;
//	private String routeXaxisLable;


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getColname() {
		return colname;
	}

	public void setColname(String colname) {
		this.colname = colname;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getTname() {
		return tname;
	}

	public void setTname(String tname) {
		this.tname = tname;
	}

	public String getDimdesc() {
		return dimdesc;
	}

	public void setDimdesc(String dimdesc) {
		this.dimdesc = dimdesc;
	}

	public String getValType() {
		return valType;
	}

	public void setValType(String valType) {
		this.valType = valType;
	}

	public String getDimord() {
		return dimord;
	}

	public void setDimord(String dimord) {
		this.dimord = dimord;
	}

	public String getOrdcol() {
		return ordcol;
	}

	public void setOrdcol(String ordcol) {
		this.ordcol = ordcol;
	}

}
