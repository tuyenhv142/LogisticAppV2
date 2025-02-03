package com.example.qr_code_project.modal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProductModal implements Serializable {
    private int id;
    private String title;
    private int quantity;
    private String code;
    private String location;
    private String image;
    private int area;


    public ProductModal(int id, String title, int quantity, String location, String code, String image) {
        this.id = id;
        this.title = title;
        this.quantity = quantity;
        this.location = location;
        this.code = code;
        this.image = image;
        this.area = -1;
    }


    public int getArea() {
        return area;
    }

    public void setArea(int area) {
        this.area = area;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
