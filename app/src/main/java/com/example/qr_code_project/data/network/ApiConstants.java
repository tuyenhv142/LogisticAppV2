package com.example.qr_code_project.data.network;

public class ApiConstants {
//    public static final String BASE_URL = "https://172.20.10.3:7060/api/";

    public static final String BASE_URL = "http://52.184.83.97:5000/api/";

//    public static final String ACCOUNT = BASE_URL+"Account/";
    public static final String INBOUND = BASE_URL+"Inbound/";
    public static final String DELIVERY = BASE_URL+"Outbound/";
    public static final String PRODUCT = BASE_URL+"Product/";
    public static final String SWAP = BASE_URL+"Plan/";
    public static final String STATUS = BASE_URL+"Status/";

//    public static final String ACCOUNT_PROFILE = ACCOUNT+"Showrofile";
//    public static final String ACCOUNT_LOGIN = ACCOUNT+"LoginData";
//    public static final String ACCOUNT_LOAD_OTP = ACCOUNT+"LoadOTP";
//    public static final String ACCOUNT_CHECK_CODE = ACCOUNT+"checkCode";
//    public static final String ACCOUNT_UPDATE_PASSWORD = ACCOUNT+"UpdatePassworData";
    public static final String INBOUND_SUBMIT = INBOUND+"Update";
//    public static final String PACKAGE_SUBMIT = DELIVERY+"UpdatePack";
    public static final String DELIVERY_UPDATE = DELIVERY+"UpdateCode";
    public static final String SWAP_LOCATION = SWAP+"FindAll";
//    public static final String SWAP_LOCATION_CLAIM = SWAP+"FindConfirmationByAccount?page=1&pageSize=20";
//    public static final String SWAP_LOCATION_CONFIRM = SWAP+"UpdatePlanConfirmation";
    public static final String SWAP_LOCATION_SUBMIT = SWAP+"Update";
    public static final String TOKEN_ADD = BASE_URL+"UserTokenApp/Add";

    public static String getFindOneCodeInboundUrl(String code){
        return INBOUND + "FindCode?code=" + code;
    }

    public static String getFindOneCodeDeliveryUrl(String code){
        return DELIVERY + "FindCode?code=" + code;
    }

    public static String UpdateOutboundIsPack(String code){
        return DELIVERY + "UpdatePack?code=" + code;
    }

    public static String getFindOneCodeProductUrl(String code){
        return PRODUCT + "FindOneCode?id=" + code;
    }

    public static String getFindCodeLocationProductUrl(String code){
        return PRODUCT + "findOneCode?name=" + code;
    }

    public static String getFindOneCodeSwapLocationUrl(int id){
        return SWAP + "FindOne?id=" + id;
    }

    public static String getFindByPlanUrl(int id){
        return STATUS + "FindByPlan?id=" + id;
    }

}
