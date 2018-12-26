
package Proxy;

import map.Relation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author chenshihong02
 * @version 1.0
 * @created 2018/12/23 下午6:40
 **/
public class RuntimeHandler implements InvocationHandler {

    //运行时对象
    private Object obj;

    public RuntimeHandler(Object obj) {
        this.obj = obj;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("hashCode")) {
            return obj.hashCode();
        }
        //获取运行时对象中type的值 来得到对应的底层设备名
        Field type = obj.getClass().getDeclaredField("type");
        type.setAccessible(true);
        String deviceTypeName = (String) type.get(obj);

        //运行时对象方法调用 映射到 底层设备的api
        for (String k : Relation.apiMaps.keySet()) {
            if (k.equals(method.getDeclaringClass().getName() + "." + method.getName())) {
                List<String> candidateMethods = Relation.apiMaps.get(k);
                for (int i = 0; i < candidateMethods.size(); i++) {
                    if (candidateMethods.get(i).contains(deviceTypeName)) {
                        Class deviceType = Class.forName(candidateMethods.get(i).substring(0, candidateMethods.get(i).lastIndexOf(".")));
                        Method deviceMethod = deviceType.getDeclaredMethod(candidateMethods.get(i).substring(candidateMethods.get(i).lastIndexOf(".") + 1));
                        //通过运行时对象代理得到底层设备对象
                        Object deviceObject = Relation.objMaps.get(proxy);
                        //调用底层设备对象
                        return deviceMethod.invoke(deviceObject);
                    }
                }
            }
        }

        return null;
    }
}