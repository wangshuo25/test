package cn.zup.bis.dao.model;

import cn.zup.bis.entity.model.Measure;
import org.springframework.stereotype.Repository;

@Repository

public interface MeasureDao {

	void insertKpi(Measure kpi);
	
	void updateKpi(Measure kpi);
	
	void deleteKpi(Measure kpi);
	
	int getMaxKpiId();
}
