package com.visenergy.inverterCollect;

import com.flying.jdbc.SqlHelper;
import com.flying.jdbc.data.CommandType;
import com.flying.jdbc.util.DBConnection;
import com.visenergy.inverterCollect.exception.ServerFailException;
import com.visenergy.inverterCollect.po.StaticVariable;
import com.visenergy.inverterCollect.util.EncryptUtil;
import com.visenergy.inverterCollect.util.FlyingUtil;
import com.visenergy.inverterCollect.util.HttpURLConnectionUtil;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class SubstationForMysql {
	private static Log log = LogFactory.getLog(SubstationForMysql.class);
	//系统令牌加密密钥
	private static String KEY = "49C96E39BAEA44E39761707CD347E303";
	//需要传输的对象集合
	public static Map ITEMS_MAP = new HashMap();
	
	/**
	 * @return 返回默认服务器请求路径
	 */
	private static String getServerURL() {
		return "http://" + StaticVariable.SERVER_IP + ":"
				+ StaticVariable.SERVER_PORT + "/" + StaticVariable.SERVER_NAME
				+ "/common.action";
	}

	/**
	 * 检查与服务器之间的连接是否有效
	 * @return true：连接成功，false：连接失败
	 */
	public static boolean checkConnection() {
		Map paramMap = new HashMap();
		paramMap.put("command", "GK.connectionTest");
		paramMap.put("token", EncryptUtil.encrypt(StaticVariable.CLIENT_CODE,
				SubstationForMysql.KEY, 1));
		String result = HttpURLConnectionUtil.doPost(getServerURL(), paramMap,
				"UTF-8", false);
		JSONObject jsonResult = JSONObject.fromObject(result);

		log.info(jsonResult.getString("msg"));
		return jsonResult.getBoolean("success");
	}
	
	/**
	 * 检查能否访问本地数据模板文件
	 * @return true：可以，false：不可以
	 */
	public static boolean checkDataTemplate(){
		File accessFile = new File(StaticVariable.CLIENT_DATATEMPLATEADDR);
		if(!accessFile.exists()){
			log.error("模板文件路径："+StaticVariable.CLIENT_DATATEMPLATEADDR+"找不到！");
			return false;
		}else{
			log.info("模板文件存在");
			return true;
		}
	}
	
	/**
	 * 读取本地模板文件
	 * @throws DocumentException
	 */
	public static void readDataTemplate(){
		SAXReader dataTemplateReader = new SAXReader();
		Document dataTemplateDocument = null;
		try {
			dataTemplateDocument = dataTemplateReader.read(new File(StaticVariable.CLIENT_DATATEMPLATEADDR));
			
			List itemsList = dataTemplateDocument.selectNodes("/dataTemplate/items");
			//解析配置文件
			Iterator itemsIter = itemsList.iterator();
			while (itemsIter.hasNext()) {
				Element itemsElement = (Element) itemsIter.next();
				//dataTemplate.xml配置文件中，items项，clientTable不为空
				if(FlyingUtil.validateData(itemsElement.attribute("clientTable")) && FlyingUtil.validateData(itemsElement.attribute("clientTable").getValue())){
					String clientTableKey = itemsElement.attribute("clientTable").getValue().trim();
					//表属性集合
					Map tablePropertyMap = new HashMap();
					//服务器端表名
					String serverTable = clientTableKey;
					//dataTemplate.xml配置文件中，items项，serverTable为空，则传递serverTable值
					if(FlyingUtil.validateData(itemsElement.attribute("serverTable")) && FlyingUtil.validateData(itemsElement.attribute("serverTable").getValue())){
						serverTable = itemsElement.attribute("serverTable").getValue().trim();
					}
					tablePropertyMap.put("serverTable", serverTable);
					
					//服务器端表的ID名
					String serverTableIdName = "ID";
					if(FlyingUtil.validateData(itemsElement.attribute("serverTableIdName")) && FlyingUtil.validateData(itemsElement.attribute("serverTableIdName").getValue())){
						serverTableIdName = itemsElement.attribute("serverTableIdName").getValue().trim();
					}
					tablePropertyMap.put("serverTableIdName", serverTableIdName);
					
					//操作Items节点下的item
					List<Element> subItemElementList = (List<Element>) itemsElement.elements("item");
					//遍历item节点
					for(Iterator it=subItemElementList.iterator();it.hasNext();){
						Element subItemElement = (Element) it.next();
						
						//dataTemplate.xml配置文件中，item项，clientName不为空，为空项丢弃
						if(FlyingUtil.validateData(subItemElement.attribute("clientName")) && FlyingUtil.validateData(subItemElement.attribute("clientName").getValue())){
							String clientName = subItemElement.attribute("clientName").getValue().trim();
							String aliasName = clientName;
							
							//dataTemplate.xml配置文件中，item项，aliasName为空，则传递clientName值
							if(FlyingUtil.validateData(subItemElement.attribute("aliasName")) && FlyingUtil.validateData(subItemElement.attribute("aliasName").getValue())){
								aliasName = subItemElement.attribute("aliasName").getValue().trim();
							}
							tablePropertyMap.put(clientName, aliasName);
						}
					}
					//构建ITEMS_MAP对象
					SubstationForMysql.ITEMS_MAP.put(clientTableKey, tablePropertyMap);
				}else{
					log.warn("解析的dataTemplate的items的clientTable为空 ！");
					continue;
				}
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 数据发送流程
	 */
	public static void sendData(){
		DBConnection conn = SqlHelper.connPool.getConnection();
		String sql = "SELECT * FROM T_YCJY_WAITFORSEND_LOG LIMIT 0,100";
		
		log.info("查询表中的数据");
		List<Map> resultList =  null;
		try {
			resultList = SqlHelper.executeQuery(conn, CommandType.Text, sql);
		} catch (Exception e) {
			log.error("查询待传输数据出错！",e);
			return;
		}
		
		for(int i =0;i<resultList.size();i++){
			Map resultMap = resultList.get(i);
			String tableName = resultMap.get("TABLE_NAME") == null ? "" : resultMap.get("TABLE_NAME").toString();
			
			//根据tableName，判断是否为需要传输的数据项
			if(SubstationForMysql.ITEMS_MAP.get(tableName) != null){
				String tableId = resultMap.get("TABLE_ID") == null ? "" : resultMap.get("TABLE_ID").toString();
				
				if(!("".equals(tableName) || "".equals(tableId))){
					sql = "SELECT * FROM "+ tableName +" WHERE ID = '"+ tableId +"'";
					
					List<Map> sendDataList = null;
					try {
						sendDataList = SqlHelper.executeQuery(conn, CommandType.Text, sql);
					} catch (Exception e) {
						log.error("根据tableName，tableId查询传输数据出错！",e);
						continue;
					}
					
					/***********向服务器发送数据开始**********/
					if(sendDataList.size() > 0){
						Map oldSendDataMap = sendDataList.get(0);//原始发送数据
						Map newSendDataMap = new HashMap();//新发送数据
						
						//遍历得到目标发送数据
						Map tableTemplateMap = (Map)SubstationForMysql.ITEMS_MAP.get(tableName);
						
						Iterator<String> tableTemplateIter = tableTemplateMap.keySet().iterator();
						
						while(tableTemplateIter.hasNext()){
							String name = tableTemplateIter.next();
							
							//构建新的传输数据
							if(oldSendDataMap.get(name) != null){
								//对日前格式进行处理，编程字符串格式，方便网络传输
								if(oldSendDataMap.get(name) instanceof Date){
									SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
									newSendDataMap.put(tableTemplateMap.get(name), sdf.format(oldSendDataMap.get(name)));
								}else{
									newSendDataMap.put(tableTemplateMap.get(name), oldSendDataMap.get(name));
								}
							}
						}
						
			    		Map paramMap = new HashMap();
			    		paramMap.put("command", "GK.sendData");
			    		paramMap.put("clientTable", tableName);
			    		paramMap.put("serverTable", tableTemplateMap.get("serverTable"));
			    		paramMap.put("tableId", tableId);
			    		paramMap.put("serverTableIdName", tableTemplateMap.get("serverTableIdName"));
			    		paramMap.put("data", JSONObject.fromObject(newSendDataMap));
			    		paramMap.put("token", EncryptUtil.encrypt(StaticVariable.CLIENT_CODE,SubstationForMysql.KEY, 1));
			    		String result = HttpURLConnectionUtil.doPost(getServerURL(), paramMap,"UTF-8", false);
			    		
			    		if("".equals(result)){
			    			log.error(new ConnectException("网络异常"));
			    		}
			    		//发送数据成功，更新本地数据
			    		JSONObject jsonResult = JSONObject.fromObject(result);	
			    		if(jsonResult.getBoolean("success")){
			    			//更新分站数据
			    			sql = "DELETE FROM T_YCJY_WAITFORSEND_LOG WHERE TABLE_ID = '"+ tableId +"'";
			    			try {
								SqlHelper.executeNonQuery(conn, CommandType.Text, sql);
							} catch (Exception e) {
								log.error("删除传输成功的数据出错！",e);
							}
			    			
			    			//TODO jsonResult.get("mark"),当服务器端发现数据模板版本有更新，需要向下传递更新数据的消息
			    		}else{
			    			log.error(new ServerFailException(jsonResult.getString("msg")));
			    		}
					}
					/***********向服务器发送数据结束**********/
				}
			}
		}
		
		SqlHelper.connPool.releaseConnection(conn);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//初始化
		GenerateData.init();
		//检查
		checkConnection();
		checkDataTemplate();
		//读取数据模板
		readDataTemplate();
		//循环发送数据
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					//发送数据
					sendData();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		// 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间 IME, TimeUnit.SECONDS);
	}
}
