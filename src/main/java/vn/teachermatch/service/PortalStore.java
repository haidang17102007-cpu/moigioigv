package vn.teachermatch.service;

import org.springframework.stereotype.Service;
import vn.teachermatch.model.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PortalStore {
    private final Map<String,AppUser> users = new LinkedHashMap<>();
    private final List<TeacherProfile> teachers = new ArrayList<>();
    private final List<Interview> interviews = new ArrayList<>();
    private final List<String[]> logs = new ArrayList<>();
    private final Set<String> granted = new HashSet<>();
    private final AtomicLong interviewIds = new AtomicLong(3);
    public PortalStore() {
        users.put("admin",new AppUser("admin","admin123","ADMIN","System Administrator"));
        users.put("staff",new AppUser("staff","staff123","STAFF","Matching Staff"));
        users.put("partner",new AppUser("partner","partner123","PARTNER","Bright English Center"));
        users.put("teacher",new AppUser("teacher","teacher123","TEACHER","Emily Martin"));
        teachers.add(new TeacherProfile(1,"Emily Martin","emily.martin@example.com","American","Ho Chi Minh City","5 years","TEFL, Bachelor's degree","READY","https://example.com/emily-demo","https://example.com/emily-cv"));
        teachers.add(new TeacherProfile(2,"Daniel Walker","daniel.walker@example.com","British","Hanoi","3 years","CELTA","READY","https://example.com/daniel-demo","https://example.com/daniel-cv"));
        teachers.add(new TeacherProfile(3,"Sarah Thompson","sarah.thompson@example.com","Australian","Da Nang","7 years","TESOL, Master's degree","PENDING","https://example.com/sarah-demo","https://example.com/sarah-cv"));
        granted.add("partner:1"); granted.add("partner:2");
        interviews.add(new Interview(1,1,"Emily Martin","Bright English Center","2026-10-20T10:00","PENDING","Not reviewed","",""));
        interviews.add(new Interview(2,2,"Daniel Walker","Bright English Center","2026-10-21T14:30","CONFIRMED","Not reviewed","","https://meet.google.com/"));
        log("SYSTEM","Seed data loaded");
    }
    public Collection<AppUser> users(){return users.values();}
    public boolean userExists(String username){return users.containsKey(username);}
    public AppUser authenticate(String username,String password){AppUser u=users.get(username); return u!=null && u.getPassword().equals(password)?u:null;}
    public void addUser(String username,String password,String role){users.put(username,new AppUser(username,password,role,username));log("ADMIN","Created account "+username);}
    public List<TeacherProfile> teachers(){return teachers;}
    public TeacherProfile teacher(long id){return teachers.stream().filter(t->t.getId()==id).findFirst().orElse(null);}
    public List<TeacherProfile> visibleTeachers(String username,String role){
        if("STAFF".equals(role)||"ADMIN".equals(role)) return teachers;
        if("PARTNER".equals(role)) return teachers.stream().filter(t->granted.contains(username+":"+t.getId())).toList();
        return teachers.stream().filter(t->t.getId()==1).toList();
    }
    public void grant(String username,long id){granted.add(username+":"+id);log("STAFF","Granted teacher "+id+" to "+username);}
    public void status(long id,String status){TeacherProfile t=teacher(id);if(t!=null){t.setStatus(status);log("STAFF","Updated "+t.getName()+" status to "+status);}}
    public List<Interview> interviews(){return interviews;}
    public List<Interview> visibleInterviews(String role,String username){
        if("STAFF".equals(role)||"ADMIN".equals(role)) return interviews;
        if("TEACHER".equals(role)) return interviews.stream().filter(i->i.getTeacherId()==1).toList();
        return interviews.stream().filter(i->i.getPartner().equalsIgnoreCase("Bright English Center")).toList();
    }
    public void requestInterview(long teacherId,String partner,String dateTime){
        TeacherProfile t=teacher(teacherId); if(t!=null){interviews.add(new Interview(interviewIds.incrementAndGet(),teacherId,t.getName(),partner,dateTime,"PENDING","Not reviewed","",""));log("PARTNER","Interview requested for "+t.getName());}
    }
    public void updateInterview(long id,String status,String result,String feedback){
        interviews.stream().filter(i->i.getId()==id).findFirst().ifPresent(i->{if(status!=null&&!status.isBlank())i.setStatus(status);if(result!=null&&!result.isBlank())i.setResult(result);if(feedback!=null)i.setFeedback(feedback);if("CONFIRMED".equals(i.getStatus()))i.setMeetingUrl("https://meet.google.com/new");log("INTERVIEW","Updated interview #"+id);});
    }
    public List<String[]> logs(){return logs;}
    public void log(String actor,String action){logs.add(0,new String[]{java.time.LocalDateTime.now().toString().replace('T',' '),actor,action});}
}
