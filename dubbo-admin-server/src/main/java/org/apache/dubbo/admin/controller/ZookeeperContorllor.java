package org.apache.dubbo.admin.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.apache.dubbo.admin.common.CommonRequest;
import org.apache.dubbo.admin.common.CommonResponse;
import org.apache.dubbo.admin.model.domain.Node;
import org.apache.dubbo.admin.service.ZookeeperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author heyudev
 * @date 2019/05/31
 */
@RestController
@RequestMapping("/{env}/zk")
public class ZookeeperContorllor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperContorllor.class);

    @Autowired
    private ZookeeperService zookeeperService;

    /**
     * @param commonRequest
     * @return
     */
    @PostMapping(value = "/info")
    public CommonResponse info(@RequestBody CommonRequest commonRequest) {
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(zookeeperService.getZookeeperInfo(String.valueOf(commonRequest.get("address"))));
        return commonResponse;
    }

    /**
     * @param commonRequest
     * @return
     */
    @ApiOperation(value = "获取zookeeper节点信息", notes = "获取zookeeper节点信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "parent", value = "上级目录,根目录为/", required = true, dataType = "string"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })
    @PostMapping(value = "/children")
    public CommonResponse children(@RequestBody CommonRequest commonRequest) {
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        try {
            List<Node> nodeList = zookeeperService.getChildren(String.valueOf(commonRequest.get("registryAddress")), String.valueOf(commonRequest.get("parent")));
            commonResponse.setData(nodeList);
        } catch (Exception e) {
            LOGGER.error("getChildren error ", e);
            commonResponse.fail("此节点数量过多，无法展示！");
        }
        return commonResponse;
    }

    /**
     * @param commonRequest
     * @return
     */
    @ApiOperation(value = "获取zookeeper节点数据", notes = "获取zookeeper节点数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "path", value = "节点路径", required = true, dataType = "string"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })
    @PostMapping(value = "/data")
    public CommonResponse data(@RequestBody CommonRequest commonRequest) {
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        try {
            String data = zookeeperService.getData(String.valueOf(commonRequest.get("registryAddress")), String.valueOf(commonRequest.get("path")));
            commonResponse.setData(data);
        } catch (Exception e) {
            commonResponse.fail(e.getMessage());
        }
        return commonResponse;
    }

    /**
     * @param commonRequest
     * @return
     */
    @ApiOperation(value = "获取依赖关系图", notes = "获取依赖关系图")
    @ApiImplicitParam(name = "commonRequest", value = "包含参数：registryAddress，注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "CommonRequest")
    @PostMapping(value = "/appsDependencies")
    public CommonResponse appsDependencies(@RequestBody CommonRequest commonRequest) {
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(zookeeperService.appsDependencies(String.valueOf(commonRequest.get("registryAddress"))));
        return commonResponse;
    }


}
