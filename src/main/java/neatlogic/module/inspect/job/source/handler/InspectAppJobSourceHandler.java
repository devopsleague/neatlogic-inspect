/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.inspect.job.source.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.job.AutoexecJobRouteVo;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AppSystemVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.constvalue.JobSource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InspectAppJobSourceHandler implements IAutoexecJobSource {

    @Override
    public String getValue() {
        return JobSource.INSPECT_APP.getValue();
    }

    @Override
    public String getText() {
        return JobSource.INSPECT_APP.getText();
    }

    @Override
    public List<AutoexecJobRouteVo> getListByUniqueKeyList(List<String> uniqueKeyList) {
        if (CollectionUtils.isEmpty(uniqueKeyList)) {
            return null;
        }
        List<Long> idList = new ArrayList<>();
        for (String str : uniqueKeyList) {
            idList.add(Long.valueOf(str));
        }
        List<AutoexecJobRouteVo> resultList = new ArrayList<>();
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        List<AppSystemVo> list = resourceCrossoverMapper.getAppSystemListByIdList(idList);
        for (AppSystemVo appSystemVo : list) {
            JSONObject config = new JSONObject();
            config.put("id", appSystemVo.getId());
            String label = "";
            if (appSystemVo != null) {
                if (StringUtils.isNotBlank(appSystemVo.getAbbrName()) && StringUtils.isNotBlank(appSystemVo.getName())) {
                    label = appSystemVo.getAbbrName() + "(" + appSystemVo.getName() + ")";
                } else if (StringUtils.isNotBlank(appSystemVo.getAbbrName())) {
                    label = appSystemVo.getAbbrName();
                } else if (StringUtils.isNotBlank(appSystemVo.getName())) {
                    label = appSystemVo.getName();
                }
            }
            resultList.add(new AutoexecJobRouteVo(appSystemVo.getId(), label, config));
        }
        return resultList;
    }
}
