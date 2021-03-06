import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Row
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}

import scala.collection.mutable.ArrayBuffer

object AssessDataETL {
  private  val sparkSession = SparkSession.builder().appName("AssessDataETL").enableHiveSupport().getOrCreate()
  private  val sc = sparkSession.sparkContext
  private  val Pfieldtag = Array(
    "sum(ACT_REGISTER_NUM_L15D)",
    "sum(ACT_REGISTER_NUM_L30D)",
    "sum(ACT_REGISTER_NUM_L90D)",
    "sum(DLVR_IN_24H_NUM_L15D)",
    "sum(DLVR_IN_24H_NUM_L30D)",
    "sum(DLVR_IN_24H_NUM_L90D)",
    "sum(VEND_RPLY_RTN_IN_12H_L15D)",
    "sum(VEND_RPLY_RTN_IN_12H_L30D)",
    "sum(VEND_RPLY_RTN_IN_12H_L90D)",
    "sum(VEND_RPLY_COMPLAINT_IN_24H_L15D)",
    "sum(VEND_RPLY_COMPLAINT_IN_24H_L30D)",
    "sum(VEND_RPLY_COMPLAINT_IN_24H_L90D)",
    "sum(ORDR_GOOD_CMT_NUM_L15D)",
    "sum(ORDR_GOOD_CMT_NUM_L30D)",
    "sum(ORDR_GOOD_CMT_NUM_L90D)",
    "sum(SALES_RTN_RT_L15D)",
    "sum(SALES_RTN_RT_L30D)",
    "sum(SALES_RTN_RT_L90D)",
    "sum(DLY_SALES_L15D)",
    "sum(DLY_SALES_L30D)",
    "sum(DLY_SALES_L90D)",
    "sum(COMMISSION_HIGH_GOODS_NUM_L15D)",
    "sum(COMMISSION_HIGH_GOODS_NUM_L30D)",
    "sum(COMMISSION_HIGH_GOODS_NUM_L90D)",
    "sum(SERV_RPLY_IN_30S_RT_L15D)",
    "sum(SERV_RPLY_IN_30S_RT_L30D)",
    "sum(SERV_RPLY_IN_30S_RT_L90D)"
  )

  private  val QsqlStatement ="select vend_cd,sum(ACT_REGISTER_NUM_L15D) " + //--成功报名乐拼购活动商品数
                              ",sum(LATE_DLVR_NUM_L15D)"+                     //--乐拼购订单延迟发货次数
                              ",sum(DLVR_IN_24H_NUM_L15D)"+                 //乐拼购订单24h内发货次数
                              ",sum(SHAM_DLVR_NUM_L15D)"+                   //虚假发货次数
                              ",sum(VEND_RPLY_RTN_IN_12H_L15D)"+            //商家12h内响应乐拼购退货订单次数
                              ",sum(VEND_RPLY_RTN_OVR_48H_L15D)"+           //商家超48h响应乐拼购退货订单次数
                              ",sum(VEND_RPLY_COMPLAINT_IN_24H_L15D)"+      //商家24h内响应乐拼购投诉订单次数
                              ",sum(VEND_RPLY_COMPLAINT_OVR_24H_L15D)"+     //商家超24h响应乐拼购投诉订单次数
                              ",sum(SERV_INTERVENE_RTN_NUM_L15D)"+          //客服介入乐拼购退货订单量
                              ",sum(SERV_INTERVENE_RTN_RT_L15D)"+           //客服介入乐拼购退货订单比率
                              ",sum(SERV_INTERVENE_COMPLAINT_NUM_L15D)"+    //客服介入乐拼购投诉订单量
                              ",sum(SERV_INTERVENE_COMPLAINT_RT_L15D)"+     //客服介入乐拼购投诉订单比率
                              ",sum(SERV_RPLY_IN_30S_RT_L15D)"+             //客服30秒响应率
                              ",sum(SERV_NEGATIVE_CMT_NUM_L15D)"+           //客服差评数
                              ",sum(ORDR_NEGATIVE_CMT_NUM_L15D)"+           //乐拼购订单差评数
                              ",sum(NEGATIVE_CMT_RPLY_OVER_24H_L15D)"+      //乐拼购订单差评超24小时回复数
                              ",sum(ORDR_GOOD_CMT_NUM_L15D)"+               //乐拼购订单好评次数
                              ",sum(REFUSE_7DAY_RTN_NUM_L15D)"+             //拒绝“7天无理由退换货”乐拼购退货订单数
                              ",sum(SELL_FAKE_NUM_L15D)"+                   //出售假冒、盗版商品次数
                              ",sum(COUNTERFEIT_MATERIAL_NUM_L15D)"+        //假冒材质成份次数
                              ",sum(SELL_INFERIOR_GOODS_NUM_L15D)"+         //出售不合格商品次数
                              ",sum(SHAM_DESCRIBE_NUM_L15D)"+               //描述不符次数
                              ",sum(ULTRA_VIRES_NUM_L15D)"+                 //不当使用他人权利次数
                              ",sum(BREACH_PROMISE_NUM_L15D)"+              //违背承诺次数
                              ",sum(VIRTUAL_HIGH_PRC_NUM_L15D)"+            //价格虚高次数
                              ",sum(HARASS_NUM_L15D)"+                      //恶意骚扰次数
                              ",sum(INVERSION_OF_PRC_NUM_L15D)"+            //乐拼购商品价格倒挂次数
                              ",sum(INVERSION_OF_INV_NUM_L15D)"+            //乐拼购商品库存倒挂次数
                              ",sum(SALES_RTN_RT_L15D)"+                    //乐拼购订单退货率
                              ",sum(SALES_RTN_NUM_L15D)"+                   //乐拼购订单退货量
                              ",sum(DLY_SALES_L15D)"+                       //乐拼购日均销量
                              ",sum(COMMISSION_HIGH_GOODS_NUM_L15D)"+       //高佣商品量
                              ",sum(ACT_REGISTER_NUM_L30D)"+                //成功报名乐拼购活动商品数
                              ",sum(LATE_DLVR_NUM_L30D)"+                   //乐拼购订单延迟发货次数
                              ",sum(DLVR_IN_24H_NUM_L30D)"+                 //乐拼购订单24h内发货次数
                              ",sum(SHAM_DLVR_NUM_L30D)"+                   //虚假发货次数
                              ",sum(VEND_RPLY_RTN_IN_12H_L30D)"+            //商家12h内响应乐拼购退货订单次数
                              ",sum(VEND_RPLY_RTN_OVR_48H_L30D)"+           //商家超48h响应乐拼购退货订单次数
                              ",sum(VEND_RPLY_COMPLAINT_IN_24H_L30D)"+      //商家24h内响应乐拼购投诉订单次数
                              ",sum(VEND_RPLY_COMPLAINT_OVR_24H_L30D)"+     //商家超24h响应乐拼购投诉订单次数
                              ",sum(SERV_INTERVENE_RTN_NUM_L30D)"+          //客服介入乐拼购退货订单量
                              ",sum(SERV_INTERVENE_RTN_RT_L30D)"+           //客服介入乐拼购退货订单比率
                              ",sum(SERV_INTERVENE_COMPLAINT_NUM_L30D)"+    //客服介入乐拼购投诉订单量
                              ",sum(SERV_INTERVENE_COMPLAINT_RT_L30D)"+     //客服介入乐拼购投诉订单比率
                              ",sum(SERV_RPLY_IN_30S_RT_L30D)"+             //客服30秒响应率
                              ",sum(SERV_NEGATIVE_CMT_NUM_L30D)"+           //客服差评数
                              ",sum(ORDR_NEGATIVE_CMT_NUM_L30D)"+           //乐拼购订单差评数
                              ",sum(NEGATIVE_CMT_RPLY_OVER_24H_L30D)"+      //乐拼购订单差评超24小时回复数
                              ",sum(ORDR_GOOD_CMT_NUM_L30D)"+               //乐拼购订单好评次数
                              ",sum(REFUSE_7DAY_RTN_NUM_L30D)"+             //拒绝“7天无理由退换货”乐拼购退货订单数
                              ",sum(SELL_FAKE_NUM_L30D)"+                   //出售假冒、盗版商品次数
                              ",sum(COUNTERFEIT_MATERIAL_NUM_L30D)"+        //假冒材质成份次数
                              ",sum(SELL_INFERIOR_GOODS_NUM_L30D)"+         //出售不合格商品次数
                              ",sum(SHAM_DESCRIBE_NUM_L30D)"+               //描述不符次数
                              ",sum(ULTRA_VIRES_NUM_L30D)"+                 //不当使用他人权利次数
                              ",sum(BREACH_PROMISE_NUM_L30D)"+              //违背承诺次数
                              ",sum(VIRTUAL_HIGH_PRC_NUM_L30D)"+            //价格虚高次数
                              ",sum(HARASS_NUM_L30D)"+                      //恶意骚扰次数
                              ",sum(INVERSION_OF_PRC_NUM_L30D)"+            //乐拼购商品价格倒挂次数
                              ",sum(INVERSION_OF_INV_NUM_L30D)"+            //乐拼购商品库存倒挂次数
                              ",sum(SALES_RTN_RT_L30D)"+                    //乐拼购订单退货率
                              ",sum(SALES_RTN_NUM_L30D)"+                   //乐拼购订单退货量
                              ",sum(DLY_SALES_L30D)"+                       //乐拼购日均销量
                              ",sum(COMMISSION_HIGH_GOODS_NUM_L30D)"+       //--高佣商品量
                              ",sum(ACT_REGISTER_NUM_L90D)"+                //--成功报名乐拼购活动商品数
                              ",sum(LATE_DLVR_NUM_L90D)"+                   //--乐拼购订单延迟发货次数
                              ",sum(DLVR_IN_24H_NUM_L90D)"+                 // --乐拼购订单24h内发货次数
                              ",sum(SHAM_DLVR_NUM_L90D)"+                   //--虚假发货次数
                              ",sum(VEND_RPLY_RTN_IN_12H_L90D)"+            // --商家12h内响应乐拼购退货订单次数
                              ",sum(VEND_RPLY_RTN_OVR_48H_L90D)"+           // --商家超48h响应乐拼购退货订单次数
                              ",sum(VEND_RPLY_COMPLAINT_IN_24H_L90D)"+      //--商家24h内响应乐拼购投诉订单次数
                              ",sum(VEND_RPLY_COMPLAINT_OVR_24H_L90D)"+     //--商家超24h响应乐拼购投诉订单次数
                              ",sum(SERV_INTERVENE_RTN_NUM_L90D)"+          // --客服介入乐拼购退货订单量
                              ",sum(SERV_INTERVENE_RTN_RT_L90D)"+           //--客服介入乐拼购退货订单比率
                              ",sum(SERV_INTERVENE_COMPLAINT_NUM_L90D)"+    // --客服介入乐拼购投诉订单量
                              ",sum(SERV_INTERVENE_COMPLAINT_RT_L90D)"+     //--客服介入乐拼购投诉订单比率
                              ",sum(SERV_RPLY_IN_30S_RT_L90D)"+             //--客服90秒响应率
                              ",sum(SERV_NEGATIVE_CMT_NUM_L90D)"+           //--客服差评数
                              ",sum(ORDR_NEGATIVE_CMT_NUM_L90D)"+           //--乐拼购订单差评数
                              ",sum(NEGATIVE_CMT_RPLY_OVER_24H_L90D)"+      //--乐拼购订单差评超24小时回复数
                              ",sum(ORDR_GOOD_CMT_NUM_L90D)"+               //--乐拼购订单好评次数
                              ",sum(REFUSE_7DAY_RTN_NUM_L90D)"+             //--拒绝“7天无理由退换货”乐拼购退货订单数
                              ",sum(SELL_FAKE_NUM_L90D)"+                   //--出售假冒、盗版商品次数
                              ",sum(COUNTERFEIT_MATERIAL_NUM_L90D)"+        // --假冒材质成份次数
                              ",sum(SELL_INFERIOR_GOODS_NUM_L90D)"+         // --出售不合格商品次数
                              ",sum(SHAM_DESCRIBE_NUM_L90D)"+               // --描述不符次数
                              ",sum(ULTRA_VIRES_NUM_L90D)"+                 //--不当使用他人权利次数
                              ",sum(BREACH_PROMISE_NUM_L90D)"+              // --违背承诺次数
                              ",sum(VIRTUAL_HIGH_PRC_NUM_L90D)"+            // --价格虚高次数
                              ",sum(HARASS_NUM_L90D)"+                      //--恶意骚扰次数
                              ",sum(INVERSION_OF_PRC_NUM_L90D)"+            // --乐拼购商品价格倒挂次数
                              ",sum(INVERSION_OF_INV_NUM_L90D)"+            // --乐拼购商品库存倒挂次数
                              ",sum(SALES_RTN_RT_L90D)"+                    //--乐拼购订单退货率
                              ",sum(SALES_RTN_NUM_L90D)"+                   //--乐拼购订单退货量
                              ",sum(DLY_SALES_L90D)"+                       // --乐拼购日均销量
                              ",sum( COMMISSION_HIGH_GOODS_NUM_L90D)"+      //--高佣商品量
                              " from SOPDM.TDM_RES_PGS_KPI_D where STATIS_DATE <= 'DATE(YYYYMMDD)' " +
                              "AND STATIS_DATE>REGEXP_REPLACE(DATE_SUB(FROM_UNIXTIME(TO_UNIX_TIMESTAMP('DATE(YYYYMMDD)','yyyyMMdd'),'yyyy-MM-dd'),30),'-','')" +
                              "group by VEND_CD"
  private  val CsqlStatement = "CREATE TABLE IF NOT EXISTS sopdm.TDM_RES_PGS_STATISYICS_KPI_D"+
    "("+
    "VEND_CD                                  STRING COMMENT '店铺编码',"+                                      //--店铺编码
    "ACT_REGISTER_NUM_L15D                    DOUBLE COMMENT '成功报名乐拼购活动商品数',"+                      //--成功报名乐拼购活动商品数
    "LATE_DLVR_NUM_L15D                       DOUBLE COMMENT '乐拼购订单延迟发货次数',"+                        //--乐拼购订单延迟发货次数
    "DLVR_IN_24H_NUM_L15D                     DOUBLE COMMENT '乐拼购订单24h内发货次数',"+                       //--乐拼购订单24h内发货次数
    "SHAM_DLVR_NUM_L15D                       DOUBLE COMMENT '虚假发货次数',"+                                  //--虚假发货次数
    "VEND_RPLY_RTN_IN_12H_L15D                DOUBLE COMMENT '商家12h内响应乐拼购退货订单次数',"+               //--商家12h内响应乐拼购退货订单次数
    "VEND_RPLY_RTN_OVR_48H_L15D               DOUBLE COMMENT '商家超48h响应乐拼购退货订单次数',"+               //--商家超48h响应乐拼购退货订单次数
    "VEND_RPLY_COMPLAINT_IN_24H_L15D          DOUBLE COMMENT '商家24h内响应乐拼购投诉订单次数',"+               //--商家24h内响应乐拼购投诉订单次数
    "VEND_RPLY_COMPLAINT_OVR_24H_L15D         DOUBLE COMMENT '商家超24h响应乐拼购投诉订单次数',"+               //--商家超24h响应乐拼购投诉订单次数
    "SERV_INTERVENE_RTN_NUM_L15D              DOUBLE COMMENT '客服介入乐拼购退货订单量'," +                     //--客服介入乐拼购退货订单量
    "SERV_INTERVENE_RTN_RT_L15D               DOUBLE COMMENT '客服介入乐拼购退货订单比率',"+                    //--客服介入乐拼购退货订单比率
    "SERV_INTERVENE_COMPLAINT_NUM_L15D        DOUBLE COMMENT '客服介入乐拼购投诉订单量',"+                      //--客服介入乐拼购投诉订单量
    "SERV_INTERVENE_COMPLAINT_RT_L15D         DOUBLE COMMENT '客服介入乐拼购投诉订单比率',"+                    //--客服介入乐拼购投诉订单比率
    "SERV_RPLY_IN_30S_RT_L15D                 DOUBLE COMMENT '客服30秒响应率',"+                                //--客服30秒响应率
    "SERV_NEGATIVE_CMT_NUM_L15D               DOUBLE COMMENT '客服差评数',"+                                    //--客服差评数
    "ORDR_NEGATIVE_CMT_NUM_L15D               DOUBLE COMMENT '乐拼购订单差评数'," +                             //--乐拼购订单差评数
    "NEGATIVE_CMT_RPLY_OVER_24H_L15D          DOUBLE COMMENT '乐拼购订单差评超24小时回复数'," +                 //--乐拼购订单差评超24小时回复数
    "ORDR_GOOD_CMT_NUM_L15D                   DOUBLE COMMENT '乐拼购订单好评次数'," +                           //--乐拼购订单好评次数
    "REFUSE_7DAY_RTN_NUM_L15D                 DOUBLE COMMENT '拒绝“7天无理由退换货”乐拼购退货订单数'," +      //--拒绝“7天无理由退换货”乐拼购退货订单数
    "SELL_FAKE_NUM_L15D                       DOUBLE COMMENT '出售假冒、盗版商品次数',"+                        //--出售假冒、盗版商品次数
    "COUNTERFEIT_MATERIAL_NUM_L15D            DOUBLE COMMENT '假冒材质成份次数',"+                              //--假冒材质成份次数
    "SELL_INFERIOR_GOODS_NUM_L15D             DOUBLE COMMENT '出售不合格商品次数',"+                            //--出售不合格商品次数
    "SHAM_DESCRIBE_NUM_L15D                   DOUBLE COMMENT '描述不符次数',"+                                  //--描述不符次数
    "ULTRA_VIRES_NUM_L15D                     DOUBLE COMMENT '不当使用他人权利次数',"+                          //--不当使用他人权利次数
    "BREACH_PROMISE_NUM_L15D                  DOUBLE COMMENT '违背承诺次数',"+               //--违背承诺次数
    "VIRTUAL_HIGH_PRC_NUM_L15D                DOUBLE COMMENT '价格虚高次数',"+               //--价格虚高次数
    "HARASS_NUM_L15D                          DOUBLE COMMENT '恶意骚扰次数',"+               //--恶意骚扰次数
    "INVERSION_OF_PRC_NUM_L15D                DOUBLE COMMENT '乐拼购商品价格倒挂次数',"+     //--乐拼购商品价格倒挂次数
    "INVERSION_OF_INV_NUM_L15D                DOUBLE COMMENT '乐拼购商品库存倒挂次数',"+     //--乐拼购商品库存倒挂次数
    "SALES_RTN_RT_L15D                        DOUBLE COMMENT '乐拼购订单退货率',"+           //--乐拼购订单退货率
    "SALES_RTN_NUM_L15D                       DOUBLE COMMENT '乐拼购订单退货量',"+           //--乐拼购订单退货量
    "DLY_SALES_L15D                           DOUBLE COMMENT '乐拼购日均销量',"+             //--乐拼购日均销量
    "COMMISSION_HIGH_GOODS_NUM_L15D           DOUBLE COMMENT '高佣商品量',"+                 //--高佣商品量
    "ACT_REGISTER_NUM_L30D                    DOUBLE COMMENT '成功报名乐拼购活动商品数',"+   //--成功报名乐拼购活动商品数
    "LATE_DLVR_NUM_L30D                       DOUBLE COMMENT '乐拼购订单延迟发货次数',"+     //--乐拼购订单延迟发货次数
    "DLVR_IN_24H_NUM_L30D                     DOUBLE COMMENT '乐拼购订单24h内发货次数'," +   //--乐拼购订单24h内发货次数
    "SHAM_DLVR_NUM_L30D                       DOUBLE COMMENT '虚假发货次数'," +              //--虚假发货次数
    "VEND_RPLY_RTN_IN_12H_L30D                DOUBLE COMMENT '商家12h内响应乐拼购退货订单次数'," +              //--商家12h内响应乐拼购退货订单次数
    "VEND_RPLY_RTN_OVR_48H_L30D               DOUBLE COMMENT '商家超48h响应乐拼购退货订单次数'," +              //--商家超48h响应乐拼购退货订单次数
    "VEND_RPLY_COMPLAINT_IN_24H_L30D          DOUBLE COMMENT '商家24h内响应乐拼购投诉订单次数'," +              //--商家24h内响应乐拼购投诉订单次数
    "VEND_RPLY_COMPLAINT_OVR_24H_L30D         DOUBLE COMMENT '商家超24h响应乐拼购投诉订单次数'," +              //--商家超24h响应乐拼购投诉订单次数
    "SERV_INTERVENE_RTN_NUM_L30D              DOUBLE COMMENT '客服介入乐拼购退货订单量'," +                     //--客服介入乐拼购退货订单量
    "SERV_INTERVENE_RTN_RT_L30D               DOUBLE COMMENT '客服介入乐拼购退货订单比率',"+                    //--客服介入乐拼购退货订单比率
    "SERV_INTERVENE_COMPLAINT_NUM_L30D        DOUBLE COMMENT '客服介入乐拼购投诉订单量'," +                     //--客服介入乐拼购投诉订单量
    "SERV_INTERVENE_COMPLAINT_RT_L30D         DOUBLE COMMENT '客服介入乐拼购投诉订单比率',"+                    //--客服介入乐拼购投诉订单比率
    "SERV_RPLY_IN_30S_RT_L30D                 DOUBLE COMMENT '客服30秒响应率',"+            //--客服30秒响应率
    "SERV_NEGATIVE_CMT_NUM_L30D               DOUBLE COMMENT '客服差评数',"+                //--客服差评数
    "ORDR_NEGATIVE_CMT_NUM_L30D               DOUBLE COMMENT '乐拼购订单差评数',"+          //--乐拼购订单差评数
    "NEGATIVE_CMT_RPLY_OVER_24H_L30D          DOUBLE COMMENT '乐拼购订单差评超24小时回复数',"+               //--乐拼购订单差评超24小时回复数
    "ORDR_GOOD_CMT_NUM_L30D                   DOUBLE COMMENT '乐拼购订单好评次数',"+                         //--乐拼购订单好评次数
    "REFUSE_7DAY_RTN_NUM_L30D                 DOUBLE COMMENT '拒绝“7天无理由退换货”乐拼购退货订单数',"+    //--拒绝“7天无理由退换货”乐拼购退货订单数
    "SELL_FAKE_NUM_L30D                       DOUBLE COMMENT '出售假冒、盗版商品次数',"+                     //--出售假冒、盗版商品次数
    "COUNTERFEIT_MATERIAL_NUM_L30D            DOUBLE COMMENT '假冒材质成份次数',"+                           //--假冒材质成份次数
    "SELL_INFERIOR_GOODS_NUM_L30D             DOUBLE COMMENT '出售不合格商品次数',"+                         //--出售不合格商品次数
    "SHAM_DESCRIBE_NUM_L30D                   DOUBLE COMMENT '描述不符次数',"+                               //--描述不符次数
    "ULTRA_VIRES_NUM_L30D                     DOUBLE COMMENT '不当使用他人权利次数',"+                       //--不当使用他人权利次数
    "BREACH_PROMISE_NUM_L30D                  DOUBLE COMMENT '违背承诺次数',"+               //--违背承诺次数
    "VIRTUAL_HIGH_PRC_NUM_L30D                DOUBLE COMMENT '价格虚高次数',"+               //--价格虚高次数
    "HARASS_NUM_L30D                          DOUBLE COMMENT '恶意骚扰次数',"+               //--恶意骚扰次数
    "INVERSION_OF_PRC_NUM_L30D                DOUBLE COMMENT '乐拼购商品价格倒挂次数',"+     //--乐拼购商品价格倒挂次数
    "INVERSION_OF_INV_NUM_L30D                DOUBLE COMMENT '乐拼购商品库存倒挂次数',"+     //--乐拼购商品库存倒挂次数
    "SALES_RTN_RT_L30D                        DOUBLE COMMENT '乐拼购订单退货率',"+           //--乐拼购订单退货率
    "SALES_RTN_NUM_L30D                       DOUBLE COMMENT '乐拼购订单退货量',"+           //--乐拼购订单退货量
    "DLY_SALES_L30D                           DOUBLE COMMENT '乐拼购日均销量',"+             //--乐拼购日均销量
    "COMMISSION_HIGH_GOODS_NUM_L30D           DOUBLE COMMENT '高佣商品量',"+                 //--高佣商品量
    "ACT_REGISTER_NUM_L90D                    DOUBLE COMMENT '成功报名乐拼购活动商品数',"+   //--成功报名乐拼购活动商品数
    "LATE_DLVR_NUM_L90D                       DOUBLE COMMENT '乐拼购订单延迟发货次数',"+     //--乐拼购订单延迟发货次数
    "DLVR_IN_24H_NUM_L90D                     DOUBLE COMMENT '乐拼购订单24h内发货次数',"+    //--乐拼购订单24h内发货次数
    "SHAM_DLVR_NUM_L90D                       DOUBLE COMMENT '虚假发货次数',"+               //--虚假发货次数
    "VEND_RPLY_RTN_IN_12H_L90D                DOUBLE COMMENT '商家12h内响应乐拼购退货订单次数',"+               //--商家12h内响应乐拼购退货订单次数
    "VEND_RPLY_RTN_OVR_48H_L90D               DOUBLE COMMENT '商家超48h响应乐拼购退货订单次数',"+               //--商家超48h响应乐拼购退货订单次数
    "VEND_RPLY_COMPLAINT_IN_24H_L90D          DOUBLE COMMENT '商家24h内响应乐拼购投诉订单次数',"+               //--商家24h内响应乐拼购投诉订单次数
    "VEND_RPLY_COMPLAINT_OVR_24H_L90D         DOUBLE COMMENT '商家超24h响应乐拼购投诉订单次数',"+               //--商家超24h响应乐拼购投诉订单次数
    "SERV_INTERVENE_RTN_NUM_L90D              DOUBLE COMMENT '客服介入乐拼购退货订单量',"+                      //--客服介入乐拼购退货订单量
    "SERV_INTERVENE_RTN_RT_L90D               DOUBLE COMMENT '客服介入乐拼购退货订单比率',"+                    //--客服介入乐拼购退货订单比率
    "SERV_INTERVENE_COMPLAINT_NUM_L90D        DOUBLE COMMENT '客服介入乐拼购投诉订单量',"+                      //--客服介入乐拼购投诉订单量
    "SERV_INTERVENE_COMPLAINT_RT_L90D         DOUBLE COMMENT '客服介入乐拼购投诉订单比率',"+                    //--客服介入乐拼购投诉订单比率
    "SERV_RPLY_IN_30S_RT_L90D                 DOUBLE COMMENT '客服90秒响应率',"+           //--客服90秒响应率
    "SERV_NEGATIVE_CMT_NUM_L90D               DOUBLE COMMENT '客服差评数',"+               //--客服差评数
    "ORDR_NEGATIVE_CMT_NUM_L90D               DOUBLE COMMENT '乐拼购订单差评数',"+         //--乐拼购订单差评数
    "NEGATIVE_CMT_RPLY_OVER_24H_L90D          DOUBLE COMMENT '乐拼购订单差评超24小时回复数',"+               //--乐拼购订单差评超24小时回复数
    "ORDR_GOOD_CMT_NUM_L90D                   DOUBLE COMMENT '乐拼购订单好评次数',"+                         //--乐拼购订单好评次数
    "REFUSE_7DAY_RTN_NUM_L90D                 DOUBLE COMMENT '拒绝“7天无理由退换货”乐拼购退货订单数',"+    //--拒绝“7天无理由退换货”乐拼购退货订单数
    "SELL_FAKE_NUM_L90D                       DOUBLE COMMENT '出售假冒、盗版商品次数',"+                     //--出售假冒、盗版商品次数
    "COUNTERFEIT_MATERIAL_NUM_L90D            DOUBLE COMMENT '假冒材质成份次数',"+                           //--假冒材质成份次数
    "SELL_INFERIOR_GOODS_NUM_L90D             DOUBLE COMMENT '出售不合格商品次数',"+                         //--出售不合格商品次数
    "SHAM_DESCRIBE_NUM_L90D                   DOUBLE COMMENT '描述不符次数',"+                               //--描述不符次数
    "ULTRA_VIRES_NUM_L90D                     DOUBLE COMMENT '不当使用他人权利次数',"+                       //--不当使用他人权利次数
    "BREACH_PROMISE_NUM_L90D                  DOUBLE COMMENT '违背承诺次数',"+               //--违背承诺次数
    "VIRTUAL_HIGH_PRC_NUM_L90D                DOUBLE COMMENT '价格虚高次数',"+               //--价格虚高次数
    "HARASS_NUM_L90D                          DOUBLE COMMENT '恶意骚扰次数',"+               //--恶意骚扰次数
    "INVERSION_OF_PRC_NUM_L90D                DOUBLE COMMENT '乐拼购商品价格倒挂次数',"+     //--乐拼购商品价格倒挂次数
    "INVERSION_OF_INV_NUM_L90D                DOUBLE COMMENT '乐拼购商品库存倒挂次数',"+     //--乐拼购商品库存倒挂次数
    "SALES_RTN_RT_L90D                        DOUBLE COMMENT '乐拼购订单退货率',"+           //--乐拼购订单退货率
    "SALES_RTN_NUM_L90D                       DOUBLE COMMENT '乐拼购订单退货量',"+           //--乐拼购订单退货量
    "DLY_SALES_L90D                           DOUBLE COMMENT '乐拼购日均销量',"+             //--乐拼购日均销量
    "COMMISSION_HIGH_GOODS_NUM_L90D           DOUBLE COMMENT '高佣商品量'," +                //--高佣商品量
    "ETL_TIME                                 STRING "+                                      //--处理时间
    ") STORED AS RCFILE"
  private val fieldtag = Array(
      "VEND_CD",                                          //--店铺编码
      "ACT_REGISTER_NUM_L15D",                            //--成功报名乐拼购活动商品数
      "LATE_DLVR_NUM_L15D",                               //--乐拼购订单延迟发货次数
      "DLVR_IN_24H_NUM_L15D",                             //--乐拼购订单24h内发货次数
      "SHAM_DLVR_NUM_L15D",                               //--虚假发货次数
      "VEND_RPLY_RTN_IN_12H_L15D",                        //--商家12h内响应乐拼购退货订单次数
      "VEND_RPLY_RTN_OVR_48H_L15D",                       //--商家超48h响应乐拼购退货订单次数
      "VEND_RPLY_COMPLAINT_IN_24H_L15D",                  //--商家24h内响应乐拼购投诉订单次数
      "VEND_RPLY_COMPLAINT_OVR_24H_L15D",                 //--商家超24h响应乐拼购投诉订单次数
      "SERV_INTERVENE_RTN_NUM_L15D",                      //--客服介入乐拼购退货订单量
      "SERV_INTERVENE_RTN_RT_L15D",                       //--客服介入乐拼购退货订单比率
      "SERV_INTERVENE_COMPLAINT_NUM_L15D",                //--客服介入乐拼购投诉订单量
      "SERV_INTERVENE_COMPLAINT_RT_L15D",                 //--客服介入乐拼购投诉订单比率
      "SERV_RPLY_IN_30S_RT_L15D",                         //--客服30秒响应率
      "SERV_NEGATIVE_CMT_NUM_L15D",                       //--客服差评数
      "ORDR_NEGATIVE_CMT_NUM_L15D",                       //--乐拼购订单差评数
      "NEGATIVE_CMT_RPLY_OVER_24H_L15D",                  //--乐拼购订单差评超24小时回复数
      "ORDR_GOOD_CMT_NUM_L15D",                           //--乐拼购订单好评次数
      "REFUSE_7DAY_RTN_NUM_L15D",                         //--拒绝“7天无理由退换货”乐拼购退货订单数
      "SELL_FAKE_NUM_L15D",                               //--出售假冒、盗版商品次数
      "COUNTERFEIT_MATERIAL_NUM_L15D",                    //--假冒材质成份次数
      "SELL_INFERIOR_GOODS_NUM_L15D",                     //--出售不合格商品次数
      "SHAM_DESCRIBE_NUM_L15D",                           //--描述不符次数
      "ULTRA_VIRES_NUM_L15D",                             //--不当使用他人权利次数
      "BREACH_PROMISE_NUM_L15D",                          //--违背承诺次数
      "VIRTUAL_HIGH_PRC_NUM_L15D",                        //--价格虚高次数
      "HARASS_NUM_L15D",                                  //--恶意骚扰次数
      "INVERSION_OF_PRC_NUM_L15D",                        //--乐拼购商品价格倒挂次数
      "INVERSION_OF_INV_NUM_L15D",                        //--乐拼购商品库存倒挂次数
      "SALES_RTN_RT_L15D",                                //--乐拼购订单退货率
      "SALES_RTN_NUM_L15D",                               //--乐拼购订单退货量
      "DLY_SALES_L15D",                                   //--乐拼购日均销量
      "COMMISSION_HIGH_GOODS_NUM_L15D",                   //--高佣商品量
      "ACT_REGISTER_NUM_L30D",                            //--成功报名乐拼购活动商品数
      "LATE_DLVR_NUM_L30D",                               //--乐拼购订单延迟发货次数
      "DLVR_IN_24H_NUM_L30D",                             //--乐拼购订单24h内发货次数
      "SHAM_DLVR_NUM_L30D",                               //--虚假发货次数
      "VEND_RPLY_RTN_IN_12H_L30D",                        //--商家12h内响应乐拼购退货订单次数
      "VEND_RPLY_RTN_OVR_48H_L30D",                       //--商家超48h响应乐拼购退货订单次数
      "VEND_RPLY_COMPLAINT_IN_24H_L30D",                  //--商家24h内响应乐拼购投诉订单次数
      "VEND_RPLY_COMPLAINT_OVR_24H_L30D",                 //--商家超24h响应乐拼购投诉订单次数
      "SERV_INTERVENE_RTN_NUM_L30D",                      //--客服介入乐拼购退货订单量
      "SERV_INTERVENE_RTN_RT_L30D",                       //--客服介入乐拼购退货订单比率
      "SERV_INTERVENE_COMPLAINT_NUM_L30D",                //--客服介入乐拼购投诉订单量
      "SERV_INTERVENE_COMPLAINT_RT_L30D",                 //--客服介入乐拼购投诉订单比率
      "SERV_RPLY_IN_30S_RT_L30D",                         //--客服30秒响应率
      "SERV_NEGATIVE_CMT_NUM_L30D",                       //--客服差评数
      "ORDR_NEGATIVE_CMT_NUM_L30D",                       //--乐拼购订单差评数
      "NEGATIVE_CMT_RPLY_OVER_24H_L30D",                  //--乐拼购订单差评超24小时回复数
      "ORDR_GOOD_CMT_NUM_L30D",                           //--乐拼购订单好评次数
      "REFUSE_7DAY_RTN_NUM_L30D",                         //--拒绝“7天无理由退换货”乐拼购退货订单数
      "SELL_FAKE_NUM_L30D",                               //--出售假冒、盗版商品次数
      "COUNTERFEIT_MATERIAL_NUM_L30D",                    //--假冒材质成份次数
      "SELL_INFERIOR_GOODS_NUM_L30D",                     //--出售不合格商品次数
      "SHAM_DESCRIBE_NUM_L30D",                           //--描述不符次数
      "ULTRA_VIRES_NUM_L30D",                             //--不当使用他人权利次数
      "BREACH_PROMISE_NUM_L30D",                          //--违背承诺次数
      "VIRTUAL_HIGH_PRC_NUM_L30D",                        //--价格虚高次数
      "HARASS_NUM_L30D",                                  //--恶意骚扰次数
      "INVERSION_OF_PRC_NUM_L30D",                        //--乐拼购商品价格倒挂次数
      "INVERSION_OF_INV_NUM_L30D",                        //--乐拼购商品库存倒挂次数
      "SALES_RTN_RT_L30D",                                //--乐拼购订单退货率
      "SALES_RTN_NUM_L30D",                               //--乐拼购订单退货量
      "DLY_SALES_L30D",                                   //--乐拼购日均销量
      "COMMISSION_HIGH_GOODS_NUM_L30D",                   //--高佣商品量
      "ACT_REGISTER_NUM_L90D",                            //--成功报名乐拼购活动商品数
      "LATE_DLVR_NUM_L90D",                               //--乐拼购订单延迟发货次数
      "DLVR_IN_24H_NUM_L90D",                             //--乐拼购订单24h内发货次数
      "SHAM_DLVR_NUM_L90D",                               //--虚假发货次数
      "VEND_RPLY_RTN_IN_12H_L90D",                        //--商家12h内响应乐拼购退货订单次数
      "VEND_RPLY_RTN_OVR_48H_L90D",                       //--商家超48h响应乐拼购退货订单次数
      "VEND_RPLY_COMPLAINT_IN_24H_L90D",                  //--商家24h内响应乐拼购投诉订单次数
      "VEND_RPLY_COMPLAINT_OVR_24H_L90D",                 //--商家超24h响应乐拼购投诉订单次数
      "SERV_INTERVENE_RTN_NUM_L90D",                      //--客服介入乐拼购退货订单量
      "SERV_INTERVENE_RTN_RT_L90D",                       //--客服介入乐拼购退货订单比率
      "SERV_INTERVENE_COMPLAINT_NUM_L90D",                //--客服介入乐拼购投诉订单量
      "SERV_INTERVENE_COMPLAINT_RT_L90D",                 //--客服介入乐拼购投诉订单比率
      "SERV_RPLY_IN_30S_RT_L90D",                         //--客服90秒响应率
      "SERV_NEGATIVE_CMT_NUM_L90D",                       //--客服差评数
      "ORDR_NEGATIVE_CMT_NUM_L90D",                       //--乐拼购订单差评数
      "NEGATIVE_CMT_RPLY_OVER_24H_L90D",                  //--乐拼购订单差评超24小时回复数
      "ORDR_GOOD_CMT_NUM_L90D",                           //--乐拼购订单好评次数
      "REFUSE_7DAY_RTN_NUM_L90D",                         //--拒绝“7天无理由退换货”乐拼购退货订单数
      "SELL_FAKE_NUM_L90D",                               //--出售假冒、盗版商品次数
      "COUNTERFEIT_MATERIAL_NUM_L90D",                    //--假冒材质成份次数
      "SELL_INFERIOR_GOODS_NUM_L90D",                     //--出售不合格商品次数
      "SHAM_DESCRIBE_NUM_L90D",                           //--描述不符次数
      "ULTRA_VIRES_NUM_L90D",                             //--不当使用他人权利次数
      "BREACH_PROMISE_NUM_L90D",                          //--违背承诺次数
      "VIRTUAL_HIGH_PRC_NUM_L90D",                        //--价格虚高次数
      "HARASS_NUM_L90D",                                  //--恶意骚扰次数
      "INVERSION_OF_PRC_NUM_L90D",                        //--乐拼购商品价格倒挂次数
      "INVERSION_OF_INV_NUM_L90D",                        //--乐拼购商品库存倒挂次数
      "SALES_RTN_RT_L90D",                                //--乐拼购订单退货率
      "SALES_RTN_NUM_L90D",                               //--乐拼购订单退货量
      "DLY_SALES_L90D",                                   //--乐拼购日均销量
      "COMMISSION_HIGH_GOODS_NUM_L90D",                   //--高佣商品量
      "ETL_TIME"
  )
   def main(args: Array[String]): Unit = {
     //var statisticsDF = sparkSession.sql(sqlStatement).cache()
     var statisticsDF = readFromHiveTable(sparkSession, QsqlStatement)
     val maxDF = statisticsDF.agg(
       Map(
         "sum(ACT_REGISTER_NUM_L15D)" -> "max",                //成功报名乐拼购活动商品数
         "sum(LATE_DLVR_NUM_L15D)" -> "max",                   //乐拼购订单延迟发货次数
         "sum(DLVR_IN_24H_NUM_L15D)" -> "max",                 //乐拼购订单24h内发货次数
         "sum(SHAM_DLVR_NUM_L15D)" -> "max",                   //虚假发货次数
         "sum(VEND_RPLY_RTN_IN_12H_L15D)" -> "max",            //商家12h内响应乐拼购退货订单次数
         "sum(VEND_RPLY_RTN_OVR_48H_L15D)" -> "max",           //商家超48h响应乐拼购退货订单次数
         "sum(VEND_RPLY_COMPLAINT_IN_24H_L15D)" -> "max",      //商家24h内响应乐拼购投诉订单次数
         "sum(VEND_RPLY_COMPLAINT_OVR_24H_L15D)" -> "max",     //商家超24h响应乐拼购投诉订单次数
         "sum(SERV_INTERVENE_RTN_NUM_L15D)" -> "max",          //客服介入乐拼购退货订单量
         "sum(SERV_INTERVENE_RTN_RT_L15D)" -> "max",           //客服介入乐拼购退货订单比率
         "sum(SERV_INTERVENE_COMPLAINT_NUM_L15D)" -> "max",    //客服介入乐拼购投诉订单量
         "sum(SERV_INTERVENE_COMPLAINT_RT_L15D)" -> "max",     //客服介入乐拼购投诉订单比率
         "sum(SERV_RPLY_IN_30S_RT_L15D)" -> "max",             //客服30秒响应率
         "sum(SERV_NEGATIVE_CMT_NUM_L15D)" -> "max",           //客服差评数
         "sum(ORDR_NEGATIVE_CMT_NUM_L15D)" -> "max",           //乐拼购订单差评数
         "sum(NEGATIVE_CMT_RPLY_OVER_24H_L15D)" -> "max",      //乐拼购订单差评超24小时回复数
         "sum(ORDR_GOOD_CMT_NUM_L15D)" -> "max",               //乐拼购订单好评次数
         "sum(REFUSE_7DAY_RTN_NUM_L15D)" -> "max",             //拒绝“7天无理由退换货”乐拼购退货订单数
         "sum(SELL_FAKE_NUM_L15D)" -> "max",                   //出售假冒、盗版商品次数
         "sum(COUNTERFEIT_MATERIAL_NUM_L15D)" -> "max",        //假冒材质成份次数
         "sum(SELL_INFERIOR_GOODS_NUM_L15D)" -> "max",         //出售不合格商品次数
         "sum(SHAM_DESCRIBE_NUM_L15D)" -> "max",               //描述不符次数
         "sum(ULTRA_VIRES_NUM_L15D)" -> "max",                 //不当使用他人权利次数
         "sum(BREACH_PROMISE_NUM_L15D)" -> "max",              //违背承诺次数
         "sum(VIRTUAL_HIGH_PRC_NUM_L15D)" -> "max",            //价格虚高次数
         "sum(HARASS_NUM_L15D)" -> "max",                      //恶意骚扰次数
         "sum(INVERSION_OF_PRC_NUM_L15D)" -> "max",            //乐拼购商品价格倒挂次数
         "sum(INVERSION_OF_INV_NUM_L15D)" -> "max",            //乐拼购商品库存倒挂次数
         "sum(SALES_RTN_RT_L15D)" -> "max",                    //乐拼购订单退货率
         "sum(SALES_RTN_NUM_L15D)" -> "max",                   //乐拼购订单退货量
         "sum(DLY_SALES_L15D)" -> "max",                       //乐拼购日均销量
         "sum(COMMISSION_HIGH_GOODS_NUM_L15D)" -> "max",       //高佣商品量
         "sum(ACT_REGISTER_NUM_L30D)" -> "max",                //成功报名乐拼购活动商品数
         "sum(LATE_DLVR_NUM_L30D)" -> "max",                   //乐拼购订单延迟发货次数
         "sum(DLVR_IN_24H_NUM_L30D)" -> "max",                 //乐拼购订单24h内发货次数
         "sum(SHAM_DLVR_NUM_L30D)" -> "max",                   //虚假发货次数
         "sum(VEND_RPLY_RTN_IN_12H_L30D)" -> "max",            //商家12h内响应乐拼购退货订单次数
         "sum(VEND_RPLY_RTN_OVR_48H_L30D)" -> "max",           //商家超48h响应乐拼购退货订单次数
         "sum(VEND_RPLY_COMPLAINT_IN_24H_L30D)" -> "max",      //商家24h内响应乐拼购投诉订单次数
         "sum(VEND_RPLY_COMPLAINT_OVR_24H_L30D)" -> "max",     //商家超24h响应乐拼购投诉订单次数
         "sum(SERV_INTERVENE_RTN_NUM_L30D)" -> "max",          //客服介入乐拼购退货订单量
         "sum(SERV_INTERVENE_RTN_RT_L30D)" -> "max",           //客服介入乐拼购退货订单比率
         "sum(SERV_INTERVENE_COMPLAINT_NUM_L30D)" -> "max",    //客服介入乐拼购投诉订单量
         "sum(SERV_INTERVENE_COMPLAINT_RT_L30D)" -> "max",     //客服介入乐拼购投诉订单比率
         "sum(SERV_RPLY_IN_30S_RT_L30D)" -> "max",             //客服30秒响应率
         "sum(SERV_NEGATIVE_CMT_NUM_L30D)" -> "max",           //客服差评数
         "sum(ORDR_NEGATIVE_CMT_NUM_L30D)" -> "max",           //乐拼购订单差评数
         "sum(NEGATIVE_CMT_RPLY_OVER_24H_L30D)" -> "max",      //乐拼购订单差评超24小时回复数
         "sum(ORDR_GOOD_CMT_NUM_L30D)" -> "max",               //乐拼购订单好评次数
         "sum(REFUSE_7DAY_RTN_NUM_L30D)" -> "max",             //拒绝“7天无理由退换货”乐拼购退货订单数
         "sum(SELL_FAKE_NUM_L30D)" -> "max",                   //出售假冒、盗版商品次数
         "sum(COUNTERFEIT_MATERIAL_NUM_L30D)" -> "max",        //假冒材质成份次数
         "sum(SELL_INFERIOR_GOODS_NUM_L30D)" -> "max",         //出售不合格商品次数
         "sum(SHAM_DESCRIBE_NUM_L30D)" -> "max",               //描述不符次数
         "sum(ULTRA_VIRES_NUM_L30D)" -> "max",                 //不当使用他人权利次数
         "sum(BREACH_PROMISE_NUM_L30D)" -> "max",              //违背承诺次数
         "sum(VIRTUAL_HIGH_PRC_NUM_L30D)" -> "max",            //价格虚高次数
         "sum(HARASS_NUM_L30D)" -> "max",                      //恶意骚扰次数
         "sum(INVERSION_OF_PRC_NUM_L30D)" -> "max",            //乐拼购商品价格倒挂次数
         "sum(INVERSION_OF_INV_NUM_L30D)" -> "max",            //乐拼购商品库存倒挂次数
         "sum(SALES_RTN_RT_L30D)" -> "max",                    //乐拼购订单退货率
         "sum(SALES_RTN_NUM_L30D)" -> "max",                   //乐拼购订单退货量
         "sum(DLY_SALES_L30D)" -> "max",                       //乐拼购日均销量
         "sum(COMMISSION_HIGH_GOODS_NUM_L30D)" -> "max",       //--高佣商品量
         "sum(ACT_REGISTER_NUM_L90D)" -> "max",                //--成功报名乐拼购活动商品数
         "sum(LATE_DLVR_NUM_L90D)" -> "max",                   //--乐拼购订单延迟发货次数
         "sum(DLVR_IN_24H_NUM_L90D)" -> "max",                 // --乐拼购订单24h内发货次数
         "sum(SHAM_DLVR_NUM_L90D)" -> "max",                   //--虚假发货次数
         "sum(VEND_RPLY_RTN_IN_12H_L90D)" -> "max",            // --商家12h内响应乐拼购退货订单次数
         "sum(VEND_RPLY_RTN_OVR_48H_L90D)" -> "max",           // --商家超48h响应乐拼购退货订单次数
         "sum(VEND_RPLY_COMPLAINT_IN_24H_L90D)" -> "max",      //--商家24h内响应乐拼购投诉订单次数
         "sum(VEND_RPLY_COMPLAINT_OVR_24H_L90D)" -> "max",     //--商家超24h响应乐拼购投诉订单次数
         "sum(SERV_INTERVENE_RTN_NUM_L90D)" -> "max",          // --客服介入乐拼购退货订单量
         "sum(SERV_INTERVENE_RTN_RT_L90D)" -> "max",           //--客服介入乐拼购退货订单比率
         "sum(SERV_INTERVENE_COMPLAINT_NUM_L90D)" -> "max",    // --客服介入乐拼购投诉订单量
         "sum(SERV_INTERVENE_COMPLAINT_RT_L90D)" -> "max",     //--客服介入乐拼购投诉订单比率
         "sum(SERV_RPLY_IN_30S_RT_L90D)" -> "max",             //--客服90秒响应率
         "sum(SERV_NEGATIVE_CMT_NUM_L90D)" -> "max",           //  --客服差评数
         "sum(ORDR_NEGATIVE_CMT_NUM_L90D)" -> "max",           //--乐拼购订单差评数
         "sum(NEGATIVE_CMT_RPLY_OVER_24H_L90D)" -> "max",      //--乐拼购订单差评超24小时回复数
         "sum(ORDR_GOOD_CMT_NUM_L90D)" -> "max",               //--乐拼购订单好评次数
         "sum(REFUSE_7DAY_RTN_NUM_L90D)" -> "max",             //--拒绝“7天无理由退换货”乐拼购退货订单数
         "sum(SELL_FAKE_NUM_L90D)" -> "max",                   //--出售假冒、盗版商品次数
         "sum(COUNTERFEIT_MATERIAL_NUM_L90D)" -> "max",        // --假冒材质成份次数
         "sum(SELL_INFERIOR_GOODS_NUM_L90D)" -> "max",         // --出售不合格商品次数
         "sum(SHAM_DESCRIBE_NUM_L90D)" -> "max",               // --描述不符次数
         "sum(ULTRA_VIRES_NUM_L90D)" -> "max",                 //--不当使用他人权利次数
         "sum(BREACH_PROMISE_NUM_L90D)" -> "max",              // --违背承诺次数
         "sum(VIRTUAL_HIGH_PRC_NUM_L90D)" -> "max",            // --价格虚高次数
         "sum(HARASS_NUM_L90D)" -> "max",                      //--恶意骚扰次数
         "sum(INVERSION_OF_PRC_NUM_L90D)" -> "max",            // --乐拼购商品价格倒挂次数
         "sum(INVERSION_OF_INV_NUM_L90D)" -> "max",            // --乐拼购商品库存倒挂次数
         "sum(SALES_RTN_RT_L90D)" -> "max",                    //--乐拼购订单退货率
         "sum(SALES_RTN_NUM_L90D)" -> "max",                   //--乐拼购订单退货量
         "sum(DLY_SALES_L90D)" -> "max",                       // --乐拼购日均销量
         "sum(COMMISSION_HIGH_GOODS_NUM_L90D)" -> "max"        //--高佣商品量
       )
     )
     val row = maxDF.take(1)(0)
     //row.schema.toArray.foreach(item=>println(item.name.substring(4,item.name.length-1)))
     val resDf = statisticsDF.rdd.map(item=>(item(0).toString,normalization(row,item)))
     val now: Date = new Date()
     val dateFormat: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
     val date = dateFormat.format(now).toString
     val storageDf = resDf.map(item=>getDataRow(item._2,item._1.toString,date))
     val fieldstr = ArrayBuffer[String]()
     val fields = row.schema.toArray.map(item=>item.name.substring(8,item.name.length-2)).toArray
     fieldstr += "VEND_CD"
     fieldstr ++= fields
     fieldstr += "ETL_TIME"
     val schema = StructType(fieldstr.toArray.map(fieldName => createField(fieldName)))
     val resDataFrame = sparkSession.createDataFrame(storageDf,schema)
     resDataFrame.show(10)
     creatTable(sparkSession, CsqlStatement)
     insertIntoHiveTable(resDataFrame,"sopdm.TDM_RES_PGS_STATISYICS_KPI_D")
  }

  def getDataRow(datas:Row,cd:String,date:String):Row = {
    val arr = ArrayBuffer[Any]()
    arr += cd
    for(data<-datas.toSeq){
      arr+=data
    }
    arr +=date
    Row.fromSeq(arr.toArray.toSeq)
  }

  def normalization(mean:Row,dis:Row): Row= {
    val arrRes = ArrayBuffer[Double]()
    val meanfield = mean.schema.toArray //按均值顺序组织数据
    val disfield = meanfield.map(item=>item.name.substring(4,item.name.length-1)).toArray //得到目标Row中的field的字符串
    for(field<-disfield){
      val mtmp = mean.getAs("max("+field+")").toString.toDouble
      val ftmp = dis.getAs(field).toString.toDouble
      if(mtmp!=0.0)
        if(InFieldtag(field))
          arrRes += ftmp / mtmp
        else
          arrRes += 1.0-(ftmp / mtmp)
      else
        if(InFieldtag(field))
          arrRes += 0.0
        else
          arrRes += 1.0
    }
    Row.fromSeq(arrRes.toArray.toSeq)
  }


  def InFieldtag(test:String): Boolean ={
    for(str<-Pfieldtag){
      if(str.trim.equals(test.trim))
        true
    }
    false
  }

  def readFromHiveTable(spark: SparkSession, sqlStatement: String): DataFrame = {
    val now: Date = new Date()
    val dateFormat: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
    val date = dateFormat.format(now).toString
    val sql = sqlStatement.replace("DATE(YYYYMMDD)",date)
    //val sql = sqlStatement.replace("DATE(YYYYMMDD)","20170801")
    val df = spark.sql(sql)
    // register a tmp table
    df.createOrReplaceTempView("tmpTable")
    df
  }

  def insertIntoHiveTable(df: DataFrame, tableName: String): Unit = {
    // overwirte
    df.write.mode(SaveMode.Overwrite).insertInto(tableName)
    // append
    //df.write.mode(SaveMode.Append).insertInto(tableName)
  }

  def createField(str:String): StructField ={
    if(str.trim.equals("ETL_TIME") || str.trim.equals("VEND_CD"))
      StructField(str,StringType,false)
    else
      StructField(str,DoubleType,true)
  }

  def creatTable(spark: SparkSession,cstr:String): Unit ={
    spark.sql("use sopdm")
    //spark.sql("DROP TABLE IF EXISTS sopdm.TDM_RES_PGS_STATISYICS_KPI_D")
    spark.sql(cstr)
  }

}
/*var statisticsDF = sparkSession.sql("select vend_cd,sum(ACT_REGISTER_NUM_L15D) " + //--成功报名乐拼购活动商品数
       ",sum(LATE_DLVR_NUM_L15D)"+                   //--乐拼购订单延迟发货次数
       ",sum(DLVR_IN_24H_NUM_L15D)"+                 //乐拼购订单24h内发货次数
       ",sum(SHAM_DLVR_NUM_L15D)"+                   //虚假发货次数
       ",sum(VEND_RPLY_RTN_IN_12H_L15D)"+            //商家12h内响应乐拼购退货订单次数
       ",sum(VEND_RPLY_RTN_OVR_48H_L15D)"+           //商家超48h响应乐拼购退货订单次数
       ",sum(VEND_RPLY_COMPLAINT_IN_24H_L15D)"+      //商家24h内响应乐拼购投诉订单次数
       ",sum(VEND_RPLY_COMPLAINT_OVR_24H_L15D)"+     //商家超24h响应乐拼购投诉订单次数
       ",sum(SERV_INTERVENE_RTN_NUM_L15D)"+          //客服介入乐拼购退货订单量
       ",sum(SERV_INTERVENE_RTN_RT_L15D)"+           //客服介入乐拼购退货订单比率
       ",sum(SERV_INTERVENE_COMPLAINT_NUM_L15D)"+    //客服介入乐拼购投诉订单量
       ",sum(SERV_INTERVENE_COMPLAINT_RT_L15D)"+     //客服介入乐拼购投诉订单比率
       ",sum(SERV_RPLY_IN_30S_RT_L15D)"+             //客服30秒响应率
       ",sum(SERV_NEGATIVE_CMT_NUM_L15D)"+           //客服差评数
       ",sum(ORDR_NEGATIVE_CMT_NUM_L15D)"+           //乐拼购订单差评数
       ",sum(NEGATIVE_CMT_RPLY_OVER_24H_L15D)"+      //乐拼购订单差评超24小时回复数
       ",sum(ORDR_GOOD_CMT_NUM_L15D)"+               //乐拼购订单好评次数
       ",sum(REFUSE_7DAY_RTN_NUM_L15D)"+             //拒绝“7天无理由退换货”乐拼购退货订单数
       ",sum(SELL_FAKE_NUM_L15D)"+                   //出售假冒、盗版商品次数
       ",sum(COUNTERFEIT_MATERIAL_NUM_L15D)"+        //假冒材质成份次数
       ",sum(SELL_INFERIOR_GOODS_NUM_L15D)"+         //出售不合格商品次数
       ",sum(SHAM_DESCRIBE_NUM_L15D)"+               //描述不符次数
       ",sum(ULTRA_VIRES_NUM_L15D)"+                 //不当使用他人权利次数
       ",sum(BREACH_PROMISE_NUM_L15D)"+              //违背承诺次数
       ",sum(VIRTUAL_HIGH_PRC_NUM_L15D)"+            //价格虚高次数
       ",sum(HARASS_NUM_L15D)"+                      //恶意骚扰次数
       ",sum(INVERSION_OF_PRC_NUM_L15D)"+            //乐拼购商品价格倒挂次数
       ",sum(INVERSION_OF_INV_NUM_L15D)"+            //乐拼购商品库存倒挂次数
       ",sum(SALES_RTN_RT_L15D)"+                    //乐拼购订单退货率
       ",sum(SALES_RTN_NUM_L15D)"+                   //乐拼购订单退货量
       ",sum(DLY_SALES_L15D)"+                       //乐拼购日均销量
       ",sum(COMMISSION_HIGH_GOODS_NUM_L15D)"+       //高佣商品量
       ",sum(ACT_REGISTER_NUM_L30D)"+                //成功报名乐拼购活动商品数
       ",sum(LATE_DLVR_NUM_L30D)"+                   //乐拼购订单延迟发货次数
       ",sum(DLVR_IN_24H_NUM_L30D)"+                 //乐拼购订单24h内发货次数
       ",sum(SHAM_DLVR_NUM_L30D)"+                   //虚假发货次数
       ",sum(VEND_RPLY_RTN_IN_12H_L30D)"+            //商家12h内响应乐拼购退货订单次数
       ",sum(VEND_RPLY_RTN_OVR_48H_L30D)"+           //商家超48h响应乐拼购退货订单次数
       ",sum(VEND_RPLY_COMPLAINT_IN_24H_L30D)"+      //商家24h内响应乐拼购投诉订单次数
       ",sum(VEND_RPLY_COMPLAINT_OVR_24H_L30D)"+     //商家超24h响应乐拼购投诉订单次数
       ",sum(SERV_INTERVENE_RTN_NUM_L30D)"+          //客服介入乐拼购退货订单量
       ",sum(SERV_INTERVENE_RTN_RT_L30D)"+           //客服介入乐拼购退货订单比率
       ",sum(SERV_INTERVENE_COMPLAINT_NUM_L30D)"+    //客服介入乐拼购投诉订单量
       ",sum(SERV_INTERVENE_COMPLAINT_RT_L30D)"+     //客服介入乐拼购投诉订单比率
       ",sum(SERV_RPLY_IN_30S_RT_L30D)"+             //客服30秒响应率
       ",sum(SERV_NEGATIVE_CMT_NUM_L30D)"+           //客服差评数
       ",sum(ORDR_NEGATIVE_CMT_NUM_L30D)"+           //乐拼购订单差评数
       ",sum(NEGATIVE_CMT_RPLY_OVER_24H_L30D)"+      //乐拼购订单差评超24小时回复数
       ",sum(ORDR_GOOD_CMT_NUM_L30D)"+               //乐拼购订单好评次数
       ",sum(REFUSE_7DAY_RTN_NUM_L30D)"+             //拒绝“7天无理由退换货”乐拼购退货订单数
       ",sum(SELL_FAKE_NUM_L30D)"+                   //出售假冒、盗版商品次数
       ",sum(COUNTERFEIT_MATERIAL_NUM_L30D)"+        //假冒材质成份次数
       ",sum(SELL_INFERIOR_GOODS_NUM_L30D)"+         //出售不合格商品次数
       ",sum(SHAM_DESCRIBE_NUM_L30D)"+               //描述不符次数
       ",sum(ULTRA_VIRES_NUM_L30D)"+                 //不当使用他人权利次数
       ",sum(BREACH_PROMISE_NUM_L30D)"+              //违背承诺次数
       ",sum(VIRTUAL_HIGH_PRC_NUM_L30D)"+            //价格虚高次数
       ",sum(HARASS_NUM_L30D)"+                      //恶意骚扰次数
       ",sum(INVERSION_OF_PRC_NUM_L30D)"+            //乐拼购商品价格倒挂次数
       ",sum(INVERSION_OF_INV_NUM_L30D)"+            //乐拼购商品库存倒挂次数
       ",sum(SALES_RTN_RT_L30D)"+                    //乐拼购订单退货率
       ",sum(SALES_RTN_NUM_L30D)"+                   //乐拼购订单退货量
       ",sum(DLY_SALES_L30D)"+                       //乐拼购日均销量
       ",sum(COMMISSION_HIGH_GOODS_NUM_L30D)"+       //--高佣商品量
       ",sum(ACT_REGISTER_NUM_L90D)"+                //--成功报名乐拼购活动商品数
       ",sum(LATE_DLVR_NUM_L90D)"+                   //--乐拼购订单延迟发货次数
       ",sum(DLVR_IN_24H_NUM_L90D)"+                 // --乐拼购订单24h内发货次数
       ",sum(SHAM_DLVR_NUM_L90D)"+                   //--虚假发货次数
       ",sum(VEND_RPLY_RTN_IN_12H_L90D)"+            // --商家12h内响应乐拼购退货订单次数
       ",sum(VEND_RPLY_RTN_OVR_48H_L90D)"+           // --商家超48h响应乐拼购退货订单次数
       ",sum(VEND_RPLY_COMPLAINT_IN_24H_L90D)"+      //--商家24h内响应乐拼购投诉订单次数
       ",sum(VEND_RPLY_COMPLAINT_OVR_24H_L90D)"+     //--商家超24h响应乐拼购投诉订单次数
       ",sum(SERV_INTERVENE_RTN_NUM_L90D)"+          // --客服介入乐拼购退货订单量
       ",sum(SERV_INTERVENE_RTN_RT_L90D)"+           //--客服介入乐拼购退货订单比率
       ",sum(SERV_INTERVENE_COMPLAINT_NUM_L90D)"+    // --客服介入乐拼购投诉订单量
       ",sum(SERV_INTERVENE_COMPLAINT_RT_L90D)"+     //--客服介入乐拼购投诉订单比率
       ",sum(SERV_RPLY_IN_30S_RT_L90D)"+             //--客服90秒响应率
       ",sum(SERV_NEGATIVE_CMT_NUM_L90D)"+           //  --客服差评数
       ",sum(ORDR_NEGATIVE_CMT_NUM_L90D)"+           //--乐拼购订单差评数
       ",sum(NEGATIVE_CMT_RPLY_OVER_24H_L90D)"+      //--乐拼购订单差评超24小时回复数
       ",sum(ORDR_GOOD_CMT_NUM_L90D)"+               //--乐拼购订单好评次数
       ",sum(REFUSE_7DAY_RTN_NUM_L90D)"+             //--拒绝“7天无理由退换货”乐拼购退货订单数
       ",sum(SELL_FAKE_NUM_L90D)"+                   //--出售假冒、盗版商品次数
       ",sum(COUNTERFEIT_MATERIAL_NUM_L90D)"+        // --假冒材质成份次数
       ",sum(SELL_INFERIOR_GOODS_NUM_L90D)"+         // --出售不合格商品次数
       ",sum(SHAM_DESCRIBE_NUM_L90D)"+               // --描述不符次数
       ",sum(ULTRA_VIRES_NUM_L90D)"+                 //--不当使用他人权利次数
       ",sum(BREACH_PROMISE_NUM_L90D)"+              // --违背承诺次数
       ",sum(VIRTUAL_HIGH_PRC_NUM_L90D)"+            // --价格虚高次数
       ",sum(HARASS_NUM_L90D)"+                      //--恶意骚扰次数
       ",sum(INVERSION_OF_PRC_NUM_L90D)"+            // --乐拼购商品价格倒挂次数
       ",sum(INVERSION_OF_INV_NUM_L90D)"+            // --乐拼购商品库存倒挂次数
       ",sum(SALES_RTN_RT_L90D)"+                    //--乐拼购订单退货率
       ",sum(SALES_RTN_NUM_L90D)"+                   //--乐拼购订单退货量
       ",sum(DLY_SALES_L90D)"+                       // --乐拼购日均销量
       ",sum( COMMISSION_HIGH_GOODS_NUM_L90D)"+      //--高佣商品量
       " from SOPDM.TDM_RES_PGS_KPI_D where STATIS_DATE <= '20170801' " +
       "AND STATIS_DATE>REGEXP_REPLACE(DATE_SUB(FROM_UNIXTIME(TO_UNIX_TIMESTAMP('20170801','yyyyMMdd'),'yyyy-MM-dd'),30),'-','') " +
       "group by VEND_CD").cache()*/

/* val sumDF = statisticsDF.agg(
       Map(
         "sum(ACT_REGISTER_NUM_L15D)" -> "max",                //成功报名乐拼购活动商品数
         "sum(LATE_DLVR_NUM_L15D)" -> "max",                   //乐拼购订单延迟发货次数
         "sum(DLVR_IN_24H_NUM_L15D)" -> "max",                 //乐拼购订单24h内发货次数
         "sum(SHAM_DLVR_NUM_L15D)" -> "max",                   //虚假发货次数
         "sum(VEND_RPLY_RTN_IN_12H_L15D)" -> "max",            //商家12h内响应乐拼购退货订单次数
         "sum(VEND_RPLY_RTN_OVR_48H_L15D)" -> "max",           //商家超48h响应乐拼购退货订单次数
         "sum(VEND_RPLY_COMPLAINT_IN_24H_L15D)" -> "max",      //商家24h内响应乐拼购投诉订单次数
         "sum(VEND_RPLY_COMPLAINT_OVR_24H_L15D)" -> "max",     //商家超24h响应乐拼购投诉订单次数
         "sum(SERV_INTERVENE_RTN_NUM_L15D)" -> "max",          //客服介入乐拼购退货订单量
         "sum(SERV_INTERVENE_RTN_RT_L15D)" -> "max",           //客服介入乐拼购退货订单比率
         "sum(SERV_INTERVENE_COMPLAINT_NUM_L15D)" -> "max",    //客服介入乐拼购投诉订单量
         "sum(SERV_INTERVENE_COMPLAINT_RT_L15D)" -> "max",     //客服介入乐拼购投诉订单比率
         "sum(SERV_RPLY_IN_30S_RT_L15D)" -> "max",             //客服30秒响应率
         "sum(SERV_NEGATIVE_CMT_NUM_L15D)" -> "max",           //客服差评数
         "sum(ORDR_NEGATIVE_CMT_NUM_L15D)" -> "max",           //乐拼购订单差评数
         "sum(NEGATIVE_CMT_RPLY_OVER_24H_L15D)" -> "max",      //乐拼购订单差评超24小时回复数
         "sum(ORDR_GOOD_CMT_NUM_L15D)" -> "max",               //乐拼购订单好评次数
         "sum(REFUSE_7DAY_RTN_NUM_L15D)" -> "max",             //拒绝“7天无理由退换货”乐拼购退货订单数
         "sum(SELL_FAKE_NUM_L15D)" -> "max",                   //出售假冒、盗版商品次数
         "sum(COUNTERFEIT_MATERIAL_NUM_L15D)" -> "max",        //假冒材质成份次数
         "sum(SELL_INFERIOR_GOODS_NUM_L15D)" -> "max",         //出售不合格商品次数
         "sum(SHAM_DESCRIBE_NUM_L15D)" -> "max",               //描述不符次数
         "sum(ULTRA_VIRES_NUM_L15D)" -> "max",                 //不当使用他人权利次数
         "sum(BREACH_PROMISE_NUM_L15D)" -> "max",              //违背承诺次数
         "sum(VIRTUAL_HIGH_PRC_NUM_L15D)" -> "max",            //价格虚高次数
         "sum(HARASS_NUM_L15D)" -> "max",                      //恶意骚扰次数
         "sum(INVERSION_OF_PRC_NUM_L15D)" -> "max",            //乐拼购商品价格倒挂次数
         "sum(INVERSION_OF_INV_NUM_L15D)" -> "max",            //乐拼购商品库存倒挂次数
         "sum(SALES_RTN_RT_L15D)" -> "max",                    //乐拼购订单退货率
         "sum(SALES_RTN_NUM_L15D)" -> "max",                   //乐拼购订单退货量
         "sum(DLY_SALES_L15D)" -> "max",                       //乐拼购日均销量
         "sum(COMMISSION_HIGH_GOODS_NUM_L15D)" -> "max",       //高佣商品量
         "sum(ACT_REGISTER_NUM_L30D)" -> "max",                //成功报名乐拼购活动商品数
         "sum(LATE_DLVR_NUM_L30D)" -> "max",                   //乐拼购订单延迟发货次数
         "sum(DLVR_IN_24H_NUM_L30D)" -> "max",                 //乐拼购订单24h内发货次数
         "sum(SHAM_DLVR_NUM_L30D)" -> "max",                   //虚假发货次数
         "sum(VEND_RPLY_RTN_IN_12H_L30D)" -> "max",            //商家12h内响应乐拼购退货订单次数
         "sum(VEND_RPLY_RTN_OVR_48H_L30D)" -> "max",           //商家超48h响应乐拼购退货订单次数
         "sum(VEND_RPLY_COMPLAINT_IN_24H_L30D)" -> "max",      //商家24h内响应乐拼购投诉订单次数
         "sum(VEND_RPLY_COMPLAINT_OVR_24H_L30D)" -> "max",     //商家超24h响应乐拼购投诉订单次数
         "sum(SERV_INTERVENE_RTN_NUM_L30D)" -> "max",          //客服介入乐拼购退货订单量
         "sum(SERV_INTERVENE_RTN_RT_L30D)" -> "max",           //客服介入乐拼购退货订单比率
         "sum(SERV_INTERVENE_COMPLAINT_NUM_L30D)" -> "max",    //客服介入乐拼购投诉订单量
         "sum(SERV_INTERVENE_COMPLAINT_RT_L30D)" -> "max",     //客服介入乐拼购投诉订单比率
         "sum(SERV_RPLY_IN_30S_RT_L30D)" -> "max",             //客服30秒响应率
         "sum(SERV_NEGATIVE_CMT_NUM_L30D)" -> "max",           //客服差评数
         "sum(ORDR_NEGATIVE_CMT_NUM_L30D)" -> "max",           //乐拼购订单差评数
         "sum(NEGATIVE_CMT_RPLY_OVER_24H_L30D)" -> "max",      //乐拼购订单差评超24小时回复数
         "sum(ORDR_GOOD_CMT_NUM_L30D)" -> "max",               //乐拼购订单好评次数
         "sum(REFUSE_7DAY_RTN_NUM_L30D)" -> "max",             //拒绝“7天无理由退换货”乐拼购退货订单数
         "sum(SELL_FAKE_NUM_L30D)" -> "max",                   //出售假冒、盗版商品次数
         "sum(COUNTERFEIT_MATERIAL_NUM_L30D)" -> "max",        //假冒材质成份次数
         "sum(SELL_INFERIOR_GOODS_NUM_L30D)" -> "max",         //出售不合格商品次数
         "sum(SHAM_DESCRIBE_NUM_L30D)" -> "max",               //描述不符次数
         "sum(ULTRA_VIRES_NUM_L30D)" -> "max",                 //不当使用他人权利次数
         "sum(BREACH_PROMISE_NUM_L30D)" -> "max",              //违背承诺次数
         "sum(VIRTUAL_HIGH_PRC_NUM_L30D)" -> "max",            //价格虚高次数
         "sum(HARASS_NUM_L30D)" -> "max",                      //恶意骚扰次数
         "sum(INVERSION_OF_PRC_NUM_L30D)" -> "max",            //乐拼购商品价格倒挂次数
         "sum(INVERSION_OF_INV_NUM_L30D)" -> "max",            //乐拼购商品库存倒挂次数
         "sum(SALES_RTN_RT_L30D)" -> "max",                    //乐拼购订单退货率
         "sum(SALES_RTN_NUM_L30D)" -> "max",                   //乐拼购订单退货量
         "sum(DLY_SALES_L30D)" -> "max",                       //乐拼购日均销量
         "sum(COMMISSION_HIGH_GOODS_NUM_L30D)" -> "max",       //--高佣商品量
         "sum(ACT_REGISTER_NUM_L90D)" -> "max",                //--成功报名乐拼购活动商品数
         "sum(LATE_DLVR_NUM_L90D)" -> "max",                   //--乐拼购订单延迟发货次数
         "sum(DLVR_IN_24H_NUM_L90D)" -> "max",                 // --乐拼购订单24h内发货次数
         "sum(SHAM_DLVR_NUM_L90D)" -> "max",                   //--虚假发货次数
         "sum(VEND_RPLY_RTN_IN_12H_L90D)" -> "max",            // --商家12h内响应乐拼购退货订单次数
         "sum(VEND_RPLY_RTN_OVR_48H_L90D)" -> "max",           // --商家超48h响应乐拼购退货订单次数
         "sum(VEND_RPLY_COMPLAINT_IN_24H_L90D)" -> "max",      //--商家24h内响应乐拼购投诉订单次数
         "sum(VEND_RPLY_COMPLAINT_OVR_24H_L90D)" -> "max",     //--商家超24h响应乐拼购投诉订单次数
         "sum(SERV_INTERVENE_RTN_NUM_L90D)" -> "max",          // --客服介入乐拼购退货订单量
         "sum(SERV_INTERVENE_RTN_RT_L90D)" -> "max",           //--客服介入乐拼购退货订单比率
         "sum(SERV_INTERVENE_COMPLAINT_NUM_L90D)" -> "max",    // --客服介入乐拼购投诉订单量
         "sum(SERV_INTERVENE_COMPLAINT_RT_L90D)" -> "max",     //--客服介入乐拼购投诉订单比率
         "sum(SERV_RPLY_IN_30S_RT_L90D)" -> "max",             //--客服90秒响应率
         "sum(SERV_NEGATIVE_CMT_NUM_L90D)" -> "max",           //  --客服差评数
         "sum(ORDR_NEGATIVE_CMT_NUM_L90D)" -> "max",           //--乐拼购订单差评数
         "sum(NEGATIVE_CMT_RPLY_OVER_24H_L90D)" -> "max",      //--乐拼购订单差评超24小时回复数
         "sum(ORDR_GOOD_CMT_NUM_L90D)" -> "max",               //--乐拼购订单好评次数
         "sum(REFUSE_7DAY_RTN_NUM_L90D)" -> "max",             //--拒绝“7天无理由退换货”乐拼购退货订单数
         "sum(SELL_FAKE_NUM_L90D)" -> "max",                   //--出售假冒、盗版商品次数
         "sum(COUNTERFEIT_MATERIAL_NUM_L90D)" -> "max",        // --假冒材质成份次数
         "sum(SELL_INFERIOR_GOODS_NUM_L90D)" -> "max",         // --出售不合格商品次数
         "sum(SHAM_DESCRIBE_NUM_L90D)" -> "max",               // --描述不符次数
         "sum(ULTRA_VIRES_NUM_L90D)" -> "max",                 //--不当使用他人权利次数
         "sum(BREACH_PROMISE_NUM_L90D)" -> "max",              // --违背承诺次数
         "sum(VIRTUAL_HIGH_PRC_NUM_L90D)" -> "max",            // --价格虚高次数
         "sum(HARASS_NUM_L90D)" -> "max",                      //--恶意骚扰次数
         "sum(INVERSION_OF_PRC_NUM_L90D)" -> "max",            // --乐拼购商品价格倒挂次数
         "sum(INVERSION_OF_INV_NUM_L90D)" -> "max",            // --乐拼购商品库存倒挂次数
         "sum(SALES_RTN_RT_L90D)" -> "max",                    //--乐拼购订单退货率
         "sum(SALES_RTN_NUM_L90D)" -> "max",                   //--乐拼购订单退货量
         "sum(DLY_SALES_L90D)" -> "max",                       // --乐拼购日均销量
         "sum(COMMISSION_HIGH_GOODS_NUM_L90D)" -> "max"        //--高佣商品量
      )
     )*/
