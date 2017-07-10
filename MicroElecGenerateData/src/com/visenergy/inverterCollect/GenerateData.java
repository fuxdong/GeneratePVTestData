package com.visenergy.inverterCollect;

import com.flying.jdbc.SqlHelper;
import com.flying.jdbc.data.CommandType;
import com.flying.jdbc.data.Parameter;
import com.flying.jdbc.db.type.BaseTypes;
import com.flying.jdbc.util.DBConnection;
import com.flying.jdbc.util.DBConnectionPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

/**
 * 
 * 生成关口电能数据
 * @author zdf
 */
public class GenerateData {
	private static Log log = LogFactory.getLog(GenerateData.class);
	static List<Map> list = new ArrayList<Map>();
	static double ELEC_PROD_HOUR = 0.00;
	static double ELEC_PROD_DAY = 0.00;
	static double ELEC_PROD_MONTH = 0.00;
	static double ELEC_PROD_YEAR = 0.00;
	static double ELEC_PROD_ALL = 0.00;
	static double CO2_CUTS = 0.00;
	static double COAL_SAVE = 0.00;

	static List inverter_id = new ArrayList();
	static List metero_time = new ArrayList();
	static Timestamp TIME = new Timestamp(System.currentTimeMillis()-190*24*3600*1000L-12*3600*1000L-11*60*1000L);
	public static void init(){
		//初始化数据库连接池
		SqlHelper.connPool = new DBConnectionPool(50);
		inverter_id.add("1820FB1857737B3BA33A8FEE25164C98");
		inverter_id.add("4D5F552ED8CC4EE981720CBDDB7FEAC6");
		inverter_id.add("6E5CEB1058E8A530A499DDC363A134C7");
		inverter_id.add("6F31267F133D958AA4865C81AEA23225");
		inverter_id.add("7CBE845153C74C3B798618B7F1F81D9D");
		inverter_id.add("977767AE5E946563CA859C0AA980FA39");
		inverter_id.add("AAA263543DC49259EBAED22960B4271F");
		inverter_id.add("C697C5B7684DFC9E10045763AD9721F3");
		inverter_id.add("F7E7FA8927BEF5E100B3E7B0D98A09CC");
		inverter_id.add("FB003754389C03E09BAC593D694102E8");
		inverter_id.add("FC8FC785AE2E7CF450BCFC876DABE6D8");
	}
	
	/**
	 * 逆变器采集随机生成数据
	 */
	public static void generateInverterCollectData(){
		String sql = "INSERT INTO T_PVMANAGE_INVERTER_COLLECT(ID,INVERTER_ID,ELEC_PROD_HOUR,ELEC_PROD_DAILY," +
				"ELEC_PROD_MONTH,ELEC_PROD_YEAR,ELEC_PROD_ALL,OUTPUT_P,CONNECT_P,PEAK_POWER,RATED_P,REACTIVE_P,DC_U," +
				"DC_I,AC_UA,AC_UB,AC_UC,AC_IA,AC_IB,AC_IC,MACHINE_TEMP,AMBIENT_TEMP,GRID_FRQ,CONVERT_EFF,CO2_CUTS," +
				"COAL_SAVE,CONVERT_BENF,CONNECT_STATUS,COMMUNICATE_STATUS,PV_CONNECT_STATUS,WARNING_STATUS,TIME) " +
				"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		DBConnection conn = SqlHelper.connPool.getConnection();
		Parameter[] params = new Parameter[32];

		for (int i=0;i<inverter_id.size();i++){
			String id = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
			if (list.size()/11 >= 1){
//				int size = list.size();
				TIME = new Timestamp(((Timestamp) list.get(i).get("TIME")).getTime() + 15*60*1000L);
				String time = TIME.toString();
				String time2 = list.get(i).get("TIME").toString();
				List<Integer> tt = SplitTime.getTime(time);
				List<Integer> tt2 = SplitTime.getTime(time2);
				int hours = Math.abs(tt.get(3)-tt2.get(3));
				int days = Math.abs(tt.get(2)-tt2.get(2)) ;
				int months = Math.abs(tt.get(1)-tt2.get(1));
				int years = Math.abs(tt.get(0)-tt2.get(0));
				double elec = 0.00;
				if (hours < 1){
					ELEC_PROD_HOUR = Double.parseDouble(list.get(i).get("ELEC_PROD_HOUR").toString()) +
							new BigDecimal(Math.random()+1).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
					elec = ELEC_PROD_HOUR - Double.parseDouble(list.get(i).get("ELEC_PROD_HOUR").toString());
					ELEC_PROD_ALL = Double.parseDouble(list.get(i).get("ELEC_PROD_ALL").toString()) + elec;
				}else {
					ELEC_PROD_HOUR = new BigDecimal(Math.random()+1).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
					ELEC_PROD_ALL = Double.parseDouble(list.get(i).get("ELEC_PROD_ALL").toString()) + ELEC_PROD_HOUR;
				}
				if (days < 1){
					if (hours < 1){
						ELEC_PROD_DAY = Double.parseDouble(list.get(i).get("ELEC_PROD_DAILY").toString()) + elec;
					}else {
						ELEC_PROD_DAY = Double.parseDouble(list.get(i).get("ELEC_PROD_DAILY").toString()) + ELEC_PROD_HOUR;
					}
				}else {
					ELEC_PROD_DAY = ELEC_PROD_HOUR;
				}
				if (months < 1){
					if (hours < 1){
						ELEC_PROD_MONTH = Double.parseDouble(list.get(i).get("ELEC_PROD_MONTH").toString()) + elec;
					}else {
						ELEC_PROD_MONTH = Double.parseDouble(list.get(i).get("ELEC_PROD_MONTH").toString()) + ELEC_PROD_HOUR;
					}
				}else {
					ELEC_PROD_MONTH = ELEC_PROD_HOUR;
				}
				if (years < 1){
					if (hours < 1){
						ELEC_PROD_YEAR = Double.parseDouble(list.get(i).get("ELEC_PROD_YEAR").toString()) + elec;
					}else {
						ELEC_PROD_YEAR = Double.parseDouble(list.get(i).get("ELEC_PROD_YEAR").toString()) + ELEC_PROD_HOUR;
					}
				}else {
					ELEC_PROD_YEAR = ELEC_PROD_HOUR;
				}

				CO2_CUTS = Double.parseDouble(list.get(i).get("CO2_CUTS").toString()) +
						new BigDecimal(Math.random()*5).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
				COAL_SAVE = Double.parseDouble(list.get(i).get("COAL_SAVE").toString()) +
						new BigDecimal(Math.random()*5).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();

			}else {
				ELEC_PROD_HOUR = 0.50;
				ELEC_PROD_DAY = 0.5;
				ELEC_PROD_MONTH = 0.5;
				ELEC_PROD_YEAR = 0.5;
				ELEC_PROD_ALL = 2000.00;
				CO2_CUTS = 10.00;
				COAL_SAVE = 10.00;
			}

			params[0] = new Parameter("ID", BaseTypes.VARCHAR,id);
			params[1] = new Parameter("INVERTER_ID", BaseTypes.VARCHAR,inverter_id.get(i));
			params[2] = new Parameter("ELEC_PROD_HOUR", BaseTypes.DECIMAL,ELEC_PROD_HOUR);
			params[3] = new Parameter("ELEC_PROD_DAILY", BaseTypes.DECIMAL,ELEC_PROD_DAY);
			params[4] = new Parameter("ELEC_PROD_MONTH", BaseTypes.DECIMAL,ELEC_PROD_MONTH);
			params[5] = new Parameter("ELEC_PROD_YEAR", BaseTypes.DECIMAL,ELEC_PROD_YEAR);
			params[6] = new Parameter("ELEC_PROD_ALL", BaseTypes.DECIMAL,ELEC_PROD_ALL);
			params[7] = new Parameter("OUTPUT_P", BaseTypes.DECIMAL,1000.00 + (new BigDecimal(Math.random()*100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[8] = new Parameter("CONNECT_P", BaseTypes.DECIMAL,1100.00 + (new BigDecimal(Math.random()*100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[9] = new Parameter("PEAK_POWER", BaseTypes.DECIMAL,1100.00 + (new BigDecimal(Math.random()*100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[10] = new Parameter("RATED_P", BaseTypes.DECIMAL,1300.00);
			params[11] = new Parameter("REACTIVE_P", BaseTypes.DECIMAL,50.00 + (new BigDecimal(Math.random()*5).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[12] = new Parameter("DC_U", BaseTypes.DECIMAL,50.00 + (new BigDecimal(Math.random()*5).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));

			params[13] = new Parameter("DC_I", BaseTypes.DECIMAL,5.00 + (new BigDecimal(Math.random()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[14] = new Parameter("AC_UA", BaseTypes.DECIMAL,100.00 + (new BigDecimal(Math.random()*5).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[15] = new Parameter("AC_UB", BaseTypes.DECIMAL,110.00 + (new BigDecimal(Math.random()*5).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[16] = new Parameter("AC_UC", BaseTypes.DECIMAL,120.00 + (new BigDecimal(Math.random()*5).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[17] = new Parameter("AC_IA", BaseTypes.DECIMAL,10.00 + (new BigDecimal(Math.random()*2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[18] = new Parameter("AC_IB", BaseTypes.DECIMAL,12.00 + (new BigDecimal(Math.random()*2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[19] = new Parameter("AC_IC", BaseTypes.DECIMAL,13.00 + (new BigDecimal(Math.random()*2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[20] = new Parameter("MACHINE_TEMP", BaseTypes.DECIMAL,(new BigDecimal(Math.random()*40).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[21] = new Parameter("AMBIENT_TEMP", BaseTypes.DECIMAL,(new BigDecimal(Math.random()*40).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[22] = new Parameter("GRID_FRQ", BaseTypes.INTEGER,(int)(Math.random()*10 + 50));

			params[23] = new Parameter("CONVERT_EFF", BaseTypes.DECIMAL,0.90 + (new BigDecimal(Math.random()*0.08).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[24] = new Parameter("CO2_CUTS", BaseTypes.DECIMAL,CO2_CUTS);
			params[25] = new Parameter("COAL_SAVE", BaseTypes.DECIMAL,COAL_SAVE);
			params[26] = new Parameter("CONVERT_BENF", BaseTypes.DECIMAL,120.00 + (new BigDecimal(Math.random()*5).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue()));
			params[27] = new Parameter("CONNECT_STATUS", BaseTypes.INTEGER,1);
			params[28] = new Parameter("COMMUNICATE_STATUS", BaseTypes.INTEGER,1);
			params[29] = new Parameter("PV_CONNECT_STATUS", BaseTypes.INTEGER,1);
			params[30] = new Parameter("WARNING_STATUS", BaseTypes.INTEGER,1);
			params[31] = new Parameter("TIME",BaseTypes.TIMESTAMP,TIME);

			Map map = new HashMap();
			//小时发电量
			map.put("ELEC_PROD_HOUR",params[2].Value);
			//日发电量
			map.put("ELEC_PROD_DAILY",params[3].Value);
			//月发电量
			map.put("ELEC_PROD_MONTH",params[4].Value);
			//年发电量
			map.put("ELEC_PROD_YEAR",params[5].Value);
			//总发电量
			map.put("ELEC_PROD_ALL",params[6].Value);
			//碳减排量
			map.put("CO2_CUTS",params[24].Value);
			//节约煤量
			map.put("COAL_SAVE",params[25].Value);
			//时间
			map.put("TIME",params[31].Value);
			list.add(map);

			try {
//			log.info("插入一条逆变器采集数据");
				SqlHelper.executeNonQuery(conn, CommandType.Text, sql, params);
//			waitForSendLog("T_PVMANAGE_INVERTER_COLLECT",id);
			} catch (Exception e) {
				conn.Close();
				log.error("逆变器采集数据插入异常",e);
			}
			if (list.size()%11==0 && list.size()>11){
				for (int j=0;j<11;j++){
					list.remove(0);
				}
			}
			SqlHelper.connPool.releaseConnection(conn);
		}


	}

	public static void generateMeteroData(){
		String sql = "INSERT INTO T_PVMANAGE_METERO(METERO_ID,SUN_STRENGTH,WIND_SPEED,WIND_DIREC,PANEL_TEMP," +
				"AMBIEN_TEMP,WIND_RATE,TIME) VALUES (?,?,?,?,?,?,?,?)";

		DBConnection conn = SqlHelper.connPool.getConnection();
		Parameter[] params = new Parameter[8];
		Timestamp m_time = new Timestamp(System.currentTimeMillis()-190*24*3600*1000L-12*3600*1000L-11*60*1000L);
		int size = metero_time.size();
		if (metero_time.size()>=1){
			m_time = new Timestamp(((Timestamp) metero_time.get(size-1)).getTime() + 15*60*1000L);
		}
		String id = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
		params[0] = new Parameter("METERO_ID", BaseTypes.VARCHAR,id);
		params[1] = new Parameter("SUN_STRENGTH", BaseTypes.DECIMAL,new BigDecimal(Math.random()*30).setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue());
		params[2] = new Parameter("WIND_SPEED", BaseTypes.DECIMAL,0.5+(new BigDecimal(Math.random()*2).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue()));
		params[3] = new Parameter("WIND_DIREC", BaseTypes.DECIMAL,new BigDecimal(Math.random()*20).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		params[4] = new Parameter("PANEL_TEMP", BaseTypes.DECIMAL,new BigDecimal(Math.random()*40).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		params[5] = new Parameter("AMBIEN_TEMP", BaseTypes.DECIMAL,new BigDecimal(Math.random()*40).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		params[6] = new Parameter("WIND_RATE", BaseTypes.INTEGER,(int)(Math.random()*9));
		params[7] = new Parameter("TIME", BaseTypes.TIMESTAMP,m_time);
		metero_time.add(m_time);
		if (size > 1){
			metero_time.remove(0);
		}
		try {
//			log.info("插入一条气象采集数据");
			SqlHelper.executeNonQuery(conn, CommandType.Text, sql, params);
//			waitForSendLog("T_PVMANAGE_METERO",id);
		} catch (Exception e) {
			conn.Close();
			log.error("气象采集数据插入异常",e);
		}
		SqlHelper.connPool.releaseConnection(conn);
	}



	public static void waitForSendLog(final String tableName,final String tableId){
		Thread t = new Thread(new Runnable(){
			@Override
			public void run(){
				DBConnection conn = SqlHelper.connPool.getConnection();
				try {
					String sql = "INSERT INTO T_PVMANAGE_LOG(TABLE_NAME,TABLE_ID) VALUES(?,?)";
					
					Parameter[] paramsLog = new Parameter[2];
					paramsLog[0] = new Parameter("TABLE_NAME", BaseTypes.VARCHAR,tableName);
					paramsLog[1] = new Parameter("TABLE_ID", BaseTypes.VARCHAR,tableId);
					
					log.info("插入一条日志");
					SqlHelper.executeNonQuery(conn, CommandType.Text, sql, paramsLog);
					
				} catch (Exception e) {
					log.error("["+tableName + " : "+ tableId + "]添加日志出错", e);
				}
				
				SqlHelper.connPool.releaseConnection(conn);
			}
		});
		
		t.start();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GenerateData.init();
		for (int i=0;i<35040;i++){
			//生成逆变器采集数据
			GenerateData.generateInverterCollectData();
			//生成气象仪采集数据
			GenerateData.generateMeteroData();
		}

//		Runnable runnable = new Runnable() {
//			public void run() {
//				for (int i=0;i<200000;i++){
//					//电能表校验
//					GenerateData.generateDnbjyData();
//				}
//
//			}
//		};
//		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
//		// 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
//		service.scheduleAtFixedRate(runnable, 0, 5, TimeUnit.SECONDS);
//		for (int i=0;i<10;i++){
//			GenerateData.generateDnbjyData();
//		}

	}
}
