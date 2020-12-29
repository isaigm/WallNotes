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
    private String imgUri;
    @ColumnInfo(name = "is_going_to_be_deleted")
    private boolean isGoingToBeDeleted;
    @ColumnInfo(name = "remind_date")
    @TypeConverters({Converters.class})
    private Date remindDate;
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
    public boolean isGoingToBeDeleted() {
        return isGoingToBeDeleted;
    }
    public void setGoingToBeDeleted(boolean goingToBeDeleted) {
        isGoingToBeDeleted = goingToBeDeleted;
    }
    public Date getRemindDate() {
        return remindDate;
    }
    public void setRemindDate(Date remindDate) {
        this.remindDate = remindDate;
    }
    public Note(String title, String content, String imgUri){
        this.title = title;
        this.content = content;
        this.imgUri = imgUri;
        this.createdAt = null;
        this.isGoingToBeDeleted = false;
        this.remindDate = null;
    }
}
