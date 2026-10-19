package vn.teachermatch.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import vn.teachermatch.model.*;
import vn.teachermatch.service.PortalStore;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Controller
public class PortalController {
    private final PortalStore store;
    public PortalController(PortalStore store){this.store=store;}
    private AppUser current(HttpSession s){return (AppUser)s.getAttribute("user");}
    private String role(HttpSession s){AppUser u=current(s);return u==null?"":u.getRole();}
    private String username(HttpSession s){AppUser u=current(s);return u==null?"":u.getUsername();}
    private String dashboardFor(String role){return switch(role){case "ADMIN"->"redirect:/admin";case "STAFF"->"redirect:/staff";case "PARTNER"->"redirect:/partner";case "TEACHER"->"redirect:/teacher";default->"redirect:/login";};}
    @GetMapping("/") public String root(HttpSession s){return current(s)==null?"redirect:/login":dashboardFor(role(s));}
    @GetMapping("/login") public String login(){return "login";}
    @PostMapping("/login") public String doLogin(@RequestParam String username,@RequestParam String password,Model model,HttpSession session){
        AppUser u=store.authenticate(username,password);
        if(u==null){model.addAttribute("error","Tên đăng nhập hoặc mật khẩu chưa đúng.");return "login";}
        session.setAttribute("user",u);store.log(username,"Logged in");return dashboardFor(u.getRole());
    }
    @GetMapping("/logout") public String logout(HttpSession s){AppUser u=current(s);if(u!=null)store.log(u.getUsername(),"Logged out");s.invalidate();return "redirect:/login";}
    @ModelAttribute public void common(Model m,HttpSession s){AppUser u=current(s);m.addAttribute("user",u);m.addAttribute("role",role(s));}
    @GetMapping("/admin") public String admin(Model m,HttpSession s){if(!"ADMIN".equals(role(s)))return "redirect:/login";m.addAttribute("users",store.users());m.addAttribute("logs",store.logs());m.addAttribute("teachers",store.teachers());return "admin";}
    @PostMapping("/admin/users") public String addUser(@RequestParam String username,@RequestParam String password,@RequestParam String role,HttpSession s){
        if(!"ADMIN".equals(role(s)))return "redirect:/login";
        if(!username.isBlank()&&!password.isBlank()&&!store.userExists(username))store.addUser(username,password,role);
        return "redirect:/admin";
    }
    @GetMapping("/staff") public String staff(Model m,HttpSession s){if(!"STAFF".equals(role(s))&&! "ADMIN".equals(role(s)))return "redirect:/login";m.addAttribute("teachers",store.teachers());m.addAttribute("interviews",store.interviews());return "staff";}
    @PostMapping("/staff/status/{id}") public String status(@PathVariable long id,@RequestParam String status,HttpSession s){if(!"STAFF".equals(role(s))&&! "ADMIN".equals(role(s)))return "redirect:/login";store.status(id,status);return "redirect:/staff";}
    @PostMapping("/staff/grant/{id}") public String grant(@PathVariable long id,@RequestParam(defaultValue="partner") String partner,HttpSession s){if(!"STAFF".equals(role(s))&&! "ADMIN".equals(role(s)))return "redirect:/login";store.grant(partner,id);return "redirect:/staff";}
    @PostMapping("/staff/interview/{id}") public String staffInterview(@PathVariable long id,@RequestParam(required=false) String status,@RequestParam(required=false) String result,@RequestParam(required=false) String feedback,HttpSession s){if(!"STAFF".equals(role(s))&&! "ADMIN".equals(role(s)))return "redirect:/login";store.updateInterview(id,status,result,feedback);return "redirect:/staff#interviews";}
    @GetMapping("/partner") public String partner(Model m,HttpSession s){if(!"PARTNER".equals(role(s)))return "redirect:/login";m.addAttribute("teachers",store.visibleTeachers(username(s),role(s)));m.addAttribute("interviews",store.visibleInterviews(role(s),username(s)));return "partner";}
    @PostMapping("/partner/interview/{id}") public String request(@PathVariable long id,@RequestParam String dateTime,HttpSession s){if(!"PARTNER".equals(role(s)))return "redirect:/login";store.requestInterview(id,"Bright English Center",dateTime);return "redirect:/partner#interviews";}
    @PostMapping("/partner/feedback/{id}") public String feedback(@PathVariable long id,@RequestParam String result,@RequestParam String feedback,HttpSession s){if(!"PARTNER".equals(role(s)))return "redirect:/login";store.updateInterview(id,null,result,feedback);return "redirect:/partner#interviews";}
    @GetMapping("/teacher") public String teacher(Model m,HttpSession s){if(!"TEACHER".equals(role(s)))return "redirect:/login";m.addAttribute("teacher",store.teacher(1));m.addAttribute("interviews",store.visibleInterviews(role(s),username(s)));return "teacher";}
    @PostMapping("/teacher/profile") public String profile(@RequestParam String name,@RequestParam String email,@RequestParam String nationality,@RequestParam String location,@RequestParam String experience,@RequestParam String qualification,HttpSession s){
        if(!"TEACHER".equals(role(s)))return "redirect:/login";TeacherProfile t=store.teacher(1);t.setName(name);t.setEmail(email);t.setNationality(nationality);t.setLocation(location);t.setExperience(experience);t.setQualification(qualification);store.log(username(s),"Updated teacher profile");return "redirect:/teacher";
    }
    @PostMapping("/teacher/upload") public String upload(@RequestParam("file") MultipartFile file,@RequestParam String documentType,HttpSession s) throws IOException {
        if(!"TEACHER".equals(role(s)))return "redirect:/login";
        if(!file.isEmpty()){Path dir=Paths.get("uploads");Files.createDirectories(dir);String safe=UUID.randomUUID()+"_"+Path.of(file.getOriginalFilename()).getFileName();Path dest=dir.resolve(safe);Files.copy(file.getInputStream(),dest,StandardCopyOption.REPLACE_EXISTING);TeacherProfile t=store.teacher(1);if("Video".equalsIgnoreCase(documentType))t.setVideoUrl("/uploads/"+safe);else t.setCvUrl("/uploads/"+safe);store.log(username(s),"Uploaded "+documentType);}
        return "redirect:/teacher";
    }
    @GetMapping("/uploads/{filename:.+}")
    @ResponseBody public ResponseEntity<Resource> uploadedFile(@PathVariable String filename) throws IOException {
        Path path=Paths.get("uploads").resolve(filename).normalize();
        if(!path.startsWith(Paths.get("uploads").toAbsolutePath().normalize()) && path.isAbsolute()) return ResponseEntity.notFound().build();
        Path absolute=path.toAbsolutePath().normalize();
        if(!absolute.startsWith(Paths.get("uploads").toAbsolutePath().normalize()) || !Files.exists(absolute)) return ResponseEntity.notFound().build();
        Resource resource=new UrlResource(absolute.toUri());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }

    @PostMapping("/teacher/interview/{id}") public String teacherInterview(@PathVariable long id,@RequestParam String status,HttpSession s){if(!"TEACHER".equals(role(s)))return "redirect:/login";store.updateInterview(id,status,null,null);return "redirect:/teacher";}
}
