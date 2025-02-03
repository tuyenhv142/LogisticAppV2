package com.example.qr_code_project.modal;

import android.os.Parcel;
import android.os.Parcelable;

public class ExportModal implements Parcelable {
    private int id;
    private final String codeEp;
    private final int items;
    private final int totalItem;

    public ExportModal(String codeEp, int items, int totalItem, int id) {
        this.codeEp = codeEp;
        this.items = items;
        this.totalItem = totalItem;
        this.id = id;
    }

    protected ExportModal(Parcel in) {
        id = in.readInt();
        codeEp = in.readString();
        items = in.readInt();
        totalItem = in.readInt();
    }

    public static final Creator<ExportModal> CREATOR = new Creator<ExportModal>() {
        @Override
        public ExportModal createFromParcel(Parcel in) {
            return new ExportModal(in);
        }

        @Override
        public ExportModal[] newArray(int size) {
            return new ExportModal[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodeEp() {
        return codeEp;
    }

    public int getItems() {
        return items;
    }

    public int getTotalItem() {
        return totalItem;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(codeEp);
        dest.writeInt(items);
        dest.writeInt(totalItem);
    }
}
