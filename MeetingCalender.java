/* ======== Domain ======== */

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

class User{
    String userid;
    String userName;
    String email;
    Calendar calendar;
}

class Meeting{
    String meetingId;
    String meetingTitle;
    LocalDateTime startTime;
    LocalDateTime endTime;
    String hostId;
    List<String> participantIds;
}

class Calendar{
    String ownerId;
    MeetingRepository meetingRepository;
    List<String> meetingIds;

    boolean canSchedule(LocalDateTime startTime, LocalDateTime endTime){
        for(String id:meetingIds){
            Meeting meeting= meetingRepository.getMeetingById(id);
            LocalDateTime meetStart=meeting.startTime;
            LocalDateTime meetEnd=meeting.endTime;

            if(startTime.isBefore(meetEnd) && meetStart.isBefore(endTime)){
                return false;
            }
        }
        return true;
    }

    void addMeeting(String meetingId){
        meetingIds.add(meetingId);
    }

    void removeMeeting(String meetingId){
        meetingIds.remove(meetingId);
    }
}

/* ======== Repository ======== */

class UserRepository{
    Map<String,User> userDb;

    boolean addUser(User user){
        userDb.put(user.userid, user);
        return true;
    }

    User getUserById(String userId){
        return userDb.get(userId);
    }
}

class MeetingRepository{
    Map<String,Meeting> meetingDb;

    boolean addMeeting(Meeting meeting){
        meetingDb.put(meeting.meetingId, meeting);
        return true;
    }

    Meeting getMeetingById(String meetingId){
        return meetingDb.get(meetingId);
    }

    boolean removeMeeting(String meetingId){
        meetingDb.remove(meetingId);
        return true;
    }
}

/* ======== Service ======== */

class MeetingService{
    UserRepository userRepository;
    MeetingRepository meetingRepository;

    Meeting scheduleMeeting(String meetingTitle, LocalDateTime start, LocalDateTime end, List<String> participantIds, User host){
        if(!host.calendar.canSchedule(start,end)){
            throw new IllegalArgumentException("Host Calendar not free");
        }
        List<String> canParticipate=new ArrayList<>();
        for(String participantId:participantIds){
            User user=userRepository.getUserById(participantId);
            if(user.calendar.canSchedule(start,end)){
                canParticipate.add(participantId);
            }
        }

        Meeting meeting=new Meeting();
        meeting.meetingId= UUID.randomUUID().toString();
        meeting.meetingTitle=meetingTitle;
        meeting.startTime=start;
        meeting.endTime=end;
        meeting.participantIds=canParticipate;
        meetingRepository.addMeeting(meeting);

        for(String id:canParticipate){
            User user=userRepository.getUserById(id);
            user.calendar.addMeeting(meeting.meetingId);
        }
        host.calendar.addMeeting(meeting.meetingId);
        return meeting;
    }

    boolean cancelMeeting(String userId, String meetingId){
        Meeting meeting=meetingRepository.getMeetingById(meetingId);
        if(meeting==null) return false;

        if(!meeting.hostId.equals(userId)){
            throw new IllegalStateException("Only host can cancel meeting");
        }

        for(String id:meeting.participantIds){
            User user=userRepository.getUserById(id);
            if(user!=null) user.calendar.removeMeeting(meetingId);
        }
        userRepository.getUserById(userId).calendar.removeMeeting(meetingId);
        return meetingRepository.removeMeeting(meetingId);
    }
}

/* ======== Controller ======== */

class MeetingController{
    MeetingService meetingService;

    Meeting scheduleMeeting(String meetingTitle, LocalDateTime start, LocalDateTime end, List<String> participantIds, User host){
        return meetingService.scheduleMeeting(meetingTitle,start,end,participantIds,host);
    }

    boolean cancelMeeting(String userId, String meetingId){
        return meetingService.cancelMeeting(userId,meetingId);
    }

/* ======== Client Code ======== */


public class GoogleCalendar1 {
    public static void main(String[] args) {
        User u1=new User();
        User u2=new User();
        MeetingController controller=new MeetingController();
        Meeting m1=controller.scheduleMeeting("Project Discussion",
                LocalDateTime.of(2025,9,29,14,00),
                LocalDateTime.of(2025,9,29,15,00),
                Arrays.asList(u2.userid),u1);

        controller.cancelMeeting(u1.userid,m1.meetingId);
    }
}
