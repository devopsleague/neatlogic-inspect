/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.configfile;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectConfigFilePathVo;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.inspect.dao.mapper.InspectConfigFileMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListInspectConfigFileResourceApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;
    @Resource
    private AutoexecJobMapper autoexecJobMapper;
    @Override
    public String getToken() {
        return "inspect/configfile/resource/list";
    }

    @Override
    public String getName() {
        return "巡检配置文件资源列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "id列表，用于刷新状态时精确匹配数据"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "协议id列表"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "状态id列表"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "inspectJobPhaseNodeStatusList", type = ApiParamType.JSONARRAY, desc = "巡检作业状态列表"),
            @Param(name = "searchField", type = ApiParamType.STRING, desc = "批量搜索字段"),
            @Param(name = "batchSearchList", type = ApiParamType.JSONARRAY, desc = "批量搜索值"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = InspectResourceVo[].class, desc = "数据列表")
    })
    @Description(desc = "巡检配置文件资源列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<InspectResourceVo> inspectResourceList = new ArrayList<>();
        ResourceSearchVo searchVo = JSONObject.toJavaObject(paramObj, ResourceSearchVo.class);
        if (CollectionUtils.isNotEmpty(searchVo.getIdList())) {
            List<Long> idList = searchVo.getIdList();
            inspectResourceList = inspectConfigFileMapper.getInspectResourceListByIdList(idList);
            List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeList = autoexecJobMapper.getAutoexecJobNodeListByResourceIdList(idList);
            Map<Long, AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeMap = autoexecJobPhaseNodeList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
            for (InspectResourceVo inspectResourceVo : inspectResourceList) {
                Long id = inspectResourceVo.getId();
                AutoexecJobPhaseNodeVo autoexecJobPhaseNodeVo = autoexecJobPhaseNodeMap.get(id);
                if (autoexecJobPhaseNodeVo != null) {
                    inspectResourceVo.setJobPhaseNodeVo(autoexecJobPhaseNodeVo);
                }
            }
        } else {
            Long typeId = searchVo.getTypeId();
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            CiVo ciVo = ciCrossoverMapper.getCiById(typeId);
            if (ciVo == null) {
                throw new CiNotFoundException(typeId);
            }
            searchVo.setLft(ciVo.getLft());
            searchVo.setRht(ciVo.getRht());
            int count = inspectConfigFileMapper.getInspectResourceCount(searchVo);
            if (count > 0) {
                searchVo.setRowNum(count);
                List<Long> idList = inspectConfigFileMapper.getInspectResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
                    Map<Long, List<TagVo>> tagMap = resourceCenterResourceCrossoverService.getResourceTagByResourceIdList(idList);
                    List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeList = autoexecJobMapper.getAutoexecJobNodeListByResourceIdList(idList);
                    Map<Long, AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeMap = autoexecJobPhaseNodeList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
                    inspectResourceList = inspectConfigFileMapper.getInspectResourceListByIdList(idList);
                    List<InspectConfigFilePathVo> inspectConfigFilePathList = inspectConfigFileMapper.getInspectConfigFileLastChangeTimeListByResourceIdList(idList);
                    Map<Long, InspectConfigFilePathVo> inspectConfigFilePathMap = inspectConfigFilePathList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
                    for (InspectResourceVo inspectResourceVo : inspectResourceList) {
                        Long id = inspectResourceVo.getId();
                        InspectConfigFilePathVo inspectConfigFilePathVo = inspectConfigFilePathMap.get(id);
                        if (inspectConfigFilePathVo != null) {
                            inspectResourceVo.setLastChangeTime(inspectConfigFilePathVo.getInspectTime());
                        }
                        List<TagVo> tagList = tagMap.get(id);
                        if (CollectionUtils.isNotEmpty(tagList)) {
                            inspectResourceVo.setTagList(tagList.stream().map(TagVo::getName).collect(Collectors.toList()));
                        }
                        AutoexecJobPhaseNodeVo autoexecJobPhaseNodeVo = autoexecJobPhaseNodeMap.get(id);
                        if (autoexecJobPhaseNodeVo != null) {
                            inspectResourceVo.setJobPhaseNodeVo(autoexecJobPhaseNodeVo);
                        }
                    }
                    //排序
                    List<InspectResourceVo> resultList = new ArrayList<>();
                    for (Long id : idList) {
                        for (InspectResourceVo inspectResourceVo : inspectResourceList) {
                            if (Objects.equals(id, inspectResourceVo.getId())) {
                                resultList.add(inspectResourceVo);
                                break;
                            }
                        }
                    }
                    inspectResourceList = resultList;
                }
            }
        }
        return TableResultUtil.getResult(inspectResourceList, searchVo);
    }
}
