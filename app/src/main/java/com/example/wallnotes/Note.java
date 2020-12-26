package com.example.wallnotes;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.Date;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int uid;
    @ColumnInfo(name = "created_at")
    @TypeConverters({Converters.class})
    private Date createdAt;
    @ColumnInfo(name = "title")
    private String title;
    @ColumnInfo(name = "content")
    private String content;
    @ColumnInfo(name = "uriImg")
    private String imgUri = null;
    public int getUid() {
        return uid;
    }
    public void setUid(int uid) {
        this.uid = uid;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getImgUri() {
        return imgUri;
    }
    public void setImgUri(String imgUri) {
        this.imgUri = imgUri;
    }
    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    public Note(String title, String content, String imgUri){
        this.title = title;
        this.content = content;
        this.imgUri = imgUri;
        this.createdAt = null;
    }
}
