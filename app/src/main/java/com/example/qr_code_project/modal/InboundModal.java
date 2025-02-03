package com.example.qr_code_project.modal;

public class InboundModal {
    private int id;
    private String title;
    private String code;
    private int totalQuantity;
    private int totalProduct;
    private ProductModal productModal;

    public InboundModal() {
    }

    public InboundModal(int id, String title, int totalQuantity, String code, int totalProduct, ProductModal productModal) {
        this.id = id;
        this.title = title;
        this.totalQuantity = totalQuantity;
        this.code = code;
        this.totalProduct = totalProduct;
        this.productModal = productModal;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getTotalProduct() {
        return totalProduct;
    }

    public void setTotalProduct(int totalProduct) {
        this.totalProduct = totalProduct;
    }

    public ProductModal getProductModal() {
        return productModal;
    }

    public void setProductModal(ProductModal productModal) {
        this.productModal = productModal;
    }
}
