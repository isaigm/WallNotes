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
    @ColumnInfo(name = "audio")
    private String audio;
    @ColumnInfo(name = "location")
    private String location;
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
    public boolean isGoingToBeDeleted() {
        return isGoingToBeDeleted;
    }
    public void setGoingToBeDeleted(boolean goingToBeDeleted) {
        isGoingToBeDeleted = goingToBeDeleted;
    }
    public String getAudio() {
        return audio;
    }
    public void setAudio(String audio) {
        this.audio = audio;
    }
    public Date getRemindDate() {
        return remindDate;
    }
    public void setRemindDate(Date remindDate) {
        this.remindDate = remindDate;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public Note(String title, String content, String imgUri){
        this.title = title;
        this.content = content;
        this.imgUri = imgUri;
        this.isGoingToBeDeleted = false;
        this.remindDate = null;
        this.location = null;
    }
}
