package com.example.qr_code_project.modal;

public class AccountModal {
    private int id;
    private String username;
    private String image;
    private String phone;
    private String email;
    private String token;
    private String role;

    public AccountModal(int id, String token, String email, String phone
            , String image, String username, String role) {
        this.id = id;
        this.token = token;
        this.email = email;
        this.phone = phone;
        this.image = image;
        this.username = username;
        this.role = role;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

}

