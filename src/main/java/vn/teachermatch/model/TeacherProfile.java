package vn.teachermatch.model;

public class TeacherProfile {
    private long id;
    private String name, email, nationality, location, experience, qualification, status, videoUrl, cvUrl;
    public TeacherProfile(long id, String name, String email, String nationality, String location, String experience, String qualification, String status, String videoUrl, String cvUrl) {
        this.id=id; this.name=name; this.email=email; this.nationality=nationality; this.location=location; this.experience=experience; this.qualification=qualification; this.status=status; this.videoUrl=videoUrl; this.cvUrl=cvUrl;
    }
    public long getId(){return id;} public String getName(){return name;} public String getEmail(){return email;}
    public String getNationality(){return nationality;} public String getLocation(){return location;} public String getExperience(){return experience;}
    public String getQualification(){return qualification;} public String getStatus(){return status;} public String getVideoUrl(){return videoUrl;} public String getCvUrl(){return cvUrl;}
    public void setName(String v){name=v;} public void setEmail(String v){email=v;} public void setNationality(String v){nationality=v;}
    public void setLocation(String v){location=v;} public void setExperience(String v){experience=v;} public void setQualification(String v){qualification=v;}
    public void setStatus(String v){status=v;} public void setVideoUrl(String v){videoUrl=v;} public void setCvUrl(String v){cvUrl=v;}
}
