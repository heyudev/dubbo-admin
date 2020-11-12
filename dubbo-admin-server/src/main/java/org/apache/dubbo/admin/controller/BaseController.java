package org.apache.dubbo.admin.controller;

import org.apache.dubbo.admin.common.exception.ParamValidationException;
import org.apache.dubbo.admin.common.request.BaseRequest;

/**
 * @author heyudev
 * @date 2019/07/05
 */
public abstract class BaseController {

    public void validateRegistryAddress(String registryAddress) {
        if (registryAddress == null || "".equals(registryAddress)) {
            throw new ParamValidationException("registryAddress is required");
        }
    }

    public void validateService(String service) {
        if (service == null || "".equals(service)) {
            throw new ParamValidationException("service is required");
        }
    }

    public void validateId(String id) {
        if (id == null || "".equals(id)) {
            throw new ParamValidationException("id is required");
        }
    }

    public void validateParameter(Object parameter) {
        if (parameter == null) {
            throw new ParamValidationException("parameter is null");
        }
    }

    public void validateBaseRequest(BaseRequest baseRequest) {
        if (baseRequest == null) {
            throw new ParamValidationException("parameter is null");
        }
        validateRegistryAddress(baseRequest.getRegistryAddress());
        validateId(baseRequest.getId());
    }
}
