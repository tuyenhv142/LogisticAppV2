package com.example.qr_code_project.data.modal;

public class SwapModal {
    private int id;
    private String title;
    private String locationOldCode;
    private String locationNewCode;
    private String warehouseOld;
    private String areaOld;
    private String floorOld;
    private String warehouse;
    private String area;
    private String floor;
    private String shelfOld;
    private String shelfNew;

    public SwapModal(String floor, String area, String warehouse
            , String floorOld
            , String areaOld, String warehouseOld, String locationNewCode
            , String locationOldCode, String title, int id,String shelfOld,String shelfNew) {
        this.floor = floor;
        this.area = area;
        this.warehouse = warehouse;
        this.floorOld = floorOld;
        this.areaOld = areaOld;
        this.warehouseOld = warehouseOld;
        this.locationNewCode = locationNewCode;
        this.locationOldCode = locationOldCode;
        this.title = title;
        this.id = id;
        this.shelfOld = shelfOld;
        this.shelfNew = shelfNew;
    }

    public String getShelfOld() {
        return shelfOld;
    }

    public void setShelfOld(String shelfOld) {
        this.shelfOld = shelfOld;
    }

    public String getShelfNew() {
        return shelfNew;
    }

    public void setShelfNew(String shelfNew) {
        this.shelfNew = shelfNew;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getFloorOld() {
        return floorOld;
    }

    public void setFloorOld(String floorOld) {
        this.floorOld = floorOld;
    }

    public String getAreaOld() {
        return areaOld;
    }

    public void setAreaOld(String areaOld) {
        this.areaOld = areaOld;
    }

    public String getWarehouseOld() {
        return warehouseOld;
    }

    public void setWarehouseOld(String warehouseOld) {
        this.warehouseOld = warehouseOld;
    }

    public String getLocationNewCode() {
        return locationNewCode;
    }

    public void setLocationNewCode(String locationNewCode) {
        this.locationNewCode = locationNewCode;
    }

    public String getLocationOldCode() {
        return locationOldCode;
    }

    public void setLocationOldCode(String locationOldCode) {
        this.locationOldCode = locationOldCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
