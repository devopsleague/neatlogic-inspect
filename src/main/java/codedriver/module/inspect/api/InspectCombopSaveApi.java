package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class InspectCombopSaveApi extends PrivateApiComponentBase {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "保存模型和组合工具的关系";
    }

    @Override
    public String getToken() {
        return "inspect/combop/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "inspectCombopList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "集合和组合工具关系列表")})
    @Description(desc = "保存巡检规则接口，用于巡检模块的巡检工具保存，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray inspectCombopList = paramObj.getJSONArray("inspectCombopList");
        if (inspectCombopList == null) {
            return null;
        }
        for (int i = 0; i < inspectCombopList.size(); i++) {
            JSONObject inspectCombop = inspectCombopList.getJSONObject(i);
            String name = inspectCombop.getString("name");
            Long combopId =inspectCombop.getLong("combop_id");
            Document whereDoc = new Document();
            Document updateDoc = new Document();
            Document setDocument = new Document();
            whereDoc.put("name", name);
            updateDoc.put("combop_id", combopId);
            setDocument.put("$set", updateDoc);
            mongoTemplate.getCollection("_inspectdef").updateOne(whereDoc, setDocument);
        }
        return null;
    }
}
