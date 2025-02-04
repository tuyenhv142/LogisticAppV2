package com.example.qr_code_project.network;

public class ApiConstants {
//    public static final String BASE_URL = "https://172.20.10.2:7142/api/";
    public static final String BASE_URL = "https://192.168.37.182:7142/api/";

    public static final String LOGIN = BASE_URL+"Account/LoginData";
    public static final String INBOUND = BASE_URL+"Importform/";
    public static final String INBOUND_SUBMIT = INBOUND+"UpdateCode";
    public static final String DELIVERY = BASE_URL+"Deliverynote/";
    public static final String PACKAGE_SUBMIT = DELIVERY+"CheckPack";
    public static final String UPDATE_DELIVERY_URL = DELIVERY+"UpdateActionLocation";
    public static final String PRODUCT = BASE_URL+"Product/";
    public static final String SWAP = BASE_URL+"Plan/";
    public static final String SWAP_LOCATION = SWAP+"FindAll?page=1&pageSize=20";
    public static final String CONFIRM_SWAP_LOCATION = SWAP+"UpdatePlanConfirmation";
    public static final String STATUS = BASE_URL+"Status/";
    public static final String SUBMIT_SWAP_LOCATION = STATUS+"UpdateStatus";
    public static final String ADD_TOKEN = BASE_URL+"UserTokenApp/AddToken";

    public static String getFindOneCodeInboundUrl(String code){
        return INBOUND + "FindOneCode?code=" + code;
    }

    public static String getFindOneCodeDeliveryUrl(String code){
        return DELIVERY + "FindOneCode?code=" + code;
    }

    public static String getFindOneCodeProductUrl(String code){
        return PRODUCT + "FindOneCode?id=" + code;
    }

    public static String getFindCodeLocationProductUrl(String code){
        return PRODUCT + "FindCodeLocation?code=" + code;
    }

    public static String getFindOneCodeSwapLocationUrl(int id){
        return SWAP + "FindOne?id=" + id;
    }

    public static String getFindByPlanUrl(int id){
        return STATUS + "FindByPlan?id=" + id;
    }

}
