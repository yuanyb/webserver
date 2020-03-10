package org.webserver.container;

import org.junit.Assert;
import org.junit.Test;
import org.webserver.exception.InternalServerException;
import org.webserver.http.request.HttpRequest;

import java.util.*;



public class TargetMethodTest {

    @Test
    public void buildBeanFromRequest() throws InternalServerException, NoSuchMethodException {
//        HttpRequest request = new HttpRequest();
//        Map<String, List<String>> params = new HashMap<>();
//        params.put("user.id", Collections.singletonList("1001"));
//        params.put("user.name", Collections.singletonList("Alice"));
//        params.put("user.info.val", Collections.singletonList("ok"));
//        request.setParams(params); // 临时开放权限
//        User user = TargetMethod.buildBeanFromRequest(request, User.class, "user.");
//        Assert.assertEquals("id错误", Integer.valueOf(1001), user.getId());
//        Assert.assertEquals("name错误", "Alice", user.getName());
//        Assert.assertEquals("info.val错误", "ok", user.getInfo().getVal());
    }
}