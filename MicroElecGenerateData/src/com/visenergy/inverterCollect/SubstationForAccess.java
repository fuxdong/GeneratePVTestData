package com.visenergy.inverterCollect;


import com.visenergy.inverterCollect.exception.ServerFailException;
import com.visenergy.inverterCollect.po.StaticVariable;
import com.visenergy.inverterCollect.util.EncryptUtil;
import com.visenergy.inverterCollect.util.HttpURLConnectionUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.net.ConnectException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class SubstationForAccess {
	private static Log log = LogFactory.getLog(SubstationForAccess.class);
	//系统令牌加密密钥
	private static String KEY = "49C96E39BAEA44E39761707CD347E303";
	//传入数据key-value
	public static Map<String,String> KEY_MAP = new HashMap();
	//传入数据类型key-value
	public static Map<String,String> TYPE_MAP = new HashMap();
	
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
				SubstationForAccess.KEY, 1));
		String result = HttpURLConnectionUtil.doPost(getServerURL(), paramMap,
				"UTF-8", false);
		JSONObject jsonResult = JSONObject.fromObject(result);

		log.info(jsonResult.getString("msg"));
		return jsonResult.getBoolean("success");
	}

	/**
	 * 检查本地能否访问本地access文件
	 * @return true：可以，false：不可以
	 */
	public static boolean checkAccessAddr(){
		File accessFile = new File(StaticVariable.CLIENT_ACCESSADDR);
		if(!accessFile.exists()){
			log.error("access文件路径："+StaticVariable.CLIENT_ACCESSADDR+"找不到！");
			return false;
		}else{
			log.info("access文件存在");
			return true;
		}
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
			
			List itemList = dataTemplateDocument.selectNodes("/dataTemplate/item");
			// 遍历
			Iterator itemIter = itemList.iterator();
			while (itemIter.hasNext()) {
				Element itemElement = (Element) itemIter.next();
				if(itemElement.attribute("clientName") != null && itemElement.attribute("clientName").getValue() != null && !"".equals(itemElement.attribute("clientName").getValue())){
					SubstationForAccess.KEY_MAP.put(itemElement.attribute("clientName").getValue(), itemElement.attribute("serverName").getValue());
					
					SubstationForAccess.TYPE_MAP.put(itemElement.attribute("clientName").getValue(), itemElement.attribute("type").getValue());
				}
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 读取access数据文件
	 * @throws Exception
	 */
	public static void connectionAccess() throws Exception{
		Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
		String dbur1 = "jdbc:odbc:driver={Microsoft Access Driver (*.mdb)};DBQ="+StaticVariable.CLIENT_ACCESSADDR;
		Connection conn = DriverManager.getConnection(dbur1);
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select * from CheckRecord_Timing");
		while (rs.next()) {
			Map serverMap = new HashMap();
			Iterator clientKeyIterator = SubstationForAccess.KEY_MAP.keySet().iterator();
			while(clientKeyIterator.hasNext()){
				String clientKeyName = (String)clientKeyIterator.next();
				
				String typeName = SubstationForAccess.TYPE_MAP.get(clientKeyName);
				
				if("VARCHAR".equals(typeName)){
					serverMap.put(SubstationForAccess.KEY_MAP.get(clientKeyName), new String(rs.getBytes(clientKeyName),"GBK"));
				}else if("INT".equals(typeName)){
					serverMap.put(SubstationForAccess.KEY_MAP.get(clientKeyName), rs.getInt(clientKeyName));
				}else if("FLOAT".equals(typeName)){
					serverMap.put(SubstationForAccess.KEY_MAP.get(clientKeyName),rs.getFloat(clientKeyName));
				}
			}
			JSONObject jsonObject = JSONObject.fromObject(serverMap);
			System.out.println(jsonObject);
		}
		rs.close();
		stmt.close();
		conn.close();
	}
	
	/**
	 * 数据发送流程
	 */
	public static void sendData(){
        BufferedReader reader = null;
        try {
        	/***********读取时间戳文件开始**********/
            log.info("以行为单位读取文件内容，一次读一整行：");
            reader = new BufferedReader(new FileReader(StaticVariable.CLIENT_TIMESTAMPADDR));
            
            StringBuffer timestampBuffer = new StringBuffer();
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
            	timestampBuffer.append(tempString);
                line++;
            }
            reader.close();
            /***********读取时间戳文件结束**********/
            
            /***********根据时间戳查询数据库开始**********/
            JSONObject timestampJson = JSONObject.fromObject(timestampBuffer.toString());
            int id = timestampJson.getInt("id");
            
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
    		String dbur1 = "jdbc:odbc:driver={Microsoft Access Driver (*.mdb)};DBQ="+StaticVariable.CLIENT_ACCESSADDR;
    		Connection conn = DriverManager.getConnection(dbur1);
    		Statement stmt = conn.createStatement();
    		ResultSet rs = stmt.executeQuery("select * from CheckRecord_Timing where id > " + id);
    		
    		List<Map> resultList = new ArrayList<Map>();
    		int timestampId = id;
    		while (rs.next()) {
    			//获取时间戳ID
    			int idtemp = rs.getInt("ID");
    			if(idtemp > timestampId){
    				timestampId = idtemp;
    			}
    			//获取需要上传的数据
    			Map serverMap = new HashMap();
    			Iterator clientKeyIterator = SubstationForAccess.KEY_MAP.keySet().iterator();
    			while(clientKeyIterator.hasNext()){
    				String clientKeyName = (String)clientKeyIterator.next();
    				
    				String typeName = SubstationForAccess.TYPE_MAP.get(clientKeyName);
    				
    				if("VARCHAR".equals(typeName)){
    					serverMap.put(SubstationForAccess.KEY_MAP.get(clientKeyName), new String(rs.getBytes(clientKeyName),"GBK"));
    				}else if("INT".equals(typeName)){
    					serverMap.put(SubstationForAccess.KEY_MAP.get(clientKeyName), rs.getInt(clientKeyName));
    				}else if("FLOAT".equals(typeName)){
    					serverMap.put(SubstationForAccess.KEY_MAP.get(clientKeyName),rs.getFloat(clientKeyName));
    				}
    			}
    			resultList.add(serverMap);
    		}
    		rs.close();
    		stmt.close();
    		conn.close();
            
            /***********根据时间戳查询数据库结束**********/
            
    		/***********向服务器发送数据开始**********/
    		Map paramMap = new HashMap();
    		paramMap.put("command", "GK.sendData");
    		paramMap.put("data", JSONArray.fromObject(resultList).toString());
    		paramMap.put("token", EncryptUtil.encrypt(StaticVariable.CLIENT_CODE,
    				SubstationForAccess.KEY, 1));
    		String result = HttpURLConnectionUtil.doPost(getServerURL(), paramMap,
    				"UTF-8", false);
    		if("".equals(result)){
    			throw new ConnectException("网络异常");
    		}
    		JSONObject jsonResult = JSONObject.fromObject(result);	
    		if(jsonResult.getBoolean("success")){
    			timestampJson.put("id", timestampId);
    			timestampJson.put("time", SimpleDateFormat.getDateTimeInstance().format(new Date()));
    			
    			//TODO jsonResult.get("mark"),当服务器端发现数据模板版本有更新，需要向下传递更新数据的消息
    		}else{
    			throw new ServerFailException(jsonResult.getString("msg"));
    		}
    		/***********向服务器发送数据结束**********/
    		
    		/***********将时间戳写回文件开始**********/
    		FileWriter fw = new FileWriter(StaticVariable.CLIENT_TIMESTAMPADDR);
    	    PrintWriter out = new PrintWriter(fw);
    	    out.write(timestampJson.toString());
    	    out.println();
    	    fw.close();
    	    out.close();
    		/***********将时间戳写回文件结束**********/
        } catch (ConnectException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (ServerFailException e) {
			e.printStackTrace();
		} finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//检查
		checkConnection();
		checkAccessAddr();
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
		// 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
		service.scheduleAtFixedRate(runnable, 0, StaticVariable.SERVER_UPLOADTIME, TimeUnit.SECONDS);
	}
}
