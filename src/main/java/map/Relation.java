package map;

import Proxy.ProxyUtils;
import device.Gree;
import device.Panasonic;
import device.Midea;
import device.Opple;
import runtime.AirCondition;
import runtime.AirConditionImpl;
import runtime.Light;
import runtime.LightImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author chenshihong02
 * @version 1.0
 * @created 2018/12/23 下午6:50
 **/
public class Relation {

	// 类之间的映射 即 k - 底层设备类 v - 运行时设备类
	public static Map<String, String> classMaps = new HashMap<>();

	// 方法之间的映射 即 k - 运行时api v - 底层设备api
	public static Map<String, List<String>> apiMaps = new HashMap<>();

	// 底层设备对象与运行时对象之间的映射 k - 运行时对象 v - 底层设备对象
	public static Map<Object, Object> objMaps = new HashMap<>();

	/**
	 * 这边应该是读取配置文件得到映射关系 ，但我这边直接初始化映射关系
	 */
	public static void config() throws Exception {

		// 类之间的映射关系
		classMaps.put(Gree.class.getName(), AirConditionImpl.class.getName());
		classMaps.put(Panasonic.class.getName(), AirConditionImpl.class.getName());
		classMaps.put(Midea.class.getName(), LightImpl.class.getName());
		classMaps.put(Opple.class.getName(), LightImpl.class.getName());

		// 方法之间的映射关系
		apiMaps.put(AirCondition.class.getName() + "." + AirCondition.class.getMethod("cool").getName(),
				Arrays.asList(new String[] { Gree.class.getName() + "." + Gree.class.getMethod("cool").getName(),
						Panasonic.class.getName() + "." + Panasonic.class.getMethod("down").getName() }));

		apiMaps.put(Light.class.getName() + "." + Light.class.getMethod("illumine").getName(),
				Arrays.asList(new String[] {
						Midea.class.getName() + "." + Midea.class.getMethod("IncreaseLedBrightness").getName(),
						Opple.class.getName() + "." + Opple.class.getMethod("RaiseBrightness").getName() }));

		for (List<String> s : apiMaps.values()) {
			System.out.println(s);
		}

	}

	/**
	 * 通过配置文件生成底层设备，这边我就手动写设备
	 */
	public static void generateDeviceAndRuntime() throws Exception {
		// 底层设备生成 返回一个运行时对象
		AirCondition gree = (AirCondition) generate(Gree.class.getName());
		AirCondition panasonic = (AirCondition) generate(Panasonic.class.getName());

		Light midea = (Light) generate(Midea.class.getName());
		Light opple = (Light) generate(Opple.class.getName());

		// 运行时对象调用
		gree.cool();
		panasonic.cool();
//		
		midea.illumine();
		opple.illumine();
	}

	/**
	 * device 底层设备类名
	 * 
	 * @param device
	 */
	private static Object generate(String device) throws Exception {
		// 生成底层设备对象
		Object deviceObj = Class.forName(device).newInstance();
		// 通过类映射关系 获取到底层设备 对应的 运行时类
		for (String deviceType : classMaps.keySet()) {
			if (deviceType.equals(device)) {
				String runtimeType = classMaps.get(deviceType);
				Class<?> runtimeClass = Class.forName(runtimeType);
				// 生成运行时对象
				Object runtimeObj = runtimeClass.newInstance();
				// 将运行时对象的类型设置成底层设备的类型
				Field type = runtimeClass.getDeclaredField("type");

				Method[] methods = runtimeClass.getDeclaredMethods();
				String functionType = "";
				//寻找当前设备的方法，使用正则匹配
				for (Method m : methods) {
					String temp = m.toString().replaceAll("public void runtime.", "").replaceAll("()", "");
					String pattern = "(\\D*)(\\.)(\\D*)(\\()";
					Pattern r = Pattern.compile(pattern);
					Matcher matcher = r.matcher(temp);
					if (matcher.find())
						functionType = matcher.group(3);

				}

//				System.out.println(functionType);

				Method method = runtimeClass.getDeclaredMethod(functionType);

				type.setAccessible(true);
				type.set(runtimeObj, deviceType);

				// 生成运行时对象的代理对象
				Object proxyObj = ProxyUtils.getProxy(runtimeObj);
				// 将运行时对象代理与底层设备对象放入objMaps
				objMaps.put(proxyObj, deviceObj);
				return proxyObj;
			}
		}
		return null;
	}

	/**
	 * 测试
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		config();
		generateDeviceAndRuntime();
	}
}